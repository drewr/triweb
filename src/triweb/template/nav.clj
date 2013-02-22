(ns triweb.template.nav
  (:require [net.cgrand.enlive-html :refer [defsnippet html html-snippet
                                            text select sniptest content
                                            clone-for emit* do-> append
                                            set-attr first-child]]
            [net.cgrand.enlive-html :as h]))

(def navhtml "
<ul class=\"ul-nav\">
<li class=\"menu left menu-block\">
<a href=\"\">About Us</a>
<ul style=\"display:none\">
<li><a href=\"something\">Something</a></li>
</ul>
</li>

<li class=\"menu leftctr menu-block\">
<a href=\"\">Life@Trinity</a>
<ul style=\"display:none\">
<li><a href=\"something\">Something</a></li>
</ul>
</li>

<li class=\"center menu-block\">&nbsp;</li>

<li class=\"menu rightctr menu-block\">
<a href=\"\">Ministry</a>
<ul style=\"display:none\">
<li><a href=\"something\">Something</a></li>
</ul>
</li>

<li class=\"menu right menu-block\">
<a href=\"\">Sermons</a>
<ul style=\"display:none\">
<li><a href=\"something\">Something</a></li>
</ul>
</li>
</ul>
")

(def navmd "
# one
## left
* Foo Biz
* /foo2test.html:Foo Bar!
* Something

# two
## leftctr
* bar1

# bar
## rightctr
* bar1

# baz
## right
* baz23
")

(defn title-to-url [title]
  (->> title
       (re-seq #"[A-Za-z0-9]")
       (apply str)
       .toLowerCase
       (format "/%s.html")))

(defn navitem [item]
  (let [[a b] (.split item ":")]
    (if (and a b)
      {:name b
       :href a}
      {:name a
       :href (title-to-url a)})))

(defn sections [ht]
  (->> #{[:h1] [:h2] [:ul]}
       (select (html-snippet ht))
       (partition 3)))

(defn navmenu [[menu k subs]]
  {(keyword (text k))
   {:menu (text menu)
    :class (text k)
    :options (->> [:li]
                  (select subs)
                  (map text)
                  (map navitem))}})

(defn navmap [ht]
  (->> (sections ht)
       (map navmenu)
       (apply merge)))

(defn menu [m]
  (->> (html
        [:li {:class (str "menu-head " (:class m))}
         [:a (:menu m)]
         [:ul {:class "menu"
               :style "display:none"}
          (for [o (:options m)]
            [:li
             [:a {:href (:href o)} (:name o)]])]])
       emit*
       (apply str)))

(defn make [navhtml center]
  (let [m (navmap navhtml)]
    (html-snippet
     "<ul class=\"ul-nav\">"
     (menu (:left m))
     (menu (:leftctr m))
     center
     (menu (:rightctr m))
     (menu (:right m))
     "</ul")))

(defn section-items [section]
  (->> (select section [:li])
       (map h/text)
       (map navitem)))

(defn match-section [uri sections]
  (let [promote (fn [item]
                  (if (= uri (:href item))
                    (assoc item :active true)
                    item))]
    (->> sections
         (map section-items)
         (some (fn [items]
                 (let [uriset (->> items (map :href) set)]
                   (when (uriset uri)
                     (map promote items))))))))

(defn sidebar [navhtml uri]
  (let [items (match-section uri (sections navhtml))]
    (->> items
         (map (fn [i]
                (html
                 [:li
                  [:a (merge
                       {:href (:href i)}
                       (when (:active i)
                         {:class "active"}))
                   (:name i)]])))
         (apply concat))))
