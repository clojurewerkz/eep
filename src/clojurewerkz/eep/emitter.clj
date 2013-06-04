(ns ^{:doc "Generic event emitter implementation heavily inspired by gen_event in Erlang/OTP"}
  clojurewerkz.eep.emitter
  (:require [clojure.set :as clj-set])
  (:import [java.util.concurrent Executors AbstractExecutorService]))

(def global-handler :___global)

(def ^{:doc "Default thread pool size, calculated as # available processors + 1"}
  pool-size (-> (Runtime/getRuntime)
                .availableProcessors
                inc))

(defn make-executor
  ([]
     (make-executor pool-size))
  ([size]
     (Executors/newFixedThreadPool size)))

(defprotocol IEmitter
  (add-aggregator [_ t f initial-state] [_ t f initial-state executor] "Adds handler to the current emmiter.
Handler's tasks will be sumitted to Emitter's ThreadPool.

Handler state is stored in atom, that is first initialized with `initial-state`.

  3-arity version: `(event-type f initial-state)`")
  (add-observer [_ t f] [_ t f executor] "Adds an observer to current emitter.

`(f handler-state new-value)` is a function of 2 arguments, first one is current Handler state,
second one is a new value. Function return becomes a new Handler state.

  2-arity version: `(event-type f)`

`(f handler-state)` is a function of 1 argument, that's used to add a Stateless Handler,
potentially having side-effects. By enclosing emitter you can achieve capturing state of all
or any handlers.")
  (handler-registered? [_ t f])

  ;; TODO: add optional metadata to handlers, that may serve as an ability to remove handlers when
  ;; handler function auto-generated
  (delete-handler [_ t f] "Removes the handler `f` from the current emitter, that's used for event
type `t`. ")
  (delete-handlers [_ t] "Removes all handlers for given type.")
  (delete-handler-by [_ t f] "Removes the handler using the matcher function `f`.")
  (which-handlers [_] [_ t] "Returns all currently registered Handlers for Emitter")
  (flush-futures [_] "Under some circumstances, you may want to make sure that all the pending tasks
are executed by some point. By calling `flush-futures`, you force-complete all the pending tasks.")
  (notify [_ type args] "Asynchronous (default) event dispatch function. All the Handlers (both
stateful and stateless). Pretty much direct routing.")
  (notify-some [_ type-checker args] "Asynchronous notification, with function that matches an event type.
Pretty much topic routing.")
  (! [_ type args] "Erlang-style alias for `notify`")
  (swap-handler [_ t old-f new-f] "Replaces `old-f` event handlers with `new-f` event handlers for type
`t`")
  (stop [_] "Cancels all pending tasks, stops event emission.")

  (instrument [_] [_ t] "Returns instrumentation details for all the handlers"))

(defprotocol IHandler
  (run [_ args])
  (state [_]))

(defn- collect-garbage
  "As we may potentially accumulate rather large amount of futures, we have to garbage-collect them."
  [futures]
  (filter #(not (.isDone %)) futures))

(defn- instrument-executor
  [^AbstractExecutorService executor]
  {:pool-size (.getPoolSize executor)
   :active-threads (.getActiveCount executor)
   :task-count (.getTaskCount executor)
   :queued-tasks (.size (.getQueue executor))})

(deftype Aggregator [executor f state_]
  IHandler
  (run [_ args]
    (.submit executor #(swap! state_ f args)))

  (state [_]
    @state_)

  Object
  (toString [_]
    (str "Handler: " f ", state: " @state_) ))

(deftype Observer [executor f]
  IHandler
  (run [_ args]
    (.submit executor #(f args)))

  (state [_]
    nil))

(defn- get-handlers
  [t handlers]
  (clj-set/union (get-in handlers [t]) (global-handler handlers)))

(defn- add-handler-intern
  [handlers event-type handler]
  (swap! handlers #(update-in % [event-type]
                              (fn [v]
                                (if (nil? v)
                                  #{handler}
                                  (conj v handler))))))

(defn- delete-handler-intern
  [handlers event-type matcher]
  (swap! handlers #(update-in % [event-type]
                              (fn [v]
                                (apply disj v (filter matcher v))))))

(deftype Emitter [handlers futures executor]
  IEmitter

  (add-aggregator [_ event-type f initial-state]
    (add-handler-intern handlers event-type (Aggregator. executor f (atom initial-state))))

  (add-aggregator [_ event-type f initial-state executor]
    (add-handler-intern handlers event-type (Aggregator. executor f (atom initial-state))))

  (add-observer [_ event-type f]
    (add-handler-intern handlers event-type (Observer. executor f)))

  (add-observer [_ event-type f executor]
    (add-handler-intern handlers event-type (Observer. executor f)))

  (delete-handlers [_ event-type]
    (swap! dissoc handlers event-type))

  (delete-handler [_ event-type f]
    (delete-handler-intern handlers event-type #(= f (.f %))))

  (delete-handler-by [_ event-type f]
    (delete-handler-intern handlers event-type f))


  (notify [_ t args]
    (doseq [h (get-handlers t @handlers)]
      (let [future (run h args)]
        (swap! futures #(conj % future))))
    (swap! futures collect-garbage))

  (flush-futures [_]
    (doseq [future @futures] (if-not (.isDone future) (.get future))))

  (! [this t args]
    (notify this t args))

  (which-handlers [_]
    @handlers)

  (which-handlers [_ t]
    (t @handlers))

  (instrument [_]
    (into {}
          (for [[t handlers] @handlers]
            [t (mapv #(instrument-executor (.executor %)) handlers)])))

  (instrument [_ t]
    (map #(instrument-executor (.executor %)) (t @handlers)))

  (toString [_]
    (str "Handlers: " (mapv #(.toString %) @handlers))))

(defn new-emitter
  "Creates a fresh Event Emitter with the default executor."
  []
  (Emitter. (atom {}) (atom []) (make-executor)))
