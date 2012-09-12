(ns clj-liquibase.test-sql-visitor
  (:import
    (liquibase.sql.visitor SqlVisitor
                           AppendSqlVisitor PrependSqlVisitor
                           RegExpReplaceSqlVisitor ReplaceSqlVisitor))
  (:require [clj-liquibase.sql-visitor :as vis])
  (:use test-util)
  (:use clojure.test))


(deftest test-can-make-append-visitor
  (doseq [[k r] {"some text" "some text"
                 :some-text  "some-text"}]
    (let [v (vis/make-append-visitor k)]
      (is (= r (.getValue v))))))


(deftest test-can-make-prepend-visitor
  (doseq [[k r] {"some text" "some text"
                 :some-text  "some-text"}]
    (let [v (vis/make-prepend-visitor k)]
      (is (= r (.getValue v))))))


(deftest test-can-make-replace-visitor
  (doseq [[rk rr wk wr] [["some text"  "some text" "new text" "new text"]
                         [:some-text   "some-text" :new-text  "new-text"]
                         [#"some-text" "some-text" :new-text  "new-text"]]]
    (let [v (vis/make-replace-visitor rk wk)]
      (is (= rr (.getReplace v)))
      (is (= wr (.getWith v))))))


(deftest test-misc-visitor-constraints
  (let [v (vis/make-append-visitor "text")
        f (vis/for-dbms!          :mysql    v)
        r (vis/apply-to-rollback! true      v)
        c (vis/for-contexts!      :some-ctx v)]
    (is (= #{"mysql"}    (.getApplicableDbms f)) "for-dbms!")
    (is (= true          (.isApplyToRollback r)) "apply-to-rollback!")
    (is (= #{"some-ctx"} (.getContexts c)) "for-contexts!")))


(deftest test-make-visitors
  (let [vs (vis/make-visitors
             :include (map (partial vis/for-dbms! :mysql)
                           (vis/make-visitors :append "engine=InnoDB"))
             :append  " -- creating table\n"
             :replace [:integer :bigint]
             :replace {:string "VARCHAR(256)"
                       #"varchar*" "VARCHAR2"}
             :prepend "IF NOT EXIST")]
    (is (coll? vs))
    (is (= 6 (count vs)))
    (is (every? vis/visitor? vs))))


(defn test-ns-hook []
  (test-can-make-append-visitor)
  (test-can-make-prepend-visitor)
  (test-can-make-replace-visitor)
  (test-misc-visitor-constraints)
  (test-make-visitors))