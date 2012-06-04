(ns simple-cms.core
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]            
            [simple-cms.views :as views]
            [simple-cms.content :as content]))

(def site-content-dir "/home/ray/Workspace/simple-cms/resources/content")

(content/update-site-metadata! site-content-dir)

(defn update-site-meta
  [api-key]
  (when (= api-key "secret")
    (content/update-site-metadata! site-content-dir)
    "<p>Sorted!</p>"
    ))

(defroutes main-routes
  (GET "/" [] (views/render-article-list (content/get-latest-items)))
  (GET ["/content/:article-id" :article-id #".+"]  [article-id] (views/render-single-article article-id))
  (GET "/tags/:tag" [tag] (views/render-article-list (content/get-latest-items :tag tag)))
  (POST "/api/update-site-meta" [api-key] (update-site-meta api-key))
  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (handler/site main-routes))
