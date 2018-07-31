(ns triweb.load
  (:gen-class)
  (:require [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as spec.test]
            [environ.core :as env]
            [triweb.elasticsearch :as es]
            [triweb.http :as http]
            [triweb.media :as media]
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
  (let [conn (es/connect (env/env :es-url))]
    (media/load-sermons-in-es
     (env/env :sermons-file) conn (env/env :es-index))))

