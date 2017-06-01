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
   [46  "1 Corinthians"]
   [47  "2 Corinthians"]
   [48  "Galatians"]
   [49  "Ephesians"]
   [50  "Philippians"]
   [51  "Colossians"]
   [52  "1 Thessalonians"]
   [53  "2 Thessalonians"]
   [54  "1 Timothy"]
   [55  "2 Timothy"]
   [56  "Titus"]
   [57  "Philemon"]
   [58  "Hebrews"]
   [59  "James"]
   [60  "1 Peter"]
   [61  "2 Peter"]
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
            (map? vs) (if (= 1 (count vs))
                        (str (key (first vs))
                             ":" (str/join "-" (val (first vs))))
                        (str (key (first vs))
                             ":" (first (val (first vs)))
                             "-" (key (second vs))
                             ":" (first (val (second vs)))))))
        proc-book (fn [[k v]]
                    (str k " " (let [strs (proc-vs v)]
                                 (if (sequential? strs)
                                   (str/join "; " strs)
                                   strs))))]
    (->> (apply merge-with
                m (map #(decode-range (:gte %) (:lte %)) refs))
         (map proc-book)
         (str/join "; "))))

(defn match-refs [s]
  (re-find #"([0-9]+):([0-9-,]+)" s))

(defn encode-verse [book chapter verse]
  (str
   (pad (book-number book) 2)
   (pad chapter 3)
   (pad verse 3)))

(defn parse-verse-range [book ch s]
  (let [[start end] (str/split s #"-")]
    [(encode-verse book ch start)
     (if end
       (encode-verse book ch end)
       (encode-verse book ch start))]))

(defn parse-verse-range-different-chapters [book s]
  (let [[start end] (str/split s #"-")
        [c1 v1] (str/split start #":")
        [c2 v2] (str/split end #":")]
    [(encode-verse book c1 v1)
     (encode-verse book c2 v2)]))

(defn parse-verses
  "A range (1-5), a single verse number (2), or a nil (the whole
  chapter)."
  [book ch s]
  (if s
    (map (partial parse-verse-range book ch) (str/split s #", ?"))
    ;; Whole chapter, might need to look up the real value, but not
    ;; for searching
    [[(encode-verse book ch 1) (encode-verse book ch 999)]]
    ))

(def verses-same-chapter
  #"[0-9]+:[0-9-,]+")

(def verses-different-chapter
  #"[0-9]+:[0-9]+-[0-9]+:[0-9]+")

(defn parse-ref [book s]
  (cond
    (re-find verses-different-chapter s)
    (parse-verse-range-different-chapters book s)
    (re-find verses-same-chapter s)
    (let [[chap vs] (str/split s #":")]
      (parse-verses book chap vs))))

(defn parse [s]
  (when (contains-book? s)
    (let [n (count (book-matches s))
          f (fn [acc section]
              (let [book (first (book-matches section))
                    curr (or book (:current acc))]
                (merge acc
                       {:current curr
                        curr (conj
                              (acc curr)
                              (parse-ref
                               curr
                               (.trim (str/replace section curr ""))))})))]
      (->>
       (-> (reduce f {:current nil} (map #(.trim %) (str/split s #"; ?")))
           (dissoc :current))
       (mapcat val)
       (apply concat)
       (map (fn [[gte lte]]
              {:gte gte :lte lte}))))))

(def grammar
  (-> "scripture/grammar.ebnf"
      io/resource
      slurp
      (str/replace "%%BOOKS%%" (->> books
                                    (map second)
                                    (map #(str "'" % "'"))
                                    (interpose "|")
                                    (apply str)))))

(def parser
  (i/parser grammar))

(defn parse [s]
  (parser s))
