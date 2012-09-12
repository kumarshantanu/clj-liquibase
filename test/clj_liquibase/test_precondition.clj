(ns clj-liquibase.test-precondition
  (:import
    (java.util Date)
    (liquibase.exception MigrationFailedException PreconditionFailedException))
  (:require
    [org.bituf.clj-miscutil     :as mu]
    [org.bituf.clj-dbspec       :as sp]
    [clj-liquibase.core         :as lb]
    [clj-liquibase.change       :as ch]
    [clj-liquibase.precondition :as pc]
    [test-clj-liquibase         :as tl])
  (:use test-util)
  (:use clojure.test))


(deftest test-changelog-prop-defined
  (fail))


(def changeset-1 ["id=1" "author=shantanu" [tl/ct-change1 tl/ct-change2
                                            (ch/insert-data
                                              :sample-table-1 {:name   "Henry"
                                                               :gender "M"})
                                            (ch/create-view
                                              :sample-view-1 "SELECT * FROM sample_table_1")
                                            (ch/create-sequence
                                              :foo)
                                            (ch/create-index
                                              :sample-table-1 [:name]
                                              :index-name :sample-1-index-1)
                                            (ch/add-foreign-key-constraint
                                              :f-cons         ; constraint-name
                                              :sample-table-2 ; base-table-name
                                              [:f-id]         ; base-column-names
                                              :sample-table-1 ; referenced-table-name
                                              [:id]           ; referenced-column-names
                                              )]])


(def changeset-2 ["id=2" "author=shantanu" [tl/ct-change3]])


;(lb/defchangelog clog-1 "precond" [changeset-1])


;(lb/defchangelog clog-2 "precond" [changeset-1 changeset-2])


(deftest test-changeset-executed
  (sp/with-dbspec (tl/dbspec)
    (lb/with-lb
      (let [cs-0 (into changeset-1
                   [:pre-cond [(pc/changeset-executed
                                 "precond" "id=0" "author=shantanu")]])
            cs-1 changeset-1
            cs-2 (into changeset-2
                   [:pre-cond [(pc/changeset-executed
                                 "precond" "id=1" "author=shantanu")]])]
        (lb/defchangelog clog-0 "precond" [cs-0])
        (lb/defchangelog clog-1 "precond" [cs-1])
        (lb/defchangelog clog-2 "precond" [cs-1 cs-2])
        (tl/clb-setup)
        (let [[r e] (mu/maybe (lb/update clog-0))]
          (is (instance? MigrationFailedException e))
          (is (instance? PreconditionFailedException (.getCause ^Exception e))))
        (lb/update clog-1)
        (lb/update clog-2)))))


(deftest test-column-exists
  (sp/with-dbspec (tl/dbspec)
    (lb/with-lb
      (let [cs-1 changeset-1
            cs-2 (into changeset-2
                   [:pre-cond [(pc/column-exists
                                 "" :sample-table-1 :id)]])]
        (lb/defchangelog clog-1 "precond" [cs-1])
        (lb/defchangelog clog-2 "precond" [cs-1 cs-2])
        (tl/clb-setup)
        (lb/update clog-1)
        (lb/update clog-2)))))


(deftest test-dbms
  (sp/with-dbspec (tl/dbspec)
    (lb/with-lb
      (let [cs-1 changeset-1
            cs-2 (into changeset-2
                   [:pre-cond [(pc/dbms :h2)]])]
        (lb/defchangelog clog-1 "precond" [cs-1])
        (lb/defchangelog clog-2 "precond" [cs-1 cs-2])
        (tl/clb-setup)
        (lb/update clog-1)
        (lb/update clog-2)))))


(deftest test-foreign-key-exists
  (sp/with-dbspec (tl/dbspec)
    (lb/with-lb
      (let [cs-1 changeset-1
            cs-2 changeset-2]
        (lb/defchangelog clog-1 "precond" [cs-1])
        (lb/defchangelog clog-2 "precond"
                         [cs-1 (into cs-2
                                 [:pre-cond [(pc/foreign-key-exists
                                               "" :sample-table-2 :f-cons)]])])
        (tl/clb-setup)
        (lb/update clog-1)
        (lb/update clog-2)))))


(deftest test-index-exists
  (sp/with-dbspec (tl/dbspec)
    (lb/with-lb
      (let [cs-1 changeset-1
            cs-2 changeset-2]
        (lb/defchangelog clog-1 "precond" [cs-1])
        (lb/defchangelog clog-2 "precond"
                         [cs-1 (into cs-2
                                 [:pre-cond [(pc/index-exists
                                               "" :sample-table-1 [:name]
                                               :sample-1-index-1)]])])
        (tl/clb-setup)
        (lb/update clog-1)
        (lb/update clog-2)))))


(deftest test-primary-key-exists
  (sp/with-dbspec (tl/dbspec)
    (lb/with-lb
      (let [cs-1 changeset-1
            cs-2 changeset-2]
        (lb/defchangelog clog-1 "precond" [cs-1])
        (lb/defchangelog clog-2 "precond"
                         [cs-1 (into cs-2
                                 [:pre-cond [(pc/primary-key-exists
                                               "" :sample-table-1
                                               :pk-sample-table-1)]])])
        (tl/clb-setup)
        (lb/update clog-1)
        (lb/update clog-2)))))


