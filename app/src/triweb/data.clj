(ns triweb.data
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (org.apache.commons.codec.digest DigestUtils)))

(defn md5sum-file [filename]
  (DigestUtils/md5Hex
   (io/input-stream
    (io/file filename))))

(defn strip-trailing [ch s]
  (if (and (string? s)
           (str/ends-with? (str/trimr s) ch))
    (let [pat (re-pattern (str "(.*?)[" ch "]+$"))
          [_ m] (re-find pat s)]
      m)
    s))
