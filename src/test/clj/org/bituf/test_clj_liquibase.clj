(ns org.bituf.test-clj-liquibase
  (:require
    [org.bituf.clj-liquibase :as clb])
  (:use org.bituf.test-util)
  (:use clojure.test))


(defn clb-setup
  "Setup database for running tests"
  []
  (todo))


;; ===== ChangeSet =====


(deftest test-make-changeset
  (testing "make-changeset"
    (clb-setup)))


(deftest test-changeset
  (testing "changeset"
    (clb-setup)))


;; ===== DatabaseChangeLog =====


(deftest test-make-db-changelog
  (testing "make-db-changelog"
    (clb-setup)))


(deftest test-defchangelog
  (testing "defchangelog"
    (clb-setup)))


;; ===== Actions =====


(deftest test-update
  (testing "update"
    (clb-setup)))


(deftest test-update-by-count
  (testing "update-by-count"
    (clb-setup)))


(deftest test-tag
  (testing "tag"
    (clb-setup)))


(deftest test-rollback-to-tag
  (testing "rollback-to-tag"
    (clb-setup)))


(deftest test-rollback-to-date
  (testing "rollback-to-date"
    (clb-setup)))


(deftest test-rollback-by-count
  (testing "rollback-by-count"
    (clb-setup)))


(defn test-ns-hook []
  ;; ===== ChangeSet =====
  (test-make-changeset)
  (test-changeset)
  ;; ===== DatabaseChangeLog =====
  (test-make-db-changelog)
  (test-defchangelog)
  ;; ===== Actions =====
  (test-update)
  (test-update-by-count)
  (test-tag)
  (test-rollback-to-tag)
  (test-rollback-to-date)
  (test-rollback-by-count))
