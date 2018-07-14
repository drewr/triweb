(ns triweb.media
  (:require [cheshire.core :as json]
            [clj-time.core :as time]
            [clj-time.format :as time.format]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [elasticsearch.document :as es.doc]
            [elasticsearch.indices :as indices]
            [triweb.scripture :as scripture]
            [triweb.podcast :as podcast]
            [triweb.search :as search])
  (:import (java.util.zip GZIPInputStream GZIPOutputStream)))

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

(defn make-doc [path]
  (let [path-str (str path)
        file-str (str (.getFileName path))
        [_ date-str] (re-find #"(\d{4}-\d{2}-\d{2}).*" file-str)
        doc (assoc (-> path-str
                       slurp
                       (json/decode true))
              :date date-str)]
    (merge doc {:id (make-id doc)
                :scripture (scripture/unparse (:scripture doc))})))

(defn make-docs [dir]
  (for [path (->> (file-seq (io/file dir "source"))
                  (filter #(.isFile %))
                  (map #(.toPath %)))]
    (make-doc path)))

(defn index-dir [dir conn index]
  (let [source-dir (io/file dir "source")
        settings-file (io/file dir "settings.json")
        settings (-> settings-file slurp (json/decode true))]
    (indices/ensure conn index {:body settings})
    (doseq [doc (make-docs source-dir)]
      (es.doc/index conn index search/_type (:id doc) {:body (dissoc doc :id)}))
    (indices/refresh conn index)))

(defn compile-sermons [dir]
  (let [target (io/file "target" "sermons.json.gz")
        docs (->> dir
                  make-docs
                  (filter #(= (:type %) "Sermon")))]
    (with-open [^java.io.Writer w (-> target
                                      io/output-stream
                                      GZIPOutputStream.
                                      io/writer)]
      (.write w (json/encode docs)))))

(def load-sermons
  (memoize
   (fn [f url-prefix]
     (let [docs (-> f
                    io/input-stream
                    GZIPInputStream.
                    io/reader
                    slurp
                    (json/decode true))]
       (reduce #(conj %1 [(:date %2)
                          (assoc %2
                            :link (format "%s/%s-%s.mp3"
                                          url-prefix
                                          (:date %2)
                                          (:slug %2)))])
               {} docs)))))
