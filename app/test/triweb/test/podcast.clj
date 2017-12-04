(ns triweb.test.podcast
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.test :refer :all]
            [clojure.spec.test.alpha]
            [environ.core :as env]
            [elasticsearch.connection.http :as es.conn]
            [elasticsearch.document :as es]
            [triweb.podcast :as podcast]))

(clojure.spec.test.alpha/instrument)

(def ES (es.conn/make {:url (env/env :es-url)}))
(def IDX "trinity")

(def html "<div class=\"content-wrap\">
  <div id=\"sidebar\">
  <ul class=\"sidenav\">
<li><a class=\"active\" href=\"/sermons/current.html\">Current Series</a></li><li><a href=\"/sermons/archives.html\">Archives</a></li><li><a href=\"/sermons/series/judges.html\">The Book Of Judges</a></li></ul>
  </div>
<div id=\"content\"><h2>Authentic Christianity: 2 Corinthians</h2><p><em>The content below is syndicated.</em> <em>If you would like to subscribe,</em> <em>head over to our <a href=\"http://www.itunes.com/podcast?id=395441827\">iTunes Page</a>.</em> <em>If you don’t use iTunes, you can subscribe directly to the</em> <em><a href=\"http://feeds.feedburner.com/TrinityChurchNashville\">feed link</a>.</em> <a href=\"http://feeds.feedburner.com/TrinityChurchNashville\" class=\"feedicon\"> <img src=\"/img/feed-icon-14x14.png\" /></a> </p><p><strong>Sunday, 14 May 2017</strong><br />The God-Centered Life - 2 Corinthians 10:1-18<br /><em>Matt McCullough</em> — <a href=\"http://media.trinitynashville.org/2017-05-14-2-Cor-10-The-God-Centered-Life.mp3\">MP3</a><br />What do you want your life to be known for?</p>")

(deftest edn
  (let [s (podcast/sermon-seq (java.io.StringReader. html))]
    (is (= (count s) 1))
    (is (= "The God-Centered Life | Matt McCullough | 2 Corinthians 10:1-18"
           (-> s first :podcast/title)))
    (is (= "2017-05-14" (-> s first :podcast/date str)))))

(deftest xml
  (let [s (podcast/build-items
           (podcast/sermon-seq (java.io.StringReader. html)))]
    (is (= (count s) 1))
    (is (= "The God-Centered Life | Matt McCullough | 2 Corinthians 10:1-18"
           (-> s first first :content first)))))
