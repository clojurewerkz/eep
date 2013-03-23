(ns clojurewerkz.eep.stats-test
  (:use clojure.test
        clojurewerkz.eep.stats))

(deftest sum-test
  (is (= 6 (sum [1 2 3]))))

(deftest mean-test
  (is (= 2 (mean [1 2 3]))))