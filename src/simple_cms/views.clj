(ns simple-cms.views
  (:use [simple-cms.content :only (get-latest-items get-item-count get-item get-tags)])
  (:require [net.cgrand.enlive-html :as html]
            [clj-time.format :as tf]))

(defn tag-class-for-count
  "Computes the class for a tag with count `n` (log2 scale)"
  [n]
  (letfn [(pow [n m] (reduce * (repeat m n)))]
    (loop [m 5]
      (if (>= n (pow 2 m)) (str "tag" (inc m)) (recur (dec m))))))

(defn article-url
  "Returns the url for `article`"
  [article]
  (str "/content/" (:id article)))

(defn tag-url
  "Returns the url for `tag`"
  [tag]
  (str "/tags/" tag))

(defn format-date
  "Formats a date as a string, e.g. 'Mon 1 Dec 2010'"
  [d]
  (tf/unparse (tf/formatter "E d MMMM yyyy") d))

(def article-tmpl (html/html-resource "templates/article.html"))

(def layout-tmpl (html/html-resource "templates/layout.html"))

(def read-more-tmpl (html/html-resource "templates/read-more.html"))

(html/defsnippet read-more read-more-tmpl [:a] [article]
  [:a] (html/set-attr :href (article-url article)))

(html/defsnippet tag layout-tmpl [:#tagcloud :ul :> html/first-child] [[tag-name tag-count]]
  [:li] (html/set-attr :class (tag-class-for-count tag-count))
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
  [:li] (html/clone-for [t tags] (html/content (category t))))

(html/defsnippet article article-tmpl  [:div.article] [item]
  [:.article-head :a] (html/do->
                       (html/content (:title item))
                       (html/set-attr :href (article-url item)))
  [:.article-subhead :span.author] (html/content (:author item))
  [:.article-subhead :span.pubdate] (html/content (format-date (:pubdate item)))
  [:.article-content] (html/content (:content item)))

(html/deftemplate layout layout-tmpl [& {:keys [content msg]}]
  [:#info-msg] (when msg (html/content msg))
  [:#tagcloud :ul] (html/content (tagcloud (get-tags)))
  [:ul#categories] (html/content (categories (sort-by first (get-tags))))
  [:#main-content] (html/content content))

(defn article-teaser
  [item]
  (let [teaser (article (assoc item :content (html/select (:content item) [:p.teaser])))]
    (html/transform teaser [:.article-content] (html/append (read-more item)))))

(defn render-single-article
  [id]
  (when-let [a (get-item id)]
    (layout :content (article a))))

(defn render-article-list
  [& {:keys [msg articles]}]
  (let [articles (filter identity (map #(get-item (:id %)) articles))]
    (layout :content (interpose {:tag :hr} (map article-teaser articles)))))


