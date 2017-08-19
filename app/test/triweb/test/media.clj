(ns triweb.test.media
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.test :refer :all]
            [environ.core :as env]
            [elasticsearch.connection.http :as es.conn]
            [elasticsearch.document :as es]
            [triweb.media :as media]))

(def ES (es.conn/make {:url (env/env :es-url)}))
(def IDX "trinity")

(deftest bootstrap
  (media/index-dir "search/" ES IDX)
  (clojure.pprint/pprint
   (es/search ES IDX {:body
                      {:query
                       {:match
                        {:date "2017-03-26"}}}})))


