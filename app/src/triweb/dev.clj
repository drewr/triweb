(ns triweb.dev
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [triweb.handler :refer [app]]))

(defonce server
  (run-jetty #'app {:port 8000 :join? false}))
