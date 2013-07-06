(ns clojurewerkz.eep.reactor
  (:import [reactor R]
           [reactor.core Reactor Environment]
           [reactor.fn.dispatch ThreadPoolExecutorDispatcher Dispatcher RingBufferDispatcher]
           [com.lmax.disruptor.dsl ProducerType]
           [com.lmax.disruptor YieldingWaitStrategy]
           [reactor.fn.selector Selector]
           [reactor.fn Consumer Event]
           clojure.lang.IFn
           clojure.lang.IPersistentMap))

(defn ^IPersistentMap event->map
  [^Event event]
  {:data     (.getData event)
   :reply-to (.getReplyTo event)
   :headers  (into {} (.getHeaders event))
   :id       (.getId event)})

(defn ^Consumer from-fn
  "Instantiates a reactor consumer from a Clojure
   function"
  [^IFn f]
  (reify Consumer
    (accept [this event]
      (f (event->map event)))))



(defn environment
  []
  (Environment.))

(def dispatcher-types
  {:event-loop "eventLoop"
   :thread-pool "threadPoolExecutor"
   :ring-buffer "ringBuffer"})

(defn ^Reactor create
  "Creates a reactor instance"
  [& {:keys [dispatcher-type dispatcher env]}]
  (let [reactor (R/reactor)]
    (if env
      (.using reactor env)
      (.using reactor (environment)))
    (when dispatcher
      (.using reactor dispatcher))
    (when dispatcher-type
      (.dispatcher reactor (dispatcher-type dispatcher-types)))
    (.get reactor)))

(defn on
  "Registers a Clojure function as event handler for a particular kind of events.

   1-arity will register a handler for all events on the root (default) reactor.
   2-arity takes a selector and a handler and will use the root reactor.
   3-arity takes a reactor instance, a selector and a handler."
  ([^Reactor reactor ^Selector selector ^IFn f]
     (.on reactor selector (from-fn f))))

(defn on-any
  "Registers a Clojure function as event handler for all events
   using default selector."
  ([^Reactor reactor ^IFn f]
     (.on reactor (from-fn f))))

;; TODO: error handlers

(defn notify
  ([^Reactor reactor key payload]
     (.notify reactor ^Object key (Event. payload)))
  ([^Reactor reactor key payload ^IFn completion-fn]
     (.notify reactor ^Object key (Event. payload) ^Consumer (from-fn completion-fn))))

(defn responds-to?
  #_ ([key]
     (.respondsToKey reactor key))
  ([^Reactor reactor key]
     (.respondsToKey reactor key)))
