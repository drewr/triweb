(ns triweb.media.mp3
  (:require [amazonica.aws.s3 :as s3]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [triweb.data :refer [md5sum-file]]))

(defn list-all-objects [req]
  (let [res (s3/list-objects req)
        next-request (assoc req :marker (:next-marker res))]
    (concat
     (:object-summaries res)
     (when (:truncated? res)
       (lazy-seq (list-all-objects next-request))))))

(defn get-seconds [mp3]
  (let [res (sh/sh "mp3info" "-p" "%S" mp3)]
    (if (pos? (:exit res))
      (throw (ex-info "can't get mp3 info" res))
      (Long/parseLong (:out res)))))

(defn update-duration! [bucket key]
  (let [mp3 key]
    (when (not (.exists (io/file mp3)))
      (let [res (s3/get-object :bucket-name bucket :key key)]
        (io/copy (:input-stream res) (io/file mp3))
        (.close (:input-stream res))))
    (let [secs (get-seconds mp3)
          res (s3/copy-object
               :source-bucket-name bucket
               :source-key key
               :destination-bucket-name bucket
               :destination-key key
               :access-control-list {:grant-permission ["AllUsers" "Read"]}
               :new-object-metadata
               {:user-metadata
                {:seconds secs
                 :md5 (md5sum-file mp3)}})]
      (clojure.pprint/pprint
       [bucket key
        (-> (s3/get-object :bucket-name bucket :key key)
            :object-metadata
            :user-metadata)]))))

(defn update-duration-url! [url]
  (let [[_ bucket key] (re-find #"https?://([^/]+)/?(.*)" url)]
    (when (and bucket key)
      (update-duration! bucket key))))

(defn update-duration [bucket key]
  (->> (list-all-objects {:bucket-name bucket
                          :prefix key})
       (map #(update-duration! bucket (:key %)))
       doall)
  (shutdown-agents))
