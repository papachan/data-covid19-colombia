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


(def content (slurp (io/resource "datos.json")))
(def json-data (json/parse-string content))
(def data (json-data "data"))

(let [date (clojure.string/replace (last (map second (rest (first data)))) #"/" "-")
      file-name (clojure.string/join ["data/" "Datos_" date ".csv"])]
  (spit file-name "" :append false)
  (with-open [out-file (io/writer file-name)]
    (csv/write-csv out-file (first data))))

;; From datos.gov.co
(defn fetch-file
  [^String d n]
  (let [uri (URL. (str "https://www.datos.gov.co/resource/gt2j-8ykr.json?fecha_de_diagn_stico=" d))
        dest (io/file n)
        conn ^HttpURLConnection (.openConnection ^URL uri)]
    (.connect conn)
    (with-open [is (.getInputStream conn)]
      (io/copy is dest))))

(defn process-data
  [file]
  (let [data (slurp file)
        content (when-not (empty? data)
                  (mapv #(mapv % [:id_de_caso
                                :fecha_de_diagn_stico
                                :ciudad_de_ubicaci_n
                                :departamento
                                :atenci_n
                                :edad
                                :sexo
                                :tipo
                                :pa_s_de_procedencia]) (clojure.walk/keywordize-keys (json/parse-string data))))]
    content))

(defn start-crawler
  [maxi]
  (loop [i maxi
         out ""]
    (when (> i 0)
      (let [fmt (f/formatter "dd/MM/yyyy")
            name (clojure.string/join ["temp" i ".json"])
            dat (f/unparse fmt (-> i t/days t/ago))
            ;; _ (fetch-file dat (clojure.string/join ["resources/" name]))
            data (when (io/resource name)
                   (process-data (io/resource name)))
            res (if-not (empty? data)
                  (subs (str data) 1 (- (count (str data)) 1)))]
        (recur (dec i)
               (str out res))))))

(def fmt (f/formatter "dd/MM/yyyy"))
(def start-date (f/parse (f/formatters :year-month-day) "2020-03-06"))
(def days-diff (t/in-days (t/interval start-date (t/now))))

(start-crawler days-diff)
