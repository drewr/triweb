(ns triweb.http
  (:require [clj-http.conn-mgr :as conn]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha]
            [ring.adapter.jetty :as jetty]
            [triweb.core :as app]))

(defn run [port es-conn es-index]
  (jetty/run-jetty
   (app/make-with-es app/app es-conn es-index)
   {:port (if (string? port) (Integer/parseInt port) port)
    :send-server-version? false}))
