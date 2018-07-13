(ns triweb.http
  (:require [clj-http.conn-mgr :as conn]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha]
            [ring.adapter.jetty :as jetty]
            [triweb.core]))

(defn run [port]
  (jetty/run-jetty
   triweb.core/app
   {:port (if (string? port) (Integer/parseInt port) port)
    :send-server-version? false}))
