(ns clojurewerkz.eep.windows
  (:require [clojurewerkz.eep.clocks :as clocks]
            [com.ifesdjeen.utils.circular-buffer :as cb])
)

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

(defn sliding-window-simple
  [size aggregate emit-fn]
  (let [buffer (atom (cb/circular-buffer size))]
    (fn [value]
      (swap! buffer conj value)
      (when (cb/full? @buffer)
        (emit-fn (aggregate @buffer))))))

(defn tumbling-window-simple
  [size aggregate emit-fn]
  (let [buffer (atom (cb/circular-buffer size))]
    (fn [value]
      (swap! buffer conj value)
      (when (cb/full? @buffer)
        (emit-fn (aggregate @buffer))
        (reset! buffer (cb/circular-buffer size))))))

(defn monotonic-window-simple
  [clock-orig aggregate emit-fn]
  (let [clock (atom clock-orig)
        buffer (atom [])]
    (fn [value]
      (swap! buffer conj value)
      (swap! clock clocks/tick)
      (when (clocks/elapsed? @clock)
        (emit-fn (aggregate @buffer))
        (reset! buffer [])
        (swap! clock clocks/reset)))))
