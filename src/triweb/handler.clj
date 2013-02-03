(ns triweb.handler
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :as r]
            [triweb.template :refer [wrap-template]]
            [triweb.template :as tmpl]))

(defroutes handler
  (GET "/" [] "home")
  (route/not-found "no go!"))

(def app
  (-> #'handler
      (wrap-template)
      (wrap-resource "static")))
