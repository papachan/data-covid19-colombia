(ns frontend.ui
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent :refer [atom]]
            ["chartist" :as chartist]
            ["react-chartist" :default react-graph]
            [frontend.data :as data :refer [process-data]]
            [goog.string :refer [format]]))


(defn row
  [{:keys [title value]}]
  [:div
   {:className "row"}
   title
   [:span
    {:className "highlighted"}
    value]])

(defn block-stats
  [{:keys [title value style]} data]
  [:div
   {:className "block-stats"}
   [:div
    {:className "title"}
    title]
   [:div
    {:className style}
    value]])

(defn show-chart
  [data]
  (let [options {:fullWidth true
                 :height "420px"
                 :lineSmooth false}]
    (chartist/Line. ".ct-chart" (clj->js data) (clj->js options))))

(defn chart-component
  [{:keys [data title]}]
  (when-not (empty? data)
    (let [[labels-fechas
           series1
           series2] (process-data data)]
      (reagent/create-class
       {:component-did-mount #(show-chart {:labels labels-fechas
                                           :series [series1 series2]})
        :display-name        "chart-component"
        :reagent-render      (fn []
                               [:div
                                [:div {:id "chart4"}
                                 [:div {:className "title"} title]
                                 [:div {:class "ct-chart"}]]])}))))

(defn chart-bars-component3
  [{:keys [data title]}]
  (when-not (empty? data)
    (let [formatter (goog.i18n.DateTimeFormat. "dd")
          series (->> data
                      rest
                      (map #(js/parseInt (clojure.string/replace (:accumulate %) #"," ""))))
          labels (->> data
                      rest
                      (map #(.format formatter (js/Date. (:date %)))))]
      [:div
       {:id "chart7"}
       [:div {:className "title"} title]
       [:> react-graph
        {:data {:labels labels
                :series [series]}
         :type "Bar"}]])))


(def map-fields-name
  (let [fields [:1_zeros_to_nine
                :2_tens
                :3_twenties
                :4_thirties
                :5_fourties
                :6_fifties
                :7_sixties
                :8_seventies
                :9_eighties
                :10_nineties]
        texts (vec (map #(format "%s a %s" % (+ % 9)) (range 0 100 10)))]
    (apply hash-map (interleave fields texts))))

(defn chart-bars-component2
  [{:keys [data title]}]
  (when-not (empty? data)
    (let [segment-by-age (->> data
                              (filter #(some #{"Fallecido" "fallecido"} %))
                              (map #(nth % 6))
                              (map js/parseInt)
                              (group-by #(cond (<= 0 % 9)   :1_zeros_to_nine
                                               (<= 10 % 19) :2_tens
                                               (<= 20 % 29) :3_twenties
                                               (<= 30 % 39) :4_thirties
                                               (<= 40 % 49) :5_fourties
                                               (<= 50 % 59) :6_fifties
                                               (<= 60 % 69) :7_sixties
                                               (<= 70 % 79) :8_seventies
                                               (<= 80 % 89) :9_eighties
                                               (>= % 90)    :10_nineties))
                              (map (fn [[k vs]]
                                     {(map-fields-name k) (count vs)}))
                              (into (sorted-map)))
          series (vals segment-by-age)
          labels (keys segment-by-age)
          options {:height "220px"}]
      [:div
       {:id "chart6"}
       [:div {:className "title"} title]
       [:> react-graph
        {:data {:labels labels
                :series [series]}
         :options options
         :type "Bar"}]])))

(defn chart-bars-component
  [{:keys [data title]}]
  (when-not (empty? data)
    (let [labels {"recuperado" "recovered"
                  "casa" "at home"
                  "hospital" "hospitalized"
                  "hospital uci" "ICU hospitalization"
                  "fallecido" "deads"
                  "recuperado (hospital)" "recovered in hospital"
                  "n/a" "N/A"}
          statuses (->> data
                        (map #(nth % 4))
                        (remove nil?)
                        (map clojure.string/lower-case)
                        frequencies)
          labels-fechas (->> statuses
                             (map #(labels (key %)))
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
       [:div {:className "title"} title]
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
   " papachan - "
   [:a {:href "https://twitter.com/papachan"} "Twitter"]
   " "
   [:a {:href "https://github.com/papachan/data-covid19-colombia"} "Github"]])
