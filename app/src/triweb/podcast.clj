(ns triweb.podcast
  (:require [clj-http.client :as http]
            [clojure.core.memoize :as memo]
            [clojure.data.xml :as xml]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as h]
            [ring.util.response :as r]
            [triweb.elasticsearch :as es]
            [triweb.template :as t]
            [triweb.time :as time])
  (:import (java.text SimpleDateFormat)))

(def CACHE-SECS 3600)
(def MP3-CACHE-SECS 300)
(def STORAGE_ROOT "https://storage.googleapis.com/trinitynashville-media")
(def SERMON_API_URL "http://sermon-api.trinitynashville.org/podcast.xml")

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
(s/def :podcast/scripture string?)

(s/def :mp3/url string?)
(s/def :mp3/bytes integer?)
(s/def :mp3/seconds integer?)
(s/def :mp3/duration string?)

(s/def :podcast/mp3
  (s/keys :req [:mp3/url
                :mp3/bytes
                :mp3/duration
                :mp3/seconds
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
(def date-in (SimpleDateFormat. "EEEE, dd MMM y"))

(defn get-html [url]
  (h/html-resource (java.net.URL. url)))

(defn mp3-info* [mp3]
  (when mp3
    (try
      (let [headers (-> mp3 http/head :headers)
            obj {:mp3/url mp3
                 :mp3/bytes (when-let [l (headers "content-length")]
                              (Long/parseLong l))
                 :mp3/duration (headers "x-goog-meta-duration")
                 :mp3/seconds (Integer/parseInt
                               (or (headers "x-goog-meta-seconds") "-1"))}]
        (if (= ::s/invalid (s/conform :podcast/mp3 obj))
          (throw
           (ex-info (str "bad mp3: " mp3)
                    (s/explain-data :podcast/mp3 obj)))
          obj))
      (catch Exception e
        (log/errorf "problem with %s" mp3)
        (throw e)))))

(def mp3-info
  (memo/ttl mp3-info* :ttl/threshold (* MP3-CACHE-SECS 1000)))

(defn infuse-speaker-into-title [maybe-title speaker]
  (let [[t ref] (if (string? maybe-title)
                  (str/split maybe-title #" - " 2))]
    (format "%s%s"
            (format "%s | %s" t speaker ref)
            (if ref
              (format " | %s" ref)
              ""))))

(defn html-to-edn [entry]
  (when-let [mp3 (mp3-info (-> entry (h/select [:a]) first :attrs :href))]
    (let [date (when-let [d (-> entry (h/select [:strong]) h/texts first)]
                 (->> d
                      (.parse date-in)
                      (.format date-ymd)))
          speaker (let [s (-> entry (h/select [:em]) first :content first)]
                    (if s
                      (.trim (re-find #"[-A-Za-z.' ]+" s))
                      ""))
          title (infuse-speaker-into-title (-> entry :content (nth 2)) speaker)
          body (-> entry :content last .trim)
          guid (:mp3/url mp3)
          link "http://web01.trinitynashville.org/sermons/current.html"
          subtitle (format "Speaker: %s" speaker)
          summary body
          desc body]
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
        (if (= ::s/invalid (s/conform :podcast/entry obj))
          (throw
           (Exception.
            (format "entry has errors: %s"
                    (with-out-str
                      (clojure.pprint/pprint
                       (s/explain-data :podcast/entry obj))))))
          obj)))))

(defn media-to-entry [media]
  (when-let [mp3 (mp3-info (format "%s/%s-%s.mp3" STORAGE_ROOT (:date media) (:slug media)))]
    (let [date (:date media)
          speaker (:speaker media)
          title (:title media)
          title (infuse-speaker-into-title title speaker)
          body (:blurb media)
          guid (:mp3/url mp3)
          link SERMON_API_URL
          subtitle (format "Speaker: %s" speaker)
          summary body
          desc body]
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
        (if (= ::s/invalid (s/conform :podcast/entry obj))
          (throw
           (Exception.
            (format "entry has errors: %s"
                    (with-out-str
                      (clojure.pprint/pprint
                       (s/explain-data :podcast/entry obj))))))
          obj)))))

(s/fdef edn-to-xml
  :args (s/cat :entry :podcast/entry))
(defn edn-to-xml [entry]
  [(xml/element
    :item {}
    (xml/element :title {} (:podcast/title entry))
    #_(xml/element :link {} link)
    (xml/element :description {} (:podcast/description entry))
    (xml/element :itunes:subtitle {} (:podcast/subtitle entry))
    (xml/element :itunes:summary {} (:podcast/summary entry))
    (xml/element :pubDate {} (->> (time/podcast-date
                                   (str (:podcast/date entry) "T14:00:00.000-06:00"))))
    (xml/element :guid {} (:podcast/guid entry))
    (xml/element :enclosure
                 {:url (-> entry :podcast/mp3 :mp3/url)
                  :length (-> entry :podcast/mp3 :mp3/bytes)
                  :type "audio/mpeg"})
    (xml/element :itunes:duration {} (-> entry :podcast/mp3 :mp3/duration)))])

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

(defn podcast-xml [items]
  (xml/element
   :rss {:version "2.0"
         :xmlns:itunes "http://www.itunes.com/dtds/podcast-1.0.dtd"}
   (xml/element
    :channel {}
    (xml/element :title {} "Trinity Church of Nashville, TN")
    (xml/element :link {} "https://www.trinitynashville.org")
    (xml/element :description {} "Sermons from Trinity Church by Matt McCullough and others.")
    (xml/element :language {} "en-US")
    (xml/element :lastBuildDate {} "Sun, 12 Nov 2017 12:00:00 CDT")
    (xml/element :itunes:explicit {} "no")
    (xml/element :itunes:author {} "Trinity Church of Nashville")
    (xml/element :itunes:keywords {} "reformed, gospel, baptist, eakin, grace, dave hunt")
    (xml/element :itunes:image {:href "https://storage.googleapis.com/trinitynashville/img/podcast6.png"})
    (xml/element :itunes:owner {}
                 (xml/element :itunes:email {} "web@trinitynashville.org")
                 (xml/element :itunes:name {} "Drew Raines"))
    (xml/element :category {} "Religion & Spirituality")
    (xml/element :category {} "Christianity")
    (xml/element :itunes:category {:text "Religion & Spirituality"}
                 (xml/element :itunes:category {:text "Religion & Spirituality"}))
    (xml/element :copyright {} (format "&#xA9;2010-%s Trinity Church of Nashville. This work is
licensed under a Creative Commons Attribution-Noncommercial-No
Derivative Works 3.0 United States License. To view a copy of this
license, visit
http://creativecommons.org/licensenc-nd/3.0/us/" (time/current-year)))
    items)))

(defn latest-entries-as-xml [conn idx number]
  (->> (es/find-latest conn idx "Sermon" number)
       (map media-to-entry)
       (map edn-to-xml)))

(defn podcast-str [conn idx number]
  (xml/emit-str
   (podcast-xml (latest-entries-as-xml conn idx number))))
