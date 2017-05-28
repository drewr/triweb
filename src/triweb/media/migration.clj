(ns triweb.media.migration
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [clojure.walk :as walk]
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

(def urls
  ["http://trinitynashville.org/audio/2.html"
   ])

(defn parse-title [s]
  (when (string? s)
    (let [[title other] (str/split s #" - ")]
      (when other
        {:podcast/title title
         :media/scripture other}))))

(defn get-slug [url]
  (let [[_ slug] (re-find #"https?://[^/]+/?([^.]+)" url)]
    slug))

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
           (merge s title)))
       (filter identity)))

(defn make-media-entry [x]
  (let [x3 {
            :media/blurb           (-> x :podcast/summary)
            :media/date            (-> x :podcast/date)
            :media/speaker         (-> x :podcast/speaker)
            :media/title           (-> x :podcast/title)
            :media/scripture       (-> x :media/scripture)
            :media/has-audio       (pos? (-> x :podcast/mp3 :mp3/bytes))
            :media/has-audio-error false
            :media/series          "FIXME"
            :media/slug            (get-slug (-> x :podcast/mp3 :mp3/url))
            :media/tags            []
            :media/type            "Sermon"
            :media/published       true
            }
        x4 (s/conform :media/entry x3)]
    (if (= ::s/invalid x4)
      (throw
       (ex-info "doesn't conform" (s/explain-data :media/entry x3)))
      x4)))

(defn jsonify [x]
  (json/encode
   (into (sorted-map)
         ;; Remove ns from keywords.  Isn't `stringify-keys` real
         ;; purpose, but since it uses `name` it works.
         (walk/stringify-keys x))
   {:pretty true}))

(defn scrape-sermons [urls dir*]
  (doseq [s (->> urls
                 (pmap (comp sermons-with-standard-titles #(java.net.URL. %)))
                 (apply concat)
                 (sort-by :podcast/date)
                 (pmap make-media-entry))]
    (let [dir (doto (io/file dir*) .mkdirs)
          f (io/file dir (str (:media/date s) ".json"))]
      (spit f (jsonify (dissoc s :media/date))))))

(comment
  (scrape-sermons urls "./search/source-tmp"))
