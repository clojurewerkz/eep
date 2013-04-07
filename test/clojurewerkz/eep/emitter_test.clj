(ns clojurewerkz.eep.emitter-test
  (:use clojure.test
        clojurewerkz.eep.emitter)
  (:require [clojurewerkz.eep.stats :as stats]
            [clojurewerkz.eep.windows :as windows]
            [clojurewerkz.eep.clocks :as clocks]))

(deftest t-add-handler
  (let [emitter (new-emitter)]
    (add-handler emitter :count + 100)
    (add-handler emitter :count + 100)
    (add-handler emitter :count println)
    (is (= 3 (count (which-handlers emitter :count))))))

(deftest t-delete-handler
  (let [emitter (new-emitter)]
    (add-handler emitter :count + 100)
    (add-handler emitter :count + 100)
    (add-handler emitter :count println)
    (delete-handler emitter :count println)
    (is (= 2 (count (which-handlers emitter :count)))))

  (let [emitter (new-emitter)]
    (add-handler emitter :count + 100)
    (add-handler emitter :count + 100)
    (add-handler emitter :count println)
    (is (= 3 (count (which-handlers emitter :count))))
    (delete-handler emitter :count +)
    (is (= 1 (count (which-handlers emitter :count)))))

  (let [emitter (new-emitter)]
    (add-handler emitter :count + 100)
    (add-handler emitter :count + 100)
    (add-handler emitter :count (vary-meta println assoc :our-func true))
    (is (= 3 (count (which-handlers emitter :count))))
    (delete-handler-by emitter :count #(:our-func (meta (.handler %))))
    (is (= 2 (count (which-handlers emitter :count))))))

(deftest a-test
  (let [emitter (new-emitter)]
    (add-handler emitter :count + 100)

    (sync-notify emitter :count 1)
    (sync-notify emitter :count 1)
    (sync-notify emitter :count 1)

    (is (= 103 (state (first (:count (which-handlers emitter))))))))

(deftest t-some-function
  (let [emitter (new-emitter)]
    (add-handler emitter :count (fn [orig new]
                                  (Thread/sleep 200)
                                  (+ orig new))
                 100)

    (notify emitter :count 1)
    (is (= 100 (state (first (:count (which-handlers emitter))))))
    (Thread/sleep 200)
    (is (= 101 (state (first (:count (which-handlers emitter)))))))

  (let [emitter (new-emitter)
        latch   (java.util.concurrent.CountDownLatch. 5)]
    (add-handler emitter :countdown (fn [_]
                                  (.countDown latch)))
    (dotimes [i 5]
      (notify emitter :countdown 1))

    (is (.await latch 500 java.util.concurrent.TimeUnit/MILLISECONDS))))
