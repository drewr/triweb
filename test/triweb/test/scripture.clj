(ns triweb.test.scripture
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
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
  (is (= (scripture/parse
          "1 Corinthians 15:1,2-3,5; Romans 16:1-17:8; Genesis")
         [{:gte 46015001, :lte 46015001}
          {:gte 46015002, :lte 46015003}
          {:gte 46015005, :lte 46015005}
          {:gte 45016001, :lte 45017008}
          {:gte 1001001, :lte 1999999}]))
  (doseq [x (->> (file-seq (io/file "search/source"))
                 (filter #(.endsWith (str %) "json"))
                 (map #(:scripture (json/decode (slurp %) true)))
                 (into #{}))]
    (is (= x (scripture/parse (scripture/unparse x))))))
