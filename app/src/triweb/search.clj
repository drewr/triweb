(ns triweb.search
  (:require [elasticsearch.document :as es.doc]))

(def _type "t")

#_(defn index [conn index doc]
  (es.doc/index conn index _type (make-id doc)))
