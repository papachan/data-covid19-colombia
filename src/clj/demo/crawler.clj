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
(defn fetch-file
  [^String d]
  (let [uri (URL. (str "https://www.datos.gov.co/resource/gt2j-8ykr.json?fecha_de_diagn_stico=" d))
        dest (io/file "resources/temp.json")
        conn ^HttpURLConnection (.openConnection ^URL uri)]
    (.connect conn)
    (with-open [is (.getInputStream conn)]
      (io/copy is dest))))

(def fmt (f/formatter "dd/MM/yyyy"))
(def start-date (f/parse (f/formatters :year-month-day) "2020-03-06"))
(def days-diff (t/in-days (t/interval start-date (t/now))))

;; reverse dates from zero case day
(loop [i days-diff]
  (when (> i 0)
    (println (f/unparse fmt (-> i t/days t/ago)))
    (recur (dec i))))

(fetch-file (f/unparse fmt (-> 1 t/days t/ago)))

(def content (slurp (io/resource "temp.json")))
(def data (json/parse-string content))
(count data)

(first data)

data

(->> data
     (map #(prn %)))
