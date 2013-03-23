(ns clojurewerkz.eep.stats)

(defn sum
  "Calculates sum"
  [buffer]
  (apply + buffer))

(defn mean
  "Calculates mean"
  [vals]
  (let [non-nil (map identity vals)]
    (/ (reduce + non-nil) (count non-nil))))