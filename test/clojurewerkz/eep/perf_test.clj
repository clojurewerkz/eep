(ns clojurewerkz.eep.perf-test
  (:use clojure.test
        clojurewerkz.eep.emitter)
  (:require [clojurewerkz.eep.stats :as stats]
            [clojurewerkz.eep.windows :as windows]
            [clojurewerkz.eep.clocks :as clocks]))

(deftest perf-test
  (let [emitter (new-emitter)]
    (defaggregator emitter :count (fn [a b]
                                    (+ a b)) 0)
    (time
     (dotimes [i 2000000]
       (notify emitter :count 1)))

    (is (= 2000000 (state (get-handler emitter :count))))))
