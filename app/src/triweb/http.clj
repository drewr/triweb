(ns triweb.http
  (:require [clj-http.conn-mgr :as conn]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha]
            [clojure.tools.logging :as log]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.params :refer [wrap-params]]
            [triweb.admin :as admin]
            [triweb.http.auth]
            [triweb.http.home]
            [triweb.http.common :refer [text-response]]
            [triweb.http.routes :refer [handle-route router]]))

(defn wrap-log [app]
  (fn [req]
    (log/debug
     (format "%s %s"
             (.toString (java.util.Date.))
             (with-out-str (pr req))))
    (app req)))

;; the :ring :handler
(def app
  (-> router
      wrap-params
      (wrap-resource "static")
      (wrap-content-type)
      wrap-log))

(defn run [port]
  (jetty/run-jetty
   triweb.core/app
   {:port (if (string? port) (Integer/parseInt port) port)
    :send-server-version? false}))

(defmethod handle-route :ping
  [req]
  (text-response "pong"))
