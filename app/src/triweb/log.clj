(ns triweb.log
  (:require [clojure.java.io :as io]))

(defn log
  ([s]
   (log "%s" s))
  ([fmt & args]
   (locking log
     (with-open [out (java.io.PrintWriter.
                      (io/writer "/tmp/ring.log" :append true))]
       (.println out (apply format fmt args)))
     (flush))))
