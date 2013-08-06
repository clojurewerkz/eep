(ns clojurewerkz.eep.selectors
  (:import [reactor.event.selector Selector Selectors]
           reactor.function.Fn))

(defn ^Selector $
  [^String sel]
  (Selectors/$ sel))

(defn ^Selector regex
  [^String regex]
  (Selectors/R regex))
