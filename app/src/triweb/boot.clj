(ns triweb.boot
  (:gen-class)
  (:require [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as spec.test]
            [environ.core :as env]
            [triweb.elasticsearch :as es]
            [triweb.http :as http]
            [triweb.time :as time]))

(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (locking #'println
       (println "uncaught exception on" (.getName thread) "at" (time/now))
       (println ex)
       (when-let [data (ex-data ex)]
         (when (:die data)
           (flush)
           (shutdown-agents)
           (System/exit 1)))))))

(defn -main [& args]
  (when (env/env :dev)
    (spec.test/instrument))
  (when-not (env/env :es-url)
    (log/info "ES_URL not populated, defaulting to http://localhost:9200"))
  (let [conn (es/connect (env/env :es-url "http://localhost:9200"))
        es-info (es/check conn)]
    (if-let [v (:number es-info)]
      (log/infof "connected to elasticsearch-%s" v)
      (log/errorf "something has happened connecting to elasticsearch: %s" (pr-str es-info)))
    (println "starting web server")
    (http/run (or (env/env :port) 9000) conn)))
