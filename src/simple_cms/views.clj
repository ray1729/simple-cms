(ns simple-cms.views
  (:use [simple-cms.content :only (get-latest-items get-item-count get-item-meta get-item-content get-tags get-code-snippet)]
        [simple-cms.properties :only (get-property)])
  (:require [net.cgrand.enlive-html :as html]
            [clj-time.core :as ct]
            [clj-time.format :as tf]))

(defn feed-url
  [tag]
  (if tag
    (str (get-property :base-url) "feed/" tag)
    (str (get-property :base-url) "feed")))

(defn article-url
  "Returns the url for `article`"
  [article]
  (str (get-property :base-url) "content/" (:id article)))

(defn tag-url
  "Returns the url for `tag`"
  [tag]
  (str (get-property :base-url) "tags/" tag))

(defn newer-items-url
  [tag page num-items]
  (when (> page 1)
    (str (get-property :base-url)
         (when tag (str "tags/" tag))
         "?page=" (dec page))))

(defn older-items-url
  [tag page num-items]
  (let [pagesize (get-property :page-size)]
    (when (> num-items (* page pagesize))
      (str (get-property :base-url)
           (when tag (str "tags/" tag))
           "?page=" (inc page)))))

(defn tag-class
  "Computes the class for a tag with count `n` (log2 scale)"
  [n]
  (letfn [(pow [n m] (reduce * (repeat m n)))]
    (loop [m 5]
      (if (>= n (pow 2 m)) (str "tag" (inc m)) (recur (dec m))))))

(defn format-date
  "Formats a date as a string, e.g. 'Mon 1 Dec 2010'"
  [d]
  (tf/unparse (tf/formatter "E d MMMM yyyy") d))

(defn format-feed-date
  "Formats a date for use in an atom feed, e.g. '2012-156T19:51:57Z'"
  [d]
  (tf/unparse (tf/formatters :date-time-no-ms) d))

(def layout-tmpl (html/html-resource "templates/layout.html"))

(def feed-tmpl (html/xml-resource "templates/feed.xml"))

(def code-snippet-tmpl (html/html-resource "templates/code-snippet.html"))

(def brush-for
  {"pl" "perl", "clj" "clojure", "sh" "bash", "sql" "sql"})

(html/defsnippet code-snippet code-snippet-tmpl [:pre] [[code suffix]]
  [:pre] (html/do->
          (html/set-attr :class (str "brush: " (or (brush-for suffix) "plain")))
          (html/content code)))

(defn expand-code-snippets
  [content]
  (html/at content [:snippet] (fn [s] (code-snippet (get-code-snippet (:src (:attrs s)))))))

(html/defsnippet atom-category feed-tmpl [:feed :> :entry :> :category]
  [tag]
  [:category] (html/content tag))

(html/defsnippet atom-entry feed-tmpl [:feed :> :entry] [item]
  [:title] (html/content (:title item))
  [:link] (html/set-attr :href (article-url item))
  [:id] (html/content (article-url item))
  [:category] (when-let [tags (:tags item)] (html/clone-for [tag tags] (html/substitute (atom-category tag))))
  [:updated] (html/content (format-feed-date (:pubdate item)))
  [:content] (html/content (html/emit* (expand-code-snippets
                                        (html/unwrap (first (html/select (get-item-content (:id item)) [:body])))))))

(html/deftemplate atom-feed feed-tmpl [& {:keys [title tag url items updated]}]
  [:feed :> :title] (if title (html/content title) identity)
  [:feed :> [:link (html/attr= :rel "self")]] (html/set-attr :href url)
  [:feed :> :id] (html/content url)
  [:feed :> :updated] (html/content (format-feed-date updated))
  [:feed :> :category] (when tag (html/content tag))
  [:feed :> :entry] (html/clone-for [item items] (html/substitute (atom-entry item))))

(html/defsnippet tag layout-tmpl [:#tagcloud :ul :> html/first-child] [[tag-name tag-count]]
  [:li] (html/set-attr :class (tag-class tag-count))
  [:li :a] (html/do->
            (html/set-attr :href (tag-url tag-name))
            (html/content tag-name)))

(html/defsnippet tagcloud layout-tmpl [:#tagcloud :ul] [tags]
  [:ul] (html/content (map tag tags)))

(html/defsnippet category layout-tmpl [:ul#categories :> html/first-child] [[tag-name tag-count]]
  [:li :a] (html/do->
            (html/set-attr :href (tag-url tag-name))
            (html/content (str tag-name " (" tag-count ")"))))

(html/defsnippet categories layout-tmpl [:ul#categories :> html/first-child] [tags]
  [:li] (html/clone-for [t tags] (html/substitute (category t))))

(html/defsnippet article layout-tmpl  [:#main-content :div.article] [item & {:keys [teaser]}]
  [:.article-head :a] (html/do->
                       (html/content (:title item))
                       (html/set-attr :href (article-url item)))
  [:.article-subhead :span.author] (html/content (:author item))
  [:.article-subhead :span.pubdate] (html/content (format-date (:pubdate item)))
  [:.article-content] (fn [_] (expand-code-snippets (if teaser (:teaser item) (get-item-content (:id item)))))
  [:a#read-more] (when teaser (html/do->
                               (html/remove-attr :id)
                               (html/set-attr :href (article-url item)))))

(html/defsnippet pager layout-tmpl [:ul.pager] [older-items newer-items]
  [:li#link-older] (fn [node]
                     (when older-items
                       (html/at node [:a] (html/set-attr :href older-items))))
  [:li#link-newer] (fn [node]
                     (when newer-items
                       (html/at node [:a] (html/set-attr :href newer-items)))))


(html/deftemplate layout layout-tmpl [& {:keys [content mesg feed older-items newer-items]}]
  [:link#atom-feed] (html/do-> (html/remove-attr :id) (if feed (html/set-attr :href feed) identity))
  [:#info-msg] (when mesg (html/content mesg))
  [:#tagcloud :ul] (html/substitute (tagcloud (get-tags)))
  [:ul#categories] (html/content (categories (sort-by first (get-tags))))
  [:#main-content] (html/content content)
  [:ul.pager]      (when (or older-items newer-items)
                     (html/content (pager older-items newer-items))))

(defn render-article
  [id & {:keys [preview?]}]
  (let [m (get-item-meta id)]
    (when (and m (or preview? (:pubdate m)))
      (layout :content (article m :teaser false)))))

(defn render-latest-items
  [& {:keys [tag page]}]
  (let [page      (if page (Integer/parseInt page) 1)
        num-items (get-item-count :tag tag)
        items     (get-latest-items :tag tag :page page)
        mesg      (when tag (str "Showing items tagged '" tag "'"))]
    (layout :mesg mesg
            :feed (feed-url tag)
            :older-items (older-items-url tag page num-items)
            :newer-items (newer-items-url tag page num-items)
            :content (interpose {:tag :hr}
                                (map #(article % :teaser true) items)))))

(defn latest-date
  [dts]
  (reduce (fn [dt-a dt-b] (if (ct/after? dt-a dt-b) dt-a dt-b)) dts))

(defn render-feed
  [& {:keys [tag]}]
  (let [items (get-latest-items :tag tag)]
    {:headers {"Content-Type" "application/atom+xml"}
     :body (atom-feed :tag     tag
                      :url     (feed-url tag)
                      :updated (latest-date (map :pubdate items))
                      :items   items)}))
