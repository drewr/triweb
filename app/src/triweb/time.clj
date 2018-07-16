(ns triweb.time
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.periodic :as p]
            [clj-time.predicates :as pr]))

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

(defn parse-ymd [s]
  (try
    (f/parse (f/formatter :date) s)
    (catch Exception _)))

(defn next-sunday [date]
  (->> (p/periodic-seq date (t/days 1))
       (filter pr/sunday?)
       first))

(defn previous-possible-date
  "Given a date, that may not be in the coll, look for the next
  possible date in the coll."
  [date coll]
  (loop [xs (filter #(instance? org.joda.time.DateTime %) coll)]
    (if (t/within? (t/interval (first xs) (second xs)) date)
      (first xs)
      (recur (drop 1 xs)))))

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
