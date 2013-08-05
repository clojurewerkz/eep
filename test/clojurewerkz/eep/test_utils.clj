(ns clojurewerkz.eep.test-utils
  (:import [java.util.concurrent CountDownLatch TimeUnit]))

(defn wrap-countdown
  "Countdown latch before executing function, proxy fn"
  [latch f]
  (fn [& values]
    (let [res (apply f values)]
      (.countDown @latch)
      res)))

(defn make-latch
  "Creates a new latch"
  [i]
  (atom (CountDownLatch. i)))

(defn reset-latch
  "Resets latch count to i"
  [latch i]
  (reset! latch (CountDownLatch. i)))

(defn await-latch
  "Awaits for latch for 500ms"
  [latch]
  (.await @latch 500 TimeUnit/MILLISECONDS))

(defmacro after-latch
  "Awaits for latch for 500ms"
  [latch & body]
  `(do
     (.await (deref ~latch) 2000 TimeUnit/MILLISECONDS)
     ~@body))
