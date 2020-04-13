(ns demo.download
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

(let [uri (URL. "https://e.infogram.com/api/live/flex/0e44ab71-9a20-43ab-89b3-0e73c594668f/832a1373-0724-4182-a188-b958f9bf0906?")
      dest (io/file "resources/datos.json")
      conn ^HttpURLConnection (.openConnection ^URL uri)]
  (.connect conn)
  (with-open [is (.getInputStream conn)]
    (io/copy is dest)))

(def content (slurp (io/resource "datos.json")))
(def json-data (json/parse-string content))
(def data (json-data "data"))

;; ultima fecha del archivo json
(->> data
     first
     rest
     vec
     (map #(second %))
     last)
