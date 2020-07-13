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

;; download tests reports from datos.gov.co
(defn download-csv
  [fname]
  (let [uri (URL. (str "https://www.datos.gov.co/api/views/8835-5baf/rows.csv?accessType=DOWNLOAD&bom=true&format=true"))
        pathfile (str/join ["resources/" fname])
        dest (io/file pathfile)
        conn ^HttpURLConnection (.openConnection ^URL uri)]
    (.connect conn)
    (with-open [is (.getInputStream conn)]
      (io/copy is dest))))

(defn convert-to-json
  [name fname]
  (let [[header & rows] (csv/read-csv (slurp (str/join ["resources/" name])))
        vals (mapv #(hash-map :date (first %) :accumulate (Integer/parseInt (str/replace (second %) #"," ""))) rows)
        output-file (str/join ["docs/" fname])]
    (with-open [wrtr (io/writer output-file)]
      (.write wrtr (j/write-str vals)))))

(defn filter-empty-rows
  [dat]
  (filter #(not (empty? (first %))) dat))

(defn convert-to-json
  [name fname]
  (let [[header & rows] (csv/read-csv (slurp (str/join ["resources/" name])))
        vals (->> rows
                  filter-empty-rows
                  (mapv #(hash-map :date (first %) :accumulate (Integer/parseInt (str/replace (second %) #"," "")))))
        output-file (str/join ["docs/" fname])]
    (with-open [wrtr (io/writer output-file)]
      (.write wrtr (j/write-str vals)))))

(comment
  (do
   (download-csv "report.csv")
   (convert-to-json "report.csv" "covid-tests.json"))
  )
