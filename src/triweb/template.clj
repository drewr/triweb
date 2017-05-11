(ns triweb.template
  (:require [carica.core :refer [config]]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [compojure.core :refer [defroutes GET ANY]]
            [compojure.route :as route]
            [me.raynes.cegdown :as markdown]
            [net.cgrand.enlive-html :as h]
            [ring.util.response :as r]
            [triweb.log :refer [log]]
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

(defn snip-tmpl [name]
  (-> name
      slurp-markdown
      (or "&nbsp;")
      h/html-snippet))

(h/defsnippet footer (find-tmpl "home.html")
  [:footer] [ver]
  [:span.softwareversion] (h/html-content ver))

(h/deftemplate home (find-tmpl "home.html") [nav footer hero
                                             sunday upcoming join]
  [:div.nav] (h/content nav)
  [:div.home-hero-copy] (h/content hero)
  [:div.home-left] (h/content (or sunday (h/html-snippet "&nbsp;")))
  [:div.home-middle] (h/content upcoming)
  [:div.home-right] (h/content join)
  [:footer] (h/substitute footer))

(h/deftemplate interior (find-tmpl "interior.html") [nav footer side c]
  [:div.nav] (h/content nav)
  [:ul.sidenav] (h/content side)
  [:#content] (h/html-content c)
  [:footer] (h/substitute footer))

(defn append-index-if-slash [path]
  (if (.endsWith path "/")
    (str (file path "index.html"))
    path))

(defn render [uri]
  (let [navmd (slurp-markdown "home/_nav.txt")
        side (nav/sidebar (slurp-markdown "home/_nav.txt") uri)
        nav (nav/make navmd centerhtml)
        ftr (footer (-> "version.txt" io/resource slurp .trim))]
    (if (= uri "/")
      (home nav
            ftr
            (snip-tmpl "home/_hero.txt")
            (snip-tmpl "home2/_sunday.txt")
            (snip-tmpl "home2/_upcoming.txt")
            (snip-tmpl "home2/_join.txt"))
      (if (.endsWith uri ".html")
        (let [uri (append-index-if-slash uri)
              uri (.replace uri ".html" ".txt")]
          (when-let [page-html (slurp-markdown uri)]
            (interior nav ftr side page-html)))))))

(defn wrap-template [app]
  (fn [req]
    (let [uri (or (:path-info req)
                  (:uri req))]
      (if-let [html (render uri)]
        (-> (r/response html)
            (r/content-type "text/html")
            (r/charset "UTF-8"))
        (app req)))))

(defroutes handler
  (GET "/" [] "home")
  (ANY "/upload" req (let []
                       (clojure.pprint/pprint req)
                       {:headers {"Access-Control-Allow-Origin" "*"}
                        :body ((h/template (find-tmpl "upload.html") []))}))
  (route/resources "/" {:root "static"})
  (route/not-found "Page does not exist"))
