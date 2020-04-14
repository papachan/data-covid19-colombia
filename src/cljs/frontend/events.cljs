(ns frontend.events
  (:require [re-frame.core :as re-frame]
            [ajax.core :as ajax]
            [day8.re-frame.http-fx]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))

;;; Events Handlers ;;;

(def default-db
  {:data ""})

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   default-db))

(re-frame/reg-event-db
 ::set-data-db
 (fn-traced
  [db [_ data]]
  (assoc db :data (:data data))))

;;; Subscriptions ;;;

(re-frame/reg-sub
 ::data
 (fn [{:keys [data]}]
   (when data
     (rest (first data)))))

;;; Http calls ;;;
(re-frame/reg-event-fx
 ::load-data
 (fn-traced
  [db _]
  {:http-xhrio {:method :get
                :uri "https://raw.githubusercontent.com/papachan/data-covid19-colombia/master/resources/datos.json"
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success [::set-data-db]}}))
