(ns org.bituf.test-clj-liquibase
  (:import
    (org.bituf.clj_dbspec IRow)
    (org.bituf.clj-dbspec Row))
  (:require
    [clojure.string                 :as sr]
    [clojure.pprint                 :as pp]
    [org.bituf.clj-miscutil         :as mu]
    [org.bituf.clj-liquibase        :as lb]
    [org.bituf.clj-liquibase.change :as ch]
    [org.bituf.clj-dbcp             :as dbcp]
    [org.bituf.clj-dbspec           :as spec])
  (:use org.bituf.test-util)
  (:use clojure.test))


(defn clb-setup
  "Setup database for running tests"
  []
  (todo))


(defn make-ds
  []
  (dbcp/h2-memory-datasource)
  ;(dbcp/mysql-datasource "localhost" "bituf" "root" "root")
  )


(defn dbspec
  []
  (spec/make-dbspec (make-ds)))


(def ct-change1 (mu/! (ch/create-table :sample-table-1
                        [[:id     :int          :null false :pk true :autoinc true]
                         [:name   [:varchar 40] :null false]
                         [:gender [:char 1]     :null false]])))

(def ct-change2 (mu/! (ch/create-table "sampletable2"
                        [[:id     :int          :null false :pk true :autoinc true]
                         [:name   [:varchar 40] :null false]
                         [:gender [:char 1]     :null false]])))


(def changeset-1 ["id=1" "author=shantanu" [ct-change1]])


(def changeset-2 ["id=2" "author=shantanu" [ct-change2]])


;; ===== ChangeSet =====


(deftest test-make-changeset
  (testing "make-changeset"
    (let [pf (partial lb/make-changeset "id=1" "author=shantanu" [ct-change1])]
      (is (lb/changeset? (pf :filepath "dummy")) "Minimum arguments")
      (is (lb/changeset? (pf
                           :logical-filepath   "somename"
                           :dbms               :mysql
                           :run-always         true
                           :run-on-change      false
                           :context            "some-ctx"
                           :run-in-transaction true
                           :fail-on-error      true
                           :comment            "describe this"
                           :pre-conditions     nil
                           :rollback-changes   []
                           :valid-checksum     "1234"
                           )) "Optional arguments with longname")
      (is (lb/changeset? (pf
                           :filepath   "dummy"
                           :always     false
                           :on-change  true
                           :ctx        "sample"
                           :in-txn     false
                           :fail-err   true
                           :pre-cond   nil
                           :rollback   []
                           :valid-csum "something"
                           )) "Optional arguments with shortname"))))


;; ===== DatabaseChangeLog =====


(deftest test-make-changelog
  (testing "make-changelog"
    (let [cl? lb/changelog?
          fp  "dummy"
          mcl lb/make-changelog]
      (is (thrown? IllegalArgumentException (cl? (mcl fp []))) "No changeset")
      (is (cl? (mcl fp [changeset-1]            )) "1 changeset")
      (is (cl? (mcl fp [changeset-1 changeset-2])) "2 changesets")
      (is (cl? (mcl fp [changeset-1 changeset-2]
                 :pre-conditions     nil))
        "With optional args - long names")
      (is (cl? (mcl fp [changeset-1 changeset-2]
                 :pre-cond   nil))
        "With optional args - short names"))))


(lb/defchangelog clog-1 [changeset-1])


(lb/defchangelog clog-2 [changeset-1 changeset-2])


(deftest test-defchangelog
  (testing "defchangelog"
    (is (fn? clog-1))
    (is (fn? clog-2))))


;; ===== Actions =====


(deftest test-update
  (testing "update"
    (lb/with-dbspec (dbspec)
      (lb/update clog-1)
      ;; TODO find tables
      (mu/!
        (let [conn ^java.sql.Connection (:connection spec/*dbspec*)
              _        (assert (mu/not-nil? conn))
              dbmdata  (.getMetaData conn)
              _        (assert (mu/not-nil? dbmdata))
              catalogs (spec/get-catalogs dbmdata)
              schemas  (spec/get-schemas  dbmdata)
              tables   (spec/get-tables   dbmdata)
              columns  (spec/get-columns dbmdata :table-pattern "SAMPLE_TABLE_1"
                         )]
          (println "\n**** All catalogs ****")
          (mu/! (mu/print-table (map #(.asMap ^IRow %) catalogs)))
          
          (println "\n**** All schemas ****")
          (mu/! (mu/print-table (map #(.asMap ^IRow %) schemas)))
          
          (println "\n**** All tables ****")
          (when (not (empty? tables))
            (pp/pprint (keys (.asMap ^IRow (first tables))))
            (mu/! (mu/print-table (map #(.asVec ^IRow %) tables)))
            )
          
          (println "\n**** All columns ****")
          (when (not (empty? columns))
            (pp/pprint (keys (.asMap ^IRow (first columns))))
            (mu/! (mu/print-table (map #(.asVec ^IRow %) columns)))
            )
          ))
      )))


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
  ;; ===== DatabaseChangeLog =====
  (test-make-changelog)
  (test-defchangelog)
  ;; ===== Actions =====
  (test-update)
  (test-update-by-count)
  (test-tag)
  (test-rollback-to-tag)
  (test-rollback-to-date)
  (test-rollback-by-count))
