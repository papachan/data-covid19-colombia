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

(defn process-data
  [data]
  (let [min-date "2020-03-25"
        formatter (goog.i18n.DateTimeFormat. "dd/MM/yyyy")
        fn-parse (fn [[k v]] [(d/parse-date k) v])
        fn-unparse (fn [[k v]] [(.format formatter k) v])
        dates-freq (->> data
                        (map second)
                        vec
                        frequencies)
        sorted-dates (->> dates-freq
                          (map fn-parse)
                          (filter (fn [s]
                                   (> (.getTime (first s)) (.getTime (js/Date. min-date)))))
                          sort
                          (map fn-unparse))

        series-zero (->> dates-freq
                         (reduce-kv (fn [m k v] (assoc m (d/parse-date k) 0)) {})
                         (filter (fn [s]
                                   (> (.getTime (first s)) (.getTime (js/Date. min-date)))))
                         sort
                         (map fn-unparse)
                         (into {}))

        contamined (->> data
                        (remove #(some #{"Recuperado"
                                         "recuperado"
                                         ;; "Fallecido"
                                         ;; "fallecido"
                                         ;; "Recuperado (Hospital)"
                                         } %))
                        (map #(.format formatter (d/parse-date (second %))))
                        frequencies
                        (apply merge series-zero)
                        (map fn-parse)
                        sort)

        deaths (->> data
                    (filter #(some #{"Fallecido" "fallecido"} %))
                    (map #(.format formatter (d/parse-date (second %))))
                    frequencies
                    (apply merge series-zero)
                    (map fn-parse)
                    sort)

        labels-fechas (->> sorted-dates
                           (map #(first (clojure.string/split (first %) #"/")))
                           vec)

        series1 (->> contamined
                     (filter (fn [s] (> (.getTime (first s)) (.getTime (js/Date. min-date)))))
                     sum
                     (map second))
        series2 (->> deaths
                     (filter (fn [s] (> (.getTime (first s)) (.getTime (js/Date. min-date)))))
                     sum
                     (map second))]
    [labels-fechas series1 series2]))
