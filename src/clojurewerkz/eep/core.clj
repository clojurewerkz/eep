(ns clojurewerkz.eep.core)

(defprotocol Clock
  (at [_])
  (increment [_])
  (ticked? [_])
  (expired? [_]))

(defprotocol Resetable
  (reset [_]))

(defprotocol Stat
  (title [_])
  (accumulate [this _])
  (compensate [this _])
  (emit [this]))

(defprotocol Ticking
  (tick [_] ""))

(defprotocol IWindow
  (enqueue [_ v])
  (clock [_]))