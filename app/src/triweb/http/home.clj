(ns triweb.http.home
  (:require [triweb.http.common :refer [redirect html-response text-response]]
            [triweb.http.routes :refer [handle-next handle-route]]
            [triweb.template.home :as home]))

(defmethod handle-route :default-route
  [req]
  (html-response (#'home/home "Bob"))
  #_(text-response
   (with-out-str
     (clojure.pprint/pprint [:default
                             (select-keys req [:uri :route-handler])]))))

