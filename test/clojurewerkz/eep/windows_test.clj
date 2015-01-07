(ns clojurewerkz.eep.windows-test
  (:require [clojurewerkz.eep.emitter :as e]
            [clojurewerkz.eep.stats :as s]
            [clojurewerkz.eep.clocks :as c]
            [clojure.test :refer :all]
            [clojurewerkz.eep.windows :refer :all]
            [clojurewerkz.eep.test-utils :refer :all]))

(defn sum
  [buffer]
  (apply + buffer))

(def timespan 100)

(deftest simple-sliding-window-test
  (let [last-val (atom nil)
        window (sliding-window-simple 5
                                      sum
                                      #(reset! last-val %))]
    (window 1)
    (window 2)
    (window 3)
    (window 4)
    (window 5)
    (is (= 15 @last-val))
    (window 6)
    (is (= 20 @last-val))
    (window 7)
    (is (= 25 @last-val))))

(deftest simple-tumbling-window-test
  (let [last-val (atom nil)
        window (tumbling-window-simple 5
                                       sum
                                       #(reset! last-val %))]
    (is (nil? @last-val))
    (window 1)
    (window 2)
    (window 3)
    (window 4)
    (window 5)
    (is (= 15 @last-val))

    (window 6)
    (is (= 15 @last-val))
    (window 7)
    (window 8)
    (window 9)
    (window 10)
    (is (= 40 @last-val))))

(deftest simple-monotonic-window-test
  (let [last-val (atom nil)
        window (monotonic-window-simple (c/make-counting-clock 5)
                                        sum
                                        #(reset! last-val %))]
    (is (nil? @last-val))
    (window 1)
    (is (nil? @last-val))
    (window 1)
    (window 1)
    (window 1)
    (window 1)
    (is (= nil @last-val))
    (window 1)
    (is (= 5 @last-val)))

  (let [last-val (atom nil)
        window (monotonic-window-simple (c/make-wall-clock timespan)
                                        sum
                                        #(reset! last-val %))]
    (is (nil? @last-val))
    (window 1)
    (is (nil? @last-val))
    (window 1)
    (window 1)
    (window 1)
    (Thread/sleep timespan)
    (window 1)
    (is (= 4 @last-val))
    (window 1)
    (is (= 4 @last-val))

    (Thread/sleep timespan)
    (window 1)
    (is (= 2 @last-val))))
