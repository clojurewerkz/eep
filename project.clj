(defproject clojurewerkz/eep "1.0.0-beta1"
  :description "Embedded Event Processing in Clojure"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.ifesdjeen/utils "0.4.0"]
                 [clojurewerkz/meltdown "1.1.0"]
                 [rhizome "0.2.0"]]
  :profiles {:1.7 {:dependencies [[org.clojure/clojure "1.7.0-alpha4"]]}
             :master {:dependencies [[org.clojure/clojure "1.7.0-master-SNAPSHOT"]]}
             :dev {:plugins [[codox "0.8.10"]]
                   :codox {:sources ["src/clojure"]
                           :output-dir "doc/api"}}}
  :aliases  {"all" ["with-profile" "dev:dev,1.7:dev,master"]}
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
