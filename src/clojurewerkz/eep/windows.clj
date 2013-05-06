(ns clojurewerkz.eep.windows
  (:require [clojurewerkz.eep.emitter :as e]
            [clojurewerkz.eep.clocks :as clocks]
            [com.ifesdjeen.utils.circular-buffer :as cb])
  (:import [java.util Timer TimerTask Date]))

(def timer (Timer. true))

;;
;; Implementation
;;

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
  (let [clock  (atom clock-orig)
        buffer (atom [])]
    (fn [value]
      (swap! clock clocks/tick)
      (when (clocks/elapsed? @clock)
        (emit-fn (aggregate @buffer))
        (reset! buffer [])
        (swap! clock clocks/reset))
      (swap! buffer conj value))))

(defn timed-window-simple
  [clock-orig tick-period aggregate emit-fn]
  (let [clock  (atom clock-orig)
        buffer (atom [])
        task   (proxy [TimerTask] []
                 (run []
                   (swap! clock clocks/tick)
                   (when (clocks/elapsed? @clock)
                     (when (not (empty? @buffer))
                       (emit-fn (aggregate @buffer))
                       (reset! buffer []))
                     (swap! clock clocks/reset))))]
    (.scheduleAtFixedRate timer task 0 tick-period)
    (fn [value]
      (swap! buffer conj value))))

(defn register-window
  "Registers window with given handler name for given emitter, helper function"
  [emitter n window]
  (e/add-observer emitter n window))
