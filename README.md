# Clojure Embedded Event Processing

eep-clj consists of two parts first one is a lightweight generic Event Handling system,
and second one is windowed steam operations.

eep-clj have started as an initiative after other EEP projects:

  * [eep-js (JavaScript)](https://github.com/darach/eep-erl)
  * [eep-erl (Erlang)](https://github.com/darach/eep-erl)
  * [eep-php (PHP)](https://github.com/ianbarber/eep-php)

# Project status

This is a thought experiment, API can change at any moment. Nothing is considered stable
or final. We keep modifying, improving things in order to provide best, Clojuric way to
handle events and streams.

# Event Emitter

Idea and API of Event Emitter was inspired by Erlang [gen_event behavior](http://www.erlang.org/doc/man/gen_event.html)

Currently, event execution order is not guaranteed. It is currently solved by sync-notify function,
that is being executed synchronously. Althought that undermines usage of STM in that case. Future
versions will contain implementation with order guarantee.

## Usage

FIXME

## License

Copyright © 2013 Michael Klishin, Alex P

Distributed under the Eclipse Public License, the same as Clojure.
