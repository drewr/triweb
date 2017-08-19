(ns triweb.scripture
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [instaparse.core :as i]))

(def books
  [[1  "Genesis"]
   [2  "Exodus"]
   [3  "Leviticus"]
   [4  "Numbers"]
   [5  "Deuteronomy"]
   [6  "Joshua"]
   [7  "Judges"]
   [8  "Ruth"]
   [9  "1 Samuel"]
   [10  "2 Samuel"]
   [11  "1 Kings"]
   [12  "2 Kings"]
   [13  "1 Chronicles"]
   [14  "2 Chronicles"]
   [15  "Ezra"]
   [16  "Nehemiah"]
   [17  "Esther"]
   [18  "Job"]
   [19  "Psalms"]
   [19  "Psalm"]
   [20  "Proverbs"]
   [21  "Ecclesiastes"]
   [22  "Song of Solomon"]
   [23  "Isaiah"]
   [24  "Jeremiah"]
   [25  "Lamentations"]
   [26  "Ezekiel"]
   [27  "Daniel"]
   [28  "Hosea"]
   [29  "Joel"]
   [30  "Amos"]
   [31  "Obadiah"]
   [32  "Jonah"]
   [33  "Micah"]
   [34  "Nahum"]
   [35  "Habakkuk"]
   [36  "Zephaniah"]
   [37  "Haggai"]
   [38  "Zechariah"]
   [39  "Malachi"]
   [40  "Matthew"]
   [41  "Mark"]
   [42  "Luke"]
   [43  "John"]
   [44  "Acts"]
   [45  "Romans"]
   [46  "1 Cor"]
   [47  "2 Cor"]
   [46  "1 Corinthians"]
   [47  "2 Corinthians"]
   [48  "Galatians"]
   [49  "Ephesians"]
   [50  "Philippians"]
   [51  "Colossians"]
   [52  "1 Thes"]
   [53  "2 Thes"]
   [52  "1 Thessalonians"]
   [53  "2 Thessalonians"]
   [54  "1 Timothy"]
   [55  "2 Timothy"]
   [56  "Titus"]
   [57  "Philemon"]
   [58  "Hebrews"]
   [59  "James"]
   [60  "1 Pet"]
   [61  "2 Pet"]
   [60  "1 Peter"]
   [61  "2 Peter"]
   [62  "1 Jn"]
   [63  "2 Jn"]
   [64  "3 Jn"]
   [62  "1 John"]
   [63  "2 John"]
   [64  "3 John"]
   [65  "Jude"]
   [66  "Revelation"]])

(def book-name
  (into {} books))

(def book-number
  (into {} (map #(vector (second %) (first %)) books)))

(defn pad [v n]
  (format (str "%0" n "d") (Integer/parseInt (str v))))

(defn book-matches [s]
  (for [b (map second books)
        :when (re-find (re-pattern b) s)]
    b))

(defn contains-book? [s]
  (when (string? s)
    (some true?
          (for [b (keys book-number)]
            (.contains s b)))))

(defn make-book-range-maker [c1 v1 c2 v2]
  (fn [b]
    {:gte (Long/parseLong
           (str (pad b 2)
                (pad c1 3)
                (pad v1 3)))
     :lte (Long/parseLong
           (str (pad b 2)
                (pad c2 3)
                (pad v2 3)))}))

(defn strip-leading-zeros [n-str]
  (apply str (drop-while #(= \0 %) n-str)))

(defn decode
  "Add 1001001 to map containing zero or more references."
  [coll verse*]
  (let [verse (if (string? verse*)
                (Integer/parseInt verse*)
                verse*)
        f (fn [xs v]
            (->> xs (cons v) sort vec))
        [_ book chap ver] (re-find #"(\d{1,2})(\d{3})(\d{3})" (str verse))
        bn (book-name (Integer/parseInt book))
        ch (Integer/parseInt (strip-leading-zeros chap))
        ve (Integer/parseInt (strip-leading-zeros ver))]
    (update-in coll [bn ch] (fnil f []) ve)))

(defn decode-range [v1 v2]
  (-> (sorted-map) (decode v1) (cond->
                                   (not= v1 v2) (decode v2))))

(defn unparse [refs]
  (let [m (fn [v1 v2]
            (vector v1 v2))
        proc-vs
        (fn proc-vs [vs]
          (cond
            (vector? vs) (map proc-vs vs)
            (map? vs) (cond
                        ;; "Genesis"
                        (= {1 [1] 999 [999]} vs)
                        nil

                        ;; "Genesis 12"
                        (and (= 1 (count vs))
                             (= [1 999] (val (first vs))))
                        (str (key (first vs)))

                        ;; "Genesis 1:1"
                        (= 1 (count vs))
                        (str (key (first vs))
                             ":" (str/join "-" (val (first vs))))

                        ;; "Genesis 1-3"
                        (and (= 2 (count vs))
                             (= [999] (val (second vs))))
                        (str (key (first vs)) "-" (key (second vs)))

                        ;; "Genesis 1:1-2:1"
                        :else
                        (str (key (first vs))
                             ":" (first (val (first vs)))
                             "-" (key (second vs))
                             ":" (first (val (second vs)))))))
        proc-book (fn [[k v]]
                    (str k (when-let [strs (proc-vs v)]
                             (str " "
                                  (if (sequential? strs)
                                    (str/join "; " strs)
                                    strs)))))]
    (->> (apply merge-with
                m (map #(decode-range (:gte %) (:lte %)) refs))
         (map proc-book)
         (str/join "; "))))

(def grammar
  (-> "scripture/grammar.ebnf"
      io/resource
      slurp
      (str/replace "%%BOOKS%%" (->> books
                                    (map second)
                                    (map #(str "'" % "'"))
                                    (interpose "|")
                                    (apply str)))))

(def xforms
  {:a (fn [& refs]
        (apply concat refs))

   :bookref (fn [[_ book] & refs]
              (let [bn (book-number book)]
                (if refs
                  (map (fn [f]
                         (f bn))
                       (mapcat second refs))
                  [((make-book-range-maker 1 1 999 999) bn)])))

   :just-chapter (fn [ch]
                   [(make-book-range-maker ch 1 ch 999)])

   :chapter-range (fn [ch1 ch2]
                    [(make-book-range-maker ch1 1 ch2 999)])

   :multi-chapter-verse-range
   (fn [& a]
     [(apply make-book-range-maker a)])

   :chapter-then-verses
   (fn [ch vs]
     (let [f (fn [x]
               (cond
                 (number? x)
                 (make-book-range-maker ch x ch x)
                 (vector? x)
                 (let [[_ v1 v2] x]
                   (make-book-range-maker ch v1 ch v2))))]
       (map f (rest vs))))

   :verse (fn [[_ num]]
            (Integer/parseInt num))

   :chapter (fn [[_ num]]
              (Integer/parseInt num))

   })

(def parser
  (i/parser grammar))

(defn parse [s]
  (i/transform
   xforms (parser s)))
