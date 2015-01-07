(ns clojurewerkz.eep.emitter-test
  (:require [clojurewerkz.meltdown.reactor :as reactor]
            [clojurewerkz.eep.emitter :refer :all]
            [clojurewerkz.eep.stats :as stats]
            [clojurewerkz.eep.windows :as windows]
            [clojurewerkz.eep.clocks :as clocks]
            [clojure.test :refer :all]
            [clojurewerkz.eep.test-utils :refer :all]))

;; Dispatcher types: #{:event-loop :thread-pool :ring-buffer}

(deftest test-aggregator
  (let [em      (create :dispatcher-type :ring-buffer)
        latch   (make-latch 3)]
     (defaggregator em :count (wrap-countdown latch +) 100)

     (dotimes [i 3]
       (notify em :count 1))

     (after-latch latch
                  (is (= 103 (state (get-handler em :count)))))
     (stop em)))

(deftest test-caggregator
  (let [em    (create :dispatcher-type :ring-buffer)
        latch (make-latch 3)]
     (defcaggregator em :count (wrap-countdown latch +) 100)

     (dotimes [i 3]
       (notify em :count 1))

     (Thread/sleep 10)
     (after-latch latch
                  (is (= 103 (state (get-handler em :count)))))
     (stop em)))

(deftest test-defobserver
  (let [em      (create :dispatcher-type :ring-buffer)
        latch   (make-latch 1)]
     (defaggregator em :count (wrap-countdown latch +) 100)
     (is (= 100 (state (get-handler em :count))))

     (notify em :count 1)

     (after-latch latch
                  (is (= 101 (state (get-handler em :count)))))
     (stop em))

  (let [em      (create :dispatcher-type :ring-buffer)
        latch   (make-latch 5)]
     (defobserver em :countdown (fn [_]
                                         (.countDown @latch)))
     (dotimes [i 5]
       (notify em :countdown 1))

     (is (.await @latch 500 java.util.concurrent.TimeUnit/MILLISECONDS))
     (stop em)))

(deftest test-filter-pipe
  (let [em    (create :dispatcher-type :ring-buffer)
        latch (make-latch 2)]
     (deffilter em :entrypoint even? :summarizer)
     (defaggregator em :summarizer (wrap-countdown latch +) 0)
     (notify em :entrypoint 1)
     (notify em :entrypoint 2)
     (notify em :entrypoint 3)
     (notify em :entrypoint 4)
     (notify em :entrypoint 5)
     (after-latch latch
                  (is (= 6 (state (get-handler em :summarizer)))))
     (stop em)))

(deftest test-multicast
  (testing "Basic multicast abilities"
    (let [em     (create :dispatcher-type :ring-buffer)
           latch (make-latch 9)
           f     (wrap-countdown latch +)]
       (defmulticast em :entrypoint [:summarizer1 :summarizer2 :summarizer3])
       (defaggregator em :summarizer1 f 0)
       (defaggregator em :summarizer2 f 0)
       (defaggregator em :summarizer3 f 0)
       (notify em :entrypoint 1)
       (notify em :entrypoint 2)
       (notify em :entrypoint 3)

       (Thread/sleep 10)
       (after-latch latch
                    (is (= 6 (state (get-handler em :summarizer1))))
                    (is (= 6 (state (get-handler em :summarizer2))))
                    (is (= 6 (state (get-handler em :summarizer3)))))
       (stop em)))

  (testing "Re-adding multicast"
    (let [em    (create :dispatcher-type :ring-buffer)
          latch (make-latch 3)
          f     (wrap-countdown latch +)]
       (defmulticast em :entrypoint [:summarizer1])
       (defmulticast em :entrypoint [:summarizer2])
       (defmulticast em :entrypoint [:summarizer3])
       (defaggregator em :summarizer1 f 0)
       (defaggregator em :summarizer2 f 0)
       (defaggregator em :summarizer3 f 0)

       (notify em :entrypoint 1)
       (notify em :entrypoint 2)
       (notify em :entrypoint 3)

       (Thread/sleep 10)
       (after-latch latch
                    (is (= 6 (state (get-handler em :summarizer1))))
                    (is (= 6 (state (get-handler em :summarizer2))))
                    (is (= 6 (state (get-handler em :summarizer3)))))
       (stop em))))

