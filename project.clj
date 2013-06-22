(defproject simple-cms "1.0.1-SNAPSHOT"
  :description "Simple CMS using Compojure, Enlive and Bootstrap"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
                 [enlive "1.1.1"]
                 [clj-time "0.5.1"]]
  :ring {:handler simple-cms.core/app
         :init simple-cms.core/init
         :destroy simple-cms.core/destroy})
