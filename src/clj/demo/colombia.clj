(ns demo.colombia
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

(let [uri (URL. "https://e.infogram.com/api/live/flex/0e44ab71-9a20-43ab-89b3-0e73c594668f/832a1373-0724-4182-a188-b958f9bf0906?")
      dest (io/file "resources/datos.json")
      conn ^HttpURLConnection (.openConnection ^URL uri)]
  (.connect conn)
  (with-open [is (.getInputStream conn)]
    (io/copy is dest)))

(def content (slurp (io/resource "datos.json")))
(def json-data (json/parse-string content))
(def data (json-data "data"))

(let [date (clojure.string/replace (last (map #(nth % 1) (rest (first data)))) #"/" "-")
      file-name (clojure.string/join ["data/" "Datos_" date ".csv"])]
  (spit file-name "" :append false)
  (with-open [out-file (io/writer file-name)]
    (csv/write-csv out-file (first data))))

(defn make-serie
  [coll]
  (loop [in coll,
         out {}]
    (if (seq in)
      (recur (rest in)
             (conj out {(first in) 0}))
      out)))


(def fmt (f/formatter "dd/MM/yyyyy"))

(let [rows (into [] (rest (first data)))
      bogota (filter #(some #{"Bogotá"} %) rows)
      statuses (into [] (map #(clojure.string/lower-case (nth % 4)) bogota))
      types (into [] (map #(clojure.string/lower-case (nth % 7)) bogota))
      ages (into [] (map #(clojure.string/lower-case (nth % 5)) bogota))
      only-infected (remove (fn [s] (= "recuperado" (clojure.string/lower-case (nth s 4)))) rows)
      by-regions (frequencies (map #(nth % 2) only-infected))

      series-fechas (make-serie (sort (into #{} (map #(nth % 1) rows))))
      deaths (frequencies (into [] (map #(nth % 1) (filter #(some #{"Fallecido"} %) rows))))
      result (apply merge series-fechas deaths)]

  ;; all deaths by dates
  (apply merge series-fechas deaths)

  ;; all cases per dates
  (sort (frequencies (into [] (map #(nth % 1) rows))))

  ;; all statuses available
  (distinct (map #(clojure.string/lower-case (nth % 4)) rows))

  ;; all provinces
  (sort (distinct (map #(nth % 2) rows)))

  ;; ;; count fallecidos en bogota
  (count (filter #(= "fallecido" %) statuses)) ;; => 3 => 5 => 12 => 14

  (count rows) ;; => 491 => 539 => 608 => 702 => 798 => 906 => 1065 => 1267 => 1406

  ;; (count (filter #{"Bogotá"}
  ;;          (map #(nth % 2) rows))) ;; => 225 => 264 => 297 => 353 => 390 => 472 => 587 => 725 => 861

  ;; number of rows per dates
  (count (filter #{"08/04/2020"}
                 (map #(nth % 1) rows))) ;; =>
  ;; 27/3/2020 => 48
  ;; 28/3/2020 => 69
  ;; 29/3/2020 => 94
  ;; 30/3/2020 => 96
  ;; 31/3/2020 => 108
  ;; 01/4/2020 => 159
  ;; 02/4/2020 => 96
  ;; 03/4/2020 => 106
  ;; 04/4/2020 => 139
  ;; 05/4/2020 => 79
  ;; 06/4/2020 => 94
  ;; 07/4/2020 => 201
  ;; 08/4/2020 => 274

  ;; statuses to lower case
  (map #(clojure.string/lower-case (nth % 4)) (filter #(some #{"Bogotá"} %) rows))

  ;; (map #(nth % 4)
  ;;      (filter #(some #{"Bogotá"} %) rows))

  ;; numero de relacionados en bogota
  (count (filter #(= "relacionado" %) types)) ;; => 71 ;; => 73 ;; => 89 ;; => 95 => 115 => 134 => 154
  (count (filter #(= "importado" %) types)) ;; => 126 => 139 => 154 => 178 => 183 => 195 => 222

  ;; numero de casos relacionados y en estudio en bogota
  (count (filter (fn [s] (or (= "relacionado" s) (= "en estudio" s))) types)) ;; => 207 => 277 => 365 => 589

  ;; por edades
  ;; => {"20 a 29" 67, "70 a 79" 18, "60 a 69" 44, "50 a 59" 64, "40 a 49" 72, "10 a 19" 10, "80 a 89" 7, "30 a 39" 104, "0 a 9" 4}
  (frequencies ages)

  ;; ;; suma por regiones
  (frequencies (map #(nth % 2) only-infected))
  (by-regions "Bogotá") ;; => 294 => 350 => 371 => 451 => 566 => 651 => 733 => 926

  ;; => {"recuperado" 66, "casa" 749, "hospital" 114, "hospital uci" 38, "fallecido" 25}
  (frequencies statuses)

  ;; Fallecidos total
  (count (filter #{"Fallecido"}
                 (map #(nth % 4) rows))) ;; => 14 ;; => 16 => 17 => 25 => 32 => 35 => 46 => 50 => 54

  ;; fallecidos por regiones:
  ;; => {"Villavicencio" 1, "Cali" 5, "Montería" 1, "Pereira" 1, "Neiva" 2, "Ciénaga de Oro" 1, "Santander de Quilichao" 1, "Cartagena" 3, "Bogotá" 25, "Santa Marta" 2, "Cúcuta" 1, "Soledad" 2, "Tunja" 1, "Barranquilla" 2, "Medellín" 1, "Popayán" 1, "Villapinzón" 2, "Montenegro" 1, "Zipaquirá" 1}
  (frequencies (into [] (map #(nth % 2) (filter #(some #{"Fallecido"} %) rows))))

  ;; timeseries
  (map (fn [[k v]] [(f/unparse fmt k) v]) (sort (map (fn [[k v]] [(f/parse fmt k) v]) result)))

  ;; all rows from 3 days ago
  ;; => {"06/04/02020" 94, "07/04/02020" 201, "08/04/02020" 274}
  (into {} (map (fn [[k v]] [(f/unparse fmt k) v]) (frequencies (filter (fn [s] (> (coerce/to-long s) (coerce/to-long (-> 3 t/days t/ago)))) (map #(f/parse fmt (nth % 1)) rows)))))

  ;; all cases from 1 week ago
  ;; => 1148
  (count (filter (fn [s] (> (coerce/to-long s) (coerce/to-long (-> 8 t/days t/ago)))) (map #(f/parse fmt (nth % 1)) rows))))