(deftest test-running-as
  (sp/with-dbspec (tl/dbspec)
    (lb/with-lb
      (let [cs-1 (into changeset-1
                   [:pre-cond [(pc/running-as "sa")]])]
        (lb/defchangelog clog-1 "precond" [cs-1])
        (tl/clb-setup)
        (lb/update clog-1)))))


(deftest test-sequence-exists
  (sp/with-dbspec (tl/dbspec)
    (lb/with-lb
      (let [cs-1 changeset-1
            cs-2 (into changeset-2
                   [:pre-cond [(pc/sequence-exists "" :foo)]])]
        (lb/defchangelog clog-1 "precond" [cs-1])
        (lb/defchangelog clog-2 "precond" [cs-2])
        (tl/clb-setup)
        (lb/update clog-1)
        (lb/update clog-2)))))


(deftest test-sql
  (sp/with-dbspec (tl/dbspec)
    (lb/with-lb
      (let [cs-1 changeset-1
            cs-2 (into changeset-2
                   [:pre-cond [(pc/sql 1 "SELECT COUNT(*) FROM sample_table_1")]])]
        (lb/defchangelog clog-1 "precond" [cs-1])
        (lb/defchangelog clog-2 "precond" [cs-2])
        (tl/clb-setup)
        (lb/update clog-1)
        (lb/update clog-2)))))


(deftest test-table-exists
  (sp/with-dbspec (tl/dbspec)
    (lb/with-lb
      (let [cs-1 changeset-1
            cs-2 (into changeset-2
                   [:pre-cond [(pc/table-exists "" :sample-table-1)]])]
        (lb/defchangelog clog-1 "precond" [cs-1])
        (lb/defchangelog clog-2 "precond" [cs-2])
        (tl/clb-setup)
        (lb/update clog-1)
        (lb/update clog-2)))))


(deftest test-view-exists
  (sp/with-dbspec (tl/dbspec)
    (lb/with-lb
      (let [cs-1 changeset-1
            cs-2 (into changeset-2
                   [:pre-cond [(pc/view-exists "" :sample-view-1)]])]
        (lb/defchangelog clog-1 "precond" [cs-1])
        (lb/defchangelog clog-2 "precond" [cs-2])
        (tl/clb-setup)
        (lb/update clog-1)
        (lb/update clog-2)))))


(deftest test-pc-and
  (testing "pc-and"
    (sp/with-dbspec (tl/dbspec)
      (lb/with-lb
        (let [cs-1 changeset-1
              cs-2 (into changeset-2
                     [:pre-cond [(pc/pc-and
                                   (pc/table-exists "" :sample-table-1)
                                   (pc/view-exists  "" :sample-view-1))
                                 ]])]
          (lb/defchangelog clog-1 "precond" [cs-1])
          (lb/defchangelog clog-2 "precond" [cs-2])
          (tl/clb-setup)
          (lb/update clog-1)
          (lb/update clog-2))))))


(deftest test-pc-not
  (testing "pc-not"
    (sp/with-dbspec (tl/dbspec)
      (lb/with-lb
        (let [cs-1 changeset-1
              cs-2 (into changeset-2
                     [:pre-cond [(pc/pc-not
                                   (pc/table-exists "" :sample-table-11)
                                   (pc/view-exists  "" :sample-view-11))
                                 ]])]
          (lb/defchangelog clog-1 "precond" [cs-1])
          (lb/defchangelog clog-2 "precond" [cs-2])
          (tl/clb-setup)
          (lb/update clog-1)
          (lb/update clog-2))))))


(deftest test-pc-or
  (testing "pc-or"
    (sp/with-dbspec (tl/dbspec)
      (lb/with-lb
        (let [cs-1 changeset-1
              cs-2 (into changeset-2
                     [:pre-cond [(pc/pc-or
                                   (pc/table-exists "" :sample-table-11)
                                   (pc/view-exists  "" :sample-view-1))
                                 ]])]
          (lb/defchangelog clog-1 "precond" [cs-1])
          (lb/defchangelog clog-2 "precond" [cs-2])
          (tl/clb-setup)
          (lb/update clog-1)
          (lb/update clog-2))))))


(deftest test-pre-cond
  (testing "pre-cond"
    (let [pcs [(pc/pre-cond [(pc/dbms :h2)] :on-error :halt     :on-error-msg "halt"     :on-fail :halt     :on-fail-msg "halt")
               (pc/pre-cond [(pc/dbms :h2)] :on-error :continue :on-error-msg "continue" :on-fail :continue :on-fail-msg "continue")
               (pc/pre-cond [(pc/dbms :h2)] :on-error :mark-ran :on-error-msg "mark_ran" :on-fail :mark-ran :on-fail-msg "mark_ran")
               (pc/pre-cond [(pc/dbms :h2)] :on-error :warn     :on-error-msg "warn"     :on-fail :warn     :on-fail-msg "warn")
               (pc/pre-cond [(pc/dbms :h2)] :on-update-sql :ignore)
               (pc/pre-cond [(pc/dbms :h2)] :on-update-sql :test)
               (pc/pre-cond [(pc/dbms :h2)] :on-update-sql :fail)]]
      (doseq [each pcs]
        (sp/with-dbspec (tl/dbspec)
          (lb/with-lb
            (lb/defchangelog cl "precond" [(into changeset-1 [:pre-cond each])])
            (lb/update cl)))))))


(defn test-ns-hook []
  ;(test-changelog-prop-defined)
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