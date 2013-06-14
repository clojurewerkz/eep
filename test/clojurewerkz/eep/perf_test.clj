(ns clojurewerkz.eep.perf-test
  (:use clojure.test
        clojurewerkz.eep.test-utils
        clojurewerkz.eep.emitter)
  (:require [clojurewerkz.eep.stats :as stats]
            [clojurewerkz.eep.windows :as windows]
            [clojurewerkz.eep.clocks :as clocks]))

(deftest ^:perf perf-test
  (doseq [dt [:event-loop :thread-pool :ring-buffer]]
    (let [iterations 200
          emitter    (new-emitter :dispatcher-type dt)
          latch      (make-latch iterations)]
      (defaggregator emitter :count (wrap-countdown latch +) 0)
      (println "Dispatcher type: " dt)
      (time
       (dotimes [i iterations]
         (notify emitter :count 1)))

      (after-latch latch
                   (is (= iterations (state (get-handler emitter :count))))))))
