(ns triweb.time
  (:require [clj-time.core :as t]
            [clj-time.format :as f]))

(defn now []
  (f/unparse (f/formatter :date-time) (t/now)))

(defn current-yyyy-mm-dd []
  (f/unparse (f/formatter :year-month-day) (t/now)))

(defn before? [ymd1 ymd2]
  (t/before?
   (f/parse (f/formatter :year-month-day) ymd1)
   (f/parse (f/formatter :year-month-day) ymd2)))

(defn parse-iso [iso-string]
  (f/parse (f/formatter :date-time) iso-string))

(defn elapsed-secs
  ([iso-string]
   (elapsed-secs iso-string (t/now)))
  ([iso-string now]
   (try
     (t/in-seconds
      (t/interval
       (parse-iso iso-string) now))
     (catch IllegalArgumentException _
       -1))))
