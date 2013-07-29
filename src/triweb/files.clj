(ns triweb.files
  (:require [ring.util.response :as r]
            [triweb.template :refer [find-tmpl]]))

(defn wrap-file [handler exts]
  (fn [req]
    (let [uri (or (:path-info req) (:uri req))]
      (if (some #(.endsWith uri %) exts)
        (r/file-response (str (find-tmpl uri)))
        (handler req)))))
