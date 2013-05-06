(ns clojurewerkz.eep.emitter-test
  (:use clojure.test
        clojurewerkz.eep.emitter)
  (:require [clojurewerkz.eep.stats :as stats]
            [clojurewerkz.eep.windows :as windows]
            [clojurewerkz.eep.clocks :as clocks]))

(deftest t-add-aggregator
  (let [emitter (new-emitter)]
    (add-aggregator emitter :count + 100)
    (add-aggregator emitter :count + 100)
    (add-observer emitter :count println)
    (is (= 3 (count (which-handlers emitter :count))))))

(deftest t-delete-handler
  (let [emitter (new-emitter)]
    (add-aggregator emitter :count + 100)
    (add-aggregator emitter :count + 100)
    (add-observer emitter :count println)

    (delete-handler emitter :count println)
    (is (= 2 (count (which-handlers emitter :count)))))

  (let [emitter (new-emitter)]
    (add-aggregator emitter :count + 100)
    (add-aggregator emitter :count + 100)
    (add-observer emitter :count println)
    (is (= 3 (count (which-handlers emitter :count))))
    (delete-handler emitter :count +)
    (is (= 1 (count (which-handlers emitter :count)))))

  (let [emitter (new-emitter)]
    (add-aggregator emitter :count + 100)
    (add-aggregator emitter :count + 100)
    (add-observer emitter :count (vary-meta println assoc :our-func true))
    (is (= 3 (count (which-handlers emitter :count))))
    (delete-handler-by emitter :count #(:our-func (meta (.f %))))
    (is (= 2 (count (which-handlers emitter :count))))))

(deftest a-test
  (let [emitter (new-emitter)]
    (add-aggregator emitter :count + 100)

    (notify emitter :count 1)
    (notify emitter :count 1)
    (notify emitter :count 1)

    (Thread/sleep 150)

    (is (= 103 (state (first (:count (which-handlers emitter))))))))

(deftest t-add-observer
  (let [emitter (new-emitter)]
    (add-aggregator emitter :count (fn [orig new]
                                  (Thread/sleep 200)
                                  (+ orig new))
                 100)

    (notify emitter :count 1)
    (is (= 100 (state (first (:count (which-handlers emitter))))))
    (Thread/sleep 200)
    (is (= 101 (state (first (:count (which-handlers emitter)))))))

  (let [emitter (new-emitter)
        latch   (java.util.concurrent.CountDownLatch. 5)]
    (add-observer emitter :countdown (fn [_]
                                       (.countDown latch)))
    (dotimes [i 5]
      (notify emitter :countdown 1))

    (is (.await latch 500 java.util.concurrent.TimeUnit/MILLISECONDS))))

;; Add with-emitter macro that'd thread emitter through add-handler
