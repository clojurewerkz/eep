(ns clojurewerkz.eep.test-utils
  (:use clojure.test)
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
  (.await @latch 1000 TimeUnit/MILLISECONDS))

(defmacro after-latch
  "Awaits for latch for 500ms"
  [latch & body]
  `(do
     (assert (.await (deref ~latch) 3 TimeUnit/SECONDS)
             (str "Timed out waiting on a latch... Still "
                  (.getCount (deref ~latch)) " to go."))
     ~@body))

(defmacro with-latch
  [countdown-from & body]
  `(let [latch# (CountDownLatch. ~countdown-from)
         ~'latch latch#]
     ~@body
     (.await latch# 1 TimeUnit/SECONDS)
     (is (= 0 (.getCount latch#)))))
