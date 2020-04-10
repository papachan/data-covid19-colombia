(ns frontend.pages
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent :refer [atom]]
            ["chartist" :as chartist]
            ["react-chartist" :default react-graph]
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

(defn process-data
  [data]
  (let [formatter (goog.i18n.DateTimeFormat. "dd/MM/yyyy")
        fn-parse (fn [[k v]] [(d/parse-date k) v])
        fn-unparse (fn [[k v]] [(.format formatter k) v])
        contamined (->> data
                        (map #(nth % 1))
                        vec
                        frequencies
                        (map fn-parse)
                        sort
                        (map fn-unparse))
        series-fechas (->> contamined
                           (map (fn [[k v]] {k 0}))
                           (into {}))
        deaths (->> data
                    (filter #(some #{"Fallecido" "fallecido"} %))
                    (map #(.format formatter (d/parse-date (nth % 1))))
                    frequencies)
        series-deaths (->> (apply merge series-fechas deaths)
                           (map fn-parse)
                           sort)
        labels-fechas (into [] (map #(first (clojure.string/split (first %) #"/")) contamined))
        series1 (->> (sum contamined)
                     (map #(nth % 1))
                     vec)
        series2 (->> (sum series-deaths)
                     (map (fn [[k v]] v))
                     vec)]
    [labels-fechas series1 series2]))

(defn show-chart
  [data]
  (let [options {:fullWidth true
                 :height "420px"
                 :lineSmooth false}]
    (chartist/Line. ".ct-chart" (clj->js data) (clj->js options))))

(defn chart-component
  [data]
  (when-not (empty? data)
    (let [[labels-fechas
           series1
           series2] (process-data data)]
      (reagent/create-class
       {:component-did-mount #(show-chart {:labels labels-fechas
                                           :series [series1 series2]})
        :display-name        "chart-component"
        :reagent-render      (fn []
                               [:div {:id "chart4"
                                      :class "ct-chart"}])}))))

(defn chart-bars-component
  [data]
  (when-not (empty? data)
    (let [labels {"recuperado" "recuperados"
                  "casa" "en casa"
                  "hospital" "internados"
                  "hospital uci" "cuidados intensivos"
                  "fallecido" "fallecidos"
                  "recuperado (hospital)" "recuperados en hospital"}
          statuses (->> data
                        (map #(clojure.string/lower-case (nth % 4)))
                        (into [])
                        frequencies)
          labels-fechas (->> statuses
                             (map #(labels (nth % 0)))
                             (into []))
          series1 (->> statuses
                       (map #(nth % 1))
                       (into []))
          options {:height "220px"}]
      [:div
       {:id "chart5"}
       [:> react-graph
        {:data {:labels labels-fechas
                :series [series1]}
         :options options
         :type "Bar"}]])))

(defn block-stats
  [{:keys [title value]} data]
  [:div
   {:className "block-stats"}
   [:div
    {:className "title"}
    title]
   [:div
    {:className "stats"}
    value]])

(defn footer
  []
  [:footer
   "©  "
   (.getFullYear (js/Date.))
   " @papachan - "
   [:a {:href "https://twitter.com/papachan"} "Twitter"]
   " "
   [:a {:href "https://github.com/papachan/data-covid19-colombia"} "Github"]])

(defn home
  []
  (let [data @(re-frame/subscribe [::events/data])]
    [:<>
     [:header
      [:h1
       "Colombia Covid19 Report"]
      [:div
       {:className "description"}
       "Live report from Colombian data using clojurescript"]]
     [:div
      {:className "container"}
      [:div
       {:className "graph"}
       [chart-component data]
       [chart-bars-component data]]
      [:div
       {:id "stats"}
       [block-stats {:title "Number of deaths"
                     :value
                     (when (count data) (count (filter #(some #{"Fallecido" "fallecido"} %) data)))}]
       [block-stats {:title "Number of contamined"
                     :value
                     (when (count data)
                       (count (remove
                               (fn [s] (or (= "recuperado" s) (= "fallecido" s)))
                               (map #(clojure.string/lower-case (nth % 4)) data))))}]
       [block-stats {:title "Recovered"
                     :value
                     (when (count data) (count (filter #(some #{"Recuperado"} %) data)))}]]]
     [footer]]))
