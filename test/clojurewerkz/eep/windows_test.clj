(ns clojurewerkz.eep.windows-test
  (:use clojure.test
        clojurewerkz.eep.windows)
  (:require [clojurewerkz.eep.emitter :as e]
            [clojurewerkz.eep.stats :as s]
            [clojurewerkz.eep.clocks :as c]))

(defn sum
  [buffer]
  (apply + buffer))

(deftest simple-sliding-window-test
  (let [last-val (atom nil)
        window (sliding-window-simple 5 sum #(reset! last-val %))]
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
        window (tumbling-window-simple 5 sum #(reset! last-val %))]
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
        window (monotonic-window-simple (c/make-counting-clock 5) sum #(reset! last-val %))]
    (is (nil? @last-val))
    (window 1)
    (is (nil? @last-val))
    (window 1)
    (window 1)
    (window 1)
    (window 1)
    (is (= 5 @last-val))
    (window 1)
    (is (= 5 @last-val)))

  (let [last-val (atom nil)
        window (monotonic-window-simple (c/make-wall-clock 1000) sum #(reset! last-val %))]
    (is (nil? @last-val))
    (window 1)
    (is (nil? @last-val))
    (window 1)
    (window 1)
    (window 1)
    (Thread/sleep 1000)
    (window 1)
    (is (= 5 @last-val))
    (window 1)
    (is (= 5 @last-val))))

(deftest emitter-sliding-window-test
  (let [emitter (e/new-emitter)
        last-val (atom nil)
        window (sliding-window-simple 5 sum #(reset! last-val %))]
    (e/add-handler emitter :sliding-summing-window window)
    (e/notify emitter :sliding-summing-window 1)
    (e/notify emitter :sliding-summing-window 2)
    (e/notify emitter :sliding-summing-window 3)
    (e/notify emitter :sliding-summing-window 4)
    (e/notify emitter :sliding-summing-window 5)
    (e/flush-futures emitter)
    (is (= 15 @last-val))
    (e/notify emitter :sliding-summing-window 6)
    (e/flush-futures emitter)
    (is (= 20 @last-val))
    (e/notify emitter :sliding-summing-window 7)
    (e/flush-futures emitter)
    (is (= 25 @last-val))))

(deftest emitter-tumbling-window-test
  (let [emitter (e/new-emitter)
        last-val (atom nil)
        window (tumbling-window-simple 5 sum #(reset! last-val %))]
    (e/add-handler emitter :tumbling-summing-window window)
    (is (nil? @last-val))
    (e/notify emitter :tumbling-summing-window 1)
    (e/notify emitter :tumbling-summing-window 2)
    (e/notify emitter :tumbling-summing-window 3)
    (e/notify emitter :tumbling-summing-window 4)
    (e/notify emitter :tumbling-summing-window 5)
    (e/flush-futures emitter)
    (is (= 15 @last-val))

    (e/notify emitter :tumbling-summing-window 6)
    (e/flush-futures emitter)
    (is (= 15 @last-val))
    (e/notify emitter :tumbling-summing-window 7)
    (e/notify emitter :tumbling-summing-window 8)
    (e/notify emitter :tumbling-summing-window 9)
    (e/notify emitter :tumbling-summing-window 10)
    (e/flush-futures emitter)
    (is (= 40 @last-val))))

(deftest emitter-monotonic-window-test
  (let [emitter (e/new-emitter)
        last-val (atom nil)
        window (monotonic-window-simple (c/make-counting-clock 5) sum #(reset! last-val %))]
    (e/add-handler emitter :monotonic-summing-window window)
    (is (nil? @last-val))
    (e/notify emitter :monotonic-summing-window 1)
    (is (nil? @last-val))
    (e/notify emitter :monotonic-summing-window 1)
    (e/notify emitter :monotonic-summing-window 1)
    (e/notify emitter :monotonic-summing-window 1)
    (e/notify emitter :monotonic-summing-window 1)
    (e/flush-futures emitter)
    (is (= 5 @last-val))
    (e/notify emitter :monotonic-summing-window 1)
    (e/flush-futures emitter)
    (is (= 5 @last-val)))

  (let [emitter (e/new-emitter)
        last-val (atom nil)
        window (monotonic-window-simple (c/make-wall-clock 1000) sum #(reset! last-val %))]
    (e/add-handler emitter :monotonic-summing-window window)
    (is (nil? @last-val))
    (e/notify emitter :monotonic-summing-window 1)
    (is (nil? @last-val))
    (e/notify emitter :monotonic-summing-window 1)
    (e/notify emitter :monotonic-summing-window 1)
    (e/notify emitter :monotonic-summing-window 1)
    (Thread/sleep 1000)
    (e/notify emitter :monotonic-summing-window 1)
    (e/flush-futures emitter)
    (is (= 5 @last-val))
    (e/notify emitter :monotonic-summing-window 1)
    (e/flush-futures emitter)
    (is (= 5 @last-val))))