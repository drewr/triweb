(ns triweb.data
  (:require [clojure.java.io :as io])
  (:import (org.apache.commons.codec.digest DigestUtils)))

(defn md5sum-file [filename]
  (DigestUtils/md5Hex
   (io/input-stream
    (io/file filename))))
