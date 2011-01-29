(ns runtests
  (:require org.bituf.clj-liquibase.test-internal)
  (:require org.bituf.clj-liquibase.test-change)
  (:require org.bituf.test-clj-liquibase)
  (:use clojure.test))


(run-tests
  'org.bituf.clj-liquibase.test-internal
  'org.bituf.clj-liquibase.test-change
  'org.bituf.test-clj-liquibase)
