(ns triweb.test.http
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.test :refer :all]
            [environ.core :as env]
            [triweb.elasticsearch :as es]
            [triweb.media :as media]))

(def ES (es/connect (env/env :es-url)))
(def IDX "trinity-media")

(deftest t
  (media/index-dir "search/" ES IDX)
  (is (not (nil? (:number (es/check ES)))))
  (is (= 1 (-> (es/search ES IDX {:body
                                  {:query
                                   {:match
                                    {:date "2018-04-01"}}}})
               :hits
               :total))))
