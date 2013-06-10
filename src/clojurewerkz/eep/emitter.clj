(ns ^{:doc "Generic event emitter implementation heavily inspired by gen_event in Erlang/OTP"}
  clojurewerkz.eep.emitter
  (:require [clojurewerkz.meltdown.reactor   :as mr]
            [clojurewerkz.meltdown.selectors :as ms :refer [$]])
  (:import [java.util.concurrent ConcurrentHashMap]))

(alter-var-root #'*out* (constantly *out*))

(def global-handler :___global)

(def ^{:doc "Default thread pool size, calculated as # available processors + 1"}
  pool-size (-> (Runtime/getRuntime)
                .availableProcessors
                inc))

(defprotocol IHandler
  (run [_ args])
  (state [_]))

(defprotocol IEmitter
  (add-handler [_ event-type handler])
  (handler-registered? [_ t f])
  (delete-handler [_ t] "Removes the handler `f` from the current emitter, that's used for event
type `t`. ")
  (get-handler [_] [_ t] "Returns all currently registered Handlers for Emitter")
  (notify [_ type args] "Asynchronous (default) event dispatch function. All the Handlers (both
stateful and stateless). Pretty much direct routing.")
  (notify-some [_ type-checker args] "Asynchronous notification, with function that matches an event type.
Pretty much topic routing.")
  (! [_ type args] "Erlang-style alias for `notify`")
  (swap-handler [_ t new-f] "Replaces typed event handler with `new-f` event handler.")
  (stop [_] "Cancels all pending tasks, stops event emission.")
  (alive? [_] "Returns wether the current emitter is alive or no")
  (register-exception [_ t e]))

(defn- collect-garbage
  "As we may potentially accumulate rather large amount of futures, we have to garbage-collect them."
  [futures]
  (filter #(not (.isDone %)) futures))

(defn extract-data
  [payload]
  (get-in payload [:data]))

(defn- add-handler-intern
  [handlers event-type handler]
  (swap! handlers assoc event-type handler))

(defn- delete-handler-intern
  [handlers event-type]
  (swap! handlers dissoc event-type))

(deftype Emitter [handlers errors reactor]
  IEmitter
  (add-handler [this event-type handler]
    (when (nil? (get handler event-type))
      (add-handler-intern handlers event-type handler)
      (mr/on reactor ($ event-type) (fn [e]
                                      (run handler e)))))

  (delete-handler [_ event-type]
    (.unregister (.getConsumerRegistry reactor) event-type)
    (swap! dissoc handlers event-type))

  (swap-handler [this event-type f]
    (delete-handler this event-type)
    (add-handler this event-type f))

  (notify [_ t args]
    (mr/notify t args))

  (! [this t args]
    (notify this t args))

  (get-handler [_]
    @handlers)

  (get-handler [_ t]
    (t @handlers))

  (stop [_]
    (-> reactor
        (.getDispatcher)
        (.shutdown)))

  (alive? [_]
    (-> reactor
        (.getDispatcher)
        (.alive)))

  (register-exception [_ t e]
    (.put errors t e))

  (toString [_]
    (str "Handlers: " (mapv #(.toString %) @handlers))))

(defn new-emitter
  "Creates a fresh Event Emitter with the default executor."
  []
  (Emitter. (atom {}) (ConcurrentHashMap.) (mr/create :dispatcher-type :ring-buffer)))

;;
;; Operations
;;

(deftype Aggregator [emitter f state_]
  IHandler
  (run [_ payload]
    (swap! state_ f (extract-data payload)))

  (state [_]
    @state_)

  Object
  (toString [_]
    (str "Handler: " f ", state: " @state_) ))

(deftype Observer [emitter f]
  IHandler
  (run [_ payload]
    (f (extract-data payload)))

  (state [_]
    nil))

(deftype Filter [emitter filter-fn rebroadcast]
  IHandler
  (run [_ payload]
    (let [data (extract-data payload)]
      (when (filter-fn data)
        (notify emitter rebroadcast data))))

  (state [_] nil))

(deftype Multicast [emitter multicast-types]
  IHandler
  (run [_ payload]
    (doseq [t multicast-types]
      (notify emitter t (extract-data payload))))

  (state [_] nil))

(deftype Splitter [emitter split-fn]
  IHandler
  (run [_ payload]
    (let [data (extract-data payload)]
      (notify emitter (split-fn data) data)))

  (state [_] nil))

(deftype Transformer [emitter transform-fn rebroadcast]
  IHandler
  (run [_ payload]
    (notify emitter rebroadcast (transform-fn (extract-data payload))))

  (state [_] nil))

;;
;; Builder fns
;;

(defn deffilter
  "Defines a filter operation, that gets typed tuples, and rebroadcasts ones for which `filter-fn` returns true"
  [emitter t filter-fn rebroadcast]
  (add-handler emitter t (Filter. emitter filter-fn rebroadcast)))

(defn deftransformer
  "Defines a transformer, that gets typed tuples, transforms them with `transform-fn` and rebroadcasts them."
  [emitter t transform-fn rebroadcast]
  (add-handler emitter t (Transformer. emitter transform-fn rebroadcast)))



(defn defaggregator
  "Defines an aggregator, that is initialized with `initial-state`, then gets typed tuples and aggregates state
   by applying `aggregate-fn` to current state and tuple."
  [emitter t aggregate-fn initial-state]
  (add-handler emitter t (Aggregator. emitter aggregate-fn (atom initial-state))))

(defn defmulticast
  "Defines a multicast, that receives a typed tuple, and rebroadcasts them to several types of the given emitter."
  [emitter t m]
  (let [h (get-handler emitter t)]
    (add-handler emitter t
                 (Multicast. emitter
                             (if (isa? Multicast h)
                               (set (concat (.multicast-types h) m))
                               (set m))))))

(defn defsplitter
  [emitter t split-fn]
  (add-handler emitter t (Splitter. emitter split-fn)))

(defn defobserver
  "Defines an observer, that runs (potentially with side-effects) f for tuples of given type."
  [emitter t f]
  (add-handler emitter t (Observer. emitter f)))

;;
;; Debug utils
;;

(defmacro carefully
  "Test macro, should only be used internally"
  [emitter handler-type & body]
  `(try
     ~@body
     (catch Exception e#
       (register-exception ~emitter ~handler-type e#))))

(defn wrap-carefully
  "Helper method to help with debugging of complex flows, when something is failing and you don't really see why"
  [emitter handler-type f]
  (fn [a b]
    (carefully emitter handler-type
               (f a b))))
