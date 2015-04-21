# EEP, Embedded Event Processing in Clojure

EEP is a Clojure library for embedded event processing.
It combines a lightweight generic event handling system,
and with multiple windowed stream operations.

eep-clj is heavily influenced by other EEP projects:

  * [eep-js (JavaScript)](https://github.com/darach/eep-js)
  * [eep-erl (Erlang)](https://github.com/darach/eep-erl)
  * [eep-php (PHP)](https://github.com/ianbarber/eep-php)


## Project Maturity

EEP is a *young* and evolving project. The API may change
significantly in the near future, so use it at your own discretion.

This section will be update as the project matures.


## Maven Artifacts

### Most Recent Release

With Leiningen:

    [clojurewerkz/eep "1.0.0-beta1"]

With Maven:

    <dependency>
      <groupId>clojurewerkz</groupId>
      <artifactId>eep</artifactId>
      <version>1.0.0-beta1</version>
    </dependency>


## Documentation & Examples

### Quickstart

In order to create an emitter, use `clojurewerkz.eep.emitter/create` function:

```clj
(ns user
  (:require [clojurewerkz.eep.emitter :as eem]))

(def emitter (eem/create {}))
```

You can register event handlers on an emitter by using handler helper
functions. For example, in order to calculate sums for even and odd
numbers, you can first define a `splitter` and then two `aggregators`,
one for even and one for odd ones:

```clj
(eem/defsplitter emitter :entrypoint (fn [i] (if (even? i) :even :odd)))

(eem/defaggregator emitter :even (fn [acc i] (+ acc i)) 0)
(eem/defaggregator emitter :odd (fn [acc i] (+ acc i)) 0)
```

Here, `:entrypoint`, `:even` and `:odd` are event types, unique event
identifiers.

In order to push data to emitter, use `clojurewerkz.eep.emitter/notify`,
which takes an emitter, event type and payload:

```clj
(eem/notify emitter :entrypoint 1)
(eem/notify emitter :entrypoint 1)
(eem/notify emitter :entrypoint 1)
(eem/notify emitter :entrypoint 4)
```

You can then view the state of an aggregator like so:

```clj
(eem/state (eem/get-handler emitter :odd)) ;; 3
(eem/state (eem/get-handler emitter :even)) ;; 4
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

  * `Handler` is a function and optional state attached to it. The function
    acts as a callback, executed whenever an event is matched on the type.
    The same handler can be used for multiple event types, but
    an event type can only have one handler at most.

## Handler types

Each handler is attached to emitter with a `type`, which uniquely
identifies it within an emitter. You can only attach a single handler
for any given `type`. However, you can attach a single Handler to
multiple `types`.

Handlers may be stateful and stateless. `filter`, `splitter`,
`transformer`, `multicast` and `observer` are __stateless__. On the
other hand, `aggregator`, `buffer` and `rollup` are __stateful__.

### Stateful Handlers

`aggregator` is initialized with initial value, then gets events of
a certain type and aggregates state by applying aggregate function to
current state and an incoming event. It's similar to `reduce`
function in Clojure, except for it's applied to the stream of data.

```clj
(def emitter (eem/create {})) ;; create the emitter
(eem/defaggregator
  emitter ;; the emitter
  :accumulator ;; the event type to attach to
  (fn [acc i] (+ acc i)) ;; the function to apply to the stream
  0) ;; the initial state
;; send 0-9 down the stream
(doseq [i (range 10)]
  (eem/notify emitter :accumulator i))

;; state is 0 + 1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9
(eem/state (eem/get-handler emitter :accumulator)) ;; 45
```

`buffer` receives events of a certain type and stores them in a
circular buffer with given capacity. As soon as capacity is
reached, it drops events (first in, first out).

```clj
(def emitter (eem/create {}))
(eem/defbuffer
  emitter ;; the emitter
  :entry ;; the event type to attach to
  5) ; the maximum values.
;; send 0-9 down the stream
(doseq [i (range 10)]
  (eem/notify emitter :entry i))

;; 0 1 2 3 4 were dropped, in that order.
(eem/state (eem/get-handler emitter :entry)) ; [5 6 7 8 9]
```

`rollup` acts in a manner similar to buffer, except for it's
time-bound but not capacity-bound, so whenever a time period is
reached, it dispatches all the events to several other handlers.

```clj
;; Aggregates events for 100 milliseconds and emits them to :summarizer
;; as soon as timespan elapsed
(eem/defrollup emitter :rollup-entry 100 :summarizer)
```

### Stateless Handlers

Note: calling `state` on a stateless handler will return `nil`.

`filter` receives events of a certain type, and forwards ones for which
`filter-fn` returns `true` to one or more other handlers:

```clj
;; Filters events going through the stream, allowing only even ones
;; to go through
(def emitter (eem/create {}))
(eem/deffilter
  emitter ;; the emitter
  :filtered ;; the event type to attach to
  number? ;; function to evaluate input
  :only-numbers) ;; event-type to forward input that evaluates to true
;; buffer to receive filtered input for example
(eem/defbuffer emitter :only-numbers 5)
;; send some test data down the stream
(doseq [i [1 "a" 5 "b" 9 "c" "d" 32 "eep" 58]]
  (eem/notify emitter :filtered i))

;; all items where (number? item) was false were not forwarded
(eem/state (eem/get-handler emitter :only-numbers)) ;; [1 5 9 32 58]
```

`splitter` receives events of a certain type, and dispatches them to
type returned by predicate function. For example, you can split stream
of integers to even and odd ones and process them down the pipeline
differently.

```clj
;; Splits event stream to two parts, routing even events with :even
;; type and odd ones with :odd.
(def emitter (eem/create {}))
(eem/defsplitter
  emitter ;; the emitter
  :entry ;; the event type to attach to
  (fn [i] (if (number? i) :numbers :non-numbers))) ;; function evaluates input and returns which event type to forward to.
;; aggregator to receive the numbers for example
(eem/defaggregator emitter :numbers + 0)
;; buffer to receive the numbers for example
(eem/defbuffer emitter :non-numbers 5)
;; send some test data down the stream
(doseq [i [1 "a" 5 "b" 9 "c" "d" 32 "eep" 58]]
  (eem/notify emitter :entry i))

;; all numbers are sent to :numbers, all strings are sent to :not-numbers
(eem/state (eem/get-handler emitter :numbers)) ;; 105, which is 1 + 5 + 9 +32 + 58
(eem/state (eem/get-handler emitter :non-numbers)) ;; ["a" "b" "c" "d" "eep"], which is all the non-numbers
```

`transformer` defines a transformer that gets typed tuples, applies
transformation function to each one of them and forwards them to
one or more other handlers. It's similar to applying `map` to
elements of a list, except for function is applied to stream of data.

```clj
;; Transforms event stream by multiplying each event to 2
(def emitter (eem/create {}))

;; Define the transformer function
(defn fizzbuzzer [i]
  (cond
    (zero? (mod i 15)) "FizzBuzz"
    (zero? (mod i 5)) "Buzz"
    (zero? (mod i 3)) "Fizz"
    :else i))

(eem/deftransformer
  emitter ;; the emitter
  :entry ;; the event type to attach to
  fizzbuzzer ;; the transformer function
  :fizzbuzz) ;; the new event type to forward to
;; a buffer to receive output for example
(eem/defbuffer emitter :fizzbuzz 5)

;; send some test data down the stream
(doseq [i (range 10)]
    (eem/notify emitter :entry i))

;; Anything divided by 3 is "Fizz", anything divided by 5 is "Buzz", and anything divided by 15 is "FizzBuzz"
(eem/state (eem/get-handler emitter :fizzbuzz)) ;; ["Buzz" "Fizz" 7 8 "Fizz"]
```


`multicast` receives events of a certain type and broadcasts them
to several handlers with different types. For example, whenever an
alert is received, you may want to send notifications via email,
IRC, Jabber and append event to the log file.

```clj
;; Redistributes incoming events, routing them to multiple other event types
(def emitter (eem/create {}))
(eem/defmulticast
  emitter ;; the emitter
  :entry ;; the event type to attach to
  [:accumulator :incrementer :multiplier]) ;; vector of event types to forward to

;; set up aggregators for example
(eem/defaggregator emitter :accumulator (fn [acc i] (+ acc i)) 0)
(eem/defaggregator emitter :incrementer (fn [acc i] (+ acc 1)) 0)
(eem/defaggregator emitter :multiplier (fn [acc i] (* acc i)) 1)

;; send test data down the stream
(doseq [i [2 3 4]]
  (eem/notify emitter :entry i))

(eem/state (eem/get-handler emitter :accumulator)) ;; 9, 2 + 3 + 4
(eem/state (eem/get-handler emitter :incrementer)) ;; 3, 1 + 1 + 1
(eem/state (eem/get-handler emitter :multiplier)) ;; 24, 2 * 3 * 4

;; It's also possible to attach additional multicast entries. This will
;; append :subtractor to the list of streams broadcasted by :entry from that point forward
(eem/defmulticast emitter :entry [:subtractor])
(eem/defaggregator emitter :subtractor (fn [acc i] (- acc i)) 0)
(eem/notify emitter :entry 2)

(eem/state (eem/get-handler emitter :accumulator)) ;; 11, 2 + 3 + 4 + 2
(eem/state (eem/get-handler emitter :incrementer)) ;; 4, 1 + 1 + 1 + 1
(eem/state (eem/get-handler emitter :multiplier)) ;; 48, 2 * 3 * 4 * 2
(eem/state (eem/get-handler emitter :subtractor)) ;; -2
```

`observer` receives events of a certain type and runs function
(potentially with side-effects) on each one of them.

```clj
(def emitter (eem/create {}))

;; our function with side effects
(defn announcer [item]
  (println (str "I would like to announce: " item)))

(eem/defobserver emitter :announce announcer)

(eem/notify emitter :announce "This Item")
;; prints "I would like to announce: This Item"
```

## Topology DSL

There's a DSL that threads emitter through all handler declarations,
in order to create aggregation topologies in a more concise and obvious
way:

```clj
(def emitter
  (eem/build-topology (eem/create {})
                      :entry (eem/defsplitter (fn [i] (if (even? i) :even :odd)))
                      :even (eem/defbuffer 5)
                      :odd  (eem/defbuffer 5)))

(doseq [i (range 10)]
  (eem/notify emitter :entry i))

(eem/state (eem/get-handler emitter :even)) ;; [0 2 4 6 8]
(eem/state (eem/get-handler emitter :odd)) ;; [1 3 5 7 9]
```

Alternatively, you can use Clojure `->` for creating concise topologies:

```clj
(def emitter
  (-> (eem/create {})
      (eem/defsplitter :entry (fn [i] (if (even? i) :even :odd)))
      (eem/defbuffer :even 5)
      (eem/defbuffer :odd 5)))

(doseq [i (range 10)]
  (eem/notify emitter :entry i))

(eem/state (eem/get-handler emitter :even)) ;; [0 2 4 6 8]
(eem/state (eem/get-handler emitter :odd)) ;; [1 3 5 7 9]
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

EEP requires Clojure 1.6+.


## Development

EEP uses [Leiningen 2](https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md). Make
sure you have it installed and then run tests against all supported Clojure versions using

    lein all test

Then create a branch and make your changes on it. Once you are done with your changes and all
tests pass, submit a pull request on Github.

## License

Copyright © 2014-2015 Michael Klishin, Alex Petrov, and the ClojureWerkz team.

Double licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html) (the same as Clojure) or
the [Apache Public License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
