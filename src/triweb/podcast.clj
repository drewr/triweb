(ns triweb.podcast
  (:require [clj-http.client :as http]
            [clojure.data.xml :as xml]
            [net.cgrand.enlive-html :as h]
            [ring.util.response :as r]
            [triweb.template :as t])
  (:import (org.apache.commons.codec.digest DigestUtils)
           (java.text SimpleDateFormat)
           (java.util UUID)))

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
            guid (.toUpperCase
                  (DigestUtils/sha1Hex
                   (apply str
                          ((juxt :url
                                 :length
                                 :duration)
                           mp3))))
            link "http://www.trinitynashville.org/sermons/current.html"
            subtitle (format "Speaker: %s" speaker)
            summary body
            desc (format "%s. %s" subtitle summary)
            content [(xml/element :title {} title)
                     (xml/element :link {} link)
                     (xml/element :description {} desc)
                     (xml/element :itunes:subtitle {} subtitle)
                     (xml/element :itunes:summary {} summary)
                     (xml/element :pubDate {} date)
                     (xml/element :guid {} guid)
                     (xml/element :enclosure
                                  {:url (:url mp3)
                                   :length (:length mp3)
                                   :type "audio/mpeg"})
                     (xml/element :itunes:duration {} (:duration mp3))]]
        (xml/element :item {} content)))))

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
  [:items] (h/clone-for [item items]
                        (h/html-content item)))

(defn wrap-podcast [app]
  (fn [req]
    (let [uri (or (:path-info req)
                  (:uri req))]
      (if (= uri "/audio.xml")
        (r/content-type
         (->> "/sermons/current.html"
              sermon-seq
              build-items
              (map xml/emit-str)
              podcast
              (apply str)
              r/response)
         "text/xml")
        (app req)))))
