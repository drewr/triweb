(ns triweb.http.common
  (:require [ring.util.response :as r]))

(defn text-response [body]
  (-> body
      r/response
      (r/status 200)
      (r/content-type "text/plain")
      (r/charset "utf-8")))

(defn html-response [body]
  (-> body
      r/response
      (r/status 200)
      (r/content-type "text/html")
      (r/charset "utf-8")))

(defn redirect [url]
  (r/redirect url))

(defn error-response [msg]
  (throw (ex-info msg {})))
