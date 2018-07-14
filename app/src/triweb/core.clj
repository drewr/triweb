(ns triweb.core
  (:require [cheshire.core :as json]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.util.response :as r]
            [sibiro.core :as sibiro]
            [sibiro.extras :refer [route-handler wrap-try-alts wrap-routes]]
            [triweb.log :refer [log]]
            [triweb.podcast :refer [wrap-podcast]]
            [triweb.files :refer [wrap-file]]
            [triweb.media :as media]
            [triweb.template :refer [wrap-template]]
            [triweb.template :as tmpl]))

(defn wrap-log [app]
  (fn [req]
    (log "%s %s"
         (.toString (java.util.Date.))
         (with-out-str (pr req)))
    (app req)))

(defn text-response [body]
  {:status 200
   :headers {"Content-Type" "text/plain; charset=utf-8"}
   :body body})

(defn json-response [body]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body body})

(defmulti handle-route
  (fn [req]
    (first (:route-handler req))))

(defmethod handle-route :ping
  [req]
  (text-response "pong"))

(defmethod handle-route :default-route
  [req]
  (json-response (json/encode {:status "default"})))

(defmethod handle-route :date-view
  [req]
  (let [date (-> req :route-params :date)
        sermons (media/load-sermons (-> req :sermon-file))
        doc (get-in sermons [date])]
    (json-response (json/encode doc))))

(def raw-routes
  (let [api-prefix "/:api-version{v\\d+}"
        tile-route (str api-prefix "/:style/:zoom{\\d+}/*")
        mani-route (str api-prefix "/manifest")]
    #{[:get     "/by-date/:date"         [:date-view]]
      [:get     "/ping"                  [:ping]]
      [:head    "/ping"                  [:ping]]
      [:options "/"                      [:cors-preflight]]
      [:options ":*"                     [:cors-preflight]]
      [:any     "/"                      [:default-route]]
      [:any     ":*"                     [:default-route]]
      }))

(def router
  (let [rc (sibiro/compile-routes raw-routes)]
    (fn [req]
      (let [match (sibiro/match-uri rc (:uri req) (:request-method req))]
        (let [resp (handle-route (merge req match))]
          #_(clojure.pprint/pprint resp)
          (assoc resp
            :route-params (:route-params match)
            :route-handler (:route-handler match)))))))

(defn wrap-load-sermons [app file]
  (fn [req]
    (app (assoc req :sermon-file file))))

(def app
  (-> router
      (wrap-load-sermons "/sermons.json.gz")
      (wrap-content-type)))
