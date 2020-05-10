(ns frontend.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent :refer [atom]]
            ["chartist" :as chartist]
            ["react-chartist" :default react-graph]
            [frontend.events :as events]
            [frontend.ui :as ui :refer [row]]
            [frontend.date :as d])
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
        covid-tests @(re-frame/subscribe [::events/covid-tests])]
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
          (row {:title "First Case of Covid19 in Colombia: " :value (get-min-date data)})
          (row {:title "First Death of Covid19 in Colombia: " :value (get-first-death data)})])]
      [:div
       {:className "graph"}
       [ui/chart-component {:data data
                            :title "Cummulative number of reported cases & deaths"}]
       [ui/chart-bars-component {:data data
                                 :title "All cases by status"}]
       [ui/chart-bars-component2 {:data data
                                  :title "Deaths by Age"}]
       (when covid-tests
         [ui/chart-bars-component3 {:data covid-tests
                                    :title "Cummulative number of Covid tests"}])]
      [:div
       {:id "stats"}
       [ui/block-stats {:title "Number of deaths"
                        :style "stats bignum"
                        :value
                        (when deaths
                          (:deaths deaths))}]
       [ui/block-stats {:title "Number of cases"
                        :style "stats num"
                        :value
                        (when max-id
                          (:max_id max-id))}]
       [ui/block-stats {:title "Recovered"
                        :style "stats bignum"
                        :value
                        (when recovered
                          (:recovered recovered))}]
       [ui/block-stats {:title "Active cases (Bogotá)"
                        :style "stats bignum"
                        :value
                        (when-not (empty? data)
                          (->> data
                               (filter #(some #{"Bogotá D.C."} %))
                               (remove (fn [s] (or (= "Fallecido" (nth s 4))
                                                   (= "Recuperado" (nth s 4)))))
                               count))}]
       [ui/block-stats {:title "Number of Covid Tests"
                        :style "stats num"
                        :value
                        (when covid-tests
                          (->> covid-tests
                               (map #(js/parseInt (clojure.string/replace (:accumulate %) #"," "")))
                               last))}]]]
     [ui/footer]]))
