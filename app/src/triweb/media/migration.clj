(ns triweb.media.migration
  (:gen-class)
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
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
  ["http://stage.trinitynashville.org/audio.html"
   "http://stage.trinitynashville.org/audio/1.html"
   "http://stage.trinitynashville.org/audio/2.html"
   "http://stage.trinitynashville.org/sermons/series/judges.html"
   "http://stage.trinitynashville.org/sermons/archives.html"
   "http://stage.trinitynashville.org/sermons/current.html"])

(def urls
  ["http://stage.trinitynashville.org/audio/1.html"
   ])

(defn parse-title [s]
  (when (string? s)
    (let [[title other] (str/split s #" - ")]
      {:podcast/title title
       :media/scripture (or other "FIXME")})))

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

(defn sermons [url]
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
                 (pmap (comp sermons #(java.net.URL. %)))
                 (apply concat)
                 (sort-by :podcast/date)
                 (pmap make-media-entry))]
    (let [content (jsonify (dissoc s :media/date))
          dir (doto (io/file dir*) .mkdirs)
          f-base (io/file dir (:media/date s))
          f (io/file (str f-base ".json"))]
      (if (and (.exists f) (not= (slurp f) content))
        (let [f-new (io/file (str f-base ".json.new"))
              f-diff (io/file (str f-base ".diff"))]
          (println (str f-new))
          (spit f-new content)
          (spit f-diff (:out (sh/sh "diff" "-u" (str f) (str f-new)))))
        (do
          (println (str f))
          (spit f content))))))

(defn print-legacy-post [file]
  (let [ymd (re-find #"\d\d\d\d-\d\d-\d\d" file)
        json (-> file slurp (json/decode true))]
    (printf "**%s**  \n" (->> ymd
                              (.parse podcast/date-ymd)
                              (.format podcast/date-in)))
    (printf "%s - %s   \n" (:title json) (bible/unparse (:scripture json)))
    (printf "*%s* ---\n[MP3](%s)   \n" (:speaker json)
            (format
             "http://media.trinitynashville.org/%s-%s.mp3" ymd (:slug json)))
    (printf "%s\n" (:blurb json))))

(comment
  (scrape-sermons urls "./search/source-tmp")
  )
