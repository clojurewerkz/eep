(ns ^{:doc "Generic event emitter implementation heavily inspired by gen_event in Erlang/OTP"}
  clojurewerkz.eep.emitter
  (:require [clojure.set :as clj-set])
  (:import [java.util.concurrent Executors AbstractExecutorService]))

(alter-var-root #'*out* (constantly *out*))

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
  (add-handler [_ event-type handler])

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

(deftype Aggregator [emitter executor f state_]
  IHandler
  (run [_ args]
    (.submit executor (fn []
                        (swap! state_ f args))))

  (state [_]
    @state_)

  Object
  (toString [_]
    (str "Handler: " f ", state: " @state_) ))

(deftype Observer [emitter executor f]
  IHandler
  (run [_ args]
    (.submit executor #(f args)))

  (state [_]
    nil))

(deftype Filter [emitter executor filter-fn rebroadcast]
  IHandler
  (run [_ args]
    (.submit executor (fn []
                        (when (filter-fn args)
                          (notify emitter rebroadcast args)))))

  (state [_] nil))

(deftype Multicast [emitter executor multicast-types]
  IHandler
  (run [_ args]
    (.submit executor (fn []
                        (doseq [t multicast-types]
                          (notify emitter t args)))))

  (state [_] nil))

(deftype Transformer [emitter executor transform-fn rebroadcast]
  IHandler
  (run [_ args]
    (.submit executor #(notify emitter rebroadcast (transform-fn args))))

  (state [_] nil))

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

(defn deffilter
  "Defines a filter operation, that gets typed tuples, and rebroadcasts ones for which `filter-fn` returns true"
  ([emitter t filter-fn rebroadcast]
     (deffilter emitter t (.executor emitter) filter-fn rebroadcast))
  ([emitter t executor filter-fn rebroadcast]
     (add-handler emitter t (Filter. emitter executor filter-fn rebroadcast))))

(defn deftransformer
  "Defines a transformer, that gets typed tuples, transforms them with `transform-fn` and rebroadcasts them."
  ([emitter t transform-fn rebroadcast]
     (deftransformer emitter t (.executor emitter) transform-fn rebroadcast))
  ([emitter t executor transform-fn rebroadcast]
     (add-handler emitter t (Transformer. emitter executor transform-fn rebroadcast))))

(defn defaggregator
  "Defines an aggregator, that is initialized with `initial-state`, then gets typed tuples and aggregates state
   by applying `aggregate-fn` to current state and tuple."
  ([emitter t aggregate-fn initial-state]
     (defaggregator emitter t (.executor emitter) aggregate-fn initial-state))
  ([emitter t executor aggregate-fn initial-state]
     (add-handler emitter t (Aggregator. emitter executor aggregate-fn (atom initial-state)))))

(defn defmulticast
  "Defines a multicast, that receives a typed tuple, and rebroadcasts them to several types of the given emitter."
  ([emitter t m]
     (defmulticast emitter t (.executor emitter) m))
  ([emitter t executor m]
     (add-handler emitter t (Multicast. emitter executor m))))

(defn defobserver
  "Defines an observer, that runs (potentially with side-effects) f for tuples of given type."
  ([emitter t f]
     (defobserver emitter t (.executor emitter) f))
  ([emitter t executor f]
     (add-handler emitter t (Observer. emitter executor f))))

(deftype Emitter [handlers futures executor]
  IEmitter
  (add-handler [this event-type handler]
    (add-handler-intern handlers event-type handler))

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
