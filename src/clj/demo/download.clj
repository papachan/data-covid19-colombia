(ns demo.download
  (:require [clojure.set :as set]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.data.json :as j]
            [cheshire.core :as json]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [clj-time.coerce :as coerce])
  (:import java.net.URL
           java.net.HttpURLConnection))

(def content (slurp (io/resource "datos.json")))
(def json-data (json/parse-string content))
(def data (json-data "data"))

;; last date
(->> data
     first
     rest
     vec
     (map second)
     last)

;; check empty rows with age value
(->> data
     first
     rest
     (map #(nth % 7))
     (filter (fn [s] (empty? s))))

;; download tests reports from datos.gov.co
(defn download-csv
  [fname]
  (let [uri (URL. (str "https://www.datos.gov.co/api/views/8835-5baf/rows.csv?accessType=DOWNLOAD&bom=true&format=true"))
        pathfile (clojure.string/join ["resources/" fname])
        dest (io/file pathfile)
        conn ^HttpURLConnection (.openConnection ^URL uri)]
    (.connect conn)
    (with-open [is (.getInputStream conn)]
      (io/copy is dest))))

(defn convert-to-json
  [name fname]
  (let [[header & rows] (csv/read-csv (slurp (io/resource name)))
        vals (mapv #(hash-map :date (first %) :accumulate (second %)) rows)
        output-file (clojure.string/join ["resources/" fname])]
    (with-open [wrtr (io/writer output-file)]
      (.write wrtr (clojure.data.json/write-str vals)))))

(do
  (download-csv "report.csv")
  (convert-to-json "report.csv" "report.json"))
