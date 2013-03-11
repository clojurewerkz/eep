(ns clojurewerkz.eep.stats-test
  (:use clojure.test
        clojurewerkz.eep.core
        clojurewerkz.eep.windows)
  (:require [clojurewerkz.eep.stats :as s]
            [clojurewerkz.eep.clocks :as c]))

;; TODO: REUSE EXECUTOR IN TEST MODE
(deftest test-monotonic-window
  (testing "Monotonic window with count"
    (let [last-val (atom nil)
          swapper (fn [v] (reset! last-val v))
          mw (monotonic-window (s/make-count) (c/make-wall-clock) swapper)]
      (enqueue mw 5)
      (tick mw)
      (is (= 1 (:count @last-val)))

      (enqueue mw 4)
      (enqueue mw 10)
      (tick mw)
      (is (= 2 (:count @last-val)))

      (enqueue mw 4)
      (enqueue mw 10)
      (enqueue mw 36)
      (enqueue mw 45)
      (tick mw)
      (is (= 4 (:count @last-val)))

      (let [n 10000]
        (dotimes [i n]
          (enqueue mw i))
        (tick mw)
        (is (= n (:count @last-val))))))

  (testing "Monotonic window with average"
    (let [last-val (atom nil)
          swapper (fn [v]
                    (reset! last-val v))
          mw (monotonic-window (s/make-mean) (c/make-wall-clock) swapper)]
      (enqueue mw 5)
      (tick mw)
      (is (= 5 (:mean @last-val)))

      (enqueue mw 4)
      (enqueue mw 10)
      (tick mw)
      (is (= 7 (:mean @last-val)))

      (enqueue mw 3)
      (enqueue mw 9)
      (enqueue mw 81)
      (tick mw)
      (is (= 31 (:mean @last-val)))))

  (testing "Tumbling window"
    (let [last-val (atom nil)
          swapper (fn [v]
                    (reset! last-val v))
          tw (tumbling-window (s/make-mean) 2 swapper)]
      (enqueue tw 1)
      (enqueue tw 2)
      (is (= 1.5 (* 1.0 (:mean @last-val))))
      (enqueue tw 3)
      (enqueue tw 4)
      (is (= 3.5 (* 1.0 (:mean @last-val))))
      (enqueue tw 5)
      (enqueue tw 6)
      (is (= 5.5 (* 1.0 (:mean @last-val))))))

  (testing "Sliding window"
    (let [last-val (atom nil)
          swapper (fn [v]
                    (reset! last-val v))
          tw (sliding-window (s/make-mean) swapper 2)]
      (enqueue tw 1)
      (enqueue tw 2)
      (is (= 1.5 (* 1.0 (:mean @last-val))))
      (enqueue tw 3)
      (enqueue tw 4)
      (is (= 3.5 (* 1.0 (:mean @last-val))))
      (enqueue tw 5)
      (enqueue tw 6)
      (is (= 5.5 (* 1.0 (:mean @last-val)))))
    (let [last-val (atom nil)
          swapper (fn [v]
                    (reset! last-val v))
          tw (sliding-window (s/make-mean) swapper 3)]
      (enqueue tw 1)
      (enqueue tw 2)
      (enqueue tw 3)
      (is (= 2.0 (* 1.0 (:mean @last-val))))
      (enqueue tw 4)
      (enqueue tw 5)
      (is (= 4.0 (* 1.0 (:mean @last-val))))
      (enqueue tw 6)
      (is (= 5.0 (* 1.0 (:mean @last-val)))))))