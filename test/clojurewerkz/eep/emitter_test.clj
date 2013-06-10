(ns clojurewerkz.eep.emitter-test
  (:use clojure.test
        clojurewerkz.eep.emitter)
  (:require [clojurewerkz.eep.stats :as stats]
            [clojurewerkz.eep.windows :as windows]
            [clojurewerkz.eep.clocks :as clocks]))

(deftest a-test
  (let [emitter (new-emitter)]
    (defaggregator emitter :count + 100)
    (notify emitter :count 1)
    (notify emitter :count 1)
    (notify emitter :count 1)
    (Thread/sleep 100)
    (is (= 103 (state (get-handler emitter :count))))))

(deftest t-defobserver
  (let [emitter (new-emitter)]
    (defaggregator emitter :count (fn [orig new]
                                    (Thread/sleep 200)
                                    (+ orig new))
      100)

    (is (= 100 (state (get-handler emitter :count))))
    (notify emitter :count 1)
    (Thread/sleep 200)
    (is (= 101 (state (get-handler emitter :count)))))

  (let [emitter (new-emitter)
        latch   (java.util.concurrent.CountDownLatch. 5)]
    (defobserver emitter :countdown (fn [_]
                                      (.countDown latch)))
    (dotimes [i 5]
      (notify emitter :countdown 1))

    (is (.await latch 500 java.util.concurrent.TimeUnit/MILLISECONDS))))

;; Add with-emitter macro that'd thread emitter through add-handler

(deftest filter-pipe-test
  (let [emitter (new-emitter)]
    (deffilter emitter :entrypoint even? :summarizer)
    (defaggregator emitter :summarizer + 0)
    (notify emitter :entrypoint 1)
    (notify emitter :entrypoint 2)
    (notify emitter :entrypoint 3)
    (notify emitter :entrypoint 4)
    (notify emitter :entrypoint 5)
    (Thread/sleep 200)
    (is (= 6 (state (get-handler emitter :summarizer))))))

(deftest multicast-test
  (testing "Basic multicast abilities"
    (let [emitter (new-emitter)]
      (defmulticast emitter :entrypoint [:summarizer1 :summarizer2 :summarizer3])
      (defaggregator emitter :summarizer1 + 0)
      (defaggregator emitter :summarizer2 + 0)
      (defaggregator emitter :summarizer3 + 0)
      (notify emitter :entrypoint 1)
      (notify emitter :entrypoint 2)
      (notify emitter :entrypoint 3)
      (Thread/sleep 100)
      (is (= 6 (state (get-handler emitter :summarizer1))))
      (is (= 6 (state (get-handler emitter :summarizer2))))
      (is (= 6 (state (get-handler emitter :summarizer3))))))

  (testing "Re-adding multicast"
    (let [emitter (new-emitter)]
      (defmulticast emitter :entrypoint [:summarizer1])
      (defmulticast emitter :entrypoint [:summarizer2])
      (defmulticast emitter :entrypoint [:summarizer3])
      (defaggregator emitter :summarizer1 + 0)
      (defaggregator emitter :summarizer2 + 0)
      (defaggregator emitter :summarizer3 + 0)
      (notify emitter :entrypoint 1)
      (notify emitter :entrypoint 2)
      (notify emitter :entrypoint 3)
      (Thread/sleep 100)
      (is (= 6 (state (get-handler emitter :summarizer1))))
      (is (= 6 (state (get-handler emitter :summarizer2))))
      (is (= 6 (state (get-handler emitter :summarizer3)))))))

(deftest transform-test
  (let [emitter (new-emitter)]
    (deftransformer emitter :entrypoint (partial * 2) :summarizer)
    (defaggregator emitter :summarizer + 0)
    (notify emitter :entrypoint 1)
    (notify emitter :entrypoint 2)
    (notify emitter :entrypoint 3)
    (notify emitter :entrypoint 4)
    (notify emitter :entrypoint 5)
    (Thread/sleep 200)
    (is (= 30 (state (get-handler emitter :summarizer))))))

(deftest test-splitter
  (let [emitter (new-emitter)]
    (defsplitter emitter :entrypoint (fn [i] (if (even? i) :even :odd)))
    (defaggregator emitter :even + 0)
    (defaggregator emitter :odd + 0)
    (notify emitter :entrypoint 1)
    (notify emitter :entrypoint 2)
    (notify emitter :entrypoint 3)
    (notify emitter :entrypoint 4)
    (notify emitter :entrypoint 5)

    (Thread/sleep 100)
    (is (= 6 (state (get-handler emitter :even))))
    (is (= 9 (state (get-handler emitter :odd))))))
