(ns frontend.pages
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent :refer [atom]]
            ["chartist" :as chartist]
            [frontend.events :as events]
            [frontend.date :as d])
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

(defn make-serie
  [coll]
  (loop [in coll,
         out {}]
    (if (seq in)
      (recur (rest in)
             (conj out {(first in) 0}))
      out)))

(defn show-chart
  [data]
  (let [options {:fullWidth true
                 :height "380px"
                 :lineSmooth false}]
    (chartist/Line. ".ct-chart" (clj->js data) (clj->js options))))

(defn chart-component
  []
  (let [data @(re-frame/subscribe [::events/data])]
    (when-not (empty? data)
      (let [formatter (goog.i18n.DateTimeFormat. "dd/MM/yyyy")
            contamined (map (fn [[k v]] [(.format formatter k)
                                         v]) (sort (map (fn [[k v]] [(d/parse-date k) v]) (frequencies (into [] (map #(nth % 1) data))))))
            series-fechas (into {} (map (fn [[k v]] {k 0}) contamined))
            res (sum contamined)

            deaths (frequencies (into [] (map #(.format formatter (d/parse-date (nth % 1))) (filter #(some #{"Fallecido"} %) data))))
            result (apply merge series-fechas deaths)

            series1 (into [] (map #(nth % 1) res))
            series2 (into [] (map (fn [[k v]] v) (sum (map (fn [[k v]] [k v]) (sort (map (fn [[k v]] [(d/parse-date k) v]) result))))))

            labels-fechas (into [] (map #(first (clojure.string/split (first %) #"/")) contamined))]
        (reagent/create-class
         {:component-did-mount #(show-chart {:labels labels-fechas
                                             :series [series1 series2]})
          :display-name        "chart-component"
          :reagent-render      (fn []
                                 [:div {:id "chart4"
                                        :class "ct-chart"}])})))))

(defn home
  []
  [:div
   [:header
    [:h1
     "Colombia Covid19 Report"]
    [:div
     {:className "author"}
     "Made by @papachan"]]
   [chart-component]])
