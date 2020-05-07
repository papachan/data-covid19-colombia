(ns frontend.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent :refer [atom]]
            ["chartist" :as chartist]
            ["react-chartist" :default react-graph]
            [frontend.events :as events]
            [frontend.ui :as ui]
            [frontend.date :as d])
  (:import goog.i18n.DateTimeFormat))


(defn get-min-date
  [data]
  (let [fmt (goog.i18n.DateTimeFormat. "yyyy-MM-dd")]
    (first (map #(.format fmt (d/parse-date (second %))) data))))

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
          [:div
           {:className "row"}
           (str "First Case of Covid19 in Colombia: ")
           [:span
            {:className "highlighted"}
            (get-min-date data)]]])]
      [:div
       {:className "graph"}
       [ui/chart-component data]
       [ui/chart-bars-component data]
       [ui/chart-bars-component2 data]
       (when covid-tests
         [ui/chart-bars-component3 covid-tests])]
      [:div
       {:id "stats"}
       [ui/block-stats {:title "Number of deaths"
                     :value
                     (when deaths
                       (:deaths deaths))}]
       [ui/block-stats {:title "Number of cases"
                     :value
                     (when max-id
                       (:max_id max-id))}]
       [ui/block-stats {:title "Recovered"
                     :value
                     (when recovered
                       (:recovered recovered))}]
       [ui/block-stats {:title "Active cases (Bogotá)"
                        :value
                        (when-not (empty? data)
                          :recovered (->> data
                                          (filter #(some #{"Bogotá D.C."} %))
                                          (remove (fn [s] (or (= "Fallecido" (nth s 4))
                                                              (= "Recuperado" (nth s 4)))))
                                          count))
                        }]]]
     [ui/footer]]))
