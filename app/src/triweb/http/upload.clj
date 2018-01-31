(ns triweb.http.upload
  (:require [triweb.http.common :refer [handle-route]]))


(defmethod handle-route :upload
  [req]
  (case (:route-handler req)
    [:upload :create]
    (text-response
     (with-out-str
       (clojure.pprint/pprint req)))))



