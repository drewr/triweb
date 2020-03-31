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

(defn find-latest [conn idx types size sort-by]
  (let [q {:query
           {:bool
            {:should
             (map
              (fn [t]
                {:match
                 {:type t}})
              types)}}
           :size size
           :sort [{sort-by {"order" "desc"}}]}]
    (->> (es.doc/search conn idx {:body q})
         :hits
         :hits
         (map :_source))))
