(ns triweb.template.form
  (:require [net.cgrand.enlive-html :refer [defsnippet html html-snippet
                                            text select sniptest content
                                            clone-for emit* do-> append
                                            set-attr first-child]]
            [net.cgrand.enlive-html :as h]
            [triweb.template :refer [find-tmpl slurp-tmpl]]))

(defn login-form []
  (html-snippet (slurp-tmpl "login.html")))

(h/deftemplate auth (find-tmpl "admin.html") [somearg]
  [:div.main] (h/content (login-form)))