(deftest test-transform
  (let [em    (create :dispatcher-type :ring-buffer)
        latch (make-latch 5)]
     (deftransformer em :entrypoint (partial * 2) :summarizer)
     (defaggregator em :summarizer (wrap-countdown latch +) 0)
     (notify em :entrypoint 1)
     (notify em :entrypoint 2)
     (notify em :entrypoint 3)
     (notify em :entrypoint 4)
     (notify em :entrypoint 5)

     (Thread/sleep 10)
     (after-latch latch
                  (is (= 30 (state (get-handler em :summarizer)))))
     (stop em)))

(deftest test-splitter
  (let [em    (create :dispatcher-type :ring-buffer)
        latch (make-latch 5)
        f     (wrap-countdown latch +)]
     (defsplitter em :entrypoint (fn [i] (if (even? i) :even :odd)))
     (defaggregator em :even f 0)
     (defaggregator em :odd f 0)
     (notify em :entrypoint 1)
     (notify em :entrypoint 2)
     (notify em :entrypoint 3)
     (notify em :entrypoint 4)
     (notify em :entrypoint 5)

     (Thread/sleep 10)
     (after-latch latch
                  (is (= 6 (state (get-handler em :even))))
                  (is (= 9 (state (get-handler em :odd)))))
     (stop em)))


(deftest test-carefully
  (let [em (create :dispatcher-type :ring-buffer)]
    (defaggregator em :entrypoint (wrap-carefully em :entrypoint +) 0)
    (notify em :entrypoint "a")
    (Thread/sleep 100)
    (is (not (nil? (:entrypoint (.errors em)))))
    (stop em)))

(deftest test-threading-dsl
  (let [em        (create :dispatcher-type :ring-buffer)
        latch     (make-latch 5)
        f         (wrap-countdown latch +)]

     (-> em
         (defsplitter :entrypoint (fn [i] (if (even? i) :even :odd)))
         (defaggregator :even f 0)
         (defaggregator :odd f 0))

     (notify em :entrypoint 1)
     (notify em :entrypoint 2)
     (notify em :entrypoint 3)
     (notify em :entrypoint 4)
     (notify em :entrypoint 5)

     (after-latch latch
                  (is (= 6 (state (get-handler em :even))))
                  (is (= 9 (state (get-handler em :odd)))))
     (stop em)))

(deftest test-splitter-dsl
  (let [em        (create :dispatcher-type :ring-buffer)
        latch     (make-latch 5)
        f         (wrap-countdown latch +)]

     (build-topology em
                     :entrypoint (defsplitter (fn [i] (if (even? i) :even :odd)))
                     :even (defaggregator f 0)
                     :odd  (defaggregator f 0))

     (notify em :entrypoint 1)
     (notify em :entrypoint 2)
     (notify em :entrypoint 3)
     (notify em :entrypoint 4)
     (notify em :entrypoint 5)

     (after-latch latch
                  (is (= 6 (state (get-handler em :even))))
                  (is (= 9 (state (get-handler em :odd)))))
     (stop em)))

(deftest test-rollup
  (let [em (create :dispatcher-type :ring-buffer)]
   (defrollup     em :entrypoint 100 :buffer)
   (defaggregator em :buffer keep-last nil)
   (notify em :entrypoint 1)
   (notify em :entrypoint 2)
   (notify em :entrypoint 3)
   (is (nil? (state (get-handler em :buffer))))
   (Thread/sleep 110)
   (is (= [1 2 3] (state (get-handler em :buffer))))))

(deftest test-group-aggregate
  (is (= {:a 4 :b 6} (group-aggregate stats/sum [[:a 1] [:b 2] [:a 3] [:b 4]]))))

(deftest test-alive
  (let [em (create :dispatcher-type :ring-buffer)]
    (is (alive? em))
    (stop em)
    (is (not (alive? em)))))
