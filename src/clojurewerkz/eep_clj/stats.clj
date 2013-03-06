(ns clojurewerkz.eep-clj.stats
  (:require [clojurewerkz.eep-clj.emitter :as emitter]))

(defprotocol Clock
  (at [_])
  (increment [_])
  (ticked? [_])
  (expired? [_])
  )

(defprotocol IWindow
  (enqueue [_ v])
  (clock [_]))

(defprotocol Resetable
  (reset [_]))

(defprotocol Ticking
  (tick [_]))

(defprotocol Stat
  (title [_])
  (accumulate [this _])
  (compensate [this])
  (emit [this]))

(deftype Count [val]
  Stat
  (title [_] :count)

  (accumulate [_ _]
    (Count. (inc val)))

  (compensate [_]
    (Count. (dec val)))

  (emit [_]
    val)

  Resetable
  (reset [_]
    (println "Reseteting")
    (Count. 0))

  Object
  (toString [_]
    (str val)))

(defn make-count
  [val]
  (Count. val))

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
    (str "At: " at ", Mark: " mark))
  )

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
    (str "At: " at ", Mark: " mark))
  )


(defn make-counting-clock
  [at mark]
  (CountingClock. at mark))

(defn make-wall-clock
  [at mark]
  (WallClock. (now) 0 (now) mark))

(deftype MonotonicWindow [e]
  IWindow
  (enqueue [_ v]
    (emitter/sync-notify e :aggregate [accumulate v])
    (emitter/sync-notify e :clock increment))

  (clock [_]
    @(.state (first (emitter/which-handlers e :clock))))

  Ticking
  (tick [this]
    (emitter/sync-notify e :clock tick)
    (when (ticked? (.clock this))
      (emitter/sync-notify e :clock tick)
      (when (expired? (.clock this))
        (emitter/sync-notify e :emit (map emitter/state (emitter/which-handlers e :aggregate)))
        (emitter/sync-notify e :clock reset)
        (emitter/sync-notify e :aggregate [reset]))))

  Object
  (toString [this]
    (.toString e)))


(defn aggregate-wrap
  [prev [f v]]
  (if v
    (f prev v)
    (f prev)))

(defn tick-wrap
  [a b]
  (b a))

(defn monotonic-window
  [aggregate clock h]
  (let [e (emitter/new-emitter)]
    (emitter/add-handler e :aggregate aggregate-wrap aggregate)
    (emitter/add-handler e :clock tick-wrap clock)
    (emitter/add-handler e :emit #(h (into {} (for [i %]
                                                (let [stat (deref i)]
                                                  [(title stat) (emit stat)])))))
    (MonotonicWindow. e)))