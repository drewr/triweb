(ns triweb.dev
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [triweb.core :refer [app]]))

(defonce server
  (run-jetty #'app {:port 8000 :join? false}))
