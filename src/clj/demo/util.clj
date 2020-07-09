(ns demo.util
  (:require [clj-time.format :as f]
            [clj-time.core :as t]
            [clj-time.coerce :as coerce]))

(defn get-max-date
  ([data] (get-max-date data "dd-MM-YYYY"))
  ([dat fecha]
   (->> (rest dat)
        reverse
        (map #(f/parse (f/formatter "dd/MM/YYYY") (second %)))
        sort
        last
        (f/unparse (f/formatter fecha)))))
