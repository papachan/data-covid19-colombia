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
   (rest (first data))))

;;; Http calls ;;;
(re-frame/reg-event-fx
 ::load-data
 (fn-traced
  [db _]
  {:http-xhrio {:db (assoc db :data :loading)
                :method :get
                :uri "https://e.infogram.com/api/live/flex/0e44ab71-9a20-43ab-89b3-0e73c594668f/832a1373-0724-4182-a188-b958f9bf0906?"
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success [::set-data-db]}}))
