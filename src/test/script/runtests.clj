(ns runtests
  (:require org.bituf.clj-liquibase.test-internal)
  (:require org.bituf.clj-liquibase.test-change)
  (:require org.bituf.clj-liquibase.test-precondition)
  (:require org.bituf.clj-liquibase.test-sql-visitor)
  (:require org.bituf.test-clj-liquibase)
  (:use clojure.test))


(run-tests
  'org.bituf.clj-liquibase.test-internal
  'org.bituf.clj-liquibase.test-change
  'org.bituf.clj-liquibase.test-precondition
  'org.bituf.clj-liquibase.test-sql-visitor
  'org.bituf.test-clj-liquibase)
