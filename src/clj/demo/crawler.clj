(ns demo.crawler
  (:require [clojure.set :as set]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [cheshire.core :as json]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [clj-time.coerce :as coerce]
            [demo.download :as d]
            [demo.timeseries :as timeseries
             :refer (update-timeseries)])
  (:import java.net.URL
           java.net.HttpURLConnection))

(def header
  [:id_de_caso
   :fecha_diagnostico
   :atenci_n
   :edad
   :sexo
   :ciudad_de_ubicaci_n
   :tipo
   :estado
   :departamento
   :pa_s_de_procedencia
   :fecha_de_notificaci_n
   :fecha_recuperado
   :fecha_de_muerte])

(def header_fields
  {:id_de_caso "ID de caso"
   :fecha_diagnostico "Fecha de diagnóstico"
   :ciudad_de_ubicaci_n "Ciudad de ubicación"
   :departamento "Departamento o Distrito"
   :atenci_n "Atención**"
   :estado "Estado"
   :edad "Edad"
   :sexo "Sexo"
   :tipo "Tipo*"
   :pa_s_de_procedencia "País de procedencia"
   :fecha_de_notificaci_n "fecha de_notificaci_n"
   :fecha_recuperado "fecha recuperado"
   :fecha_de_muerte "fecha_de_muerte"})

;; From datos.gov.co
(defn max-id
  []
  (let [uri (URL. "https://www.datos.gov.co/api/id/gt2j-8ykr.json?$query=select%20MAX(%3Aid)")
        conn ^HttpURLConnection (.openConnection ^URL uri)]
    (with-open [is (.getInputStream conn)]
      ((first (json/parse-string (slurp is))) "MAX_id"))))

(defn last-user-data
  [id]
  (let [uri (URL. (str "https://www.datos.gov.co/api/id/gt2j-8ykr.json?$query=select%20*%2C%20%3Aid%20where%20:id%20%3D%20%27" id "%27"))
        conn ^HttpURLConnection (.openConnection ^URL uri)]
    (with-open [is (.getInputStream conn)]
      ((first (json/parse-string (slurp is))) "id_de_caso"))))

(defn fetch-file
  [i]
  (let [limit (if (= i 0) "999" "1000")
        step (if (= i 0) 0 1000)
        value (- (* step i) 1)
        offset (if (= i 0) "" (str "%20offset%20" value))
        uri (URL. (str "https://www.datos.gov.co/api/id/gt2j-8ykr.json?$query=select%20*%2C%20%3Aid%20limit%20" limit offset))
        fname (clojure.string/join ["resources/" "temp" (+ i 1) ".json"])
        dest (io/file fname)
        conn ^HttpURLConnection (.openConnection ^URL uri)]
    (.connect conn)
    (with-open [is (.getInputStream conn)]
      (io/copy is dest))))

(defn crawl-reports
  [max-num]
  (loop [i 0]
    (when (< i max-num)
      (fetch-file i)
      (recur (inc i)))))

(defn process-data
  [file header]
  (let [data (slurp file)]
    (when-not (empty? data)
      (mapv #(mapv % header) (clojure.walk/keywordize-keys (json/parse-string data))))))

(defn parse-json-files
  [max-num fields]
  (loop [i 1
         out [(mapv #(header_fields %) fields)]]
    (cond
      (<= i max-num)
      (let [name (clojure.string/join ["temp" i ".json"])
            data (when (io/resource name)
                   (process-data (io/resource name) fields))
            res (if (not= (count data) 0)
                  (concat out data)
                  out)]
        (recur (inc i) res))
      :else
      out)))

(defn merge-dates
  [result]
  (map #(if-not (contains? % "fecha_diagnostico")
          (merge % {"fecha_diagnostico" (get % "fecha_reporte_web")}) %)
       result))

(defn replace-all-dates
  [json-obj]
  (clojure.walk/postwalk (fn [e]
                           (cond (and (not (empty? e)) (clojure.string/ends-with? e "T00:00:00.000"))
                                 (->> e
                                      (f/parse (f/formatter :date-hour-minute-second-ms))
                                      (f/unparse (f/formatter "dd/MM/YYYY")))
                                 (= e "-   -")
                                 ""
                                 :else e))
                         json-obj))

(defn remove-junk-values
  [json-obj]
  (map (fn [x]
         (update x "fecha_diagnostico"
                 #(if (= "SIN DATO" (str/upper-case %)) (get x "fecha_reporte_web") %)))
       json-obj))

;; look for all dates formats and use a single format date
(defn clean-replace-values
  [max-num]
  (loop [i 1]
    (when (<= i max-num)
      (let [fname (clojure.string/join ["temp" i ".json"])
            body (slurp (io/resource fname))
            res (->> (json/parse-string body)
                     merge-dates
                     remove-junk-values
                     replace-all-dates)]
        (when res
          (spit (str"resources/" fname) (json/encode res)))
        (recur (inc i))))))

(defn create-file-name
  [dat]
  (->> dat
       first
       rest
       (map second)
       last
       (f/parse (f/formatter "dd/MM/YYYY"))
       (f/unparse (f/formatter "dd-MM-YYYY"))))

(defn export-csv
  [fname pat]
  (let [json-data (->> fname
                       io/resource
                       slurp
                       json/parse-string)
        data (json-data "data")
        file-name (->> (create-file-name data)
                       (format pat))]
    (spit file-name "" :append false)
    (with-open [out-file (io/writer file-name)]
      (csv/write-csv out-file (first data)))))

(def max-contamined-count (Integer/parseInt (last-user-data (max-id))))

(defn make-json-file
  [pages-count fname fields]
  (let [content (parse-json-files pages-count fields)
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
    (spit fname (json/encode data-json))))

(defn copy-file []
  (io/copy (io/file (io/resource "datos.json")) (io/file "resources/datos1.json")))

;; create new datos.json file
(comment
  (let [pages-count (Math/ceil (/ max-contamined-count 1000))]
    (do
      (copy-file)
      (crawl-reports pages-count)
      (clean-replace-values pages-count)
      (make-json-file pages-count "resources/datos.json" header)
      (make-json-file pages-count "docs/datos.json" (vec (take 7 header)))
      (export-csv "datos.json" "data/Datos_%s.csv")))
  )

(defn -main
  [& args]
  (let [pages-count (Math/ceil (/ max-contamined-count 1000))]
    (cond (= "crawl" (first args))
          (do
            (copy-file)
            (crawl-reports pages-count))
          (= "clean" (first args))
          (clean-replace-values pages-count)
          (= "export" (first args))
          (do (make-json-file pages-count "resources/datos.json" header)
              (make-json-file pages-count "docs/datos.json" (vec (take 7 header)))
              (export-csv "datos.json" "data/Datos_%s.csv"))
          (= "download" (first args))
          (do
            (d/download-csv "report.csv")
            (d/convert-to-json "report.csv" "covid-tests.json"))
          (= "update" (first args))
          (update-timeseries pages-count))))
