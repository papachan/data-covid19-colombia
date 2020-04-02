(ns frontend.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent :refer [atom]]
            ["chartist" :as chartist]
            [frontend.events :as events]
            [goog.i18n.DateTimeFormat.Format :as fmt]
            [frontend.date :as d]))


(defn fetching-data
  []
  (-> (js/fetch "https://e.infogram.com/api/live/flex/0e44ab71-9a20-43ab-89b3-0e73c594668f/832a1373-0724-4182-a188-b958f9bf0906?")
      (.then #(.json %))
      (.then (fn [j] (js->clj j :keywordize-keys true)))
      (.then (fn [res]
               (re-frame/dispatch [::events/set-data-db (:data res)])))))

(defn sum [coll]
  (loop [in coll,
         sum 0,
         out ()]
    (if (seq in)
      (recur (rest in) (+ (second (first in)) sum)
             (cons [(first (first in)) (+ (second (first in)) sum)]
                   out))
      (reverse out))))

(defn make-serie [coll]
  (loop [in coll,
         out {}]
    (if (seq in)
      (recur (rest in)
             (conj out {(first in) 0}))
      out)))

(defn show-chart
  [data]
  (let [options {:fullWidth true
                 :height "380px"
                 :lineSmooth false}]
    (chartist/Line. ".ct-chart" (clj->js data) (clj->js options))))

(defn chart-component
  []
  (let [data @(re-frame/subscribe [::events/data])]
    (when-not (empty? data)
      (let [formatter (goog.i18n.DateTimeFormat. "dd/MM/yyyy")
            contamined (map (fn [[k v]] [(.format formatter k)
                                         v]) (sort (map (fn [[k v]] [(d/parse-date k) v]) (frequencies (into [] (map #(nth % 1) data))))))
            series-fechas (into {} (map (fn [[k v]] {k 0}) contamined))
            res (sum contamined)

            deaths (frequencies (into [] (map #(.format formatter (d/parse-date (nth % 1))) (filter #(some #{"Fallecido"} %) data))))
            result (apply merge series-fechas deaths)

            series1 (into [] (map #(nth % 1) res))
            series2 (into [] (map (fn [[k v]] v) (sum (map (fn [[k v]] [k v]) (sort (map (fn [[k v]] [(d/parse-date k) v]) result))))))

            labels-fechas (into [] (map #(first (clojure.string/split (first %) #"/")) contamined))]
        (reagent/create-class
         {:component-did-mount #(show-chart {:labels labels-fechas
                                             :series [series1 series2]})
          :display-name        "chart-component"
          :reagent-render      (fn []
                                 [:div {:id "chart4"
                                        :class "ct-chart"}])})))))


(defn start []
  (reagent/render [chart-component]
                  (. js/document (getElementById "app"))))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (.log js/console "stop"))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (re-frame/dispatch-sync [::events/initialize-db])
  ;; (re-frame/dispatch [::events/initialize-db])
  (fetching-data)
  (start))
