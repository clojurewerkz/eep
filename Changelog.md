## Changes between 1.0.0-alpha5 and 1.0.0-alpha6

### Clojure 1.6

EEP now depends on `org.clojure/clojure` version `1.6.0`. It is
still compatible with Clojure 1.4 and if your `project.clj` depends on
a different version, it will be used, but 1.6 is the default now.

## Changes between 1.0.0-alpha4 and 1.0.0-alpha5

### Fixed a problem with repeated emitter evaluation

`build-topology` had a bug that caused emitter given in the form of `(create)` to be
re-evaluated each time the topology was updated. The bug does not affect codebases
that use a single emitter instance bound to an existing var.

### Fixed a problem with `add-handler` not returining an instance of emitter

Usually an emitter is stored in a var, but if you use a threading
macro such as `->` to build topologies, `add-handler` failed because
it returned a caching registry. Thew new version returns the emitter,
allowing for threading macros to work.

### Optional `downstreams` argument for properly visualising splitters.

Because splitters only receives a function that's responsible for the
routing, it's impossible for EEP to know where the events are routed
after split. You can define a splitter with an array of all possible
splits to make data flow visualisation possible.

For exmaple, following splitter will split events to even and odd ones. Along with
splitter function, pass an vector of `[:even :odd]` so that visualiser would catch it.

```clj
(defsplitter *emitter* :entrypoint (fn [i] (if (even? i) :even :odd)) [:even :odd])
```

## Changes between 1.0.0-alpha3 and 1.0.0-alpha4

### Meltdown is updated to 1.0.0-aplha3

Meltown alpha3 is a release with minor API additions.

### Fixed problem with RingBuffer dispatcher overflow

RingBuffer operates in it's own pool, adding notifications blocks RingBuffer's yielding,
therefore `notify` function block forever.

EEP now has realistic throughput tests that verify that the issue is gone.

### Added more options to emitter constructor

It is now possible to pass backing Dispatcher for reactor that's handling routing for the
Emitter and an environment.

## Changes between 1.0.0-alpha2 and 1.0.0-alpha3

### Added an option to pass timer to timed window

Previously, timed window would keep emitting tuples to dead processing graph. Now you can
take control of your timer and stop timer together with emitter.

### Meltdown is updated to 1.0.0-aplha3

Meltown alpha3 is a release with minor bugfixes

## Changes between 1.0.0-alpha1 and 1.0.0-alpha2

### Meltdown is updated to 1.0.0-aplha2

## 1.0.0-alpha1

Initial release
