(defproject clj-liquibase "0.4.0-SNAPSHOT"
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
                  :source  "1.5"
                  :target  "1.5"}
  :dependencies [[org.liquibase/liquibase-core "2.0.5"]
                 [clj-jdbcutil "0.1.0-SNAPSHOT"]
                 [clj-miscutil "0.4.1"]]
  :profiles {:dev {:dependencies [[oss-jdbc "0.8.0"]
                                  [clj-dbcp "0.8.0"]]}
             :1.2 {:dependencies [[org.clojure/clojure "1.2.1"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.0-alpha4"]]}}
  :aliases {"dev" ["with-profile" "dev,1.4"]
            "all" ["with-profile" "dev,1.2:dev,1.3:dev,1.4:dev,1.5"]}
  :warn-on-reflection true
  :min-lein-version "2.0.0"
  :jvm-opts ["-Xmx1g"])
