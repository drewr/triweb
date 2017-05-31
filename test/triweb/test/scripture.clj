(ns triweb.test.scripture
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all])
  (:require [triweb.scripture :as scripture]
            :reload-all))

(deftest text-ranges
  (is (= "2 Corinthians 1:1-2:5"
         (scripture/unparse [{:gte 47001001
                              :lte 47002005}])))
  (is (= "2 Corinthians 1:1-2:5; 5:18-21"
         (scripture/unparse [{:gte 47001001
                              :lte 47002005}
                             {:gte 47005018
                              :lte 47005021}])))
  (is (= "2 Corinthians 1:1-2:5; Genesis 1:1-2"
         (scripture/unparse [{:gte 47001001
                              :lte 47002005}
                             {:gte 1001001
                              :lte 1001002}])))
  (is (= "2 Corinthians 1:1-2:5; Genesis 1:1"
         (scripture/unparse [{:gte 47001001
                              :lte 47002005}
                             {:gte 1001001
                              :lte 1001001}]))))

(deftest parse
  (doseq [x (->> (file-seq (io/file "search-tmp/source"))
                 (filter #(.endsWith (str %) "json"))
                 (map #(:scripture (cheshire.core/decode (slurp %) true)))
                 (into #{})
                 sort
                 (take 10))]
    (is (= :foo (scripture/parse x)) x)))
