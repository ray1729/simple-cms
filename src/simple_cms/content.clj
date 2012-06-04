(ns simple-cms.content
  (:require [net.cgrand.enlive-html :as html]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clj-time.format :as tf]))

(defn has-html-suffix?
  "Returns true if `file` has the suffix .html, otherwise false"
  [file]
  (re-find #"\.html$" (str file)))

(defn strip-html-suffix
  "Remove the .html suffix from a filename"
  [file]
  (string/replace (str file) #"\.html$" ""))

(defn relative-path
  "Returns the path of `path` relative to `base`"
  [base path]
  (let [match (re-pattern (str "^\\Q" base "\\E/"))]
    (string/replace-first path match "")))

(defn extract-html-metadata
  "Returns a map of name/content extracted from the HTML <meta /> tags"
  [html]
  (into {} (map (juxt (comp keyword :name) :content)
                (map :attrs (html/select html [:head :meta])))))

(def parse-date
  "Parse a date string, returns a DateTime object"
  (partial tf/parse (tf/formatters :date)))

(defn parse-tags
  "Parse a (comma-delimited) tag string, returns a set of tags"
  [s]
  (set (string/split s #",\s*")))

(defn get-file-metadata
  "Parses the metadata in the HTML `file`, returns a map"
  [base file]
  (let [r (html/html-resource file)
        m (extract-html-metadata r)
        id (strip-html-suffix (relative-path base file))]
    (assoc m
      :file     file
      :id       id      
      :title    (html/text (first (html/select r [:head :title])))
      :pubdate  (when-let [date-str (:pubdate m)] (parse-date date-str))
      :tags     (when-let [tags-str (:tags m)] (parse-tags tags-str))
      :teaser   (html/select r [:.teaser]))))

(defn get-site-content-metadata
  "Walks the filesystem and constructs a hash-map of metadata for each
   HTML file in `site-content-directory`"
  [site-content-dir]
  (map (partial get-file-metadata site-content-dir)
       (filter has-html-suffix? (file-seq (io/file site-content-dir)))))

(def metadata (ref {}))
(def tag-index (ref {}))
(def published-items (ref []))

(defn build-tag-index
  "Builds a map keyed on tag, whose values are vectors of keys of
  items with that tag"
  [meta]
  (reduce (fn [accum [k v]] (assoc accum k (conj (get accum k []) v)))
          {}
          (for [m meta t (:tags m)] [t (:id m)])))

(defn build-published-items
  "Builds a sorted list of published items, with the most recent items
  first"
  [meta]
  (reverse (sort-by :pubdate (filter :pubdate meta))))

(defn update-site-metadata!
  [site-content-dir]
  (let [data (get-site-content-metadata site-content-dir)]
    (dosync
     (ref-set metadata (zipmap (map :id data) data))
     (ref-set published-items (build-published-items data))
     (ref-set tag-index (build-tag-index data))))
  nil)

(defn get-latest-items
  "Returns the latest published items a page at a time, optionally
  filtering for `tag`"
  [& {:keys [tag page pagesize] :or {page 1 pagesize 10}}]
  (let [wanted? (if tag (fn [item] (contains? (:tags item) tag)) identity)]
    (take pagesize (drop (* pagesize (dec page)) (filter wanted? @published-items)))))

(defn get-item-count
  [& {:keys [tag]}]  
  (if tag
    (count (get @tag-index tag))
    (count @published-items)))

(defn get-item-content
  [id]
  (when-let [item (get @metadata id)]
    (html/html-resource (:file item))))

(defn get-item-meta
  [id]
  (get @metadata id))

(defn get-tags
  []
  (map (fn [[k v]] [k (count v)]) @tag-index))
