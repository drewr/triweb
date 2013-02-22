(ns triweb.template
  (:require [carica.core :refer [config]]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [me.raynes.cegdown :as markdown]
            [net.cgrand.enlive-html :as h]
            [ring.util.response :as r]
            [triweb.template.nav :as nav])
  (:import (java.io File)))

(def centerhtml
  "<li class=\"center menu-head\"><a href=\"/\">&nbsp;</a></li>")

(defn ^String join
  ([x]
     (io/as-file x))
  ([x y]
     (File. ^File (io/as-file x) ^String y))
  ([x y & more]
     (reduce join (join x y) more)))

(defn file [& paths]
  (apply join paths))

(defn search [name]
  (or (io/resource name)
      (if (.isFile (file name))
        (file name))))

(defn find-tmpl*
  ([roots name]
     (->> roots
          (map #(str (file % name)))
          (map search)
          reverse
          (some identity)))
  ([roots name alt-ext]
     (find-tmpl* roots (s/replace name #"\..*?$"
                                  (str "." alt-ext)))))

(defn find-tmpl
  ([name]
     (find-tmpl* (config :template :roots) name)))

(defn slurp-tmpl [name]
  (when-let [tmpl (find-tmpl name)]
    (slurp tmpl)))

(defn slurp-markdown [name]
  (when-let [md (slurp-tmpl name)]
    (markdown/to-html md [:smartypants])))

(h/deftemplate home (find-tmpl "home.html") [nav]
  [:div.nav] (h/content nav))

(h/deftemplate interior (find-tmpl "interior.html") [nav side c]
  [:div.nav] (h/content nav)
  [:ul.sidenav] (h/content side)
  [:#content] (h/html-content c))

(defn append-index-if-slash [path]
  (if (.endsWith path "/")
    (str (file path "index.html"))
    path))

(defn render [uri]
  (let [navmd (slurp-markdown "home/_nav.txt")
        side (nav/sidebar (slurp-markdown "home/_nav.txt") uri)
        nav (nav/make navmd centerhtml)]
    (if (= uri "/")
      (home nav)
      (let [uri (append-index-if-slash uri)
            uri (.replace uri ".html" ".txt")]
        (when-let [page-html (slurp-markdown uri)]
          (interior nav side page-html))))))

(defn wrap-template [app]
  (fn [req]
    (if-let [html (render (:uri req))]
      (-> (r/response html)
          (r/content-type "text/html")
          (r/charset "UTF-8"))
      (app req))))
