(ns clojurewerkz.eep.throughput-test
  (:use clojure.test
        clojurewerkz.eep.emitter
        clojurewerkz.eep.test-utils)
  (:import [reactor.event.dispatch RingBufferDispatcher]
           [com.lmax.disruptor.dsl ProducerType]
           [com.lmax.disruptor YieldingWaitStrategy]))

(defn throughput-test
  [emitter iterations]
  (let [latch      (make-latch (/ iterations 2))]
    (defsplitter emitter :entry (fn [e]
                              (if (even? e) :even :countdown)))

    (defn incrementer [acc _]
      (inc acc))

    (defaggregator emitter :even incrementer 0)
    (defobserver emitter :countdown (fn [_]
                                      (.countDown @latch)))

    (let [start (System/currentTimeMillis)]
      (doseq [i (range iterations)]
        (notify emitter :entry i))

      (after-latch latch
                   (let [end     (System/currentTimeMillis)
                         elapsed (- end start)]
                     (is (= (/ iterations 2) (state (get-handler emitter :even))))
                     (println
                      (str
                       "Iterations: "
                       iterations
                       " "
                       (-> emitter
                           (.reactor)
                           (.getDispatcher)
                           (.getClass)
                           (.getSimpleName))
                       " throughput (" elapsed "ms): " (Math/round (float (/ iterations (/ elapsed 1000)))) "/sec")))))

    (stop emitter)))

(deftest ^:performance dispatcher-throughput-test
  (doseq [i [10000 100000 1000000]]
    (testing "Event Loop"
      (throughput-test (create :dispatcher-type :event-loop) i))
    (testing "Thread Pool Executor"
      (throughput-test (create :dispatcher-type :thread-pool) i))
    (testing "Ring Buffer"
      (throughput-test (create :dispatcher-type :ring-buffer) i))
    (testing "Ring Buffer"
      (throughput-test (create :dispatcher (RingBufferDispatcher. "dispatcher-name" 4096  ProducerType/MULTI (YieldingWaitStrategy.))) i))))
