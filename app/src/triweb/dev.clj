(ns triweb.dev
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.reload :refer [wrap-reload]]
            [com.akolov.enlive-reload :refer [wrap-enlive-reload]]
            [triweb.http :refer [app]]
            ))

(defonce server
  (run-jetty (-> #'app
                 wrap-reload
                 wrap-enlive-reload)
             {:port 3000 :join? false}))
