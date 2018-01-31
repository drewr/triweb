(ns triweb.core
  (:require [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.util.response :as r]
            [triweb.log :refer [log]]
            [triweb.podcast :refer [wrap-podcast]]
            [triweb.files :refer [wrap-file]]
            [triweb.template :refer [wrap-template]]
            [triweb.template :as tmpl]))

(def app
  (-> tmpl/handler
      (wrap-template)
      (wrap-podcast)
      (wrap-file [".pdf" ".txt" ".png" ".jpg" ".gif"])
      (wrap-resource "static")
      (wrap-content-type)
      (wrap-log)))
