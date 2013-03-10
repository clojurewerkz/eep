(ns clojurewerkz.eep.stats
  (:require [clojurewerkz.eep.emitter :as emitter]))

(defprotocol Clock
  (at [_])
  (increment [_])
  (ticked? [_])
  (expired? [_]))

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
  (compensate [this _])
  (emit [this]))

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
  [at mark]
  (CountingClock. at mark))

(defn make-wall-clock
  [at mark]
  (WallClock. (now) 0 (now) mark))


(deftype MonotonicWindow [e]
  IWindow
  (enqueue [_ v]
    (emitter/notify e :aggregate [accumulate v])
    (emitter/sync-notify e :clock increment))

  (clock [_]
    (emitter/state (first (emitter/which-handlers e :clock))))

  Ticking
  (tick [this]
    (emitter/sync-notify e :clock tick)
    (when (ticked? (.clock this))
      (emitter/sync-notify e :clock tick)
      (when (expired? (.clock this))
        (emitter/flush-futures e)
        (emitter/sync-notify e :emit (map emitter/state (emitter/which-handlers e :aggregate)))
        (emitter/sync-notify e :clock reset)
        (emitter/sync-notify e :aggregate [reset]))))

  Object
  (toString [this]
    (.toString e)))

(deftype PeriodicWindow [e]
  IWindow
  (enqueue [_ v]
    (emitter/notify e :aggregate [accumulate v])
    (emitter/sync-notify e :clock increment))

  (clock [_]
    (emitter/state (first (emitter/which-handlers e :clock))))

  Ticking
  (tick [this]
    (emitter/sync-notify e :clock tick)
    (when (ticked? (.clock this))
      (emitter/sync-notify e :clock tick)
      (when (expired? (.clock this))
        (emitter/flush-futures e)
        (emitter/sync-notify e :emit (map emitter/state (emitter/which-handlers e :aggregate)))
        (emitter/sync-notify e :clock reset)
        (emitter/sync-notify e :aggregate [reset])))))

(defn get-count
  [e]
  (emitter/state (first (emitter/which-handlers e :count))))

(deftype TumblingWindow [e size]
  IWindow
  (enqueue [_ v]
    (emitter/notify e :aggregate [accumulate v])
    (emitter/sync-notify e :count [accumulate v])
    (when (= size (emit (get-count e)))
      (emitter/flush-futures e)
      (emitter/sync-notify e :emit (map emitter/state (emitter/which-handlers e :aggregate)))
      (emitter/sync-notify e :aggregate [reset])
      (emitter/sync-notify e :count [reset]))))

(defn aggregate-wrap
  [prev [f v]]
  (if v
    (f prev v)
    (f prev)))

(defn tick-wrap
  [a b]
  (b a))

;; TODO: Add multiple aggregates
(defn monotonic-window
  "Creates new monotonic window. "
  [aggregate clock h]
  (let [e (emitter/new-emitter)]
    (emitter/add-handler e :aggregate aggregate-wrap aggregate)
    (emitter/add-handler e :clock tick-wrap clock)
    (emitter/add-handler e :emit #(if-not (empty? %)
                                    (h
                                     (into {} (for [i %]
                                                (let [stat i]
                                                  [(title stat) (emit stat)]))))))
    (MonotonicWindow. e)))

(defn tumbling-window
  "Creates new tumbling window. "
  [aggregate max-count h]
  (let [e (emitter/new-emitter)]
    (emitter/add-handler e :aggregate aggregate-wrap aggregate)
    (emitter/add-handler e :count aggregate-wrap (make-count))
    (emitter/add-handler e :emit #(if-not (empty? %)
                                    (h
                                     (into {} (for [i %]
                                                (let [stat i]
                                                  [(title stat) (emit stat)]))))))
    (TumblingWindow. e max-count)))
