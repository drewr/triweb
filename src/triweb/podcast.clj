(ns triweb.podcast
  (:require [clj-http.client :as http]
            [clojure.core.memoize :as memo]
            [clojure.data.xml :as xml]
            [net.cgrand.enlive-html :as h]
            [ring.util.response :as r]
            [triweb.template :as t])
  (:import (java.text SimpleDateFormat)))

(def CACHE-SECS 60)

(def date-in (SimpleDateFormat. "E, dd MMM y HH:mm:SS"))
(def date-out (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:SS zzz"))
(def time-of-day "12:00:00")

(defn get-html [url]
  (h/html-resource (java.net.URL. url)))

(defn mp3-info [mp3]
  (try
    (when mp3
      (let [headers (-> mp3 http/head :headers)]
        {:url mp3
         :length (headers "content-length")
         :duration (or (headers "x-amz-meta-runtime") "NOT FOUND")}))
    (catch Exception _)))

;; http://www.apple.com/itunes/podcasts/specs.html#example
(defn build-items [entries]
  (for [entry entries]
    (when-let [mp3 (mp3-info (-> entry (h/select [:a]) first :attrs :href))]
      (let [date (-> entry (h/select [:strong]) h/texts first)
            title (-> entry :content (nth 2))
            speaker (-> entry (h/select [:em]) first :content first)
            body (-> entry :content last .trim)
            date (.format date-out
                          (.parse date-in
                                  (format "%s %s" date time-of-day)))
            speaker (.trim (re-find #"[A-Za-z ]+" speaker))
            guid (:url mp3)
            link "http://www.trinitynashville.org/sermons/current.html"
            subtitle (format "Speaker: %s" speaker)
            summary body
            desc (format "%s. %s" subtitle summary)]
        [(xml/element :title {} title)
         #_(xml/element :link {} link)
         (xml/element :description {} desc)
         (xml/element :itunes:subtitle {} subtitle)
         (xml/element :itunes:summary {} summary)
         (xml/element :pubDate {} date)
         (xml/element :guid {} guid)
         (xml/element :enclosure
                      {:url (:url mp3)
                       :length (:length mp3)
                       :type "audio/mpeg"})
         (xml/element :itunes:duration {} (:duration mp3))]))))

(defn contains-mp3? [node]
  (->> (h/select node [:a])
       (map :attrs)
       (map :href)
       (map #(.toLowerCase %))
       (map #(.endsWith % ".mp3"))
       (some identity)))

(defn sermon-seq [path]
  (let [nodes (->> (t/render path)
                   (apply str)
                   java.io.StringReader.
                   h/html-resource)]
    (->> (h/select nodes [:#content :p])
         (filter contains-mp3?))))

(h/deftemplate podcast
  (h/xml-resource (t/find-tmpl "audio.xml")) [items]
  [:item] (h/clone-for [item items]
                       (h/html-content item)))

(defn podcast-str []
  (->> "/sermons/current.html"
       sermon-seq
       build-items
       (map xml/emit-str)
       podcast
       (apply str)))

(def podcast-cached
  (memo/memo-ttl podcast-str (* CACHE-SECS 1000)))

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
