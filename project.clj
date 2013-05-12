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
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/data.xml "0.0.7"]
                 [ring "1.1.8"]
                 [sonian/carica "1.0.2"]
                 [org.clojure/core.memoize "0.5.3"]]
  :profiles {:dev {:dependencies [[ring/ring-devel "1.1.8"]]}}
  :plugins [[lein-ring "0.8.3"]]
  :ring {:handler triweb.handler/app})
