(ns simple-cms.core
  (:use compojure.core
        [simple-cms.properties :only (get-property)]
        [simple-cms.content :only (refresh-site-content!)]
        [simple-cms.views :only (render-latest-items render-article render-feed)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler])
  (:import [java.util.concurrent Executors]))

;; Future metadata updates are done in a separate thread. Just one
;; thread, so multiple concurrent update requests are serialized.
(declare executor-service)

(defn do-refresh-site-content
  "Execute `refresh-site-content!` in a separate thread, return immediately"
  [api-key]
  (if (= api-key (get-property :api-key))
    (do (.execute executor-service #(refresh-site-content! (get-property :site-content-dir)))
        "<p>Content refresh initiated</p>")
    {:status 403 :body "<p>Invalid API key</p>"}))

(defroutes main-routes
  (GET "/" [page]
       (render-latest-items :page page))
  (GET ["/content/:article-id" :article-id #".+"] [article-id]
       (render-article article-id))
  (GET ["/preview/:article-id" :article-id #".+"] [article-id]
       (render-article article-id :preview? true))
  (GET "/tags/:tag" [tag page]
       (render-latest-items :tag tag :page page))
  (GET "/feed" []
       (render-feed))
  (GET "/feed/:tag" [tag]
       (render-feed :tag tag))
  (POST "/api/refresh-site-content/:api-key" [api-key]
        (do-refresh-site-content api-key))
  (route/files "/" {:root (get-property :site-static-dir)})
  (route/not-found "Page not found"))

(defn init
  []
  ;; Make sure the metadata is populated at application startup
  (refresh-site-content! (get-property :site-content-dir))
  (def executor-service (Executors/newSingleThreadExecutor)))

(defn destroy
  []
  (when executor-service (.shutdown executor-service)))

(def app
  (handler/site main-routes))
