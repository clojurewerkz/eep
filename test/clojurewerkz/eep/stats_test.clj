(ns clojurewerkz.eep.stats-test
  (:require [clojure.test :refer :all]
            [clojurewerkz.eep.stats :refer :all]))

(deftest sum-test
  (is (= 6 (sum [1 2 3]))))

(deftest mean-test
  (is (= 2 (mean [1 2 3]))))
