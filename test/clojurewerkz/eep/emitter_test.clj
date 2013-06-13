(ns clojurewerkz.eep.emitter-test
  (:use clojure.test
        clojurewerkz.eep.emitter
        clojurewerkz.eep.test-utils)
  (:require [clojurewerkz.eep.stats :as stats]
            [clojurewerkz.eep.windows :as windows]
            [clojurewerkz.eep.clocks :as clocks]))

(alter-var-root #'*out* (constantly *out*))

(deftest a-test
  (let [emitter (new-emitter)
        latch   (make-latch 3)]
    (defaggregator emitter :count (wrap-countdown latch +) 100)

    (dotimes [i 4]
      (notify emitter :count 1))

    (await-latch latch)
    (is (= 103 (state (get-handler emitter :count))))))

(deftest t-defobserver
  (let [emitter (new-emitter)
        latch   (make-latch 1)]
    (defaggregator emitter :count (wrap-countdown latch +) 100)
    (is (= 100 (state (get-handler emitter :count))))

    (notify emitter :count 1)
    (await-latch latch)

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
  (let [emitter (new-emitter)
        latch   (make-latch 2)]
    (deffilter emitter :entrypoint even? :summarizer)
    (defaggregator emitter :summarizer (wrap-countdown latch +) 0)
    (notify emitter :entrypoint 1)
    (notify emitter :entrypoint 2)
    (notify emitter :entrypoint 3)
    (notify emitter :entrypoint 4)
    (notify emitter :entrypoint 5)
    (await-latch latch)
    (is (= 6 (state (get-handler emitter :summarizer))))))

(deftest multicast-test
  (testing "Basic multicast abilities"
    (let [emitter (new-emitter)
          latch   (make-latch 3)
          f       (wrap-countdown latch +)]
      (defmulticast emitter :entrypoint [:summarizer1 :summarizer2 :summarizer3])
      (defaggregator emitter :summarizer1 f 0)
      (defaggregator emitter :summarizer2 f 0)
      (defaggregator emitter :summarizer3 f 0)
      (notify emitter :entrypoint 1)
      (notify emitter :entrypoint 2)
      (notify emitter :entrypoint 3)

      (await-latch latch)
      (is (= 6 (state (get-handler emitter :summarizer1))))
      (is (= 6 (state (get-handler emitter :summarizer2))))
      (is (= 6 (state (get-handler emitter :summarizer3))))))

  (testing "Re-adding multicast"
    (let [emitter (new-emitter)
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

      (await-latch latch)
      (is (= 6 (state (get-handler emitter :summarizer1))))
      (is (= 6 (state (get-handler emitter :summarizer2))))
      (is (= 6 (state (get-handler emitter :summarizer3)))))))

(deftest transform-test
  (let [emitter (new-emitter)
        latch   (make-latch 5)]
    (deftransformer emitter :entrypoint (partial * 2) :summarizer)
    (defaggregator emitter :summarizer (wrap-countdown latch +) 0)
    (notify emitter :entrypoint 1)
    (notify emitter :entrypoint 2)
    (notify emitter :entrypoint 3)
    (notify emitter :entrypoint 4)
    (notify emitter :entrypoint 5)

    (await-latch latch)
    (is (= 30 (state (get-handler emitter :summarizer))))))

(deftest test-splitter
  (let [emitter (new-emitter)
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

    (await-latch latch)
    (is (= 6 (state (get-handler emitter :even))))
    (is (= 9 (state (get-handler emitter :odd))))))


(deftest test-carefully
  (let [emitter (new-emitter)]
    (defaggregator emitter :entrypoint (wrap-carefully emitter :entrypoint +) 0)
    (notify emitter :entrypoint "a")
    (Thread/sleep 100)
    (is (not (nil? (:entrypoint (.errors emitter)))))))
