(ns clojurewerkz.eep.clocks)

(defprotocol Clock
  (time [_])
  (elapsed? [_])
  (reset [_])
  (tick [_] ""))

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
  (System/currentTimeMillis)
;;  (System/nanoTime)
  )

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
  [period]
  (CountingClock. 0 period 0))

(defn make-wall-clock
  [period]
  (WallClock. (now) period (now)))

(def period
  {:second 1000
   :minute (* 60 1000)
   :hour (* 60 60 1000)
   :day (* 24 60 60 1000)
   :week (* 24 60 60 1000)})
