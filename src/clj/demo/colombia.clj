(ns demo.colombia
  (:require [clojure.set :as set]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [cheshire.core :as json])
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

(let [rows (into [] (rest (first data)))
      bogota (filter #(some #{"Bogotá"} %) rows)
      statuses (into [] (map #(clojure.string/lower-case (nth % 4)) bogota))
      types (into [] (map #(clojure.string/lower-case (nth % 7)) bogota))
      ages (into [] (map #(clojure.string/lower-case (nth % 5)) bogota))
      only-infected (remove (fn [s] (= "recuperado" (clojure.string/lower-case (nth s 4)))) rows)
      by-regions (frequencies (map #(nth % 2) only-infected))]

  ;; all cases per dates
  (sort (frequencies (into [] (map #(nth % 1) rows))))

  ;; all statuses available
  (distinct (map #(clojure.string/lower-case (nth % 4)) rows))

  ;; all provinces
  (sort (distinct (map #(nth % 2) rows)))

  ;; ;; count fallecidos en bogota
  (count (filter #(= "fallecido" %) statuses)) ;; => 3 => 5

  (count rows) ;; => 491 => 539 => 608 => 702 => 798 => 906 => 1065

  (count (filter #{"Bogotá"}
           (map #(nth % 2) rows))) ;; => 225 => 264 => 297 => 353 => 390 => 472

  (count (filter #{"01/04/2020"}
                 (map #(nth % 1) rows))) ;; =>
  ;; 27/3/2020 => 48
  ;; 28/3/2020 => 69
  ;; 29/3/2020 => 94
  ;; 30/3/2020 => 96
  ;; 31/3/2020 => 108
  ;; 01/4/2020 => 159

  ;; statuses to lower case
  ;; (map #(clojure.string/lower-case (nth % 4)) (filter #(some #{"Bogotá"} %) rows))

  ;; (map #(nth % 4)
  ;;      (filter #(some #{"Bogotá"} %) rows))

  ;; numero de relacionados en bogota
  (count (filter #(= "relacionado" %) types)) ;; => 71 ;; => 73 ;; => 89 ;; => 95 => 115 => 134
  (count (filter #(= "importado" %) types)) ;; => 126 => 139 => 154 => 178 => 183 => 195

  ;; numero de casos relacionados y en estudio en bogota
  (count (filter (fn [s] (or (= "relacionado" s) (= "en estudio" s))) types)) ;; => 207 => 277

  ;; por edades
  ;; => {"20 a 29" 67, "70 a 79" 18, "60 a 69" 44, "50 a 59" 64, "40 a 49" 72, "10 a 19" 10, "80 a 89" 7, "30 a 39" 104, "0 a 9" 4}
  (frequencies ages)

  ;; suma por regiones
  (frequencies (map #(nth % 2) only-infected))
  (by-regions "Bogotá") ;; => 294 => 350 => 371 => 451

  ;; => {"recuperado" 21, "casa" 378, "hospital" 44, "hospital uci" 24, "fallecido" 5}
  (frequencies statuses)
  ;; by-regions

  ;; Fallecidos total
  (count (filter #{"Fallecido"}
                 (map #(nth % 4) rows))) ;; => 14 ;; => 16 => 17

  ;; fallecidos por regiones:
  ;; => => {"Santa Marta" 1, "Cali" 5, "Cartagena" 3, "Bogotá" 5, "Pereira" 1, "Soledad" 1, "Neiva" 1}
  (frequencies (into [] (map #(nth % 2) (filter #(some #{"Fallecido"} %) rows)))))
