(ns frontend.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [reagent.dom :as rd]
            [frontend.events :as events]
            [frontend.views :as views :refer [home]]))


(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rd/unmount-component-at-node root-el)
    (rd/render [home] root-el)))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (.log js/console "stop"))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch-sync [::events/load-data])
  (re-frame/dispatch-sync [::events/load-deaths])
  (re-frame/dispatch-sync [::events/load-recovered])
  (re-frame/dispatch-sync [::events/load-max-case])
  (re-frame/dispatch-sync [::events/load-covid-tests])
  (re-frame/dispatch-sync [::events/load-timeseries])
  (mount-root))
