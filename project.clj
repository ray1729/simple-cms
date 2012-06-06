(defproject simple-cms "1.0.0-SNAPSHOT"
  :description "Simple CMS using Compojure, Enlive and Bootstrap"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [compojure "1.1.0"]
                 [enlive "1.0.0"]
                 [clj-time "0.4.2"]]
  :ring {:handler simple-cms.core/app
         :init simple-cms.core/init
         :destroy simple-cms.core/destroy})
