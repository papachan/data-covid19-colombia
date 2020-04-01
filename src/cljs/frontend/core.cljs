(ns frontend.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent :refer [atom]]
            ["chartist" :as chartist]))


;;; Events Handlers ;;;

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   {:data nil}))

(re-frame/reg-event-db
 ::set-data-db
 (fn [db [_ new-value]]
   (assoc db :data new-value)))

;;; Subscriptions ;;;

(re-frame/reg-sub
 ::data
 (fn [db]
   (:data db)))

(defn fetching-data
  []
  (-> (js/fetch "https://e.infogram.com/api/live/flex/0e44ab71-9a20-43ab-89b3-0e73c594668f/832a1373-0724-4182-a188-b958f9bf0906?")
      (.then #(.json %))
      (.then (fn [j] (js->clj j :keywordize-keys true)))
      (.then (fn [res]
               (re-frame/dispatch [::set-data-db (:data res)])))))

(defn sum [coll]
  (loop [in coll,
         sum 0,
         out ()]
    (if (seq in)
      (recur (rest in) (+ (second (first in)) sum)
             (cons [(first (first in)) (+ (second (first in)) sum)]
                   out))
      (reverse out))))

(defn show-chart
  [data]
  (let [options {:fullWidth true
                 :height "380px"
                 :low 0
                 :lineSmooth false
                 :showArea true
                 :axisY {:onlyInteger true}
                 :chartPadding {:right 100
                                :left 100}}]
    (chartist/Line. ".ct-chart" (clj->js data) (clj->js options))))

(defn chart-component
  []
  (let [data @(re-frame/subscribe [::data])
        rows (rest (first data))
        fechas (sort (frequencies (into [] (map #(clojure.string/lower-case (nth % 1)) rows))))
        res (sum fechas)
        series (into [] (map #(nth % 1) res))
        fechas (into [] (map #(first (clojure.string/split (nth % 0) #"/")) res))]
    (reagent/create-class
     {:component-did-mount #(show-chart {:labels fechas
                                         :series [series]})
      :display-name        "chart-component"
      :reagent-render      (fn []
                             [:div {:class "ct-chart ct-perfect-fourth"}])})))

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
  (re-frame/dispatch-sync [::initialize-db])
  (fetching-data)
  (start))
