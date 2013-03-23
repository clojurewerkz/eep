(ns clojurewerkz.eep.experimental
  (:use clojure.test))

(defn partition-by
  [f coll])

(deftest partition-by-test
  (partition-by (fn [last current]
                  (= (quot last 10) (quot current 10)))
                [11 12 13 14 25 26 27 35 36 67 65]))