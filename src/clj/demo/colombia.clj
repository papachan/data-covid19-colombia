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

(def cares (->> rows
                (map #(nth % 4))
                (remove empty?)
                (mapv clojure.string/lower-case)))

(distinct cares)
;; => ("recuperado" "casa" "fallecido" "hospital uci" "hospital" "n/a")

(frequencies cares)
;; => {"recuperado" 6665, "casa" 16346, "fallecido" 822, "hospital uci" 234, "hospital" 1266, "n/a" 33}

(def states (->> rows
                 (map #(nth % 5))
                 (remove nil?)
                 (map #(clojure.string/lower-case %))
                 distinct)) ;; => ("leve" "asintomático" "fallecido" "grave" "moderado")

(def genres (->> rows
                 (map #(nth % 7))))

(def fields {"F" "Women"
             "M" "Men"})

(->> genres
     frequencies
     (map (fn [[k v]] {(fields k) v}))
     (into {}))
;; => {"Women" 11195, "Men" 14171}

(def all-bogota-cases (filter #(some #{"Bogotá D.C."} %) rows))

(def types (into [] (map #(clojure.string/lower-case (nth % 8)) all-bogota-cases)))

(frequencies types)

(def ages (->> all-bogota-cases
               (map #(nth % 6))
               (map #(Integer/parseInt %))
               vec))

(def only-infected (->> rows
                        (map #(nth % 4))
                        (remove empty?)
                        (map clojure.string/lower-case)
                        (remove #(or (= "recuperado" %) (= "fallecido" %)))))

(distinct only-infected)

(def only-infected (remove #(or (= "Recuperado" (nth % 4)) (empty? (nth % 4))) rows))
(def only-infected-statuses (frequencies (map clojure.string/lower-case (map #(nth % 4) only-infected))))
(def by-regions (frequencies (map #(nth % 2) only-infected)))
(def contamined (->> rows
                     (map second)
                     vec
                     frequencies
                     (map (fn [[k v]] [(f/parse fmt k) v]))
                     (into (sorted-map))))

;; ultima fecha del archivo json
(->> rows
     (map second)
     last)

;; check empty rows with age value
(->> rows
     (map #(nth % 7))
     (filter empty?)
     count)

;; check empty rows with status values
(->> rows
     (map #(nth % 4))
     (filter empty?)
     count)

;; check empty rows with ages values
(->> rows
     (map #(nth % 6))
     (filter empty?)
     count)

;; check minimum age
(let [ages (->> rows
                (map #(nth % 6))
                (map #(Integer/parseInt %)))]
  (first (into (sorted-set) ages)))

;; check zero ages values
(->> rows
     (map #(nth % 6))
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
(sort (distinct (map #(nth % 2) rows)))

(count (filter #{"Bogotá D.C."}
               (map #(nth % 2) rows))) ;; => 225 => 264 => 297 => 353 => 390 => 472 => 587 => 725 => 861 => 1030 => 1164 => 1333

;; count rows from a specific date
(count (filter #{"08/04/2020"}
               (map #(second %) rows)))

;; (map #(nth % 4)
;;      (filter #(some #{"Bogotá D.C."} %) rows))

;; numero de relacionados en bogota
(count (filter #(= "relacionado" %) types)) ;; => 71 ;; => 73 ;; => 89 ;; => 95 => 115 => 134 => 154
(count (filter #(= "importado" %) types)) ;; => 126 => 139 => 154 => 178 => 183 => 195 => 222
(count (filter #(some #{"Recuperado"} %) rows)) ;; => 5511
;; deaths
(->> rows
     (filter #(some #{"Fallecido" "fallecido"} %))
     count) ;; => 822

;; numero de casos relacionados y en estudio en bogota
(count (filter (fn [s] (or (= "relacionado" s) (= "en estudio" s))) types)) ;; => 207 => 277 => 365 => 589 => 846

;; Two groups of deads by ages
(->> rows
     (filter #(some #{"Fallecido" "fallecido"} %))
     (map #(nth % 6))
     (map #(Integer/parseInt %))
     (group-by #(< % 40))
     vals
     (map count)
     (zipmap ["mayores de 40" "menores de 40"]))
;; => {"mayores de 40" 418, "menores de 40" 27}

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

(def segments-by-age
  (->> rows
       (map #(nth % 6))
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

segments-by-age
;; => {"0 a 9" 465, "10 a 19" 740, "20 a 29" 2516, "30 a 39" 2575, "40 a 49" 1905, "50 a 59" 1590, "60 a 69" 921, "70 a 79" 587, "80 a 89" 252, "90 a 99" 62}

;; suma por regiones
(frequencies (map #(nth % 2) only-infected))

;; Bogota
(by-regions "Bogotá D.C.") ;; => 294 => 350 => 371 => 451 => 566 => 651 => 733 => 926 => 964 => 1060 => 1102 => 3000

;; Fallecidos total
(count (filter #(or (= "Fallecido" (nth % 4)) (empty? (nth % 4))) rows))
;; => 14 ;; => 16 => 17 => 25 => 32 => 35 => 46 => 50 => 54 => 109 => 112 => 445

;; fallecidos por regiones:
(frequencies (into [] (map #(clojure.string/lower-case (nth % 2)) (filter #(some #{"Fallecido" "fallecido"} %) rows))))
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
(count (remove #(or (= "Recuperado" (nth % 4)) (= "Fallecido" (nth % 4)) (empty? (nth % 4))) all-bogota-cases))
;; => 2903 => 2993 => 3291 => 3403 => 3493 => 3735 => 3898 => 4046 => 4216 => 4390 => 4459 => 4681 => 4991 => 5115 => 5217 => 5470 => 5266 => 5470 => 6005 => 6404

;; number of rows with last date
(->> rows
     (map #(f/parse fmt (second %)))
     frequencies
     sort
     last
     val)
;; => 1548


;; Number of cases in UCI marked as grave
(->> rows
     (filter #(some #{"Hospital UCI"} %))
     (map #(nth % 5))
     (remove nil?)
     (filter #(= "grave" (clojure.string/lower-case %)))
     count) ;; => 238
