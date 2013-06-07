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
    (is (= 103 (state (which-handlers emitter :count))))))

(deftest t-defobserver
  (let [emitter (new-emitter)]
    (defaggregator emitter :count (fn [orig new]
                                    (Thread/sleep 200)
                                    (+ orig new))
      100)

    (is (= 100 (state (which-handlers emitter :count))))
    (notify emitter :count 1)
    (is (= 101 (state (which-handlers emitter :count)))))

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
    (is (= 6 (state (which-handlers emitter :summarizer))))))

(deftest multicast-test
  (let [emitter (new-emitter)]
    (defmulticast emitter :entrypoint [:summarizer1 :summarizer2 :summarizer3])
    (defaggregator emitter :summarizer1 + 0)
    (defaggregator emitter :summarizer2 + 0)
    (defaggregator emitter :summarizer3 + 0)
    (notify emitter :entrypoint 1)
    (notify emitter :entrypoint 2)
    (notify emitter :entrypoint 3)
    (is (= 6 (state (which-handlers emitter :summarizer1))))
    (is (= 6 (state (which-handlers emitter :summarizer2))))
    (is (= 6 (state (which-handlers emitter :summarizer3))))))

(deftest transform-test
  (let [emitter (new-emitter)]
    (deftransformer emitter :entrypoint (partial * 2) :summarizer)
    (defaggregator emitter :summarizer + 0)
    (notify emitter :entrypoint 1)
    (notify emitter :entrypoint 2)
    (notify emitter :entrypoint 3)
    (notify emitter :entrypoint 4)
    (notify emitter :entrypoint 5)
    (is (= 30 (state (which-handlers emitter :summarizer))))))
