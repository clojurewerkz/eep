(ns clojurewerkz.eep.emitter-test
  (:use clojure.test
        clojurewerkz.eep.emitter
        clojurewerkz.eep.test-utils)
  (:require [clojurewerkz.eep.stats :as stats]
            [clojurewerkz.eep.windows :as windows]
            [clojurewerkz.eep.clocks :as clocks]))

(deftest aggregator-test
  (let [emitter (create :dispatcher-type :ring-buffer)
        latch   (make-latch 3)]
    (defaggregator emitter :count (wrap-countdown latch +) 100)

    (dotimes [i 3]
      (notify emitter :count 1))

    (after-latch latch
                 (is (= 103 (state (get-handler emitter :count)))))
    (stop emitter)))

(deftest caggregator-test
  (let [emitter (create :dispatcher-type :ring-buffer)
        latch   (make-latch 3)]
    (defcaggregator emitter :count (wrap-countdown latch +) 100)

    (dotimes [i 3]
      (notify emitter :count 1))

    (after-latch latch
                 (is (= 103 (state (get-handler emitter :count)))))
    (stop emitter)))

(deftest t-defobserver
  (let [emitter (create :dispatcher-type :ring-buffer)
        latch   (make-latch 1)]
    (defaggregator emitter :count (wrap-countdown latch +) 100)
    (is (= 100 (state (get-handler emitter :count))))

    (notify emitter :count 1)

    (after-latch latch
                 (is (= 101 (state (get-handler emitter :count)))))
    (stop emitter))

  (let [emitter (create :dispatcher-type :ring-buffer)
        latch   (java.util.concurrent.CountDownLatch. 5)]
    (defobserver emitter :countdown (fn [_]
                                      (.countDown latch)))
    (dotimes [i 5]
      (notify emitter :countdown 1))

    (is (.await latch 500 java.util.concurrent.TimeUnit/MILLISECONDS))
    (stop emitter)))

;; Add with-emitter macro that'd thread emitter through add-handler

(deftest filter-pipe-test
  (let [emitter (create :dispatcher-type :ring-buffer)
        latch   (make-latch 2)]
    (deffilter emitter :entrypoint even? :summarizer)
    (defaggregator emitter :summarizer (wrap-countdown latch +) 0)
    (notify emitter :entrypoint 1)
    (notify emitter :entrypoint 2)
    (notify emitter :entrypoint 3)
    (notify emitter :entrypoint 4)
    (notify emitter :entrypoint 5)
    (after-latch latch
                 (is (= 6 (state (get-handler emitter :summarizer)))))
    (stop emitter)))

(deftest multicast-test
  (testing "Basic multicast abilities"
    (let [emitter (create :dispatcher-type :ring-buffer)
          latch   (make-latch 9)
          f       (wrap-countdown latch +)]
      (defmulticast emitter :entrypoint [:summarizer1 :summarizer2 :summarizer3])
      (defaggregator emitter :summarizer1 f 0)
      (defaggregator emitter :summarizer2 f 0)
      (defaggregator emitter :summarizer3 f 0)
      (notify emitter :entrypoint 1)
      (notify emitter :entrypoint 2)
      (notify emitter :entrypoint 3)

      (after-latch latch
                   (is (= 6 (state (get-handler emitter :summarizer1))))
                   (is (= 6 (state (get-handler emitter :summarizer2))))
                   (is (= 6 (state (get-handler emitter :summarizer3)))))
      (stop emitter)))

  (testing "Re-adding multicast"
    (let [emitter (create :dispatcher-type :ring-buffer)
          latch   (make-latch 3)
          f       (wrap-countdown latch +)]
      (defmulticast emitter :entrypoint [:summarizer1])
      (defmulticast emitter :entrypoint [:summarizer2])
      (defmulticast emitter :entrypoint [:summarizer3])
      (defaggregator emitter :summarizer1 f 0)
      (defaggregator emitter :summarizer2 f 0)
      (defaggregator emitter :summarizer3 f 0)

      (notify emitter :entrypoint 1)
      (notify emitter :entrypoint 2)
      (notify emitter :entrypoint 3)

      (after-latch latch
                   (is (= 6 (state (get-handler emitter :summarizer1))))
                   (is (= 6 (state (get-handler emitter :summarizer2))))
                   (is (= 6 (state (get-handler emitter :summarizer3)))))
      (stop emitter))))

(deftest transform-test
  (let [emitter (create :dispatcher-type :ring-buffer)
        latch   (make-latch 5)]
    (deftransformer emitter :entrypoint (partial * 2) :summarizer)
    (defaggregator emitter :summarizer (wrap-countdown latch +) 0)
    (notify emitter :entrypoint 1)
    (notify emitter :entrypoint 2)
    (notify emitter :entrypoint 3)
    (notify emitter :entrypoint 4)
    (notify emitter :entrypoint 5)

    (after-latch latch
                 (is (= 30 (state (get-handler emitter :summarizer)))))
    (stop emitter)))

(deftest test-splitter
  (let [emitter (create :dispatcher-type :ring-buffer)
        latch   (make-latch 5)
        f       (wrap-countdown latch +)]
    (defsplitter emitter :entrypoint (fn [i] (if (even? i) :even :odd)))
    (defaggregator emitter :even f 0)
    (defaggregator emitter :odd f 0)
    (notify emitter :entrypoint 1)
    (notify emitter :entrypoint 2)
    (notify emitter :entrypoint 3)
    (notify emitter :entrypoint 4)
    (notify emitter :entrypoint 5)

    (after-latch latch
                 (is (= 6 (state (get-handler emitter :even))))
                 (is (= 9 (state (get-handler emitter :odd)))))
    (stop emitter)))


(deftest test-carefully
  (let [emitter (create :dispatcher-type :ring-buffer)]
    (defaggregator emitter :entrypoint (wrap-carefully emitter :entrypoint +) 0)
    (notify emitter :entrypoint "a")
    (Thread/sleep 100)
    (is (not (nil? (:entrypoint (.errors emitter)))))
    (stop emitter)))

(deftest test-splitter-dsl
  (let [latch   (make-latch 5)
        f       (wrap-countdown latch +)
        e       (create)
        emitter (build-topology e
                                :entrypoint (defsplitter (fn [i] (if (even? i) :even :odd)))
                                :even (defaggregator f 0)
                                :odd  (defaggregator f 0))]

    (notify emitter :entrypoint 1)
    (notify emitter :entrypoint 2)
    (notify emitter :entrypoint 3)
    (notify emitter :entrypoint 4)
    (notify emitter :entrypoint 5)

    (after-latch latch
                 (is (= 6 (state (get-handler emitter :even))))
                 (is (= 9 (state (get-handler emitter :odd)))))
    (stop emitter)))

(deftest test-rollup
  (let [emitter (create)]
    (defrollup emitter :entrypoint 100 :buffer)
    (defaggregator emitter :buffer keep-last nil)
    (notify emitter :entrypoint 1)
    (notify emitter :entrypoint 2)
    (notify emitter :entrypoint 3)
    (is (nil? (state (get-handler emitter :buffer))))
    (Thread/sleep 110)
    (is (= [1 2 3] (state (get-handler emitter :buffer))))))

(deftest group-aggregate-test
  (is (= {:a 4 :b 6} (group-aggregate stats/sum [[:a 1] [:b 2] [:a 3] [:b 4]]))))

(deftest emitter-stop-alive-test
  (let [emitter (create :dispatcher-type :ring-buffer)]
    (is (alive? emitter))
    (stop emitter)
    (is (not (alive? emitter)))))
