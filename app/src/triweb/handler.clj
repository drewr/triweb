(ns triweb.handler
  (:require [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.util.response :as r]
            [sibiro.core :as sibiro]
            [sibiro.extras :refer [route-handler wrap-try-alts wrap-routes]]
            [triweb.log :refer [log]]
            [triweb.podcast :refer [wrap-podcast]]
            [triweb.files :refer [wrap-file]]
            [triweb.template :refer [wrap-template]]
            [triweb.template :as tmpl]))

(defn wrap-log [app]
  (fn [req]
    (log "%s %s"
         (.toString (java.util.Date.))
         (with-out-str (pr req)))
    (app req)))

(defmulti handle-route
  (fn [req]
    (first (:route-handler req))))

(def raw-routes
  (let [api-prefix "/:api-version{v\\d+}"
        tile-route (str api-prefix "/:style/:zoom{\\d+}/*")
        mani-route (str api-prefix "/manifest")]
    #{[:get     "/ping"                  [:ping]]
      [:head    "/ping"                  [:ping]]
      [:options "/"                      [:cors-preflight]]
      [:options ":*"                     [:cors-preflight]]
      [:any     "/"                      [:default-route]]
      [:any     ":*"                     [:default-route]]
      }))

(def router
  (let [rc (sibiro/compile-routes raw-routes)]
    (fn [req]
      (let [match (sibiro/match-uri rc (:uri req) (:request-method req))]
        (let [resp (handle-route (merge req match))]
          #_(clojure.pprint/pprint resp)
          (assoc resp
                 :route-params (:route-params match)
                 :route-handler (:route-handler match)))))))

(def app
  (-> tmpl/handler
      (wrap-template)
      (wrap-podcast)
      (wrap-file [".pdf" ".txt" ".png" ".jpg" ".gif"])
      (wrap-resource "static")
      (wrap-content-type)
      (wrap-log)))
