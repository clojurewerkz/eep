(ns clojurewerkz.eep.emitter-test
  (:use clojure.test
        clojurewerkz.eep.emitter)
  (:require [clojurewerkz.eep.stats :as stats]
            [clojurewerkz.eep.windows :as windows]
            [clojurewerkz.eep.clocks :as clocks]))

(deftest t-defaggregator
  (let [emitter (new-emitter)]
    (defaggregator emitter :count + 100)
    (defaggregator emitter :count + 100)
    (defobserver emitter :count println)
    (is (= 3 (count (which-handlers emitter :count))))))

(deftest t-delete-handler
  (let [emitter (new-emitter)]
    (defaggregator emitter :count + 100)
    (defaggregator emitter :count + 100)
    (defobserver emitter :count println)

    (delete-handler emitter :count println)
    (is (= 2 (count (which-handlers emitter :count)))))

  (let [emitter (new-emitter)]
    (defaggregator emitter :count + 100)
    (defaggregator emitter :count + 100)
    (defobserver emitter :count println)
    (is (= 3 (count (which-handlers emitter :count))))
    (delete-handler emitter :count +)
    (is (= 1 (count (which-handlers emitter :count)))))

  (let [emitter (new-emitter)]
    (defaggregator emitter :count + 100)
    (defaggregator emitter :count + 100)
    (defobserver emitter :count (vary-meta println assoc :our-func true))
    (is (= 3 (count (which-handlers emitter :count))))
    (delete-handler-by emitter :count #(:our-func (meta (.f %))))
    (is (= 2 (count (which-handlers emitter :count))))))

(deftest a-test
  (let [emitter (new-emitter)]
    (defaggregator emitter :count + 100)

    (notify emitter :count 1)
    (notify emitter :count 1)
    (notify emitter :count 1)

    (Thread/sleep 150)

    (is (= 103 (state (first (which-handlers emitter :count)))))))

(deftest t-defobserver
  (let [emitter (new-emitter)]
    (defaggregator emitter :count (fn [orig new]
                                    (Thread/sleep 200)
                                    (+ orig new))
      100)

    (notify emitter :count 1)
    (is (= 100 (state (first (which-handlers emitter :count)))))
    (Thread/sleep 200)
    (is (= 101 (state (first (which-handlers emitter :count))))))

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
    (is (= 6 (state (first (which-handlers emitter :summarizer)))))))
