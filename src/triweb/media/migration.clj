(ns triweb.media.migration
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [elasticsearch.document :as es.doc]
            [elasticsearch.indices :as indices]
            [triweb.podcast :as podcast]
            [triweb.media :as media]
            [triweb.scripture :as bible]
            [triweb.search :as search]))

(def urls
  ["http://trinitynashville.org/audio/1.html"
   "http://trinitynashville.org/audio/2.html"
   "http://trinitynashville.org/sermons/archives.html"
   "http://trinitynashville.org/sermons/current.html"])

(defn parse-title [s]
  (when (string? s)
    (let [[title other] (str/split s #" - ")]
      (when other
        {:title title
         :other other}))))

(defn sermons-with-nonstandard-titles [url]
  (->> (for [s (podcast/sermon-seq url)]
         (let [title (parse-title (:podcast/title s))]
           (if title
             nil
             s)))
       (filter identity)))

(defn sermons-with-standard-titles [url]
  (->> (for [s (podcast/sermon-seq url)]
         (when-let [title (parse-title (:podcast/title s))]
           s))
       (filter identity)))

(comment
  (->> urls
       (mapcat (comp sermons-with-standard-titles #(java.net.URL. %)))
       (sort-by :podcast/date)
       first))
