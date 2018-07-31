(ns triweb.core
  (:require [cheshire.core :as json]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.util.response :as r]
            [sibiro.core :as sibiro]
            [sibiro.extras :refer [route-handler wrap-try-alts wrap-routes]]
            [triweb.log :refer [log]]
            [triweb.podcast :refer [wrap-podcast] :as podcast]
            [triweb.elasticsearch :as elasticsearch]
            [triweb.files :refer [wrap-file]]
            [triweb.media :as media]
            [triweb.template :refer [wrap-template]]
            [triweb.template :as tmpl]
            [triweb.time :as time]))

(def json-pretty-printer
  (json/create-pretty-printer
   (assoc json/default-pretty-print-options
     :object-field-value-separator ": "
     :indent-arrays? true)))

(defn wrap-log [app]
  (fn [req]
    (log "%s %s"
         (.toString (java.util.Date.))
         (with-out-str (pr req)))
    (app req)))

(defn text-response [body]
  (-> (r/response body)
      (r/content-type "text/plain")
      (r/charset "utf-8")))

(defn html-response [body]
  (-> (text-response body)
      (r/content-type "text/html")))

(defn json-response [body]
  (-> (text-response body)
      (r/content-type "application/json")))

(defn xml-response [body]
  (-> (text-response body)
      (r/content-type "application/xml")))

(defmulti handle-route
  (fn [req]
    (first (:route-handler req))))

(defmethod handle-route :ping
  [req]
  (text-response "pong"))

(defmethod handle-route :default-route
  [req]
  (json-response
   (json/encode {:status "default"}
                {:pretty json-pretty-printer})))

(defmethod handle-route :podcast
  [req]
  (xml-response
   (podcast/podcast-str (-> req :es :conn) (-> req :es :idx) 10)))

(defmethod handle-route :es-info
  [req]
  (let [es (-> req :es :conn)
        es-info (elasticsearch/check es)]
    (text-response
     (if-let [key (-> req :route-params :key)]
       (get es-info (keyword key) (format "key `%s` isn't there" key))
       (with-out-str
         (clojure.pprint/pprint
          (elasticsearch/check es)))))))

(defmethod handle-route :date-view
  [req]
  (let [date (-> req :route-params :date)
        sermons (media/load-sermons (-> req :sermon-file)
                                    "http://media.trinitynashville.org")
        doc (get-in sermons [date])]
    (if doc
      (json-response
       (json/encode doc {:pretty json-pretty-printer}))
      (if-let [possible-date (time/previous-possible-date
                              (time/parse-ymd date)
                              (->> sermons keys sort (map time/parse-ymd)))]
        (let [guess (time/unparse-ymd possible-date)
              body (format "Maybe you meant <a href=\"/by-date/%s\">%s</a>?"
                           guess guess)]
          (-> (r/not-found body)
              (r/content-type "text/html")
              (r/charset "utf-8")))
        (let [body (format
                    "<strong><code>%s</code></strong> doesn't look like a YYYY-MM-DD date?"
                    date)]
          (-> (r/not-found body)
              (r/content-type "text/html")
              (r/charset "utf-8")))))))

(def raw-routes
  #{[:get     "/podcast.xml"           [:podcast]]
    [:get     "/by-date/:date"         [:date-view]]
    [:get     "/ping"                  [:ping]]
    [:head    "/ping"                  [:ping]]
    [:get     "/es/info"               [:es-info]]
    [:get     "/es/info/:key"          [:es-info]]
    [:options "/"                      [:cors-preflight]]
    [:options ":*"                     [:cors-preflight]]
    [:any     "/"                      [:default-route]]
    [:any     ":*"                     [:default-route]]
    })

(def router
  (let [rc (sibiro/compile-routes raw-routes)]
    (fn [req]
      (let [match (sibiro/match-uri rc (:uri req) (:request-method req))]
        (let [resp (handle-route (merge req match))]
          (assoc resp
            :route-params (:route-params match)
            :route-handler (:route-handler match)))))))

(defn wrap-sermon-file [app file]
  (fn [req]
    (app (assoc req :sermon-file file))))

(defn wrap-es-conn [app conn]
  (fn [req]
    (app (assoc-in req [:es :conn] conn))))

(def app
  (-> router
      (wrap-sermon-file "/sermons.json.gz")
      (wrap-content-type)))

(defn make-with-es [app es-conn]
  (-> app
      (wrap-es-conn es-conn)))
