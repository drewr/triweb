(ns triweb.media.mp3
  (:require [amazonica.aws.s3 :as s3]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [triweb.data :refer [md5sum-file]])
  (:import (java.util.regex Pattern)))

(defn list-all-objects [req]
  (let [res (s3/list-objects req)
        next-request (assoc req :marker (:next-marker res))]
    (concat
     (:object-summaries res)
     (when (:truncated? res)
       (lazy-seq (list-all-objects next-request))))))

(defn duration [mp3]
  (let [pat (Pattern/compile "Time:\\ (\\d+):(\\d+)" Pattern/MULTILINE)
        res (sh/sh "eyeD3" mp3)]
    (if (pos? (:exit res))
      (throw (ex-info "can't get mp3 info" res))
      (let [[_ min secs] (re-find pat (:out res))]
        {:seconds (when (and min secs)
                    (+ (* 60 (Integer/parseInt min))
                       (Integer/parseInt secs)))
         :duration (str min ":" secs)}))))

(defn update-duration! [bucket key]
  (let [mp3 key]
    (when (not (.exists (io/file mp3)))
      (let [res (s3/get-object :bucket-name bucket :key key)]
        (io/copy (:input-stream res) (io/file mp3))
        ;; So we don't leak HTTP pool resources
        ;; https://github.com/mcohen01/amazonica/commit/5843ecdb8d64541631b22416b989a25ce1198f6e
        (slurp (:input-stream res))))
    (let [dur (duration mp3)
          _ (s3/copy-object
             :source-bucket-name bucket
             :source-key key
             :destination-bucket-name bucket
             :destination-key key
             :access-control-list {:grant-permission ["AllUsers" "Read"]}
             :new-object-metadata
             {:user-metadata
              (merge
               dur {:md5 (md5sum-file mp3)})})
          res (s3/get-object :bucket-name bucket :key key)]
      ;; ditto
      (slurp (:input-stream res))
      (clojure.pprint/pprint
       [bucket key
        (-> res
            :object-metadata
            :user-metadata)]))))

(defn update-duration-url! [url]
  (let [[_ bucket key] (re-find #"https?://([^/]+)/?(.*)" url)]
    (when (and bucket key)
      (update-duration! bucket key))))

(defn update-duration [bucket key]
  (->> (list-all-objects {:bucket-name bucket
                          :prefix key})
       (filter #(.endsWith (:key %) ".mp3"))
       (map #(update-duration! bucket (:key %)))
       doall)
  (shutdown-agents))
