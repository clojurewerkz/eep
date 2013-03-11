(ns clojurewerkz.eep.clocks
  (:use clojurewerkz.eep.core))

(deftype CountingClock [at mark]
  Clock
  (at [_]
    at)
  (increment [_]
    (CountingClock. (inc at) mark))

  (ticked? [_]
    (>= (- at mark) 1))

  Ticking
  (tick [this]
    (if (nil? mark)
      (CountingClock. at (inc at))
      this))

  Object
  (toString [_]
    (str "At: " at ", Mark: " mark)))

(defn- now
  "java.util.Date resolution is not enough for the Clock, as enqueue that's fired exactly after clock creation will
  produce a tick that yields same exact time."
  []
  (System/nanoTime))

(deftype WallClock [initial interval at mark]
  Clock
  (at [_]
    at)

  (increment [_]
    (WallClock. initial interval (now) mark))

  (ticked? [_]
    (>= (- at mark) interval))

  (expired? [_]
    (>= (- at initial) 1))

  Resetable
  (reset [_]
    (WallClock. (now) interval at nil))

  ;; Tock is not necessary, either. Mostly because we can tick twice, as we don't have shared mutable state
  ;; (tock [this elapsed]
  ;;   (if elapsed
  ;;     (WallClock. initial interval at (+ mark interval))))


  Ticking
  (tick [this]
    (if (nil? mark)
      (WallClock. initial interval at (+ at interval))
      this))


  Object
  (toString [_]
    (str "At: " at ", Mark: " mark)))

(defn make-counting-clock
  []
  (CountingClock. 0 0))

(defn make-wall-clock
  []
  (WallClock. (now) 0 (now) nil))