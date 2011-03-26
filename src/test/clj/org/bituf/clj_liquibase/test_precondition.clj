(ns org.bituf.clj-liquibase.test-precondition
  (:import
    (java.util Date))
  (:require
    [org.bituf.clj-liquibase.precondition :as pc])
  (:use org.bituf.test-util)
  (:use clojure.test))


(deftest test-changelog-prop-defined
  (fail))


(deftest test-changeset-executed
  (fail))


(deftest test-column-exists
  (fail))


(deftest test-dbms
  (fail))


(deftest test-foreign-key-exists
  (fail))


(deftest test-index-exists
  (fail))


(deftest test-primary-key-exists
  (fail))


(deftest test-running-as
  (fail))


(deftest test-sequence-exists
  (fail))


(deftest test-sql
  (fail))


(deftest test-table-exists
  (fail))


(deftest test-view-exists
  (fail))


(deftest test-pc-and
  (fail))


(deftest test-pc-not
  (fail))


(deftest test-pc-or
  (fail))


(deftest test-pre-cond
  (fail))


(defn test-ns-hook []
  (test-changelog-prop-defined)
  (test-changeset-executed)
  (test-column-exists)
  (test-dbms)
  (test-foreign-key-exists)
  (test-index-exists)
  (test-primary-key-exists)
  (test-running-as)
  (test-sequence-exists)
  (test-sql)
  (test-table-exists)
  (test-view-exists)
  (test-pc-and)
  (test-pc-not)
  (test-pc-or)
  (test-pre-cond))