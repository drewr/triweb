(defproject org.trinitynashville/triweb "$appVersion$"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [amazonica "0.3.101" :exclusions [joda-time
                                                   commons-logging
                                                   commons-codec/commons-codec]]
                 ;; [circleci/clj-yaml "0.5.5" :exclusions [[org.clojure/clojure]]]
                 ;; [clj-time "0.12.2"]
                 [com.mpatric/mp3agic "0.9.0"]
                 [commons-codec/commons-codec "1.10"]
                 [compojure "1.5.2" :exclusions [org.clojure/tools.reader
                                                 instaparse
                                                 ]]
                 [elastic/elasticsearch-clojure "0.99.8-20171031.194719-1"
                          :exclusions [com.fasterxml.jackson.core/jackson-core]]

                 [enlive "1.1.6" :exclude [org.clojure/clojure]]

                 [environ "1.1.0"]
                 [functionalbytes/sibiro "0.1.5"]
                 [instaparse "1.4.7"]
                 [me.raynes/cegdown "0.1.0" :exclusions [org.clojure/clojure]]
                 [org.clojure/clojure "$clojureVersion$"]
;;                 [org.clojure/core.memoize "0.5.8"]
                 [org.clojure/data.xml "0.0.8"]
;;                 [org.clojure/spec.alpha "0.1.123"]
                 [ring "1.5.1"]
                 [sonian/carica "1.0.2"]

                 ;; logging
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [net.logstash.logback/logstash-logback-encoder "4.7"
                       :exclusions [com.fasterxml.jackson.core/jackson-databind]]
                 [com.fasterxml.jackson.core/jackson-databind "2.8.2"]
                 ]
  :injections [(require 'clojure.pprint)]
  :profiles {:dev {:env {:es-url "http://localhost:9200" }}
             :package {:uberjar-name "$appUberJarBaseName$"}}
  :plugins [[lein-ring "0.11.0"]
            [lein-environ "1.1.0"]]
  :resource-paths ["resources" "lib"]
  ;; stupid :aot :all started hanging for no reason, I love computers
  :aot [triweb.boot
        triweb.http
       ]
  :ring {:handler triweb.http/app
         :uberwar-name "app.war"})
