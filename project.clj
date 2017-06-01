(defproject org.trinitynashville/triweb
  (-> "etc/version.txt" slurp .trim)
  :resource-paths ["etc" "resources"]
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [amazonica "0.3.101"]
                 [circleci/clj-yaml "0.5.5" :exclusions [[org.clojure/clojure]]]
                 [com.mpatric/mp3agic "0.9.0"]
                 [commons-codec/commons-codec "1.10"]
                 [compojure "1.5.2" :exclusions [org.clojure/tools.reader]]
                 [elastic/elasticsearch-clojure "0.99.5"]
                 [enlive "1.1.1" :exclude [org.clojure/clojure]]
                 [environ "1.1.0"]
                 [instaparse "1.4.7"]
                 [me.raynes/cegdown "0.1.0" :exclusions [org.clojure/clojure]]
                 [org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/core.memoize "0.5.8"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/spec.alpha "0.1.94"]
                 [ring "1.5.1"]
                 [sonian/carica "1.0.2"]
                 ]
  :injections [(require 'clojure.pprint)]
  :profiles {:dev {:env {:es-url "http://localhost:9200" }}}
  :plugins [[lein-ring "0.11.0"]
            [lein-environ "1.1.0"]]
  :ring {:handler triweb.handler/app})
