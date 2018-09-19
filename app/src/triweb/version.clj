(ns triweb.version
  (:require [clojure.java.io :as io]))

(defn version-string []
  (-> "version.txt" io/resource slurp .trim))
