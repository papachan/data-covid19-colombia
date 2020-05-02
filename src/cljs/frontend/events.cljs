(ns frontend.events
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :as re-frame]
            [ajax.core :as ajax]
            [day8.re-frame.http-fx]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))


(def base-url "https://www.datos.gov.co/api/id/gt2j-8ykr.json?$query=")

;;; Events Handlers ;;;

(def default-db
  {:data ""
   :stats ""
   :deaths ""
   :recovered ""
   :max-id ""})

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   default-db))

(re-frame/reg-event-db
 ::set-data-db
 (fn-traced
  [db [_ data]]
  (assoc db :data (:data data))))

(re-frame/reg-event-db
 ::set-timeserie-db
 (fn [db [_ data]]
   (assoc db :stats (:Colombia data))))

(re-frame/reg-event-db
 ::set-deaths
 (fn-traced
  [db [_ data]]
  (assoc db :deaths (first data))))

(re-frame/reg-event-db
 ::set-recovered
 (fn-traced
  [db [_ data]]
  (assoc db :recovered (first data))))

(re-frame/reg-event-db
 ::set-max-case
 (fn-traced
  [db [_ data]]
  (assoc db :max-id (first data))))

;;; Subscriptions ;;;

(re-frame/reg-sub
 ::data
 (fn [{:keys [data]}]
   (when data
     (rest (first data)))))

(re-frame/reg-sub
 ::stats
 (fn [{:keys [stats]}]
   (when stats
     stats)))

(re-frame/reg-sub
 ::deaths
 (fn [{:keys [deaths]}]
   (when deaths
     deaths)))

(re-frame/reg-sub
 ::recovered
 (fn [{:keys [recovered]}]
   (when recovered
     recovered)))

(re-frame/reg-sub
 ::max-id
 (fn [{:keys [max-id]}]
   (when max-id
     max-id)))

;;; Http calls ;;;
(re-frame/reg-event-fx
 ::load-data
 (fn-traced
  [db _]
  {:http-xhrio {:method :get
                :uri "https://raw.githubusercontent.com/papachan/data-covid19-colombia/master/resources/datos.json"
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success [::set-data-db]}}))

(re-frame/reg-event-fx
 ::load-stats
 (fn-traced
  [db _]
  {:http-xhrio {:method :get
                :uri "https://pomber.github.io/covid19/timeseries.json"
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success [::set-timeserie-db]}}))

(re-frame/reg-event-fx
 ::load-deaths
 (fn-traced
  [db _]
  {:http-xhrio {:method :get
                :uri (str base-url "select%20*%2C%20%3Aid%20%20|%3E%20select%20count(*)%20as%20deaths%20where%20(upper(`estado`)%20%3D%20upper(%27Fallecido%27))%20")
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success [::set-deaths]}}))

(re-frame/reg-event-fx
 ::load-recovered
 (fn-traced
  [db _]
  {:http-xhrio {:method :get
                :uri (str base-url "select%20*%2C%20%3Aid%20%20|%3E%20select%20count(*)%20as%20recovered%20where%20(upper(`atenci_n`)%20%3D%20upper(%27Recuperado%27))%20")
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success [::set-recovered]}}))

(re-frame/reg-event-fx
 ::load-max-case
 (fn-traced
  [db _]
  {:http-xhrio {:method :get
                :uri (str base-url "select%20*%2C%20%3Aid%20%20|%3E%20select%20count(*)%20as%20max_id")
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success [::set-max-case]}}))
