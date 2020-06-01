(ns frontend.ui
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r :refer [atom]]
            [frontend.data :as data :refer [process-data
                                            map-fields-name
                                            get-segments-by-ages
                                            get-series-by-genres
                                            get-series-by-status]]
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
           series2] (process-data data)]
      [:div
       [:div {:className "chart"}
        [line-chart {:width   680
                     :height  460
                     :data    {:labels   labels
                               :datasets [{:label "covid cases"
                                           :data series1
                                           :fill false
                                           :borderColor "blue"}]}
                     :options {:responsive false
                               :legend {:position "top"}
                               :title {:display true
                                       :text title
                                       :fontSize 20}
                               :scales {:xAxes [{:gridLines {:display false}}]
                                        :yAxes [{:type @type
                                                 :display true
                                                 :gridLines {:display false}}]}}}]
        [:div
         {:className "box-options"}
         [:span
          "Linear"]
         [:input
              {:type :checkbox
               :className "input-btn"
               :checked (not @scaled?)
               :on-change (fn [e]
                            (reset! type "linear"))
               :on-click (fn [e]
                           (if @scaled?
                             (swap! scaled? not)))}]
         [:span "Logarithmic"]
         [:input
          {:type :checkbox
           :className "input-btn"
           :checked @scaled?
           :on-change (fn [e]
                        (reset! type "logarithmic"))
           :on-click (fn [e]
                       (if-not @scaled?
                         (swap! scaled? not)))}]]
        ]])))

(defn show-bar-component
  [{:keys [title label-name canvas-id labels series]}]
  (let [context (.getContext (.getElementById js/document canvas-id) "2d")
        chart-data {:type "bar"
                    :data {:labels labels
                           :datasets [{:data series
                                       :label label-name
                                       :backgroundColor "#FFCC00"
                                       :borderWidth 0}]}
                    :options {:responsive false
                              :legend {:position "top"}
                              :title {:display true
                                      :text title
                                      :fontSize 16}
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
                              [:canvas {:id "rev-chartjs"
                                        :width 690
                                        :height 360}]])})))

(defn show-doughnut-component2
  [{:keys [data title align]}]
  (let [[series labels] (get-series-by-status data)]
    (let [options {:width 280
                   :height 280
                   :data {:labels labels
                          :datasets [{:data series
                                      :backgroundColor ["deeppink" "darkslateblue" "blueviolet" "crimson" "cornflowerblue" "cadetblue" "darkorange"]
                                      :borderWidth 0}]}
                   :options {:responsive false
                             :legend {:position "top"}
                             :title {:display true
                                     :text title
                                     :fontSize 16}}}]
      [:div {:className (str "doughnut-charts reduced " align)}
       [doughnut-chart options]])))

(defn chart-bars-component2
  [{:keys [data title label-name]}]
  (let [[series labels] (get-segments-by-ages data)
        canvas-id 3]
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
                              [:canvas {:id (str "rev-chartjs-" canvas-id)
                                        :width 600
                                        :height 250}]])})))

(defn show-doughnut-component
  [{:keys [data title align]}]
  (let [[series labels] (get-series-by-genres data)]
    (let [options {:width 220
                   :height 220
                   :data {:labels labels
                          :datasets [{:data series
                                      :backgroundColor ["#FFCC00" "blueviolet"]
                                      :borderWidth 0}]}
                   :options {:responsive false
                             :legend {:position "top"}
                             :title {:display true
                                     :text title
                                     :fontSize 16}}}]
      [:div {:className (str "doughnut-charts reduced " align)}
       [doughnut-chart options]])))

(defn chart-bars-component3
  [{:keys [data title label-name]}]
  (when-not (empty? data)
    (let [formatter (goog.i18n.DateTimeFormat. "dd MMM")
          series (->> data
                      rest
                      (map #(js/parseInt (clojure.string/replace (:accumulate %) #"," ""))))
          labels (->> data
                      rest
                      (map #(clojure.string/lower-case (.format formatter (js/Date. (:date %))))))
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
                                [:canvas {:id (str "rev-chartjs-" canvas-id)
                                          :width 600
                                          :height 250
                                          }]])}))))

(defn footer
  []
  [:footer
   "©  "
   (.getFullYear (js/Date.))
   " papachan - "
   [:a {:href "https://twitter.com/papachan"} "Twitter"]
   " "
   [:a {:href "https://github.com/papachan/data-covid19-colombia"} "Github"]])
