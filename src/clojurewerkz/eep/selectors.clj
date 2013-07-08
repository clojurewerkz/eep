(ns clojurewerkz.eep.selectors
  (:import reactor.event.selector.Selector
           reactor.Fn))

(defn ^Selector $
  [^String sel]
  (Fn/$ sel))

(defn ^Selector regex
  [^String regex]
  (Fn/R regex))
