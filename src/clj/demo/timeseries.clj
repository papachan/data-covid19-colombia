(ns demo.timeseries
  (:require [clojure.set :as set]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [cheshire.core :as json]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [clj-time.coerce :as coerce])
  (:import java.net.URL
           java.net.HttpURLConnection))


(def fmt (f/formatter "dd/MM/yyyyy"))
(def content (slurp (io/resource "datos.json")))
(def json-data (json/parse-string content))
(def data (json-data "data"))
(def rows (->> data
               first
               rest
               vec))

;; timeseries
(def all-dates (->> rows
                    (map second)
                    frequencies
                    (map #(f/parse fmt (key %)))
                    sort))

(def series-fechas (into (sorted-map) (map vector all-dates (vec (repeat (count all-dates) 0)))))

(def timeseries-deaths (->> rows
                            (filter #(some #{"Fallecido" "fallecido"} %))
                            (map #(f/parse fmt (second %)))
                            frequencies
                            (into (sorted-map))))

(def series1 (->> timeseries-deaths
                  (apply merge series-fechas)
                  (map (fn [[k v]] [(f/unparse fmt k) v]))))

;; number of cases per dates
(into (sorted-map) (vec (frequencies (into [] (map #(f/parse fmt (second %)) rows)))))
