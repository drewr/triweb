(ns triweb.media
  (:require [cheshire.core :as json]
            [clj-time.core :as time]
            [clj-time.format :as time.format]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.spec :as s]
            [elasticsearch.document :as es.doc]
            [elasticsearch.indices :as indices]
            [triweb.search :as search]))

(def ymd-fmt
  (time.format/formatter "yyyy-MM-dd"))

(s/def :media/type string?)

(s/def :media/series string?)

(s/def :media/speaker string?)

(s/def :media/title string?)

(s/def :media/date string?)

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

