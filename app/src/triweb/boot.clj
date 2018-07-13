(ns triweb.boot
  (:gen-class)
  (:require [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as spec.test]
            [environ.core :as env]
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
  (when (System/getenv "DEV")
    (spec.test/instrument))
  (println "starting web server")
  (http/run (or (env/env :port) 9000)))
