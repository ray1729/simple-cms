(ns simple-cms.core
  (:use compojure.core
        [simple-cms.properties :only (get-property)]
        [simple-cms.content :only (update-site-metadata!)]
        [simple-cms.views :only (render-latest-items render-article render-feed render-contact-details)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]))

(update-site-metadata! (get-property :site-content-dir))

;; (defn update-site-meta
;;   [api-key]
;;   (when (= api-key "secret")
;;     (content/update-site-metadata! site-content-dir)
;;     "<p>Sorted!</p>"
;;     ))

(defroutes main-routes
  (GET "/" []
       (render-latest-items))
  (GET "/contact" []
       (render-contact-details))
  (GET ["/content/:article-id" :article-id #".+"] [article-id]
       (render-article article-id))
  (GET ["/preview/:article-id" :article-id #".+"] [article-id]
       (render-article article-id :preview? true))
  (GET "/tags/:tag" [tag]
       (render-latest-items :tag tag))
  (GET "/feed" []
       (render-feed))
  (GET "/feed/:tag" [tag]
       (render-feed :tag tag))
  ;; (POST "/api/update-site-meta" [api-key]
  ;;       (update-site-meta api-key))
  (route/files "/" {:root (get-property :site-static-dir)})
  (route/not-found "Page not found"))

(def app
  (handler/site main-routes))
