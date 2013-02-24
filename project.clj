(defproject org.trinitynashville/triweb
  (-> "etc/version.txt" slurp read-string)
  :resources-paths ["etc" "resources"]
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.0-RC16"]
                 [compojure "1.1.5"]
                 [enlive "1.1.1" :exclude [org.clojure/clojure]]
                 [me.raynes/cegdown "0.1.0" :exclude [org.clojure/clojure]]
                 [ring "1.1.8"]
                 [sonian/carica "1.0.2"]]
  :plugins [[lein-ring "0.8.2"]]
  :ring {:handler triweb.handler/app})
