(ns frontend.core
  (:require [reagent.core :as reagent :refer [atom]]
            ["chartist" :as chartist]))


(defn fetching-data
  []
  (-> (js/fetch "https://e.infogram.com/api/live/flex/0e44ab71-9a20-43ab-89b3-0e73c594668f/832a1373-0724-4182-a188-b958f9bf0906?")
      (.then #(.json %))
      (.then (fn [j] (js->clj j :keywordize-keys true)))
      (.then (fn [res]
               (:data res)))))

(defn show-chart
  [data]
  (let [options {:fullWidth true
                 :height "380px"
                 :low 0
                 :lineSmooth false
                 :showArea true
                 :axisY {:onlyInteger true}
                 :chartPadding {:right 100
                                :left 100}}
        ]
    (chartist/Line. ".ct-chart" (clj->js data) (clj->js options))))

(defn chart-component
  []
  (let [data (fetching-data)]
    (reagent/create-class
     {:component-did-mount #(show-chart {:labels ["Mar" "Jun" "Jul"]
                                         :series [[1 12 10] [1 40 80]]})
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
  (start))
