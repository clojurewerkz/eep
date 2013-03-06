(ns clojurewerkz.eep-clj.stats-test
  (:use clojure.test
        clojurewerkz.eep-clj.stats))

(deftest test-monotonic-window
  (let [last-val (atom nil)
        swapper (fn [v] (reset! last-val v))
        mw (monotonic-window (make-count 0) (make-wall-clock 0 nil) swapper)]
    (enqueue mw 5)
    (Thread/sleep 10)
    (tick mw)

    (is (= 1 (:count @last-val)))
    (enqueue mw 4)
    (enqueue mw 10)
    (Thread/sleep 10)
    (tick mw)
    (is (= 2 (:count @last-val)))

    (enqueue mw 4)
    (enqueue mw 10)
    (enqueue mw 36)
    (enqueue mw 45)
    ;; (Thread/sleep 10)
    (tick mw)
    (is (= 4 (:count @last-val)))
    (Thread/sleep 10)
    (is (= 4 (:count @last-val)))
    )

  )