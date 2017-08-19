(ns triweb.media
  (:require [cheshire.core :as json]
            [clj-time.core :as time]
            [clj-time.format :as time.format]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [elasticsearch.document :as es.doc]
            [elasticsearch.indices :as indices]
            [triweb.podcast :as podcast]
            [triweb.search :as search]))

(def ymd-fmt
  (time.format/formatter "yyyy-MM-dd"))

(s/def :media/blurb string?)
(s/def :media/date string?)
(s/def :media/has-audio boolean?)
(s/def :media/has-audio-error boolean?)
(s/def :media/series string?)
(s/def :media/slug string?)
(s/def :media/speaker string?)
(s/def :media/tags (s/coll-of string?))
(s/def :media/title string?)
(s/def :media/type string?)

(s/def :media/entry
  (s/keys
   :req [:media/blurb
         :media/date
         :media/has-audio
         :media/has-audio-error
         :media/series
         :media/slug
         :media/speaker
         :media/tags
         :media/title
         :media/type]))

(defn from-legacy-yaml [source]
  )

(defn make-id [{:keys [slug date]}]
  (format "%s-%s" date slug))

(defn index-dir [dir conn index]
  (let [source-dir (io/file dir "source")
        settings-file (io/file dir "settings.json")
        settings (-> settings-file slurp (json/decode true))]
    (indices/ensure conn index {:body settings})
    (doseq [path (->> (file-seq source-dir)
                      (filter #(.isFile %))
                      (map #(.toPath %)))]
      (let [path-str (str path)
            file-str (str (.getFileName path))
            [_ date-str] (re-find #"(\d{4}-\d{2}-\d{2}).*" file-str)
            doc (assoc (-> path-str
                           slurp
                           (json/decode true))
                       :date date-str)]
        (es.doc/index conn index search/_type (make-id doc) {:body doc})))
    (indices/refresh conn index)))
