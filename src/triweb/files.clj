(ns triweb.files
  (:require [ring.util.response :as r]
            [triweb.template :refer [find-tmpl]]))

(defn matches-ext? [path exts]
  (some #(.endsWith (.toLowerCase path) (.toLowerCase %)) exts))

(defn servable-file? [path exts]
  (and (matches-ext? path exts) (find-tmpl path)))

(defn wrap-file [handler exts]
  (fn [req]
    (let [uri (or (:path-info req) (:uri req))]
      (if-let [f (servable-file? uri exts)]
        (r/file-response (str f))
        (handler req)))))
