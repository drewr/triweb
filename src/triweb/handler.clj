(ns triweb.handler
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.util.response :as r]
            [triweb.log :refer [log]]
            [triweb.template :refer [wrap-template]]
            [triweb.template :as tmpl]))

(defroutes handler
  (GET "/" [] "home")
  (route/resources "/" {:root "static"})
  (route/not-found "no go!"))

(defn wrap-log [app]
  (fn [req]
    (log "%s %s"
         (.toString (java.util.Date.))
         (with-out-str (pr req)))
    (app req)))

(def app
  (-> handler
      (wrap-template)
      (wrap-resource "static")
      (wrap-content-type)
      (wrap-log)))
