(ns ^{:doc "Generic implementation clocks to be used in windowed oprations"}
  clojurewerkz.eep.clocks)

(defprotocol Clock
  (time [this] "Returns current clock time.")
  (elapsed? [this] "Returns wether clock is elapsed")
  (reset [_] "Resets clock")
  (tick [_] "Makes a tick within clock, updating internal counter"))

(deftype CountingClock [initial period current]
  Clock
  (time [_]
    current)

  (elapsed? [_]
    (> (- current initial) period))

  (tick [_]
    (CountingClock. initial period (inc current)))

  (reset [_]
    (CountingClock. current period current))

  Object
  (toString [_]
    (str "Initial: " initial ", Period: " period ", Current:" current)))

(defn- now
  "java.util.Date resolution is not enough for the Clock, as enqueue that's fired exactly after clock creation will
  produce a tick that yields same exact time."
  []
  (System/currentTimeMillis))

(deftype WallClock [initial period current]
  Clock
  (time [_]
    current)

  (elapsed? [_]
    (>= (- current initial) period))

  (tick [this]
    (WallClock. initial period (now)))

  (reset [_]
    (WallClock. (now) period (now)))

  Object
  (toString [_]
    (str "Initial: " initial ", Period: " period ", Current:" current)))


(defn make-counting-clock
  "Simplest clock implementation that increments counter on each clock tick."
  [period]
  (CountingClock. 0 period 0))

(defn make-wall-clock
  "Wall clock, using System/currentTimeMillis to check wether timer is elapsed"
  [period]
  (WallClock. (now) period (now)))

(def period
  {:second 1000
   :seconds 1000
   :minute (* 60 1000)
   :minutes (* 60 1000)
   :hour (* 60 60 1000)
   :hours (* 60 60 1000)
   :day (* 24 60 60 1000)
   :days (* 24 60 60 1000)
   :weeks (* 24 60 60 1000)})
