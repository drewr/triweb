(ns triweb.http
  (:require [clj-http.conn-mgr :as conn]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha]
            [ring.adapter.jetty :as jetty]
            [triweb.handler :as triweb]))

(defn run [settings]
  (jetty/run-jetty
   triweb/app
   {:port 9000
    :send-server-version? false}))
