(ns clojurewerkz.eep.stats-test
  (:use clojure.test
        clojurewerkz.eep.stats))

(deftest test-monotonic-window
  (testing "Monotonic window with count"
    (let [last-val (atom nil)
          swapper (fn [v] (reset! last-val v))
          mw (monotonic-window (make-count) (make-wall-clock 0 nil) swapper)]
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
          mw (monotonic-window (make-mean) (make-wall-clock 0 nil) swapper)]
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
          tw (tumbling-window (make-mean) 2 swapper)]
      (enqueue tw 1)
      (enqueue tw 2)
      (is (= 1.5 (* 1.0 (:mean @last-val))))
      (enqueue tw 3)
      (enqueue tw 4)
      (is (= 3.5 (* 1.0 (:mean @last-val))))
      (enqueue tw 5)
      (enqueue tw 6)
      (is (= 5.5 (* 1.0 (:mean @last-val)))))))