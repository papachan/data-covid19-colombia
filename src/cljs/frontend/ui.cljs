(ns frontend.ui
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent :refer [atom]]
            ["chartist" :as chartist]
            ["react-chartist" :default react-graph]
            [frontend.data :as data :refer [process-data]]))


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

(defn chart-bars-component2
  [data]
  (when-not (empty? data)
    (let [series (->> data
                      (filter #(some #{"Fallecido" "fallecido"} %))
                      (map #(js/parseInt (nth % 6)))
                      (group-by (fn [v] (< v 40)))
                      vals
                      (map count)
                      (into []))
          labels ["deads over 40 years old"
                  "deads less than 40 years old"]
          options {:height "220px"}]
      [:div
       {:id "chart6"}
       [:> react-graph
        {:data {:labels labels
                :series [series]}
         :options options
         :type "Bar"}]])))

(defn chart-bars-component
  [data]
  (when-not (empty? data)
    (let [labels {"recuperado" "recovered"
                  "casa" "at home"
                  "hospital" "hospitalized"
                  "hospital uci" "ICU hospitalization"
                  "fallecido" "deads"
                  "recuperado (hospital)" "recovered in hospital"
                  "n/a" "N/A"}
          statuses (->> data
                        (map #(clojure.string/lower-case (nth % 4)))
                        vec
                        frequencies)
          labels-fechas (->> statuses
                             (map #(labels (first %)))
                             vec)
          series1 (->> statuses
                       (map #(nth % 1))
                       vec)
          options {:height "210px"
                   :reverseData true
                   :horizontalBars true
                   :axisY {:offset 120}}]
      [:div
       {:id "chart5"}
       [:> react-graph
        {:data {:labels labels-fechas
                :series [series1]}
         :options options
         :type "Bar"}]])))

(defn footer
  []
  [:footer
   "©  "
   (.getFullYear (js/Date.))
   " @papachan - "
   [:a {:href "https://twitter.com/papachan"} "Twitter"]
   " "
   [:a {:href "https://github.com/papachan/data-covid19-colombia"} "Github"]])
