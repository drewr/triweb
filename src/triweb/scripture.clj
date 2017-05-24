(ns triweb.scripture
  (:require [clojure.string :as str]))

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
  (-> (sorted-map) (decode v1) (decode v2)))

(defn refs-str [refs]
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
