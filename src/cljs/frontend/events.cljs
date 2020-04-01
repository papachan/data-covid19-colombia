(ns frontend.events
  (:require [re-frame.core :as re-frame]))

;;; Events Handlers ;;;

(def default-db
  {:data ""})

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   default-db))

(re-frame/reg-event-db
 ::set-data-db
 (fn [db [_ new-value]]
   (assoc db :data new-value)))

;;; Subscriptions ;;;

(re-frame/reg-sub
 ::data
 (fn [db]
   (rest (first (:data db)))))
