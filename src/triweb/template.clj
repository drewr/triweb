(ns triweb.template)

(defn wrap-template [app]
  (fn [req]
    (prn "****tmpl" req)))
