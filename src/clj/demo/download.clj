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


;; from datos.gov
(defn download-raw-json
  []
  (let [uri (URL. (str "https://www.datos.gov.co/api/views/gt2j-8ykr/rows.json?accessType=DOWNLOAD"))
        dest (io/file "resources/raw_data.json")
        conn ^HttpURLConnection (.openConnection ^URL uri)]
    (.connect conn)
    (with-open [is (.getInputStream conn)]
      (io/copy is dest))))
