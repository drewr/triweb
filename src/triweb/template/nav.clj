(ns triweb.template.nav
  (:require [net.cgrand.enlive-html :refer [defsnippet html html-snippet
                                            text select sniptest content
                                            clone-for emit* do-> append
                                            set-attr first-child]]
            [me.raynes.cegdown :as markdown]
            ))

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

(defn navitem [item]
  (let [[a b] (.split item ":")]
    (if (and a b)
      {:name b
       :href a}
      {:name a
       :href (->> a
                  (re-seq #"[A-Za-z0-9]")
                  (apply str)
                  .toLowerCase
                  (format "/%s.html"))})))

(defn navmenu [[menu k subs]]
  {(text k)
   {:menu (text menu)
    :class (text k)
    :options (->> [:li]
                  (select subs)
                  (map text)
                  (map navitem))}})

(defn navmap [ht]
  (->> #{[:h1] [:h2] [:ul]}
       (select (html-snippet ht))
       (partition 3)
       (map navmenu)
       (apply merge)))



(defn menu [m]
  (html
   [:li {:class (str "menu " (:class m))}
    [:a (:menu m)]
    [:ul {:style "display:none"}
     (for [o (:options m)]
       [:li
        [:a {:href (:href o)} (:name o)]])]]))


(->> (get (navmap (markdown/to-html navmd)) "left")
     menu
     emit*
     (apply str))
