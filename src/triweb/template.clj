(ns triweb.template
  (:require [carica.core :refer [config]]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [me.raynes.cegdown :as markdown]
            [net.cgrand.enlive-html :as html]
            [net.cgrand.enlive-html :refer [deftemplate html-content]]
            [ring.util.response :as r]
            [triweb.template.nav :as nav])
  (:import (java.io File)))

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
  (or (if-let [url (io/resource name)]
        name)
      (if (.isFile (file name))
        (file name))))

(defn find-tmpl*
  "Find path to tempate.  If it's a local file return File object.  If
  it's a resource return relative string."
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

(deftemplate home (find-tmpl "home.html") [])

(deftemplate interior (find-tmpl "interior.html") [nav c]
  [:div.nav] (html-content nav)
  [:#content] (html-content c))

(defn append-index-if-slash [path]
  (if (.endsWith path "/")
    (str (file path "index.html"))
    path))

(defn render [uri]
  (if (= uri "/")
    (home)
    (let [uri (append-index-if-slash uri)
          uri (.replace uri ".html" ".txt")]
      (when-let [txt (slurp-tmpl uri)]
        (interior (nav/html )
         (markdown/to-html txt))))))

(defn wrap-template [app]
  (fn [req]
    (if-let [html (render (:uri req))]
      (-> (r/response html)
          (r/content-type "text/html")
          (r/charset "UTF-8"))
      (app req))))
