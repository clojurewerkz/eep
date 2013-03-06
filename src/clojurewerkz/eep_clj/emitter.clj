(ns ^{:doc "Event Emitter, right now it's a poor man's clone of "}
  clojurewerkz.eep-clj.emitter
  (:require [clojure.set :as clj-set])
  (:import [java.util.concurrent ExecutorService Executors]))

(def global-handler :___global)

(def pool-size (-> (Runtime/getRuntime)
                   (.availableProcessors)
                   inc))

(defonce executor (Executors/newFixedThreadPool pool-size))

(defprotocol IEmitter
  (add-handler [_ t f initial-state] [_ f initial-state] "Adds handler to the current emmiter.
Handler state is stored in atom, that is first initialized with `initial-state`.

`f` is a function of 2 arguments, first one is current Handler state
3-arity version registers a global handler")
  (delete-handler [_ t f])
  (which-handlers [_] [_ t])
  (notify [_ type args])
  (sync-notify [_ type args])
  (! [_ type args])
  (swap-handler [_ old-f new-f])
  (stop [_]))

(defprotocol IHandler
  (run [_ args])
  (state [_]))

(defmacro run-async
  [h & args]
  `(let [runnable# (fn [] (run ~h ~@args))]
     (.execute executor runnable#)))

;; state is atom, handler is fn
(deftype Handler [handler state_]
  IHandler
  (run [_ args]
    (swap! state_ handler args))
  (state [_]
    state_)

  Object
  (toString [_]
    (str "Handler: " handler ", state: " @state_) ))

(deftype StatelessHandler [handler]
  IHandler
  (run [_ args]
    (handler args)))

(defn- get-handlers
  [t handlers]
  (clj-set/union (t handlers) (global-handler handlers)))

(defn- add-handler-intern
  [handlers event-type handler]
  (swap! handlers #(update-in % [event-type]
                              (fn [v]
                                (if (nil? v)
                                  #{handler}
                                  (conj v handler)))
                              )))
(deftype Emitter [handlers]
  IEmitter
  (add-handler [_ event-type f initial-state]
    (add-handler-intern handlers event-type (Handler. f (atom initial-state))))

  (add-handler [_ event-type f]
    (add-handler-intern handlers event-type (StatelessHandler. f)))

  (delete-handler [_ event-type f]
    (swap! handlers (fn [h]
                      (update-in h [event-type]
                                   (fn [v]
                                     (disj v (first (filter #(= f (.handler %)) v))))))))

  (notify [_ t args]
    (doseq [h (get-handlers t @handlers)]
      (run-async h args)))

  (sync-notify [_ t args]
    (doseq [h (get-handlers t @handlers)]
      (run h args)))

  (! [this t args]
    (notify this t args))

  (which-handlers [_]
    @handlers)

  (which-handlers [_ t]
    (t @handlers))

  (toString [_]
    (str "Handlers: " (mapv #(.toString %) @handlers))))

(defn new-emitter
  []
  (Emitter. (atom {})))
