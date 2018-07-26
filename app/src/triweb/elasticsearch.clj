(ns triweb.elasticsearch
  (:require [elasticsearch.connection.http :refer [make]]
            [elasticsearch.connection :as conn]
            [elasticsearch.document :as es.doc]))

(defn connect [url]
  (make {:url url}))

(defn check [conn]
  (conn/version conn))

(defn search [conn & args]
  (apply es.doc/search conn args))
