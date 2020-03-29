(ns demo.colombia
  (:require [clojure.set :as set]
            [clojure.java.io :refer [reader]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [cheshire.core :as json])
  (:import java.net.URL
           java.net.HttpURLConnection))

(let [uri (URL. "https://e.infogram.com/api/live/flex/0e44ab71-9a20-43ab-89b3-0e73c594668f/832a1373-0724-4182-a188-b958f9bf0906?")
      dest (io/file "src/datos.json")
      conn ^HttpURLConnection (.openConnection ^URL uri)
      _ (.connect conn)]
  (with-open [is (.getInputStream conn)]
    (io/copy is dest)))

(let [content (slurp (io/resource "datos.json"))
      json-data (json/parse-string content)
      data (json-data "data")
      date (clojure.string/replace (last (map #(nth % 1) (rest (first data)))) #"/" "-")
      file-name (clojure.string/join ["data/" "Datos_" date ".csv"])]
  (spit file-name "" :append false)
  (with-open [out-file (io/writer file-name)]
    (csv/write-csv out-file (first data))))

(let [content (slurp (io/resource "datos.json"))
      json-data (json/parse-string content)
      data (json-data "data")
      rows (into [] (rest (first data)))
      bogota (filter #(some #{"Bogotá"} %) rows)
      statuses (into [] (map #(clojure.string/lower-case (nth % 4)) bogota))
      types (into [] (map #(clojure.string/lower-case (nth % 7)) bogota))
      ages (into [] (map #(clojure.string/lower-case (nth % 5)) bogota))
      only-infected (remove (fn [s] (= "recuperado" (clojure.string/lower-case (nth s 4)))) rows)
      by-regions (frequencies (map #(nth % 2) only-infected))]
  ;; all statuses available
  (distinct (map #(clojure.string/lower-case (nth % 4)) rows))

  ;; all provinces
  (sort (distinct (map #(nth % 2) rows)))

  ;; ;; count fallecidos en bogota
  (count (filter #(= "fallecido" %) statuses)) ;; => 3 => 5

  ;; ;; (print bogota)
  (count rows) ;; => 491 => 539 => 608 => 702

  (count (filter #{"Bogotá"}
           (map #(nth % 2) rows))) ;; => 225 => 264 => 297

  (count (filter #{"28/3/2020"}
                 (map #(nth % 1) rows)))
  ;; 27/3/2020 => 48
  ;; 28/3/2020 => 69
  ;; 29/3/2020 => 94

  (count (filter #{"29/3/2020"}
                 (map #(nth % 1) rows))) ;; => 94


  (filter #(some #{"fallecido"} %) (map #(clojure.string/lower-case (nth % 4)) (filter #(some #{"Bogotá"} %) rows)))

  ;; statuses to lower case
  (map #(clojure.string/lower-case (nth % 4)) (filter #(some #{"Bogotá"} %) rows))

  (map #(nth % 4)
       (filter #(some #{"Bogotá"} %) rows))

  ;; numero de relacionados en bogota
  (count (filter #(= "relacionado" %) types)) ;; => 71 ;; => 73 ;; => 89
  (count (filter #(= "importado" %) types)) ;; => 126 => 139 => 154

  ;; por edades
  ;; => {"10 a 19" 6, "20 a 29" 53, "30 a 39" 76, "40 a 49" 58, "60 a 69" 36, "70 a 79" 14, "50 a 59" 48, "80 a 89" 6}
  (frequencies ages)

  ;; suma por regiones
  (frequencies (map #(nth % 2) only-infected))
  (by-regions "Bogotá") ;; => 294

  ;; {"recuperado" 3, "en casa" 259, "hospital uci" 16, "hospital" 14, "fallecido" 5}
  (frequencies statuses))
