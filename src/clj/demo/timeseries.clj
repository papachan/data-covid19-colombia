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


(def fmt (f/formatter "dd/MM/YYYY"))

(defn get-max-date
  ([data] (get-max-date data "dd-MM-YYYY"))
  ([dat fecha]
   (->> (rest dat)
        reverse
        (map #(f/parse (f/formatter "dd/MM/YYYY") (second %)))
        sort
        last
        (f/unparse (f/formatter fecha)))))

;; (def content (slurp (io/resource "datos.json")))
;; (def json-data (json/parse-string content))
;; (def data (json-data "data"))
;; (def rows (->> data
;;                first
;;                rest
;;                vec))

;; ;; timeseries
;; (def all-dates (->> rows
;;                     (map second)
;;                     frequencies
;;                     (map #(f/parse fmt (key %)))
;;                     sort))

;; (def series-fechas (into (sorted-map) (map vector all-dates (vec (repeat (count all-dates) 0)))))

;; (def timeseries-deaths (->> rows
;;                             (filter #(some #{"Fallecido" "fallecido"} %))
;;                             (map #(f/parse fmt (second %)))
;;                             frequencies
;;                             (into (sorted-map))))

;; (def series1 (->> timeseries-deaths
;;                   (apply merge series-fechas)
;;                   (map (fn [[k v]] [(f/unparse fmt k) v]))))

;; number of cases per dates
;; (into (sorted-map) (vec (frequencies (into [] (map #(f/parse fmt (second %)) rows)))))

(defn process-data
  [file]
  (let [data (slurp file)]
    (when-not (empty? data)
      (mapv #(mapv % [:id_de_caso
                      :fecha_diagnostico
                      :ciudad_de_ubicaci_n
                      :departamento
                      :atenci_n
                      :estado
                      :edad
                      :sexo
                      :tipo
                      :pa_s_de_procedencia
                      :fecha_de_notificaci_n
                      :fecha_recuperado
                      :fecha_de_muerte
                      ]) (clojure.walk/keywordize-keys (json/parse-string data))))))

(defn create-timeseries-file
  [max-num]
  (loop [i 1
         out []]
    (cond
      (<= i max-num)
      (let [name (clojure.string/join ["temp" i ".json"])
            data (when (io/resource name)
                   (process-data (io/resource name)))
            res (if (not= (count data) 0)
                  (concat out data)
                  out)]
        (recur (inc i) res))
      :else
      out)))

(defn sum-deaths
  [data]
  (->> data
       (filter #(some #{"Fallecido" "fallecido"} %))
       count))

(defn read-data
  [fname]
  (let [content (slurp (io/resource fname))
        json-data (json/parse-string content)
        rows (->> (json-data "data")
                  first
                  rest
                  vec)]
    rows))

;; create a new timeseries file
(defn update-timeseries
  [pages-count]
  (if (not (-> "resources/datos1.json" clojure.java.io/file .exists))
    "Error didnt copy previous datos.json file"
    (let [rows (create-timeseries-file pages-count)
          all-cases (->> rows
                         (mapv #(f/parse fmt (second %)))
                         frequencies
                         (into (sorted-map))
                         (map (fn [[k v]] [(f/unparse fmt k) v])))
          content (slurp "docs/timeseries.json")
          json-data (json/parse-string content)
          rows (read-data "datos.json")
          diff (- (sum-deaths rows) (sum-deaths (read-data "datos1.json")))
          series-deaths (conj (json-data "deaths") [(get-max-date rows "dd/MM/YYYY") diff])]
      (spit "docs/timeseries.json" (json/encode {:cases all-cases
                                                 :deaths series-deaths})))))
