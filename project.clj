(defproject org.trinitynashville/triweb
  (-> "etc/version.txt" slurp .trim)
  :resource-paths ["etc" "resources"]
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[circleci/clj-yaml "0.5.5" :exclusions [[org.clojure/clojure]]]
                 [clj-time "0.13.0"]
                 [commons-codec/commons-codec "1.10"]
                 [compojure "1.5.2" :exclusions [org.clojure/tools.reader]]
                 ;;[clj-http "2.3.0"]
                 [elastic/elasticsearch-clojure "0.99.5-20170220.205659-11"]
                 [enlive "1.1.1" :exclude [org.clojure/clojure]]
                 [me.raynes/cegdown "0.1.0" :exclusions [org.clojure/clojure]]
                 [org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/data.xml "0.0.8"]
                 [ring "1.5.1"]
                 [sonian/carica "1.0.2"]
                 [org.clojure/core.memoize "0.5.8"]]
  :plugins [[lein-ring "0.11.0"]]
  :ring {:handler triweb.handler/app})
