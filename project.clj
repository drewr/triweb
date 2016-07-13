(defproject org.trinitynashville/triweb
  (-> "etc/version.txt" slurp .trim)
  :resource-paths ["etc" "resources"]
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[commons-codec/commons-codec "1.7"]
                 [compojure "1.1.5"]
                 [clj-http "0.7.0"]
                 [enlive "1.1.1" :exclude [org.clojure/clojure]]
                 [me.raynes/cegdown "0.1.0" :exclude [org.clojure/clojure]]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/data.xml "0.0.7"]
                 [ring "1.3.1"]
                 [sonian/carica "1.0.2"]
                 [org.clojure/core.memoize "0.5.3"]]
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler triweb.handler/app})
