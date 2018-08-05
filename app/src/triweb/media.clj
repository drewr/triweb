(ns triweb.media
  (:require [cheshire.core :as json]
            [clj-time.core :as time]
            [clj-time.format :as time.format]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [elasticsearch.document :as es.doc]
            [elasticsearch.indices :as indices]
            [elasticsearch.scroll :as es.scroll]
            [triweb.elasticsearch :as triweb.es]
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
    (merge doc {:id (make-id doc)})))

(defn make-docs [dir]
  (for [path (->> (file-seq (io/file dir "source"))
                  (filter #(.isFile %))
                  (map #(.toPath %)))]
    (make-doc path)))

(defn index-dir [dir conn index]
  (let [settings-file (io/file dir "settings.json")
        settings (-> settings-file slurp (json/decode true))]
    (indices/ensure conn index {:body settings})
    (doseq [doc (make-docs dir true)]
      (try
        (es.doc/index conn index search/_type (:id doc) {:body (dissoc doc :id)})
        (catch Exception e
          (clojure.pprint/pprint doc)
          (throw e))))
    (indices/refresh conn index)))

(defn compile-sermons [dir]
  (let [target (io/file "target" "sermons.json.gz")
        docs (->> (make-docs dir)
                  (filter #(= (:type %) "Sermon")))]
    (with-open [^java.io.Writer w (-> target
                                      io/output-stream
                                      GZIPOutputStream.
                                      io/writer)]
      (.write w (json/encode docs)))))

(defn decompress-and-deserialized-file [f]
  (-> f
      io/input-stream
      GZIPInputStream.
      io/reader
      slurp
      (json/decode true)))

(def load-sermons
  (memoize
   (fn [f url-prefix]
     (let [docs (decompress-and-deserialized-file f)]
       (reduce #(conj %1 [(:date %2)
                          (assoc %2
                            :link (format "%s/%s-%s.mp3"
                                          url-prefix
                                          (:date %2)
                                          (:slug %2))
                            :scripture (scripture/unparse
                                        (:scripture %2)))])
               {} docs)))))

(defn load-sermons-in-es [f conn idx]
  (let [docs (decompress-and-deserialized-file f)]
    (doseq [d docs]
      (let [id (make-id d)]
        (println id)
        (es.doc/index conn idx "_doc" id {:body d})))))

(defn enrich [url-prefix doc]
  (assoc doc
    :link (format "%s/%s-%s.mp3"
                  url-prefix
                  (:date doc)
                  (:slug doc))
    :scripture (scripture/unparse
                (:scripture doc))))

(defn find-by-date [conn idx date url-prefix]
  (let [q {:query
           {:bool
            {:must
             [{:match {:date date}}
              {:match {:type "Sermon"}}]}}}]
    (->> (es.doc/search conn idx {:body q})
         :hits
         :hits
         (map :_source)
         (map (partial enrich url-prefix)))))

(defn available-sermon-dates [conn idx]
  (let [q {:query
           {:match
            {:type "Sermon"}}
           :sort [{:date :asc}]}]
    (->> (es.scroll/scroll conn idx {:body q})
         (map :_source)
         (map :date))))
