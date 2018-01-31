(ns triweb.http.auth
  (:require [triweb.http.common :refer [redirect html-response text-response]]
            [triweb.http.routes :refer [handle-next handle-route]]
            [triweb.template.form :as form]))

(defn logged-in? [req]
  true)

(defmethod handle-route :auth
  [req]
  (if (logged-in? req)
    (handle-next req)
    (redirect "/something/better/")))

(defmethod handle-route :login-form
  [req]
  (html-response
   (#'form/auth :to-be-used)))

(defmethod handle-route :login-post
  [req]
  (if (logged-in? req)
    (text-response
     (with-out-str
       (clojure.pprint/pprint req)))))


