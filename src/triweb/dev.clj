(ns triweb.dev
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [triweb.handler :refer [app]]))

(defn serve! [port]
  (run-jetty #'app {:port port :join? false}))
