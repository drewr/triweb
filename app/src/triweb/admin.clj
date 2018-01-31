(ns triweb.admin
  (:require [triweb.http.common :refer [text-response]]
            [triweb.http.routes :refer [handle-route]]))

(defmethod handle-route :admin
  [req]
  (case (:route-handler req)
    [:admin :index]
    (text-response
     (with-out-str
       (clojure.pprint/pprint req)))

    [:admin :foo]
    (text-response "foo!")))
