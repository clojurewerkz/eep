# EEP, Embedded Event Processing in Clojure

EEP is a Clojure library for embedded event processing.
It combines a lightweight generic event handling system,
and with multiple windowed stream operations.

eep-clj is heavily influenced by other EEP projects:

  * [eep-js (JavaScript)](https://github.com/darach/eep-js)
  * [eep-erl (Erlang)](https://github.com/darach/eep-erl)
  * [eep-php (PHP)](https://github.com/ianbarber/eep-php)


## Project Maturity

EEP is a *very young* project. Nothing is considered stable or final.



## Supported Clojure Versions

EEP is built from the ground up for Clojure 1.4 and up.


## Maven Artifacts

### Most Recent Release

With Leiningen:

    [clojurewerkz/eep "0.3.0"]

With Maven:

    <dependency>
      <groupId>clojurewerkz</groupId>
      <artifactId>eep</artifactId>
      <version>0.3.0</version>
    </dependency>


## Documentation & Examples

Documentation site is not ready yet.


## Development

EEP uses [Leiningen 2](https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md). Make
sure you have it installed and then run tests against all supported Clojure versions using

    lein2 all test

Then create a branch and make your changes on it. Once you are done with your changes and all
tests pass, submit a pull request on Github.

## License

Copyright © 2013 Michael Klishin, Alex P

Distributed under the Eclipse Public License, the same as Clojure.
