(ns ^{:doc "Generic event emitter implementation heavily inspired by gen_event in Erlang/OTP"}
  clojurewerkz.eep.emitter
  (:require [clojurewerkz.meltdown.reactor   :as mr]
            [clojurewerkz.meltdown.selectors :as ms :refer [$]]))

(alter-var-root #'*out* (constantly *out*))

(def global-handler :___global)

(def ^{:doc "Default thread pool size, calculated as # available processors + 1"}
  pool-size (-> (Runtime/getRuntime)
                .availableProcessors
                inc))

(defprotocol IEmitter
  (add-handler [_ event-type handler])

  (handler-registered? [_ t f])

  ;; TODO: add optional metadata to handlers, that may serve as an ability to remove handlers when
  ;; handler function auto-generated
  (delete-handler [_ t] "Removes the handler `f` from the current emitter, that's used for event
type `t`. ")
  (which-handlers [_] [_ t] "Returns all currently registered Handlers for Emitter")
  (notify [_ type args] "Asynchronous (default) event dispatch function. All the Handlers (both
stateful and stateless). Pretty much direct routing.")
  (notify-some [_ type-checker args] "Asynchronous notification, with function that matches an event type.
Pretty much topic routing.")
  (! [_ type args] "Erlang-style alias for `notify`")
  (swap-handler [_ t old-f new-f] "Replaces `old-f` event handlers with `new-f` event handlers for type
`t`")
  (stop [_] "Cancels all pending tasks, stops event emission.")

  (instrument [_] "Returns instrumentation details"))

(defprotocol IHandler
  (run [_ args])
  (state [_]))

(defn- collect-garbage
  "As we may potentially accumulate rather large amount of futures, we have to garbage-collect them."
  [futures]
  (filter #(not (.isDone %)) futures))

(deftype Aggregator [emitter f state_]
  IHandler
  (run [_ args]
    (swap! state_ f (get-in args [:data])))

  (state [_]
    @state_)

  Object
  (toString [_]
    (str "Handler: " f ", state: " @state_) ))

(deftype Observer [emitter f]
  IHandler
  (run [_ args]
    (f (get-in args [:data])))

  (state [_]
    nil))

(deftype Filter [emitter filter-fn rebroadcast]
  IHandler
  (run [_ args]
    (let [args (get-in args [:data])]
      (when (filter-fn args)
        (notify emitter rebroadcast args))))

  (state [_] nil))

(deftype Multicast [emitter multicast-types]
  IHandler
  (run [_ args]
    (doseq [t multicast-types]
      (notify emitter t (get-in args [:data]))))

  (state [_] nil))

(deftype Transformer [emitter transform-fn rebroadcast]
  IHandler
  (run [_ args]
    (notify emitter rebroadcast (transform-fn (get-in args [:data]))))

  (state [_] nil))

(defn- add-handler-intern
  [handlers event-type handler]
  (swap! handlers assoc event-type handler))

(defn- delete-handler-intern
  [handlers event-type]
  (swap! handlers dissoc event-type))

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
  (add-handler emitter t (Multicast. emitter m)))

(defn defobserver
  "Defines an observer, that runs (potentially with side-effects) f for tuples of given type."
  [emitter t f]
  (add-handler emitter t (Observer. emitter f)))

(deftype Emitter [handlers reactor]
  IEmitter
  (add-handler [this event-type handler]
    (add-handler-intern handlers event-type handler)
    (mr/on reactor ($ event-type) (fn [e]
                                    (run handler e))))

  (delete-handler [_ event-type]
    (swap! dissoc handlers event-type))

  (notify [_ t args]
    (mr/notify t args))

  (! [this t args]
    (notify this t args))

  (which-handlers [_]
    @handlers)

  (which-handlers [_ t]
    (t @handlers))

  (instrument [_]

    )


  (toString [_]
    (str "Handlers: " (mapv #(.toString %) @handlers))))

(defn new-emitter
  "Creates a fresh Event Emitter with the default executor."
  []
  (Emitter. (atom {}) (mr/create)))
