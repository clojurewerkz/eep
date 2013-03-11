(ns clojurewerkz.eep.windows
  (:use clojurewerkz.eep.core
        clojurewerkz.eep.clocks
        clojurewerkz.eep.stats)
  (:require [clojurewerkz.eep.emitter :as emitter]))

;;
;; Implementation
;;

(defn- aggregate-wrap
  [prev [f v]]
  (if v
    (f prev v)
    (f prev)))

(defn- tick-wrap
  [a b]
  (b a))

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


(defn- get-first-of-type
  [e t]
  (emit (emitter/state (first (emitter/which-handlers e t)))))

(defn- get-count
  [e]
  (get-first-of-type e :count))

(deftype TumblingWindow [e size]
  IWindow
  (enqueue [_ v]
    (emitter/notify e :aggregate [accumulate v])
    (emitter/sync-notify e :count [accumulate v])
    (when (= size (get-count e))
      (emitter/flush-futures e)
      (emitter/sync-notify e :emit (map emitter/state (emitter/which-handlers e :aggregate)))
      (emitter/sync-notify e :aggregate [reset])
      (emitter/sync-notify e :count [reset]))))

(deftype Buffer [val]
  Stat
  (title [_] :buffer)

  (accumulate [a [index new-val]]
    (Buffer. (assoc val index new-val)))

  (compensate [_ _] nil)

  (emit [_]
    val))

(defn make-buffer
  [size]
  (Buffer. (apply vector (take size (repeat nil)))))

(deftype SlidingWindow [e size]
  IWindow
  (enqueue [_ v]
    (emitter/notify e :aggregate [accumulate v])
    (let [idx (get-count e)]
      (if (>= idx (dec size))
        (do
          (emitter/flush-futures e)
          (emitter/sync-notify e :emit (map emitter/state (emitter/which-handlers e :aggregate)))
          (emitter/notify e :aggregate [compensate (get (get-first-of-type e :buffer) (mod (inc idx) size))])
          (emitter/notify e :buffer [accumulate [(mod idx size) v]]))
        (emitter/notify e :buffer [accumulate [idx v]])))
    (emitter/sync-notify e :count [accumulate v])
    )
  )

(defn sliding-window
  [aggregate h size]
  (let [e (emitter/new-emitter)]
    (emitter/add-handler e :aggregate aggregate-wrap aggregate)
    (emitter/add-handler e :count aggregate-wrap (make-count))
    (emitter/add-handler e :buffer aggregate-wrap (make-buffer size))
    (emitter/add-handler e :emit #(if-not (empty? %)
                                    (h
                                     (into {} (for [i %]
                                                (let [stat i]
                                                  [(title stat) (emit stat)]))))))
    (SlidingWindow. e size)
    ))
;;
;; API
;;

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

(defn periodic-window
  "Creates new periodic window. "
  [aggregate h]
  (let [clock (make-wall-clock)
        e (emitter/new-emitter)]
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
