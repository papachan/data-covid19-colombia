(ns frontend.data
  (:require [frontend.date :as d]
            [goog.string.format]
            [goog.string :refer [format]])
  (:import goog.i18n.DateTimeFormat))


(def population 48759958)

(def formatter (goog.i18n.DateTimeFormat. "dd/MM/yyyy"))

;; minimun date to display in chart
(def min-date (js/Date. "2020-03-25"))

(defn get-last-date
  [dat]
  (->> dat
       (map #(d/parse-date (second %)))
       frequencies
       sort
       last
       val))

(defn cases-by-population
  [num]
  (let [result (float (* (/ num population) 1000000))]
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
                     (filter #(some #{"Fallecido" "fallecido"} %))
                     (map #(nth % 6))
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

(defn sum
  [coll]
  (loop [in coll,
         sum 0,
         out ()]
    (if (seq in)
      (recur (rest in) (+ (second (first in)) sum)
             (cons [(first (first in)) (+ (second (first in)) sum)]
                   out))
      (reverse out))))

(def fn-parse (fn [[k v]] [(d/parse-date k) v]))

(defn process-data
  [data]
  (letfn [(get-frequencies
            [series-zero in]
            (->> in
                 (map #(.format formatter (d/parse-date (second %))))
                 (filter (fn [s]
                           (> (.getTime (d/parse-date s))
                              (.getTime min-date))))
                 frequencies
                 (apply merge series-zero)
                 (map fn-parse)
                 sort))]
    (let [fn-unparse (fn [[k v]] [(.format formatter k) v])
          ;; statuses (distinct (->> data
          ;;                         (map #(nth % 4))))
          dates-freq (->> data
                          (map second)
                          vec
                          frequencies)
          sorted-dates (->> dates-freq
                            (map fn-parse)
                            (filter (fn [s]
                                      (> (.getTime (first s)) (.getTime min-date))))
                            sort
                            (map fn-unparse))

          series-zero (->> dates-freq
                           (reduce-kv (fn [m k v] (assoc m (d/parse-date k) 0)) {})
                           (filter (fn [s]
                                     (> (.getTime (first s)) (.getTime min-date))))
                           sort
                           (map fn-unparse)
                           (into {}))

          deaths (->> data
                      (filter #(some #{"Fallecido"} %))
                      (get-frequencies series-zero))

          labels-fechas (->> sorted-dates
                             (map #(first (clojure.string/split (first %) #"/")))
                             vec)

          series1 (->> (get-frequencies series-zero data)
                       sum
                       (map second))

          series2 (->> deaths
                       sum
                       (map second))]
      [labels-fechas series1 series2])))
