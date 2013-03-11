(ns clojurewerkz.eep.stats
  (:use clojurewerkz.eep.core)
  (:require [clojurewerkz.eep.emitter :as emitter]))

(deftype Count [val]
  Stat
  (title [_] :count)

  (accumulate [_ _]
    (Count. (inc val)))

  (compensate [_ _]
    (Count. (dec val)))

  (emit [_]
    val)

  Resetable
  (reset [_]
    (Count. 0))

  Object
  (toString [_]
    (str val)))

(deftype Mean [mean c d]
  Stat
  (title [_] :mean)

  (accumulate [_ new-val]
    (let [new-c (inc c)
          new-d (- new-val mean)]
      (Mean. (+ mean (/ new-d new-c)) new-c new-d)))

  (compensate [_ new-val]
    (let [new-c (dec c)
          new-d (- mean new-val)]
      (Mean. (+ mean (/ new-d new-c)) new-c new-d)))

  (emit [_]
    mean)

  Resetable
  (reset [_]
    (Mean. 0 0 0))

  Object
  (toString [_]
    (str mean)))

(defn make-count
  []
  (Count. 0))

(defn make-mean
  []
  (Mean. 0 0 0))
