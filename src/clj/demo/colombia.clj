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


(def fmt (f/formatter "dd/MM/yyyyy"))
(def content (slurp (io/resource "datos.json")))
(def json-data (json/parse-string content))
(def data (json-data "data"))

(let [rows (into [] (rest (first data)))
      bogota (filter #(some #{"BOGOTÁ" "BOGOTA"} %) rows)
      statuses (into [] (map #(clojure.string/lower-case (nth % 4)) bogota))
      types (into [] (map #(clojure.string/lower-case (nth % 7)) bogota))
      ages (into [] (map #(clojure.string/lower-case (nth % 5)) bogota))
      only-infected (remove (fn [s] (= "recuperado" (clojure.string/lower-case (nth s 4)))) rows)
      by-regions (frequencies (map #(nth % 2) only-infected))

      ;; timeserie contamined
      contamined (->> rows
                      (map second)
                      vec
                      frequencies
                      (map (fn [[k v]] [(f/parse fmt k) v]))
                      (into (sorted-map)))

      series-fechas   (->> rows
                           (map second)
                           vec
                           frequencies
                           (reduce-kv (fn [m k v] (assoc m k 0)) {}))
      timeseries-deaths (frequencies (into [] (map second (filter #(some #{"Fallecido" "fallecido"} %) rows))))
      timeseries (->> (apply merge series-fechas timeseries-deaths)
                      (map (fn [[k v]] [(f/parse fmt k) v]))
                      sort
                      (map (fn [[k v]] [(f/unparse fmt k) v])))]

  ;; ultima fecha del archivo json
  (last (map second rows))

  ;; all deaths by dates
  (apply merge series-fechas timeseries-deaths)

  ;; all cases per dates
  (into (sorted-map) (vec (frequencies (into [] (map #(f/parse fmt (second %)) rows)))))

  ;; all new rows per dates
  (->> rows
       (map #(f/parse fmt (second %)))
       frequencies
       vec
       (into (sorted-map)))

  ;; all statuses available
  (distinct (map #(clojure.string/lower-case (nth % 4)) rows))

  ;; all provinces
  (sort (distinct (map #(nth % 2) rows)))

  ;; ;; count fallecidos en bogota
  (count (filter #(= "fallecido" %) statuses)) ;; => 3 => 5 => 12 => 14 => 45 => 60

  (count rows) ;; => 491 => 539 => 608 => 702 => 798 => 906 => 1065 => 1267 => 1406

  (count (filter #{"BOGOTA"}
           (map #(nth % 2) rows))) ;; => 225 => 264 => 297 => 353 => 390 => 472 => 587 => 725 => 861 => 1030 => 1164 => 1333

  ;; count rows from a specific date
  (count (filter #{"08/04/2020"}
                 (map #(second %) rows)))

  ;; statuses
  (map #(clojure.string/lower-case (nth % 4)) (filter #(some #{"BOGOTA"} %) rows))

  ;; (map #(nth % 4)
  ;;      (filter #(some #{"Bogotá"} %) rows))

  ;; numero de relacionados en bogota
  (count (filter #(= "relacionado" %) types)) ;; => 71 ;; => 73 ;; => 89 ;; => 95 => 115 => 134 => 154
  (count (filter #(= "importado" %) types)) ;; => 126 => 139 => 154 => 178 => 183 => 195 => 222

  ;; numero de casos relacionados y en estudio en bogota
  (count (filter (fn [s] (or (= "relacionado" s) (= "en estudio" s))) types)) ;; => 207 => 277 => 365 => 589 => 846

  ;; por edades
  ;; => {"20 a 29" 67, "70 a 79" 18, "60 a 69" 44, "50 a 59" 64, "40 a 49" 72, "10 a 19" 10, "80 a 89" 7, "30 a 39" 104, "0 a 9" 4}
  (frequencies ages)

  ;; ;; suma por regiones
  (frequencies (map #(nth % 2) only-infected))
  (by-regions "BOGOTA") ;; => 294 => 350 => 371 => 451 => 566 => 651 => 733 => 926 => 964 => 1060 => 1102

  ;; => {"recuperado" 62, "casa" 883, "hospital" 138, "hospital uci" 31, "fallecido" 45, "recuperado (hospital)" 5}
  (frequencies statuses)

  ;; Fallecidos total
  (count (filter #{"fallecido"}
                 (map #(clojure.string/lower-case (nth % 4)) rows)))
  ;; => 14 ;; => 16 => 17 => 25 => 32 => 35 => 46 => 50 => 54 => 109 => 112

  ;; fallecidos por regiones:
  ;; => {"cienaga de oro" 1, "villapinzon" 2, "monteria" 1, "espinal" 1, "ipiales" 2, "bogota" 60, "santander de quilichao" 1, "cucuta" 2, "neiva" 3, "turbaco" 1, "zona bananera" 1, "palmira" 2, "cartagena" 9, "buenaventura" 1, "montenegro" 1, "barrancabermeja" 1, "armenia" 1, "zipaquira" 1, "ginebra" 1, "pasto" 2, "ocaña" 1, "el dovio" 1, "cali" 20, "medellin" 1, "tenjo" 1, "miranda" 1, "popayan" 1, "ibague" 1, "santa marta" 6, "villavicencio" 4, "suesca" 1, "soledad" 2, "pereira" 3, "la dorada" 2, "tunja" 1, "barranquilla" 3, "tumaco" 1}
  (frequencies (into [] (map #(clojure.string/lower-case (nth % 2)) (filter #(some #{"Fallecido" "fallecido"} %) rows))))

  ;; all rows from 3 days ago
  ;; => {"11/04/02020" 236, "12/04/02020" 67, "13/04/02020" 76}
  (->> rows
       (map #(f/parse fmt (second %)))
       (filter (fn [s] (> (coerce/to-long s) (coerce/to-long (-> 3 t/days t/ago)))))
       frequencies
       (map (fn [[k v]] [(f/unparse fmt k) v]))
       (into {}))

  ;; all cases from 1 week ago
  ;; => 1312 => 1206 => 1442 => 1370 => 1273 => 1179
  (->> rows
       (map #(f/parse fmt (second %)))
       (filter (fn [s] (> (coerce/to-long s) (coerce/to-long (-> 8 t/days t/ago)))))
       count)

  ;; deads between ages
  ;; => {"mayores de 40" 132, "menores de 40" 12}
  (->> rows
       (filter #(some #{"Fallecido" "fallecido"} %))
       (map #(Integer/parseInt (nth % 5)))
       (group-by (fn [v] (< v 40)))
       vals
       (map count)
       (zipmap ["mayores de 40" "menores de 40"])))
