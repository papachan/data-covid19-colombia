(ns frontend.data
  (:require [frontend.date :as d]
            [clojure.string :as str]
            [goog.string.format]
            [goog.string :refer [format]])
  (:import goog.i18n.DateTimeFormat))


;; Colombian population
(def population 48759958)

(def formatter (goog.i18n.DateTimeFormat. "dd/MM/YYYY"))

(defn get-all-dates
  [dat]
  (->> dat
       (filter #(some #{"Fallecido" "fallecido"} %))
       (map #(d/parse-date (second %)))
       sort))

(defn get-series-by-status
  [dat]
  (let [labels {"recuperado" "recovered"
                "casa" "at home"
                "hospital" "hospitalized"
                "hospital uci" "ICU hospitalization"
                "fallecido" "deads"
                "recuperado (hospital)" "recovered in hospital"
                "n/a" "N/A"}
        statuses (->> dat
                      (map #(nth % 2))
                      (remove nil?)
                      (map clojure.string/lower-case)
                      frequencies)
        labels (->> statuses
                    (mapv #(labels (key %))))
        series (->> statuses
                    (mapv #(nth % 1)))]
    [series labels]))

(defn get-series-by-genres
  [dat]
  (let [fields {:F "Women" :M "Men"}
        genres (->> dat
                    (map #(clojure.string/upper-case (nth % 4)))
                    frequencies)
        series (->> genres
                    (mapv second))
        labels (->> genres
                    (mapv #(fields (keyword (first %)))))]
    [series labels]))

(defn get-last-date
  [dat]
  (->> dat
       (map #(d/parse-date (second %)))
       frequencies
       sort
       last
       val))

(defn cases-by-population
  [total]
  (let [result (float (* (/ total population) 100000))]
    (.toFixed result 2)))

(def map-fields-name
  (let [fields [:a_zeros_to_nine
                :b_tens
                :c_twenties
                :d_thirties
                :e_fourties
                :f_fifties
                :g_sixties
                :h_seventies
                :i_eighties
                :j_nineties]]
    (into (sorted-map)
          (map (fn [field n]
                 [field (format "%s a %s" n (+ n 9))])
               fields
               (range 0 100 10)))))

(defn get-segments-by-ages
  [dat]
  (let [segment (->> dat
                     (filter (fn [s] (= "Fallecido" (nth s 2))))
                     (map #(nth % 3))
                     (map js/parseInt)
                     (group-by #(cond (<= 0 % 9)   :a_zeros_to_nine
                                      (<= 10 % 19) :b_tens
                                      (<= 20 % 29) :c_twenties
                                      (<= 30 % 39) :d_thirties
                                      (<= 40 % 49) :e_fourties
                                      (<= 50 % 59) :f_fifties
                                      (<= 60 % 69) :g_sixties
                                      (<= 70 % 79) :h_seventies
                                      (<= 80 % 89) :i_eighties
                                      (>= % 90)    :j_nineties))
                     (map (fn [[k vs]]
                            {(map-fields-name k) (count vs)}))
                     (into (sorted-map)))]
    [(vals segment) (keys segment)]))

(defn reduce-sum
  ([coll] (reduce-sum coll [] 0))
  ([coll out acc]
   (if-let [in (seq coll)]
     (recur (rest in)
            (cons [(first (first in)) (+ (second (first in)) acc)] out)
            (+ (second (first in)) acc))
     (reverse out))))

(def fn-unparse (fn [[k v]] [(.format formatter k) v]))
(def fn-parse (fn [[k v]] [(d/parse-date k) v]))

(defn limit-by-date
  [data]
  (let [starting-date (js/Date. "2020-03-16")
        fmt (goog.i18n.DateTimeFormat. "dd MMM")
        unparse (fn [[k v]] [(str/lower-case (.format fmt k)) v])]
    (->> data
         (map fn-parse)
         (filter (fn [[k v]]
                   (> (.getTime k) (.getTime starting-date))))
         sort
         (mapv unparse))))

(defn deltas
  [coll]
  (vec (concat '(0) (map - (rest coll) coll))))

(defn get-accumulate-tests
  [dat]
  (->> dat
       rest
       (mapv :accumulate)))

(defn process-data
  [data]
  (let [labels (->> data
                    :cases
                    limit-by-date
                    (mapv #(first (clojure.string/split (first %) #"/"))))
        series1 (->> data
                     :cases
                     reduce-sum
                     limit-by-date
                     (mapv second))
        series2 (->> data
                     :deaths
                     reduce-sum
                     limit-by-date
                     (mapv second))]
    [labels series1 series2]))

(defn get-last-test
  [num]
  (->> num
       (map #(js/parseInt (:accumulate %)))
       last))

(defn get-last-delta
  [num]
  (->> num
       (map #(js/parseInt (:accumulate %)))
       deltas
       last))
