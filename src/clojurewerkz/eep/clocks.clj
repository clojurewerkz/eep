;; This source code is dual-licensed under the Apache License, version
;; 2.0, and the Eclipse Public License, version 1.0.
;;
;; The APL v2.0:
;;
;; ----------------------------------------------------------------------------------
;; Copyright (c) 2014-2014 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;; ----------------------------------------------------------------------------------
;;
;; The EPL v1.0:
;;
;; ----------------------------------------------------------------------------------
;; Copyright (c) 2014-2014 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team.
;; All rights reserved.
;;
;; This program and the accompanying materials are made available under the terms of
;; the Eclipse Public License Version 1.0,
;; which accompanies this distribution and is available at
;; http://www.eclipse.org/legal/epl-v10.html.
;; ----------------------------------------------------------------------------------

(ns clojurewerkz.eep.clocks
  "Generic implementation clocks to be used in windowed oprations"
  (:refer-clojure :exclude [time]))

(defprotocol Clock
  (time [this] "Returns current clock time.")
  (elapsed? [this] "Returns wether clock is elapsed")
  (reset [_] "Resets clock")
  (tick [_] "Makes a tick within clock, updating internal counter"))

(deftype CountingClock [initial period current]
  Clock
  (time [_]
    current)

  (elapsed? [_]
    (> (- current initial) period))

  (tick [_]
    (CountingClock. initial period (inc current)))

  (reset [_]
    (CountingClock. current period current))

  Object
  (toString [_]
    (str "Initial: " initial ", Period: " period ", Current:" current)))

(defn- now
  "java.util.Date resolution is not enough for the Clock, as enqueue that's fired exactly after clock creation will
  produce a tick that yields same exact time."
  []
  (System/currentTimeMillis))

(deftype WallClock [initial period current]
  Clock
  (time [_]
    current)

  (elapsed? [_]
    (>= (- current initial) period))

  (tick [this]
    (WallClock. initial period (now)))

  (reset [_]
    (WallClock. (now) period (now)))

  Object
  (toString [_]
    (str "Initial: " initial ", Period: " period ", Current:" current)))


(defn make-counting-clock
  "Simplest clock implementation that increments counter on each clock tick."
  [period]
  (CountingClock. 0 period 0))

(defn make-wall-clock
  "Wall clock, using System/currentTimeMillis to check wether timer is elapsed"
  [period]
  (WallClock. (now) period (now)))

(def period
  {:second 1000
   :seconds 1000
   :minute (* 60 1000)
   :minutes (* 60 1000)
   :hour (* 60 60 1000)
   :hours (* 60 60 1000)
   :day (* 24 60 60 1000)
   :days (* 24 60 60 1000)
   :weeks (* 24 60 60 1000)})
