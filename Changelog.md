## Changes between 1.0.0-alpha4 and 1.0.0-alpha5

No changes yet


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
