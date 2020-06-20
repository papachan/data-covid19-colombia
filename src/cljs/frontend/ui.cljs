(ns frontend.ui
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r :refer [atom]]
            [frontend.data :as data :refer [process-data
                                            map-fields-name
                                            get-segments-by-ages
                                            get-series-by-genres
                                            get-series-by-status
                                            deltas
                                            get-accumulate-tests]]
            ["chart.js" :refer [Chart]]
            ["react-chartjs-2" :as ReactChartjs2 :refer [Line]]
            [goog.string.format]
            [goog.string :refer [format]]
            [clojure.string :as str]))


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
  [{:keys [title value style percent]} data]
  [:div
   {:className "block-stats"}
   [:div
    {:className style}
    value]
   [:div
    {:className "title"}
    title]
   (when percent
     [:div
      {:className "stats percent"}
      (str percent "%")])])

(defn div-options-buttons
  [props]
  (let [{:keys [scaled?
                title-button-1
                title-button-2]} props]
    [:div
     {:className "box-options"}
     [:span
      (str/capitalize title-button-1)]
     [:input
      {:type :checkbox
       :className "input-btn"
       :checked (not @scaled?)
       :on-change (fn [e]
                    (if @scaled?
                     (swap! scaled? not)))}]
     [:span
      (str/capitalize title-button-2)]
     [:input
      {:type :checkbox
       :className "input-btn"
       :checked @scaled?
       :on-change (fn [e]
                    (if-not @scaled?
                     (swap! scaled? not)))}]]))

(defn bottom-options-chart
  [props]
  (let [{:keys [toggled?
                title1
                title2]} props]
    [:<>
     [:div
      {:className "box-options"}
      [:span
       title1]
      [:input
       {:type :checkbox
        :className "input-btn"
        :checked (not @toggled?)
        :on-change (fn [e]
                     (reset! toggled? false))}]
      [:span
       title2]
      [:input
       {:type :checkbox
        :className "input-btn"
        :checked @toggled?
        :on-change (fn [e]
                     (reset! toggled? true))}]
      ]]))

(defn show-chart-component
  [{:keys [canvas-id chart-data]}]
  (let [context (.getContext (.getElementById js/document canvas-id) "2d")]
    (Chart. context (clj->js chart-data))))

;; show line chart with timeseries
(defn line-chart-component
  [{:keys [data title]}]
  (r/with-let [scaled? (atom false)]
    (let [[labels
           series1] (process-data data)]
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
                                        :yAxes [{:type (if @scaled? "logarithmic" "linear")
                                                 :display true
                                                 :gridLines {:display false}}]}}}]
        (div-options-buttons {:scaled? scaled?
                              :title-button-1 "linear"
                              :title-button-2 "logarithmic"})]])))

(defn chart-bar-component
  [{:keys [data title label-name]}]
  (let [toggled? (atom false)
        mychart (atom nil)
        [labels
         cases
         series] (process-data data)
        dataset-options {:data (deltas series)
                         :label label-name
                         :backgroundColor "#FFCC00"
                         :borderWidth 0}]
    (r/create-class
     {:display-name        "chartjs-component"
      :component-did-update (fn [this]
                              (set! (.-datasets (.-data @mychart))
                                    (clj->js [(assoc dataset-options :data (if @toggled?
                                                                             series
                                                                             (deltas series)))]))
                              (.update @mychart))
      :component-did-mount  (fn [this]
                              (reset! mychart
                                      (show-chart-component
                                       {:canvas-id "rev-chartjs"
                                        :chart-data {:type "bar"
                                                     :data {:labels labels
                                                            :datasets [dataset-options]}
                                                     :options {:responsive false
                                                               :legend {:position "top"}
                                                               :title {:display true
                                                                       :text title
                                                                       :fontSize 16}
                                                               :scales {:xAxes [{:gridLines {:display false}}]
                                                                        :yAxes [{:gridLines {:display false}}]}}}})))
      :reagent-render (fn [props]
                        [:div
                         {:className "chart"}
                         [:canvas {:id "rev-chartjs"
                                   :width 690
                                   :height 360}]
                         (bottom-options-chart {:toggled? toggled?
                                                :title1 "Daily"
                                                :title2 "Cumulative"})])})))

(defn show-doughnut-component2
  [{:keys [data title align]}]
  (let [[series labels] (get-series-by-status data)]
    (let [options {:width 280
                   :height 280
                   :data {:labels labels
                          :datasets [{:data series
                                      :backgroundColor ["deeppink"
                                                        "darkslateblue"
                                                        "blueviolet"
                                                        "crimson"
                                                        "cornflowerblue"
                                                        "cadetblue"
                                                        "darkorange"]
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
     {:component-did-mount #(show-chart-component
                             {:chart-data {:type "bar"
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
                                                              :yAxes [{:gridLines {:display false}}]}}}
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
    (let [options {:width 250
                   :height 250
                   :data {:labels labels
                          :datasets [{:data series
                                      :backgroundColor ["#FFCC00" "blueviolet"]
                                      :borderWidth 0}]}
                   :options {:responsive false
                             :legend {:position "top"}
                             :layout {:padding {:left 0
                                                :right 0
                                                :top 20
                                                :bottom 20}}
                             :title {:display true
                                     :text title
                                     :fontSize 16}}}]
      [:div {:className (str "doughnut-charts reduced " align)}
       [doughnut-chart options]])))

(defn chart-bar-covid-tests
  [{:keys [data title label-name]}]
  (let [toggled? (atom true)
        mychart (atom nil)
        canvas-id 5
        formatter (goog.i18n.DateTimeFormat. "dd MMM")
        labels (->> data
                    rest
                    (map #(clojure.string/lower-case (.format formatter (js/Date. (:date %))))))
        series (get-accumulate-tests data)
        dataset-options {:label label-name
                         :backgroundColor "#FFCC00"
                         :borderWidth 0}]
    (when-not (empty? data)
      (r/create-class
       {:display-name         "chartjs-component"
        :component-did-update (fn [this]
                                (set! (.-datasets (.-data @mychart)) (clj->js [(assoc dataset-options :data (if @toggled?
                                                                                                              (deltas series)
                                                                                                              series))]))
                                (.update @mychart))
        :component-did-mount (fn [this]
                               (reset! mychart
                                       (show-chart-component {:chart-data
                                                              {:type "bar"
                                                               :data {:labels labels
                                                                      :datasets [(assoc dataset-options :data (deltas series))]}
                                                               :options {:responsive false
                                                                         :legend {:position "top"}
                                                                         :title {:display true
                                                                                 :text title
                                                                                 :fontSize 16}
                                                                         :scales {:xAxes [{:gridLines {:display false}}]
                                                                                  :yAxes [{:gridLines {:display false}}]}}}
                                                              :canvas-id (str "rev-chartjs-" canvas-id)})))
        :reagent-render (fn [props]
                          [:div
                           {:className "chart"}
                           [:canvas {:id (str "rev-chartjs-" canvas-id)
                                     :width 600
                                     :height 250}]
                           (bottom-options-chart {:toggled? toggled?
                                                  :title1 "Daily tests"
                                                  :title2 "Cumulative"})])}))))

(defn footer
  []
  [:footer
   "©  "
   (.getFullYear (js/Date.))
   " papachan - "
   [:a {:href "https://twitter.com/papachan"} "Twitter"]
   " "
   [:a {:href "https://github.com/papachan/data-covid19-colombia"} "Github"]])
