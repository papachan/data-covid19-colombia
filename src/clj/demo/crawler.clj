(ns demo.crawler
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


;; From datos.gov.co
(defn max-id
  []
  (let [uri (URL. "https://www.datos.gov.co/api/id/gt2j-8ykr.json?$query=select%20MAX(%3Aid)")
        conn ^HttpURLConnection (.openConnection ^URL uri)]
    (with-open [is (.getInputStream conn)]
      ((first (json/parse-string (slurp is))) "MAX_id"))))

(defn last-user-data
  []
  (let [id (max-id)
        uri (URL. (str "https://www.datos.gov.co/api/id/gt2j-8ykr.json?$query=select%20*%2C%20%3Aid%20where%20:id%20%3D%20%27" id "%27"))
        conn ^HttpURLConnection (.openConnection ^URL uri)]
    (with-open [is (.getInputStream conn)]
      ((first (json/parse-string (slurp is))) "id_de_caso"))))

(defn fetch-file
  [i]
  (let [limit (if (= i 1) "999" "1000")
        value (- (* (- i 1) 1000) 1)
        offset (if (= i 1) "" (str "%20offset%20" value))
        uri (URL. (str "https://www.datos.gov.co/api/id/gt2j-8ykr.json?$query=select%20*%2C%20%3Aid%20limit%20" limit offset))
        fname (clojure.string/join ["resources/" "temp" i ".json"])
        dest (io/file fname)
        conn ^HttpURLConnection (.openConnection ^URL uri)]
    (.connect conn)
    (with-open [is (.getInputStream conn)]
      (io/copy is dest))))

(defn crawl-reports
  [max-num]
  (loop [i 1]
    (when (<= i max-num)
      (fetch-file i)
      (recur (inc i)))))

;; old crawler legacy
;; (defn start-crawler
;;   [max-num]
;;   (loop [i 1]
;;     (when (<= i max-num)
;;       (let [name (clojure.string/join ["temp" i ".json"])
;;             date (-> (- i 1) t/days t/ago)
;;             fmt-str (if (< (t/month date) 4)
;;                       (if (< (t/day date) 13)
;;                         "d/M/yy"
;;                         "dd/M/yyyy")
;;                       "dd/MM/yyyy")
;;             str-date (->> date
;;                           (f/unparse (f/with-zone (f/formatter fmt-str) (t/default-time-zone))))]
;;         (fetch-file str-date (clojure.string/join ["resources/" name]))
;;         (recur (inc i))))))

;; (defn crawler
;;   [max-num start-date]
;;   (loop [i 0]
;;     (when (< i max-num)
;;       (let [name (clojure.string/join ["temp" (+ i 1) ".json"])
;;             date (t/plus start-date (t/days i))]
;;         (fetch-file date (clojure.string/join ["resources/" name]))
;;         (recur (inc i))))))

(defn process-data
  [file]
  (let [data (slurp file)]
    (when-not (empty? data)
      (mapv #(mapv % [:id_de_caso
                      :fecha_de_diagn_stico
                      :ciudad_de_ubicaci_n
                      :departamento
                      :atenci_n
                      :edad
                      :sexo
                      :tipo
                      :pa_s_de_procedencia
                      :fecha_recuperado
                      :fis]) (clojure.walk/keywordize-keys (json/parse-string data))))))

(defn parse-json-files
  [max-num header]
  (loop [i 1
         out [header]]
    (if (<= i max-num)
      (let [name (clojure.string/join ["temp" i ".json"])
            data (when (io/resource name)
                   (process-data (io/resource name)))
            res (if (not= (count data) 0)
                  (concat out data)
                  out)]
        (recur (inc i) res))
      out)))

(defn transform-data
  [coll body]
  (when (seq coll)
    (let [old (first coll)
          date (f/parse (f/formatter :date-hour-minute-second-ms) old)
          new-date (f/unparse (f/formatter "dd/MM/yyyy") date)
          new-data (clojure.walk/prewalk-replace {["fecha_de_diagn_stico" old]
                                                  ["fecha_de_diagn_stico" new-date]} body)]
      (if (next coll)
        (recur (next coll) new-data)
        new-data))))

