(ns simple-cms.properties)

(def properties (atom nil))

(defn read-properties
  "Read configuration from simple-cms.properties"
  []
  (into {} (doto (java.util.Properties.) 
             (.load (-> (Thread/currentThread) 
                        (.getContextClassLoader) 
                        (.getResourceAsStream "simple-cms.properties"))))))

(defn build-properties-map
  "Parse and expand the raw properties map"
  []
  (let [props (read-properties)]
    {:base-url         (get props "base-url")
     :site-dir         (get props "site-dir")
     :site-content-dir (str (get props "site-dir") "/content")
     :site-static-dir  (str (get props "site-dir") "/static")
     :code-snippet-dir (str (get props "site-dir") "/snippets")
     :page-size        (Integer/parseInt (get props "page-size"))
     :api-key          (get props "api-key")}))

(defn read-and-set-properties!
  "Build the properties map and cache the result in an atom"
  []
  (reset! properties (build-properties-map)))

(defn get-property
  "Return the value of property `k`; reads and sets the properties map
  if this has not already been done"
  [k]
  (when (nil? @properties) (read-and-set-properties!))
  (get @properties k))
