(ns triweb.template
  (:require [me.raynes.cegdown :as markdown]))

(defn wrap-template [app]
  (fn [req]
    (prn "****tmpl" req)))
