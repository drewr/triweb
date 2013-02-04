(ns triweb.template
  (:require [net.cgrand.enlive-html :refer [deftemplate html-content]]
            [me.raynes.cegdown :as markdown]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [carica.core :refer [config]]))

(defn find-tmpl*
  ([roots name]
     (when-let [tmpl (->> roots
                          (map #(io/file % name))
                          (map str)
                          (map io/resource)
                          first)]
       (.getPath tmpl)))
  ([roots name alt-ext]
     (find-tmpl roots (s/replace name #"\..*?$"
                                 (str "." alt-ext)))))

(defn find-tmpl
  ([name]
     (find-tmpl* (config :template :roots) name)))

(defn slurp-tmpl [name]
  (when-let [tmpl (find-tmpl name)]
    (slurp tmpl)))

(deftemplate t (find-tmpl "base.html") [content]
  [:#content] (html-content content))

(defn render-template [uri]
  (let [uri (if (.endsWith uri "/")
              (str (io/file uri "index.html"))
              uri)]
    (t (slurp-tmpl uri))))

(defn render-markdown [uri]
  (let [uri (if (.endsWith uri "/")
              (str (io/file uri "index.html"))
              uri)]
    (when-let [txt (slurp-tmpl (.replace uri ".html" ".txt"))]
      (t (markdown/to-html txt)))))


(defn wrap-template [app]
  (fn [req]
    (prn "****tmpl" req)
    (app req)))
