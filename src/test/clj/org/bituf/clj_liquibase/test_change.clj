(ns org.bituf.clj-liquibase.test-change
  (:require
    [org.bituf.clj-liquibase.change :as change])
  (:use org.bituf.test-util)
  (:use clojure.test))


(defn ch-setup
  "Setup database for running tests"
  []
  (todo))


;; ----- Structural Refactorings -----


(deftest test-add-columns
  (testing "add-columns with required args"
    (ch-setup)))


(deftest test-rename-column
  (testing "rename-column"
    (ch-setup)))


(deftest test-modify-column
  (testing "modify-column"
    (ch-setup)))


(deftest test-drop-column
  (testing "drop-column"
    (ch-setup)))


(deftest test-alter-sequence
  (testing "alter-sequence"
    (ch-setup)))


(deftest test-create-table
  (testing "create-table"
    (ch-setup)))


(deftest test-rename-table
  (testing "rename-table"
    (ch-setup)))


(deftest test-drop-table
  (testing "drop-table"
    (ch-setup)))


(deftest test-create-view
  (testing "create-view"
    (ch-setup)))


(deftest test-rename-view
  (testing "rename-view"
    (ch-setup)))


(deftest test-drop-view
  (testing "drop-view"
    (ch-setup)))


(deftest test-merge-columns
  (testing "merge-columns"
    (ch-setup)))


(deftest test-create-stored-procedure
  (testing "create-stored-procedure"
    (ch-setup)))


;; ----- Data Quality Refactorings -----


(deftest test-add-lookup-table
  (testing "add-lookup-table"
    (ch-setup)))


(deftest test-add-not-null-constraint
  (testing "add-not-null-constraint"
    (ch-setup)))


(deftest test-drop-not-null-constraint
  (testing "drop-not-null-constraint"
    (ch-setup)))


(deftest test-add-unique-constraint
  (testing "add-unique-constraint"
    (ch-setup)))


(deftest test-drop-unique-constraint
  (testing "drop-unique-constraint"
    (ch-setup)))


(deftest test-create-sequence
  (testing "create-sequence"
    (ch-setup)))


(deftest test-drop-sequence
  (testing "drop-sequence"
    (ch-setup)))


(deftest test-add-auto-increment
  (testing "add-auto-increment"
    (ch-setup)))


(deftest test-add-default-value
  (testing "add-default-value"
    (ch-setup)))


(deftest test-drop-default-value
  (testing "drop-default-value"
    (ch-setup)))


;; ----- Referential Integrity Refactorings -----


(deftest test-add-foreign-key-constraint
  (testing "add-foreign-key-constraint"
    (ch-setup)))


(deftest test-drop-foreign-key-constraint
  (testing "drop-foreign-key-constraint"
    (ch-setup)))


(deftest test-add-primary-key
  (testing "add-primary-key"
    (ch-setup)))


(deftest test-drop-primary-key
  (testing "drop-primary-key"
    (ch-setup)))


;; ----- Non-Refactoring Transformations -----


(deftest test-insert-data
  (testing "insert-data"
    (ch-setup)))


(deftest test-load-data
  (testing "load-data"
    (ch-setup)))


(deftest test-load-update-data
  (testing "load-update-data"
    (ch-setup)))


(deftest test-update-data
  (testing "update-data"
    (ch-setup)))


(deftest test-delete-data
  (testing "delete-data"
    (ch-setup)))


(deftest test-tag-database
  (testing "tag-database"
    (ch-setup)))


(deftest test-stop
  (testing "stop"
    (ch-setup)))


;; ----- Architectural Refactorings -----


(deftest test-create-index
  (testing "create-index"
    (ch-setup)))


(deftest test-drop-index
  (testing "drop-index"
    (ch-setup)))


(defn test-ns-hook []
  ;; ----- Structural Refactorings -----
  (test-add-columns)
  (test-rename-column)
  (test-modify-column)
  (test-drop-column)
  (test-alter-sequence)
  (test-create-table)
  (test-rename-table)
  (test-drop-table)
  (test-create-view)
  (test-rename-view)
  (test-drop-view)
  (test-merge-columns)
  (test-create-stored-procedure)
  ;; ----- Data Quality Refactorings -----
  (test-add-lookup-table)
  (test-add-not-null-constraint)
  (test-drop-not-null-constraint)
  (test-add-unique-constraint)
  (test-drop-unique-constraint)
  (test-create-sequence)
  (test-drop-sequence)
  (test-add-auto-increment)
  (test-add-default-value)
  (test-drop-default-value)
  ;; ----- Referential Integrity Refactorings -----
  (test-add-foreign-key-constraint)
  (test-drop-foreign-key-constraint)
  (test-add-primary-key)
  (test-drop-primary-key)
  ;; ----- Non-Refactoring Transformations -----
  (test-insert-data)
  (test-load-data)
  (test-load-update-data)
  (test-update-data)
  (test-delete-data)
  (test-tag-database)
  (test-stop)
  ;; ----- Architectural Refactorings -----
  (test-create-index)
  (test-drop-index)
  (test-drop-default-value)
  (test-drop-default-value)
  (test-drop-default-value)
  (test-drop-default-value))
