(ns clojurewerkz.eep.visualization
  (:use rhizome.viz
        clojurewerkz.eep.emitter))

(defn shape-by-type
  [emitter key]
  (let [handler-type (last (clojure.string/split (str (type (get-handler emitter key))) #"\."))]
    (case handler-type
      "Transformer" "box"
      "Aggregator" "box3d"
      "CommutativeAggregator" "hexagon"
      "Multicast" "diamond"
      "Buffer"  "folder"
      "Observer" "Mcircle"
      "Rollup" "doubleoctagon"
      "Filter" "triangle"
      "Splitter" "pentagon"
      :else "ellipse")))

(defn visualise-graph
  [emitter]
  (let [g (into {}
                (for [[k v] (get-handler emitter)]
                  [k (downstream v)]))]

    (println (filter (fn [k] (not (or (= :splitter k)
                                        (= :rebroadcast k))))
                     (keys g)))
    (view-graph (keys g)
                g
                :node->descriptor (fn [n]
                                    {:label (name n) :shape (shape-by-type emitter n)}))))
