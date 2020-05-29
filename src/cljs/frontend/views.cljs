(ns frontend.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent :refer [atom]]
            [frontend.events :as events]
            [frontend.ui :as ui :refer [row]]
            [frontend.date :as d]
            [frontend.util :as util :refer [format-number]]
            [frontend.data :as data :refer [cases-by-population
                                            population
                                            get-last-date]])
  (:import goog.i18n.DateTimeFormat))


(def iso-fmt (goog.i18n.DateTimeFormat. "yyyy-MM-dd"))

(defn get-min-date
  [data]
  (first (map #(.format iso-fmt (d/parse-date (second %))) data)))

(defn get-first-death
  [data]
  (let [all-dates (->> data
                       (filter #(some #{"Fallecido" "fallecido"} %))
                       (map #(d/parse-date (second %)))
                       sort)]
    (.format iso-fmt (first all-dates))))

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
          (row {:title "First Covid19 death in Colombia: " :value (get-first-death data)})])]
      [:div
       {:className "graph"}
       (when-not (empty? timeseries)
         [:<>
          [ui/chart-component
           {:data timeseries
            :title "Cummulative number of reported cases"}]
          [ui/chart-bar-comp
           {:data timeseries
            :title "Cummulative number of deaths"
            :label-name "Deaths"}]])
       (when-not (empty? data)
         [:<>
          [ui/chart-bars-component {:data data
                                    :title "All cases by status"}]
          [ui/chart-bars-component2 {:data data
                                     :title "Deaths by Group of Ages"
                                     :label-name "Deaths"}]
          [ui/chart-bars-component3 {:data data
                                     :title "Covid19 Cases by gender"
                                     :label-name "Cases"}]])
       (when covid-tests
         [ui/chart-bars-component4 {:data covid-tests
                                    :title "Cummulative number of Covid tests"
                                    :label-name "tests"}])]
      [:div
       {:id "stats"}
       [ui/block-stats {:title "New cases"
                        :style "stats bignum"
                        :value
                        (when-not (empty? data)
                          (get-last-date data))}]

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
                        :style "stats bignum"
                        :value
                        (when recovered
                          (format-number (:recovered recovered)))}]
       [ui/block-stats {:title "Active cases (Bogotá)"
                        :style "stats bignum"
                        :value
                        (when-not (empty? data)
                          (->> data
                               (filter #(some #{"Bogotá D.C."} %))
                               (remove (fn [s] (or (= "Fallecido" (nth s 4))
                                                   (= "Recuperado" (nth s 4)))))
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
                        :style "stats num"
                        :value
                        (when covid-tests
                          (->> covid-tests
                               (map #(js/parseInt (clojure.string/replace (:accumulate %) #"," "")))
                               last
                               format-number))}]]]
     [ui/footer]]))
