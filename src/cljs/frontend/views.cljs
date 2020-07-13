(ns frontend.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent :refer [atom]]
            [frontend.events :as events]
            [frontend.ui :as ui :refer [row]]
            [frontend.date :as d]
            [frontend.util :as util :refer [format-number]]
            [frontend.data :as data :refer [deltas
                                            cases-by-population
                                            population
                                            get-last-date
                                            get-all-dates]])
  (:import goog.i18n.DateTimeFormat))


(def iso-fmt (goog.i18n.DateTimeFormat. "YYYY-MM-dd"))

(defn get-min-date
  [data]
  (first (map #(.format iso-fmt (d/parse-date (second %))) data)))

(defn get-first-death
  [data]
  (.format iso-fmt (first (get-all-dates data))))

(defn home
  []
  (let [data @(re-frame/subscribe [::events/data])
        deaths @(re-frame/subscribe [::events/deaths])
        recovered @(re-frame/subscribe [::events/recovered])
        max-id @(re-frame/subscribe [::events/max-id])
        covid-tests @(re-frame/subscribe [::events/covid-tests])
        timeseries @(re-frame/subscribe [::events/timeseries])]
    [:<>
     [:header
      [:h1
       "Colombia Covid19 Report"]
      [:div
       {:className "description"}
       "Live report from Colombian data using ❤ clojurescript"]]
     [:div
      {:className "container"}
      [:div
       {:className "header-note"}
       (when-not (empty? data)
         [:div
          (row {:title "Total Colombian Population: " :value (format-number population)})
          (row {:title "First Covid19 case in Colombia: " :value (get-min-date data)})
          (row {:title "First death caused by Covid19 in Colombia: " :value (get-first-death data)})])]
      [:div
       {:className "graph"}
       (when-not (empty? data)
         [:<>
          [:div
           [ui/show-doughnut-component {:data data
                                        :title "Covid19 Cases by gender"
                                        :align "left"}]
           [ui/show-doughnut-component2 {:data data
                                         :title "All cases by status"
                                         :align "right"}]]])
       (when-not (empty? timeseries)
         [:<>
          [ui/line-chart-component
           {:data timeseries
            :title "Cumulative number of reported cases"}]
          [ui/chart-bar-component
           {:data timeseries
            :title "Number of Deaths"
            :label-name "Deaths"}]])
       (when-not (empty? data)
         [ui/chart-bars-component2 {:data data
                                    :title "Deaths by Group of Ages"
                                    :label-name "Deaths"}])
       (when covid-tests
         [ui/chart-bar-covid-tests {:data covid-tests
                                    :title "Number of Covid tests"
                                    :label-name "tests"}])]
      [:div
       {:id "stats"}
       [ui/block-stats {:title "Case Fatality Rate (CFR)"
                        :style "stats num"
                        :value
                        (when (and deaths max-id)
                          (str
                           (.toFixed
                            (* (/ (:deaths deaths) (:max_id max-id)) 100) 2)
                           "%"))}]
       [ui/block-stats {:title "New cases"
                        :style "stats bignum"
                        :value
                        (when-not (empty? data)
                          (get-last-date data))}]
       [ui/block-stats {:title "New deaths"
                        :style "stats bignum"
                        :value
                        (when-not (empty? timeseries)
                          (second (last (:deaths timeseries))))}]
       [ui/block-stats {:title "Number of deaths"
                        :style "stats bignum"
                        :value
                        (when deaths
                          (:deaths deaths))}]
       [ui/block-stats {:title "Number of cases"
                        :style "stats num"
                        :value
                        (when max-id
                          (format-number (:max_id max-id)))}]
       [ui/block-stats {:title "Recovered"
                        :style "stats topnum"
                        :value
                        (when recovered
                          (format-number (:recovered recovered)))
                        :percent
                        (when recovered
                          (.round js/Math (* (float (/ (:recovered recovered)
                                                       (:max_id max-id) )) 100)))}]
       [ui/block-stats {:title "Total cases in Bogotá"
                        :style "stats num"
                        :value
                        (when-not (empty? data)
                          (->> data
                               (filter (fn [s] (= "Bogotá D.C." (nth s 5))))
                               count
                               format-number))}]
       [ui/block-stats {:title "Active cases (Bogotá)"
                        :style "stats num"
                        :value
                        (when-not (empty? data)
                          (->> data
                               (filter #(some #{"Bogotá D.C."} %))
                               (remove (fn [s] (or (= "Fallecido" (nth s 2))
                                                   (= "Recuperado" (nth s 2)))))
                               count
                               format-number))}]
       [ui/block-stats {:title "cases per one million"
                        :style "stats num"
                        :value
                        (when max-id
                          (cases-by-population (:max_id max-id)))}]
       [ui/block-stats {:title "deaths per one million"
                        :style "stats bignum"
                        :value
                        (when deaths
                          (cases-by-population (:deaths deaths)))}]
       [ui/block-stats {:title "Number of Covid Tests"
                        :style "stats small-num"
                        :value
                        (when covid-tests
                          (->> covid-tests
                               (map #(js/parseInt (:accumulate %)))
                               last
                               format-number))}]]]
     [ui/footer]]))
