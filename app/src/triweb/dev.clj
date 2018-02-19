(ns triweb.dev
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.reload :refer [wrap-reload]]
            [triweb.http :refer [app]]
            ))

(defonce server
  (run-jetty (-> #'app
                 ;; Doesn't account for enlive
                 wrap-reload
                 )
             {:port 3000 :join? false}))
