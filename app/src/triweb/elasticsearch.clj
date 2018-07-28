(ns triweb.elasticsearch
  (:require [clojure.spec.alpha :as s]
            [elasticsearch.connection.http :refer [make]]
            [elasticsearch.connection :as conn]
            [elasticsearch.document :as es.doc]
            triweb.media))

(defn connect [url]
  (make {:url url}))

(defn check [conn]
  (conn/version conn))

(defn search [conn & args]
  (apply es.doc/search conn args))

(s/fdef )
(defn find-latest [conn idx size]
  )
