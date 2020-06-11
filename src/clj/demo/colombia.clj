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


(def now (clj-time.coerce/to-date-time (str (java.time.LocalDateTime/now))))

(def content (slurp (io/resource "datos.json")))
(def fmt (f/formatter "dd/MM/YYYY"))
(def json-data (json/parse-string content))
(def data (json-data "data"))
(def rows (->> data
               first
               rest
               vec))

(defn extract-column [column rows]
  (map #(nth % column) rows))

(def cares (->> rows
                (extract-column 2)
                (remove empty?)
                (mapv clojure.string/lower-case)))

(distinct cares)
;; => ("recuperado" "casa" "fallecido" "hospital uci" "hospital" "n/a")

(frequencies cares)
;; => {"recuperado" 11142, "fallecido" 1009, "hospital uci" 305, "hospital" 1518, "casa" 17811, "n/a" 48}

(def states (->> rows
                 (extract-column 7)
                 (remove nil?)
                 (map #(clojure.string/lower-case %))
                 distinct))
states ;; => ("leve" "asintomático" "fallecido" "grave" "moderado")

(def genres (->> rows
                 (extract-column 4)))

(def fields {"F" "Women"
             "M" "Men"})

(->> genres
     frequencies
     (map (fn [[k v]] {(fields k) v}))
     (into {}))
;; => {"Women" 14137, "Men" 17696}

(def all-bogota-cases (filter #(some #{"Bogotá D.C."} %) rows))

(def ages (->> all-bogota-cases
               (extract-column 3)
               (map #(Integer/parseInt %))))

(def only-infected (->> rows
                        (map #(nth % 2))
                        (remove empty?)
                        (map clojure.string/lower-case)
                        (remove #(or (= "recuperado" %) (= "fallecido" %)))))

(distinct only-infected)

(def only-infected (remove #(or (= "Recuperado" (nth % 2)) (empty? (nth % 2))) rows))
(def only-infected-statuses (frequencies (map clojure.string/lower-case (map #(nth % 2) only-infected))))
(def by-regions (frequencies (map #(nth % 5) only-infected)))

(def contamined (->> rows
                     (mapv second)
                     frequencies
                     (map (fn [[k v]] [(f/parse fmt k) v]))
                     (into (sorted-map))))

;; ultima fecha del archivo json
(->> rows
     (map second)
     last)

;; check empty rows with age value
(->> rows
     (map #(nth % 3))
     (filter empty?)
     count)

;; check empty rows with status values
(->> rows
     (map #(nth % 3))
     (filter empty?)
     count)

;; check empty rows with ages values
(->> rows
     (map #(nth % 3))
     (filter empty?)
     count)

;; check minimum age
(let [ages (->> rows
                (map #(nth % 3))
                (map #(Integer/parseInt %)))]
  (first (into (sorted-set) ages)))

;; check zero ages values
(->> rows
     (extract-column 3)
     (map #(Integer/parseInt %))
     (filter zero?)
     count)

;; all new rows per dates
(->> rows
     (map #(f/parse fmt (second %)))
     frequencies
     vec
     (into (sorted-map)))

;; all provinces
(vec (sort (distinct (extract-column 5 rows))))

(count (filter #{"Bogotá D.C."}
               (map #(nth % 5) rows))) ;; => 225 => 264 => 297 => 353 => 390 => 472 => 587 => 725 => 861 => 1030 => 1164 => 1333

;; count rows from a specific date
(count (filter #{"08/04/2020"} (map second rows)))

;; numero de relacionados en bogota
(def types (->> all-bogota-cases
                (mapv #(clojure.string/lower-case (nth % 6)))))

(frequencies types) ;; => {"importado" 362, "relacionado" 2191, "en estudio" 8697}

(count (filter #(= "relacionado" %) types)) ;; => 71 ;; => 73 ;; => 89 ;; => 95 => 115 => 134 => 154
(count (filter #(= "importado" %) types)) ;; => 126 => 139 => 154 => 178 => 183 => 195 => 222

(count (filter #(some #{"Recuperado"} %) rows)) ;; => 5511

;; deaths
(->> rows
     (filter #(some #{"Fallecido" "fallecido"} %))
     count) ;; => 1433

;; numero de fallecidos en bogota
(->> all-bogota-cases
     (filter #(= "Fallecido" (nth % 2)))
     count) ;; => 335

;; nuevos casos en bogota
(->> all-bogota-cases
     (filter #(= (f/unparse fmt now) (nth % 1)))
     (map #(nth % 7))
     frequencies) ;; => {"Leve" 285, "Moderado" 46, "Asintomático" 89}

(->> all-bogota-cases
     (filter #(= (f/unparse fmt now) (nth % 1)))
     (map #(nth % 2))
     frequencies) ;; => {"Casa" 372, "Hospital" 48, "N/A" 1}

(->> all-bogota-cases
     (remove #(= "Recuperado" (nth % 2)))
     (map #(nth % 2))
     frequencies) ;; => {"Fallecido" 335, "Hospital UCI" 136, "Hospital" 713, "N/A" 23, "Casa" 6865}

;; caos por generos en el reporte de hoy
(->> rows
     (filter #(= (f/unparse fmt now) (nth % 1)))
     (mapv #(clojure.string/lower-case (nth % 4)))
     frequencies) ;; => {"f" 546, "m" 659}

;; numero de casos relacionados y en estudio en bogota
(count (filter (fn [s] (or (= "relacionado" s) (= "en estudio" s))) types)) ;; => 207 => 277 => 365 => 589 => 846

;; Two groups of deads by ages
(->> rows
     (filter #(some #{"Fallecido" "fallecido"} %))
     (extract-column 3)
     (map #(Integer/parseInt %))
     (group-by #(< % 40))
     vals
     (map count)
     (zipmap ["mayores de 40" "menores de 40"]))
;; => {"mayores de 40" 990, "menores de 40" 55}

;; group by ages
(def map-fields-name
  (let [fields [:a_zeros_to_nine
                :b_tens
                :c_twenties
                :d_thirties
                :e_fourties
                :f_fifties
                :g_sixties
                :h_seventies
                :i_eighties
                :j_nineties]]
    (into (sorted-map)
          (map (fn [field n]
                 [field (format "%s a %s" n (+ n 9))])
               fields
               (range 0 100 10)))))

(defn segments-by-age
  [dat]
  (->> dat
       (map #(nth % 3))
       (map #(Integer/parseInt %))
       (group-by #(cond (<= 0 % 9)   :a_zeros_to_nine
                        (<= 10 % 19) :b_tens
                        (<= 20 % 29) :c_twenties
                        (<= 30 % 39) :d_thirties
                        (<= 40 % 49) :e_fourties
                        (<= 50 % 59) :f_fifties
                        (<= 60 % 69) :g_sixties
                        (<= 70 % 79) :h_seventies
                        (<= 80 % 89) :i_eighties
                        (>= % 90)    :j_nineties))
       (map (fn [[k vs]]
              {(map-fields-name k) (count vs)}))
       (into (sorted-map))))

(segments-by-age rows)
;; => {"0 a 9" 1517, "10 a 19" 2676, "20 a 29" 7350, "30 a 39" 7250, "40 a 49" 5262, "50 a 59" 4300, "60 a 69" 2610, "70 a 79" 1491, "80 a 89" 730, "90 a 99" 168}

(segments-by-age all-bogota-cases)
;; => {"0 a 9" 557, "10 a 19" 980, "20 a 29" 2412, "30 a 39" 2363, "40 a 49" 1847, "50 a 59" 1505, "60 a 69" 889, "70 a 79" 439, "80 a 89" 200, "90 a 99" 58}

;; suma por regiones
(def all-regions (->> only-infected
                      (extract-column 5)))

(frequencies all-regions)

;; Bogota
(by-regions "Bogotá D.C.") ;; => 294 => 350 => 371 => 451 => 566 => 651 => 733 => 926 => 964 => 1060 => 1102 => 3000 => 6628 => 6756

;; Fallecidos total
(count (filter #(or (= "Fallecido" (nth % 7)) (empty? (nth % 7))) rows))
;; => 14 ;; => 16 => 17 => 25 => 32 => 35 => 46 => 50 => 54 => 109 => 112 => 445 => 1009 => 1098

;; fallecidos por regiones:
(frequencies (into [] (map #(clojure.string/lower-case (nth % 5)) (filter #(some #{"Fallecido" "fallecido"} %) rows))))
;; => {"cienaga de oro" 1, "villapinzon" 2, "monteria" 1, "espinal" 1, "ipiales" 2, "bogota" 60, "santander de quilichao" 1, "cucuta" 2, "neiva" 3, "turbaco" 1, "zona bananera" 1, "palmira" 2, "cartagena" 9, "buenaventura" 1, "montenegro" 1, "barrancabermeja" 1, "armenia" 1, "zipaquira" 1, "ginebra" 1, "pasto" 2, "ocaña" 1, "el dovio" 1, "cali" 20, "medellin" 1, "tenjo" 1, "miranda" 1, "popayan" 1, "ibague" 1, "santa marta" 6, "villavicencio" 4, "suesca" 1, "soledad" 2, "pereira" 3, "la dorada" 2, "tunja" 1, "barranquilla" 3, "tumaco" 1}

;; all rows from 3 days ago
(->> rows
     (map #(f/parse fmt (second %)))
     (filter (fn [s] (> (coerce/to-long s) (coerce/to-long (t/minus now (t/days 3))))))
     frequencies
     (map (fn [[k v]] [(f/unparse fmt k) v]))
     (into {}))
;; => {"07/05/02020" 497, "08/05/02020" 595, "09/05/02020" 444}

;; all cases from 1 week ago
(->> rows
     (map #(f/parse fmt (second %)))
     (filter (fn [s] (> (coerce/to-long s) (coerce/to-long (-> 8 t/days t/ago)))))
     count)
;; => 3254

;; active case in Bogota
(count (remove #(or (= "Recuperado" (nth % 7)) (= "Fallecido" (nth % 7)) (empty? (nth % 7))) all-bogota-cases))
;; => 2903 => 2993 => 3291 => 3403 => 3493 => 3735 => 3898 => 4046 => 4216 => 4390 => 4459 => 4681 => 4991 => 5115 => 5217 => 5470 => 5266 => 5470 => 6005 => 6404

;; Number of cases in UCI marked as grave
(->> rows
     (filter #(some #{"Hospital UCI"} %))
     (extract-column 7)
     (remove nil?)
     (filter #(= "grave" (clojure.string/lower-case %)))
     count) ;; => 457

;; new cases
(->> rows
     (map #(f/parse fmt (second %)))
     frequencies
     sort
     last
     val) ;; => 1604
