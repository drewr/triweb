(ns triweb.http.routes
  (:require [triweb.data :refer [strip-trailing]]
            [triweb.http.common :refer [text-response html-response
                                        error-response]]
            [triweb.template.home :as home]
            [sibiro.core :as sibiro]
            [sibiro.extras :refer [route-handler wrap-try-alts wrap-routes]]))

(defmulti handle-route
  (fn [req]
    (first (:route-handler req))))

(defn handle-next [req]
  (handle-route (update req :route-handler rest)))

(def routes*
  #{[:get     "/upload/new"            [:login :upload :create]]
    [:get     "/login"                 [:login-form]]
    [:post    "/login"                 [:login-post]]
    [:get     "/ping"                  [:ping]]
    [:head    "/ping"                  [:ping]]
    [:options "/"                      [:cors-preflight]]
    [:options ":*"                     [:cors-preflight]]
    [:any     "/"                      [:default-route]]
    [:any     ":*"                     [:default-route]]
    })

(def router
  (let [rc (sibiro/compile-routes routes*)]
    (fn [req]
      (let [match (sibiro/match-uri
                   rc (strip-trailing "/" (:uri req)) (:request-method req))]
        (let [resp (handle-route (merge req match))]
          #_(clojure.pprint/pprint [req resp])
          (assoc resp
                 :route-params (:route-params match)
                 :route-handler (:route-handler match)))))))
