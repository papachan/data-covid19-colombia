(ns frontend.ui
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r :refer [atom]]
            [frontend.data :as data :refer [process-data
                                            map-fields-name
                                            get-segments-by-ages
                                            get-series-by-genres]]
            ["chart.js" :refer [Chart]]
            ["react-chartjs-2" :as ReactChartjs2 :refer [Line]]
            [goog.string.format]
            [goog.string :refer [format]]))


(def line-chart (r/adapt-react-class (goog.object/get ReactChartjs2 "Line")))

(def doughnut-chart (r/adapt-react-class (goog.object/get ReactChartjs2 "Doughnut")))

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

;; show line chart with timeseries
(defn chart-component
  [{:keys [data title]}]
  (r/with-let [scaled? (atom false)
               type (atom "linear")]
    (let [[labels
           series1
           series2] (process-data data)
          btn-txt (if @scaled? "linear" "logarithm")]
      [:div
       [:div {:className "chart"}
        [:input
         {:type :button
          :className "input-btn"
          :value btn-txt
          :on-click (fn [e]
                      (swap! scaled? not)
                      (reset! type (if @scaled? "logarithmic" "linear")))}]
        [line-chart {:width   500
                     :height  280
                     :data    {:labels   labels
                               :datasets [{:label "covid cases"
                                           :data series1
                                           :fill false
                                           :borderColor "blue"}]}
                     :options {:legend {:position "top"}
                               :title {:display true
                                       :text title}
                               :scales {:xAxes [{:gridLines {:display false}}]
                                        :yAxes [{:type @type
                                                 :display true
                                                 :gridLines {:display false}}]}}}]]])))

(defn show-bar-component
  [{:keys [title label-name canvas-id labels series]}]
  (let [context (.getContext (.getElementById js/document canvas-id) "2d")
        chart-data {:type "bar"
                    :data {:labels labels
                           :datasets [{:data series
                                       :label label-name
                                       :backgroundColor "#FFCC00"
                                       :borderWidth 0}]}
                    :options {:legend {:position "top"}
                              :title {:display true
                                      :text title}
                              :scales {:xAxes [{:gridLines {:display false}}]
                                       :yAxes [{:gridLines {:display false}}]}
                              }}]
    (Chart. context (clj->js chart-data))))

(defn show-horizontal-chart
  [{:keys [data title canvas-id]}]
  (let [context (.getContext (.getElementById js/document canvas-id) "2d")
        labels {"recuperado" "recovered"
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
        labels (->> statuses
                    (map #(labels (key %)))
                    vec)
        series (->> statuses
                    (map #(nth % 1))
                    vec)
        chart-data {:type "horizontalBar"
                    :maintainAspectRatio false
                    :data {:labels labels
                           :datasets [{:data series
                                       :label "Number of cases"
                                       :backgroundColor "darkslateblue"
                                       :borderWidth 0}]}
                    :options {:legend {:position "top"}
                              :title {:display true
                                      :text title}
                              :scales {:xAxes [{:gridLines {:display false}}]
                                       :yAxes [{:gridLines {:display false}}]}
                              }}]
    (Chart. context (clj->js chart-data))))

(defn chart-bar-comp
  [{:keys [data title label-name]}]
  (let [[labels
         cases
         series] (process-data data)
        canvas-id 1]
    (r/create-class
     {:component-did-mount #(show-bar-component {:title title
                                                 :label-name label-name
                                                 :series series
                                                 :labels labels
                                                 :canvas-id "rev-chartjs"})
      :display-name        "chartjs-component"
      :reagent-render      (fn []
                             [:div
                              {:className "chart"}
                              [:canvas {:id "rev-chartjs" :width "600" :height "380"}]])})))

(defn chart-bars-component
  [configs]
  (let [canvas-id 2]
    (r/create-class
     {:component-did-mount #(show-horizontal-chart (assoc configs :canvas-id (str "rev-chartjs-" canvas-id)))
      :display-name        "chartjs-component"
      :reagent-render      (fn []
                             [:div
                              {:className "chart"}
                              [:canvas {:id (str "rev-chartjs-" canvas-id) :width 420 :height 240}]])})))

(defn chart-bars-component2
  [{:keys [data title label-name]}]
  (let [[series labels] (get-segments-by-ages data)
        canvas-id 3]
    (r/create-class
     {:component-did-mount #(show-bar-component {:title title
                                                 :label-name label-name
                                                 :series series
                                                 :labels labels
                                                 :canvas-id (str "rev-chartjs-" canvas-id)
                                                 })
      :display-name        "chartjs-component"
      :reagent-render      (fn []
                             [:div
                              {:className "chart"}
                              [:canvas {:id (str "rev-chartjs-" canvas-id) :width 420 :height 160}]])})))

(defn chart-bars-component3
  [{:keys [data title label-name]}]
  (let [[series labels] (get-series-by-genres data)
        canvas-id 4]
    (r/create-class
     {:component-did-mount #(show-bar-component {:title title
                                                 :label-name label-name
                                                 :series series
                                                 :labels labels
                                                 :canvas-id (str "rev-chartjs-" canvas-id)})
      :display-name        "chartjs-component"
      :reagent-render      (fn []
                             [:div
                              {:className "chart"}
                              [:canvas {:id (str "rev-chartjs-" canvas-id) :width 420 :height 160}]])})))

(defn chart-bars-component4
  [{:keys [data title label-name]}]
  (when-not (empty? data)
    (let [formatter (goog.i18n.DateTimeFormat. "dd")
          series (->> data
                      rest
                      (map #(js/parseInt (clojure.string/replace (:accumulate %) #"," ""))))
          labels (->> data
                      rest
                      (map #(.format formatter (js/Date. (:date %)))))
          canvas-id 5]
      (r/create-class
       {:component-did-mount #(show-bar-component {:title title
                                                   :label-name label-name
                                                   :series series
                                                   :labels labels
                                                   :canvas-id (str "rev-chartjs-" canvas-id)})
        :display-name        "chartjs-component"
        :reagent-render      (fn []
                               [:div
                                {:className "chart"}
                                [:canvas {:id (str "rev-chartjs-" canvas-id) :width 420 :height 160}]])}))))

(defn footer
  []
  [:footer
   "©  "
   (.getFullYear (js/Date.))
   " papachan - "
   [:a {:href "https://twitter.com/papachan"} "Twitter"]
   " "
   [:a {:href "https://github.com/papachan/data-covid19-colombia"} "Github"]])
