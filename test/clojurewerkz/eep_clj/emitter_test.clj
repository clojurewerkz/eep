(ns clojurewerkz.eep-clj.emitter-test
  (:use clojure.test
        clojurewerkz.eep-clj.emitter))

(deftest a-test
  (let [emitter (new-emitter)]
    (add-handler emitter :count + 100)
    (add-handler emitter :count + 0)
    (add-handler emitter :count println)

    (sync-notify emitter :count 1)
    (sync-notify emitter :count 1)
    (sync-notify emitter :count 1)

    (println (which-handlers emitter))

    ))
