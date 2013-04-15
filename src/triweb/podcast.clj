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
  (html/html-resource (java.net.URL. url)))

(defn mp3-info [mp3]
  (try
    (when mp3
      (let [headers (-> mp3 http/head :headers)]
        {:url mp3
         :length (headers "content-length")
         :duration (or (headers "x-amz-meta-runtime") "NOT FOUND")}))
    (catch Exception _)))

(defn tag [t c]
  {:tag t
   :content (if (vector? c) c [c])})

;; http://www.apple.com/itunes/podcasts/specs.html#example
#_(defn print-last-entry [n]
    (do
      (doseq [entry (take n
                          (butlast
                           (rest
                            (html/select (get-html audio-url)
                                         [:#content :p]))))]
        (when-let [mp3 (mp3-info (-> entry (html/select [:a])
                                     first :attrs :href))]
          (let [[date title speaker _ & body] (map #(.trim %)
                                                   (-> entry html/text
                                                       (.split "\n")))
                date (.format date-out
                              (.parse date-in
                                      (format "%s %s" date time-of-day)))
                speaker (.trim (re-find #"[A-Za-z ]+" speaker))
                guid (-> (UUID/randomUUID) str .toUpperCase)
                link "http://www.trinitynashville.org/sermons/current.html"
                subtitle (format "Speaker: %s" speaker)
                summary (apply str (interpose " " body))
                desc (format "%s. %s" subtitle summary)
                content [(tag :title title)
                         (tag :link link)
                         (tag :description desc)
                         (tag :itunes:subtitle subtitle)
                         (tag :itunes:summary summary)
                         (tag :pubDate date)
                         (tag :guid guid)
                         {:tag :enclosure
                          :attrs {:url (:url mp3)
                                  :length (:length mp3)
                                  :type "audio/mpeg"}}
                         (tag :itunes:duration (:duration mp3))]]
            (xml/emit-str (tag :item content))
            (println))))))

(defn contains-mp3? [node]
  (->> (html/select node [:a])
       (map :attrs)
       (map :href)
       (map #(.toLowerCase %))
       (map #(.endsWith % ".mp3"))
       (some identity)))

(defn sermon-seq [path]
  (let [nodes (->> (t/render path)
                   (apply str)
                   java.io.StringReader.
                   html/html-resource)]
    (->> (html/select nodes [:#content :p])
         (filter contains-mp3?))))

(h/deftemplate podcast (t/find-tmpl "audio.xml") [items]
  [:item] (h/clone-for [item items]
                       (h/html-content "<foo/>")))

(defn wrap-podcast [app]
  (fn [req]
    (let [uri (or (:path-info req)
                  (:uri req))]
      (if (= uri "/audio.xml")
        (r/response "<foo>you know this is foo!</foo>")
        #_(if-let [html (t/render uri)]
            (-> (r/response html)
                (r/content-type "text/xml"))
            )
        (app req)))))
