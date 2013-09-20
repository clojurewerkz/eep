## Changes between 1.0.0-alpha5 and 1.0.0-alpha6

No changes yet

## Changes between 1.0.0-alpha4 and 1.0.0-alpha5

### Fixed a problem with repeated emitter evaluation

`build-topology` had a bug that caused emitter given in the form of `(create)` to be
re-evaluated each time the topology was updated. The bug does not affect codebases
that use a single emitter instance bound to an existing var.

### Fixed a problem with `add-handler` not returining an instance of emitter

Usually it's emitter is bound to the variable, but if you use `->` macro to create
topologies, old version of `add-handler` would not work since it returns an instance
of Caching Registry. New version works just fine, tests were added to cover that issue.

### Added optional `downstreams` argument for properly visualising `Splitter`.

Since Splitter only receives a function that's responsible for routing, it's impossible
for EEP to know where the events are routed after split. You can define a splitter
with an array of all possible splits to make visualisation possible.

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
therefore makes notify function block eternally.

In order to verify this and avoid having similar problems in future, corresponding
throughput tests with realistic numbers were added.

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
