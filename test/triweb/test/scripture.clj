(ns triweb.test.scripture
  (:require [clojure.test :refer :all])
  (:require [triweb.scripture :as scripture]
            :reload-all))

(deftest text-ranges
  (is (= "2 Corinthians 1:1-2:5"
         (scripture/refs-str [{:gte 47001001
                               :lte 47002005}])))
  (is (= "2 Corinthians 1:1-2:5; 5:18-21"
         (scripture/refs-str [{:gte 47001001
                               :lte 47002005}
                              {:gte 47005018
                               :lte 47005021}])))
  (is (= "2 Corinthians 1:1-2:5; Genesis 1:1-2"
         (scripture/refs-str [{:gte 47001001
                               :lte 47002005}
                              {:gte 1001001
                               :lte 1001002}]))))
