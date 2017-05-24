(ns triweb.podcast
  (:require [clj-http.client :as http]
            [clojure.core.memoize :as memo]
            [clojure.data.xml :as xml]
            [clojure.spec.alpha :as s]
            [net.cgrand.enlive-html :as h]
            [ring.util.response :as r]
            [triweb.template :as t])
  (:import (java.text SimpleDateFormat)))

(def CACHE-SECS 3600)

(s/def :podcast/date        (s/and string?
                                   #(re-find #"\d\d\d\d-\d\d-\d\d" %)))
(s/def :podcast/title       string?)
(s/def :podcast/subtitle    string?)
(s/def :podcast/speaker     string?)
(s/def :podcast/guid        string?)
(s/def :podcast/link        string?)
(s/def :podcast/summary     string?)
(s/def :podcast/body        string?)
(s/def :podcast/description string?)

(s/def :mp3/url string?)
(s/def :mp3/bytes integer?)
(s/def :mp3/duration string?)

(s/def :podcast/mp3
  (s/keys :req [:mp3/url
                :mp3/bytes
                :mp3/duration ;; min:secs
                ]))

(s/def :podcast/entry
  (s/keys :req [:podcast/date
                :podcast/title
                :podcast/subtitle
                :podcast/speaker
                :podcast/guid
                :podcast/link
                :podcast/summary
                :podcast/body
                :podcast/description
                :podcast/mp3]))

(def date-ymd (SimpleDateFormat. "yyyy-MM-dd"))
(def date-in (SimpleDateFormat. "E, dd MMM y"))
(def date-out (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:SS Z"))
(def time-of-day "17:00:00")

(defn get-html [url]
  (h/html-resource (java.net.URL. url)))

(defn mp3-info [mp3]
  (when mp3
    (let [headers (-> mp3 http/head :headers)
          obj {:mp3/url mp3
               :mp3/bytes (when-let [l (headers "content-length")]
                            (Long/parseLong l))
               :mp3/duration (headers "x-amz-meta-runtime")}]
      (if (= :s/invalid (s/conform :podcast/mp3 obj))
        (clojure.pprint/pprint
         (s/explain-data :podcast/mp3 obj))
        obj))))

(defn html-to-edn [entry]
  (when-let [mp3 (mp3-info (-> entry (h/select [:a]) first :attrs :href))]
    (let [date (when-let [d (-> entry (h/select [:strong]) h/texts first)]
                 (->> d
                     (.parse date-in)
                     (.format date-ymd)))
          title (-> entry :content (nth 2))
          speaker (-> entry (h/select [:em]) first :content first)
          body (-> entry :content last .trim)
          speaker (.trim (re-find #"[A-Za-z ]+" speaker))
          guid (:mp3/url mp3)
          link "http://www.trinitynashville.org/sermons/current.html"
          subtitle (format "Speaker: %s" speaker)
          summary body
          desc (format "%s. %s" subtitle summary)]
      (let [obj {:podcast/date         date
                 :podcast/title        title
                 :podcast/subtitle     subtitle
                 :podcast/speaker      speaker
                 :podcast/guid         guid
                 :podcast/link         link
                 :podcast/summary      summary
                 :podcast/body         body
                 :podcast/description  desc
                 :podcast/mp3          mp3}]
        (if (= :s/invalid (s/conform :podcast/entry obj))
          (throw
           (Exception.
            (format "entry has errors: %s\n\n%s"
                    (with-out-str
                      (clojure.pprint/pprint
                       (s/explain-data :podcast/mp3 obj))))))
          obj)))))

(s/fdef edn-to-xml
        :args (s/cat :entry :podcast/entry))
(defn edn-to-xml [entry]
  [(xml/element :title {} (:podcast/title entry))
   #_(xml/element :link {} link)
   (xml/element :description {} (:podcast/description entry))
   (xml/element :itunes:subtitle {} (:podcast/subtitle entry))
   (xml/element :itunes:summary {} (:podcast/summary entry))
   (xml/element :pubDate {} (->> (:podcast/date entry)
                                 (.parse date-ymd)
                                 (.format date-out)))
   (xml/element :guid {} (:podcast/guid entry))
   (xml/element :enclosure
                {:url (-> entry :podcast/mp3 :mp3/url)
                 :length (-> entry :podcast/mp3 :mp3/bytes)
                 :type "audio/mpeg"})
   (xml/element :itunes:duration {} (-> entry :podcast/mp3 :mp3/duration))])

;; http://www.apple.com/itunes/podcasts/specs.html#example
(defn build-items [entries]
  (for [entry entries]
    (edn-to-xml entry)))

(defn contains-mp3? [node]
  (->> (h/select node [:a])
       (map :attrs)
       (map :href)
       (map #(.toLowerCase %))
       (map #(.endsWith % ".mp3"))
       (some identity)))

(defn sermon-seq [x]
  (->> (h/select (h/html-resource x) [:#content :p])
       (filter contains-mp3?)
       (map html-to-edn)))

(defn sermon-seq-from-path [path]
  (sermon-seq (->> (t/render path)
                   (apply str)
                   java.io.StringReader.)))

(h/deftemplate podcast
  (h/xml-resource (t/find-tmpl "audio.xml")) [items]
  [:item] (h/clone-for [item items]
                       (h/html-content item)))

(defn podcast-str []
  (->> "/sermons/current.html"
       sermon-seq-from-path
       build-items
       (map xml/emit-str)
       podcast
       (apply str)))

(def podcast-cached
  (memo/ttl podcast-str :ttl/threshold (* CACHE-SECS 1000)))

(defn wrap-podcast [app]
  (fn [req]
    (let [uri (or (:path-info req)
                  (:uri req))]
      (if (= uri "/audio.xml")
        (->
         (r/response (podcast-cached))
         (r/content-type "text/xml")
         (r/charset "utf-8"))
        (app req)))))
