(ns frontend.date)

(defn parse-date
  [date-string]
  (let [date-parts (clojure.string/split date-string #"/")
        month (dec (js/parseInt (second date-parts) 10))
        year (nth date-parts 2 nil)
        date (js/Date. year month (first date-parts))]
    (when (and year (not (js/isNaN (.getTime date))) (= month (.getMonth date)))
      date)))
