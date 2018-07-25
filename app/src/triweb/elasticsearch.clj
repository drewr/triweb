(ns triweb.elasticsearch
  (:require [elasticsearch.connection.http :refer [make]]
            [elasticsearch.connection :as conn]))

(defn connect [url]
  (make {:url url}))

(defn check [conn]
  (conn/version conn))
