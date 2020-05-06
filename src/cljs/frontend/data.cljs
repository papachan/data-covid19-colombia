(ns frontend.data
  (:require [frontend.date :as d])
  (:import goog.i18n.DateTimeFormat))

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

(def formatter (goog.i18n.DateTimeFormat. "dd/MM/yyyy"))

;; minimun date to display in chart
(def min-date (js/Date. "2020-03-25"))


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
