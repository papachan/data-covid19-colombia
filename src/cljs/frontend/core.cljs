(ns frontend.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [frontend.events :as events]
            [frontend.pages :as pages :refer [home]]))

(defn fetching-data
  []
  (-> (js/fetch "https://e.infogram.com/api/live/flex/0e44ab71-9a20-43ab-89b3-0e73c594668f/832a1373-0724-4182-a188-b958f9bf0906?")
      (.then #(.json %))
      (.then (fn [j] (js->clj j :keywordize-keys true)))
      (.then (fn [res]
               (re-frame/dispatch [::events/set-data-db (:data res)])))))

(defn start []
  (reagent/render [home]
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
