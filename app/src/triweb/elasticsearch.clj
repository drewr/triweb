(ns triweb.elasticsearch
  (:require [clojure.spec.alpha :as s]
            [elasticsearch.connection.http :refer [make]]
            [elasticsearch.connection :as conn]
            [elasticsearch.document :as es.doc]))

(defn connect [url]
  (make {:url url}))

(defn check [conn]
  (conn/version conn))

(defn search [conn & args]
  (apply es.doc/search conn args))

(defn find-latest [conn idx type size sort-by]
  (let [q {:query
           {:match
            {:type type}}
           :size size
           :sort [{sort-by {"order" "desc"}}]}]
    (->> (es.doc/search conn idx {:body q})
         :hits
         :hits
         (map :_source))))
