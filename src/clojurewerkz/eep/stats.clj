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

(defn variance
  "Calculates variance, deviation from man value"
  [arr]
  (let [mean (/ (reduce + arr) (count arr))
        sqr #(* % %)]
    (/
     (reduce + (map #(sqr (- % mean)) arr))
     (- (count arr) 1))))

(defn percentage
  [total value]
  (* (/ value total) 100.0))