(defn merge-dates
  [result]
  (if-not (contains? (first result) "fecha_de_diagn_stico")
    (map #(merge % {"fecha_de_diagn_stico" (get % "fecha_diagnostico")}) result)
    result))

;; (defn search-for-dates
;;   [json-obj]
;;   (if-not (empty? json-obj)
;;     (let [dat ((first json-obj) "fecha_de_diagn_stico")
;;           vdat (clojure.string/split dat #"/")
;;           fmt (f/formatter "dd/MM/yyyy")
;;           fmt-in (if (and (count (nth vdat 2))
;;                           (= (nth vdat 2) "20"))
;;                    (f/formatter "dd/MM/yy")
;;                    (f/formatter "dd/MM/yyyy"))
;;           new-date (f/unparse fmt (f/parse fmt-in dat))]
;;       (clojure.walk/prewalk-replace {["fecha_de_diagn_stico" dat]
;;                                      ["fecha_de_diagn_stico" new-date]} json-obj))))

(defn search-for-dates
  [name]
  (let [body (slurp (io/resource name))
        json-obj (merge-dates (json/parse-string body))
        dates (if-not (empty? json-obj)
                (vec (filter
                      (fn [x] (> (count x) 10))
                      (vec (distinct (map (fn[v] (or (get v "fecha_diagnostico")
                                                     (get v "fecha_de_diagn_stico"))) json-obj))))))]
    (when dates
      (transform-data dates json-obj))))

;; export to csv
(defn export-csv
  [fname]
  (let [json-data (->> fname
                       io/resource
                       slurp
                       json/parse-string)
        data (json-data "data")
        last-date (->> data
                       first
                       rest
                       (map second)
                       last)
        date (clojure.string/replace last-date #"/" "-")
        file-name (clojure.string/join ["data/" "Datos_" date ".csv"])]
    (spit file-name "" :append false)
    (with-open [out-file (io/writer file-name)]
      (csv/write-csv out-file (first data)))))

;; look for all dates formats and use a single format date
(defn standarize-dates
  [max-num]
  (loop [i 1]
    (when (<= i max-num)
      (let [fname (clojure.string/join ["temp" i ".json"])
            result (search-for-dates fname)]
        (spit (str"resources/" fname) (json/encode result))
        (recur (inc i))))))

(def fmt (f/formatter "dd/MM/yyyyy"))
(def json-data (->> "datos.json"
                    io/resource
                    slurp
                    json/parse-string))
(def start-date (->> (json-data "data")
                     first
                     rest
                     vec
                     (map second)
                     first
                     (f/parse fmt)))

(def now (clj-time.coerce/to-date-time (str (java.time.LocalDateTime/now))))

;; (def days-diff (+ (t/in-days (t/interval start-date now)) 1))

(def header ["ID de caso"
             "Fecha de diagnóstico"
             "Ciudad de ubicación"
             "Departamento o Distrito"
             "Atención**"
             "Edad"
             "Sexo"
             "Tipo*"
             "País de procedencia"
             "fecha_recuperado"
             "fis"])

(def max-contamined-count (Integer/parseInt (last-user-data)))

;; create new datos.json file
(let [pages-count (Math/ceil (/ max-contamined-count 1000))]
  (do
    ;; (crawler days-diff start-date)
    (crawl-reports pages-count)
    (standarize-dates pages-count)
    (let [content (parse-json-files pages-count header)
          sheet-names ["PositivasNegativas"
                       "Titulo"
                       "Casos1"
                       "IndicadoresGenerales"
                       "Mapa"
                       "Copia de Mapa"
                       "Mundo"
                       "Procesadas"
                       "PCR"
                       "Etario"
                       "TotalEtario"
                       "importadosvsrelacionados"
                       "Procedencia"
                       "Estado"
                       "Estado2"
                       "Hoja 12"
                       "Historico_Muestras"
                       "fys"
                       "Laboratorios operando en Colomb"]
          data-json (when content
                      {:data [content]
                       :sheetNames ["Casos1"]
                       :allSheetNames sheet-names
                       :refreshed (coerce/to-long (t/now))})]
      (spit "resources/datos.json" (json/encode data-json))
      (export-csv "datos.json"))))
