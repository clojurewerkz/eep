(defproject clojurewerkz/eep "1.0.0-alpha6-SNAPSHOT"
  :description "Embedded Event Processing in Clojure"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.ifesdjeen/utils "0.4.0"]
                 [clojurewerkz/meltdown "1.0.0-alpha4"]
                 [rhizome "0.1.9"]]
  :profiles {:1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0-beta2"]]}
             :master {:dependencies [[org.clojure/clojure "1.6.0-master-SNAPSHOT"]]}
             :dev {:plugins [[codox 0.6.6]]
                   :codox {:sources ["src/clojure"]
                           :output-dir "doc/api"}}}
  :aliases  {"all" ["with-profile" "dev:dev,1.4:dev,1.6:dev,master"]}
  :repositories {"sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail}}
                 "springsource-milestone" {:url "http://repo.springsource.org/libs-milestone"
                                           :releases {:checksum :fail :update :always}}
                 "springsource-snapshots" {:url "http://repo.springsource.org/libs-snapshot"
                                           :snapshots true
                                           :releases {:checksum :fail :update :always}}
                 "sonatype-snapshots" {:url "http://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}
  :test-selectors     {:default     (fn [m] (not (:performance m)))
                       :performance :performance
                       :focus       :focus
                       :all         (constantly true)})
