(ns triweb.template.home
  (:require [net.cgrand.enlive-html :refer [defsnippet html html-snippet
                                            text select sniptest content
                                            clone-for emit* do-> append
                                            set-attr first-child]]
            [net.cgrand.enlive-html :as h]
            [triweb.template :refer [find-tmpl slurp-tmpl]]))

(h/deftemplate home (find-tmpl "home.html") [name]
  [:title] (h/html-content (str "Hi, " name "!")))
