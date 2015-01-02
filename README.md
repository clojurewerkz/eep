# EEP, Embedded Event Processing in Clojure

EEP is a Clojure library for embedded event processing.
It combines a lightweight generic event handling system,
and with multiple windowed stream operations.

eep-clj is heavily influenced by other EEP projects:

  * [eep-js (JavaScript)](https://github.com/darach/eep-js)
  * [eep-erl (Erlang)](https://github.com/darach/eep-erl)
  * [eep-php (PHP)](https://github.com/ianbarber/eep-php)


## Project Maturity

EEP is a *very young* and *experimental* project, but it's been used in
production for some time and is fairly stable bug-wise. The API may change
significantly in the upcoming months, so use it at your own discretion.


## Maven Artifacts

### Most Recent Release

With Leiningen:

    [clojurewerkz/eep "1.0.0-alpha5"]

With Maven:

    <dependency>
      <groupId>clojurewerkz</groupId>
      <artifactId>eep</artifactId>
      <version>1.0.0-alpha5</version>
    </dependency>


## Documentation & Examples

### Quickstart

In order to create an emitter, use `clojurewerkz.eep.emitter/create` function:

```clj
(ns my-eep-ns
  (:require clojurewerkz.eep.emitter :refer :all))

(def emitter (create :dispatcher-type :ring-buffer))
```

You can register event handlers on an emitter by using handler helper
functions. For example, in order to calculate sums for even and odd
numbers, you can first define a `splitter` and then two `aggregators`,
one for even and one for odd ones:

```clj
(defsplitter emitter :entrypoint (fn [i] (if (even? i) :even :odd)))

(defaggregator emitter :even f 0)
(defaggregator emitter :odd f 0)
```

Here, `:entrypoint`, `:even` and `:odd` are event types, unique event
identifiers.

In order to push data to emitter, use `clojurewerkz.eep.emitter/notify`,
which takes an emitter, event type and payload:

```clj
(notify emitter :entrypoint 1)
(notify emitter :entrypoint 2)
(notify emitter :entrypoint 3)
```

## Core Concepts

  * `Emitter` is responsible for handler registration and event
    routing. It holds everything together.

  * `Event`s are dispatched by user code. An event is an
    arbitrary tuple of user-defined structure. There's no validation
    provided for it.

  * `Event Type` is a unique event type identifier, used for routing. It can
    be a number, a symbol, a keyword, a string or anything else. All the events
    coming into `Emitter` have a type.

  * `Handler` is a function and optional state attached to it. The function acts as a
    callback, executed whenever an event is matched on the type.
    The same handler can be used for multiple event types, but
    an event type can only have one handler at most.

## Handler types

`handler` is a processing unit that may have state or be stateless. Each
handler is attached to emitter with a `type`, which uniquely
identifies it within an emitter. You can only attach a single handler
for any given `type`. However, you can attach a single Handler to
multiple `types`.

Examples of handlers are:

`filter` receives events of a certain type, and forwards ones for which
`filter-fn` returns `true` to one or more other handlers:

```clj
;; Filters events going through the stream, allowing only even ones
;; to go through
(deffilter emitter :entrypoint even? :summarizer)
```

`splitter` receives events of a certain type, and dispatches them to
type returned by predicate function. For example, you can split stream
of integers to even and odd ones and process them down the pipeline
differently.

```clj
;; Splits event stream to two parts, routing even events with :even
;; type and odd ones with :odd.
(defsplitter emitter :entrypoint (fn [i] (if (even? i) :even :odd)))
```

`transformer` defines a transformer that gets typed tuples, applies
transformation function to each one of them and forwards them to
one or more other handlers. It's similar to applying `map` to
elements of a list, except for function is applied to stream of data.

```clj
;; Transforms event stream by multiplying each event to 2
(deftransformer emitter :entrypoint (partial * 2) :summarizer)
```

`aggregator` is initialized with initial value, then gets events of
a certain type and aggregates state by applying aggregate function to
current state and an incoming event. It's similar to `reduce`
function in Clojure, except for it's applied to the stream of data.

```clj
;; Accumulates sum of numbers in the stream
(defaggregator emitter :even (fn [acc i] (+ acc i) 0))
```

`multicast` receives events of a certain type and broadcasts them
to several handlers with different types. For example, whenever an
alert is received, you may want to send notifications via email,
IRC, Jabber and append event to the log file.

```clj
;; Redistributes incoming events, routing them by three given types
(defmulticast emitter :entrypoint [:summarizer1 :summarizer2 :summarizer3])

;; It's also possible to attach additional multicast entries. this will
;; append :summarizer4 for to be re-distributed whenever events are
;; coming to :entrypoint
(defmulticast emitter :entrypoint [:summarizer4])
```

`observer` receives events of a certain type and runs function
(potentially with side-effects) on each one of them.

```clj
(defobserver emitter :countdown (fn [_] (.countDown latch)))
```

`buffer` receives events of a certain type and stores them in a
circular buffer with given capacity. As soon as capacity is
reached, it distributes them to several other handlers.

```clj
;; Holds most recent 20 values
(defbuffer emitter :load-times-last-20-events 20)
```

`rollup` acts in a manner similar to buffer, except for it's
time-bound but not capacity-bound, so whenever a time period is
reached, it dispatches all the events to several other handlers.

```clj
;; Aggregates events for 100 milliseconds and emits them to :summarizer
;; as soon as timespan elapsed
(defrollup emitter :entrypoint 100 :summarizer)
```

Handlers may be stateful and stateless. `filter`, `splitter`,
`transformer`, `multicast` and `observer` are __stateless__. On the
other hand, `aggregator`, `buffer` and `rollup` are stateful.

## Topology DSL

There's a DSL that threads emitter through all handler declarations,
in order to create aggregation topologies in a more concise and obvious
way:

```clj
(build-topology (create)
                :entrypoint (defsplitter (fn [i] (if (even? i) :even :odd)) [:even :odd])
                :even (defaggregator f 0)
                :odd  (defaggregator f 0))
```

Alternatively, you can use Clojure `->` for creating concise topologies:

```clj
(-> *emitter*
    (defsplitter :entrypoint (fn [i] (if (even? i) :even :odd)))
    (defaggregator :even f 0)
    (defaggregator :odd f 0))
```

## Topology visualization

You can also visualize your topology by calling `clojurewerkz.eep.visualization/visualise-graph`
and giving it an emitter. You'll get an image like this one:

[![Topology Visualization Example](http://coffeenco.de/assets/images/topology_example.png)](http://coffeenco.de/assets/images/topology_example.png)

## Windows

Windows and buffers are an essential part of event processing.
We've added the most important implementations of windowed
operations, such as sliding, tumbling, monotonic and timed windows
to EEP to allow you to use them within topologies.

### Sliding window

Sliding windows have a fixed a-priori known size.

Example: Sliding window of size 2 computing sum of values.

```
    t0     t1      (emit)   t2             (emit)       tN
  +---+  +---+---+          -...-...-
  | 1 |  | 2 | 1 |   <3>    : x : x :
  +---+  +---+---+          _...+---+---+               ...
             | 2 |              | 2 | 3 |    <5>
             +---+              +---+---+
                                    | 4 |
                                    +---+
```

Useful to hold last `size` elements.

### Tumbling window

Tumbling windows (here) have a fixed a-priori known size.

Example: Tumbling window of size 2 computing sum of values.

```
    t0     t1      (emit)    t2            t3         (emit)    t4
  +---+  +---+---+         -...-...-
  | 1 |  | 2 | 1 |   <3>   : x : x :
  +---+  +---+---+         -...+---+---+   +---+---+            ...
                                   | 3 |   | 4 | 3 |    <7>
                                   +---+   +---+---+
```

Useful to accumulate `size` elements and aggregate on overflow.

### Monotonic window

Makes a clock tick on every call. Whenever clock is elapsed, emits to aggregator.

In essence, it's an alternative implementation of tumbling-window that allows
to use custom emission control rather than having a buffer overflow check.

Useful for cases when emission should be controlled by arbitrary function,
possibly unrelated to window contents.

### Timed window

A simple timed window, that runs on wall clock. Receives events and stores them
until clock is elapsed, emits for aggregation after that.

In essence, it's an alternative implementation of tumbling-window or monotonic-window
that allows wall clock control.

Useful for accumulating events for time-bound events processing, accumulates events
for a certain period of time (for example, 1 minute), and aggregates them.

## Busy-spin

Whenever you create an emitter, you may notice that one of your cores is 100% busy. You should
not worry about it, since all dispatchers use a tight loop for dispatch, without sleeping, therefore
not yielding control back to OS, so OS defines that as 100% processor load.

## Supported Clojure Versions

EEP requires Clojure 1.4+.


## Development

EEP uses [Leiningen 2](https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md). Make
sure you have it installed and then run tests against all supported Clojure versions using

    lein all test

Then create a branch and make your changes on it. Once you are done with your changes and all
tests pass, submit a pull request on Github.

## License

Copyright © 2014 Michael Klishin, Alex P

Distributed under the Eclipse Public License, the same as Clojure.
