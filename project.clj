(defproject clj-liquibase "0.6.0"
  :description "Clojure wrapper for Liquibase"
  :url "https://github.com/kumarshantanu/clj-liquibase"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :mailing-list {:name "Bitumen Framework discussion group"
                 :archive "https://groups.google.com/group/bitumenframework"
                 :other-archives ["https://groups.google.com/group/clojure"]
                 :post "bitumenframework@googlegroups.com"}
  :java-source-paths ["java-src"]
  :javac-options {:destdir "target/classes/"
                  :source  "1.6"
                  :target  "1.6"}
  :dependencies [[org.liquibase/liquibase-core "3.0.8"]
                 [liquibase-edn "3.0.8-0.1.1"]
                 [clj-jdbcutil "0.1.0"]
                 [clj-miscutil "0.4.1"]]
  :profiles {:dev {:dependencies [[oss-jdbc "0.8.0"]
                                  [clj-dbcp "0.8.1"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]
                   :global-vars  {*unchecked-math* :warn-on-boxed}}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0-RC2"]]
                   :global-vars  {*unchecked-math* :warn-on-boxed}}}
  :aliases {"dev" ["with-profile" "dev,1.7"]
            "all" ["with-profile" "dev,1.3:dev,1.4:dev,1.5:dev,1.6:dev,1.7:dev,1.8"]}
  :global-vars {*warn-on-reflection* true}
  :min-lein-version "2.0.0"
  :jvm-opts ["-Xmx1g"])
