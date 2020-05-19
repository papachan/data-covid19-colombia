(ns frontend.util)

(defn format-number [n]
  (clojure.string/replace (str n) #"\B(?=(\d{3})+(?!\d))" ","))
