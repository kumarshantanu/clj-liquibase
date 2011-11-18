(ns org.bituf.test-clj-liquibase
  (:import
    (java.io              File)
    (java.sql             Connection SQLException)
    (javax.sql            DataSource)
    (org.bituf.clj_dbspec IRow)
    (org.bituf.clj_dbspec Row))
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
  (spec/with-connection spec/*dbspec*
    (let [conn (:connection spec/*dbspec*)]
      (assert (or (println "Testing connection") conn (println "= NULL")))
      (with-open [stmt (.createStatement ^Connection conn)]
        (doseq [each [:sample-table-1 :sample-table-2 "sampletable3"
                      :databasechangelog :databasechangeloglock]]
          (try
            (.executeUpdate stmt (format "DROP TABLE %s"
                                   (spec/db-iden each)))
            (println "Deleted table " (spec/db-iden each))
            (catch SQLException e
              (println "Ignoring exception: " e))))))))


(def db {:h2-mem {:dbcp #(dbcp/h2-memory-datasource)
                  :int  "INTEGER"}
         :mysql  {:dbcp #(dbcp/mysql-datasource "localhost" "bituf" "root" "root")
                  :int  "INT"}})


(def dialect :h2-mem)
;(def dialect :mysql)


(defn make-ds [] {:post [(instance? DataSource %)]}
  ((:dbcp (dialect db))))


(defn db-int [] {:post [(string? %)]}
  (:int (dialect db)))


(defn dbspec [] {:post [(map? %)]}
  (spec/make-dbspec (make-ds)))


(defn lb-action
  "Run Liquibase action"
  [f] {:post [(mu/not-fn? %)]
       :pre  [(fn? f)]}
  (spec/with-dbspec (dbspec)
    (lb/with-lb
      (mu/! (f)))))


(defmacro with-lb-action
  "Run body of code as Liquibase action"
  [& body]
  `(lb-action (fn [] ~@body)))


(def ct-change1 (mu/! (ch/create-table :sample-table-1
                        [[:id     :int          :null false :pk true :autoinc true :pkname :pk-sample-table-1]
                         [:name   [:varchar 40] :null false]
                         [:gender [:char 1]     :null false]])))

(def ct-change2 (mu/! (ch/create-table :sample-table-2
                        [[:id     :int          :null false :pk true :autoinc true]
                         [:f-id   :int]
                         [:name   [:varchar 40] :null false]
                         [:gender [:char 1]     :null false]])))

(def ct-change3 (mu/! (ch/create-table "sampletable3"
                        [[:id     :int          :null false :pk true :autoinc true]
                         [:name   [:varchar 40] :null false]
                         [:gender [:char 1]     :null false]])))


(def changeset-1 ["id=1" "author=shantanu" [ct-change1 ct-change2]])


(def changeset-2 ["id=2" "author=shantanu" [ct-change3]])


(lb/defchangelog clog-1 [changeset-1])


(lb/defchangelog clog-2 [changeset-1 changeset-2])


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


(deftest test-defchangelog
  (testing "defchangelog"
    (is (fn? clog-1))
    (is (fn? clog-2))))


;; ===== Actions =====


(defn update-test-helper
  "Example table-desc is below:
  [:table-name
   [:id     :int          :null false :pk true :autoinc true]
   [:name   [:varchar 40] :null false]
   [:gender [:char 1]     :null false]]"
  [table-desc & more]
  (println "Entered uth")
  (let [u-tables (into [table-desc] more)]
    (println (format "Asserting %d tables" (count u-tables)))
    (mu/!
      (doseq [each u-tables]
        (let [[^String t-name & t-cols] each
              conn ^java.sql.Connection (:connection spec/*dbspec*)
                _        (assert (mu/not-nil? conn))
                dbmdata  (.getMetaData conn)
                _        (assert (mu/not-nil? dbmdata))
                catalogs (spec/get-catalogs dbmdata)
                schemas  (spec/get-schemas  dbmdata)
                tables   (spec/get-tables   dbmdata)
                tb-names (spec/table-names  tables)
                columns  (spec/get-columns  dbmdata :table-pattern t-name)]
            (println "\n**** All catalogs ****")
            (mu/! (mu/print-table (map #(.asMap ^IRow %) catalogs)))
            
            (println "\n**** All schemas ****")
            (mu/! (mu/print-table (map #(.asMap ^IRow %) schemas)))
            
            (println "\n**** All tables ****")
            (when (not (empty? tables))
              (pp/pprint (keys (.asMap ^IRow (first tables))))
              (mu/! (mu/print-table (map #(.asVec ^IRow %) tables))))
            
            (is (= (count u-tables) (- (count tb-names) 2)))
            
            (is (-> (map sr/upper-case tb-names)
                  (mu/contains-val? t-name)) (format "%s does not contain %s"
                                               (map sr/upper-case tb-names)
                                               t-name))
            
            (println "\n**** All columns ****")
            (when (not (empty? columns))
              (pp/pprint (keys (.asMap ^IRow (first columns))))
              (mu/! (mu/print-table (map #(.asVec ^IRow %) columns))))
            
            (let [sel-cols [:table-name :column-name :type-name :is-nullable
                            :is-autoincrement]
                  act-cols (vec (map #(select-keys (.asMap ^IRow %) sel-cols)
                                  columns))
                  exp-cols (vec (map #(zipmap sel-cols %) t-cols))]
              (is (= (count act-cols) (count exp-cols)))
              (dorun (map #(is (= (mu/map-vals sr/upper-case %1)
                                 (mu/map-vals sr/upper-case %2)))
                       act-cols exp-cols))))))))


(defn update-test
  []
  (clb-setup)
  (lb/update clog-1)
  (update-test-helper
    ["SAMPLE_TABLE_1"
     ["SAMPLE_TABLE_1" "ID"     (db-int)  "NO" "YES"]
     ["SAMPLE_TABLE_1" "NAME"   "VARCHAR" "NO" "NO" ]
     ["SAMPLE_TABLE_1" "GENDER" "CHAR"    "NO" "NO" ]]
    ["SAMPLE_TABLE_2"
     ["SAMPLE_TABLE_2" "ID"     (db-int)  "NO"  "YES"]
     ["SAMPLE_TABLE_2" "F_ID"   (db-int)  "YES" "NO"]
     ["SAMPLE_TABLE_2" "NAME"   "VARCHAR" "NO"  "NO" ]
     ["SAMPLE_TABLE_2" "GENDER" "CHAR"    "NO"  "NO" ]]))


(deftest test-update
  (testing "update"
    (lb-action
      update-test)))


(defn update-idempotency-test
  []
  (clb-setup)
  (lb/update clog-1)
  (lb/update clog-1)
  (update-test-helper
    ["SAMPLE_TABLE_1"
     ["SAMPLE_TABLE_1" "ID"     (db-int)  "NO" "YES"]
     ["SAMPLE_TABLE_1" "NAME"   "VARCHAR" "NO" "NO" ]
     ["SAMPLE_TABLE_1" "GENDER" "CHAR"    "NO" "NO" ]]
    ["SAMPLE_TABLE_2"
     ["SAMPLE_TABLE_2" "ID"     (db-int)  "NO" "YES"]
     ["SAMPLE_TABLE_2" "F_ID"   (db-int)  "YES" "NO"]
     ["SAMPLE_TABLE_2" "NAME"   "VARCHAR" "NO" "NO" ]
     ["SAMPLE_TABLE_2" "GENDER" "CHAR"    "NO" "NO" ]]))


(deftest test-update-idempotency
  (testing "update(idempotency)"
    (lb-action
      update-idempotency-test)))


(defn update-by-count-test
  []
  (clb-setup)
  (lb/update-by-count clog-2 1)
  (update-test-helper
    ["SAMPLE_TABLE_1"
     ["SAMPLE_TABLE_1" "ID"     (db-int)  "NO" "YES"]
     ["SAMPLE_TABLE_1" "NAME"   "VARCHAR" "NO" "NO" ]
     ["SAMPLE_TABLE_1" "GENDER" "CHAR"    "NO" "NO" ]]
    ["SAMPLE_TABLE_2"
     ["SAMPLE_TABLE_2" "ID"     (db-int)  "NO" "YES"]
     ["SAMPLE_TABLE_2" "F_ID"   (db-int)  "YES" "NO"]
     ["SAMPLE_TABLE_2" "NAME"   "VARCHAR" "NO" "NO" ]
     ["SAMPLE_TABLE_2" "GENDER" "CHAR"    "NO" "NO" ]]))


(deftest test-update-by-count
  (testing "update-by-count"
    (lb-action
      update-by-count-test)))


(defn tag-test
  []
  (clb-setup)
  (lb/update clog-1)
  (lb/tag    "mytag")
  (is (= "mytag" (mu/! (query-value "SELECT tag FROM databasechangelog")))
    "Tag name should match"))


(deftest test-tag
  (testing "tag"
    (lb-action
      tag-test)))


(defn rollback-to-tag-test
  []
  (clb-setup)
  (lb/update clog-1)
  (lb/tag    "mytag")
  (lb/update clog-2)
  (is (zero? (count (query "SELECT * FROM sampletable3"))))
  (lb/rollback-to-tag clog-2 "mytag")
  (is (thrown? SQLException
        (query "SELECT * FROM sampletable3")) "Table should not exist"))


(deftest test-rollback-to-tag
  (testing "rollback-to-tag"
    (lb-action
      rollback-to-tag-test)))


(defn rollback-to-date-test
  []
  (clb-setup)
  (lb/update clog-1)
  (lb/tag    "mytag")
  (lb/update clog-2)
  (is (zero? (count (query "SELECT * FROM sampletable3"))))
  (lb/rollback-to-date clog-2 (java.util.Date.))
  (is (zero? (count (query "SELECT * FROM sampletable3")))))


(deftest test-rollback-to-date
  (testing "rollback-to-date"
    (lb-action
      rollback-to-date-test)))


(defn rollback-by-count-test
  []
  (clb-setup)
  (lb/update clog-1)
  (lb/tag    "tag1")
  (lb/update clog-2)
  (lb/tag    "tag2")
  (let [tt (fn [f tables] ; test table
             (doseq [each tables]
               (is (zero? (count (query (format "SELECT * FROM %s" each))))
                 (format "Table %s should exist having no rows" each)))
             (f)
             (doseq [each tables]
               (is (thrown? SQLException
                     (query (format "SELECT * FROM %s" each)))
                 (format "Table %s should not exist" each))))]
    ;; rollback 1 changeset
    (tt #(lb/rollback-by-count clog-2 1) ["sampletable3"])
    ;; rollback 1 more changeset
    (tt #(lb/rollback-by-count clog-2 1) ["sample_table_1"
                                          "sample_table_2"])))


(deftest test-rollback-by-count
  (testing "rollback-by-count"
    (lb-action
      rollback-by-count-test)))


(defn generate-doc-test
  []
  (clb-setup)
  (lb/update clog-1)
  (lb/generate-doc clog-2 "target/dbdoc"))


(deftest test-generate-doc
  (testing "generate-doc"
    (lb-action generate-doc-test)
    (is (.exists (File. "target/dbdoc/index.html")))))


(defmacro with-readonly
  [& body]
  `(spec/with-dbspec (spec/assoc-readonly spec/*dbspec*)
     ~@body))


(deftest test-generate-sql
  (testing "generate-sql"
    (with-lb-action
      (doseq [[each msg] [[(fn [w]
                             (with-readonly
                               (lb/update clog-1 [] w)))
                           "Update Database Script"]
                          [(fn [w]
                             (with-readonly
                               (lb/update-by-count clog-2 1 [] w)))
                           "Update 1 Change-sets Database Script"]
                          [(fn [w]
                             (lb/update clog-1)
                             (lb/tag    "mytag")
                             (lb/update clog-2)
                             (with-readonly
                               (lb/rollback-to-tag clog-2 "mytag" [] w)))
                           "Rollback to 'mytag' Script"]
                          [(fn [w]
                             (lb/update clog-1)
                             (lb/tag    "mytag")
                             (lb/update clog-2)
                             (with-readonly
                               (lb/rollback-to-date clog-2 (java.util.Date.) [] w)))
                           "Rollback to"]
                          [(fn [w]
                             (lb/update clog-1)
                             (lb/tag    "tag1")
                             (lb/update clog-2)
                             (lb/tag    "tag2")
                             (with-readonly
                               (lb/rollback-by-count clog-2 1 [] w)))
                           "Rollback to 1 Change-sets Script"]]]
        (clb-setup)
        (let [^String script (mu/with-stringwriter w
                               (each w))]
          (println "^^^^^^^^" script "$$$$$$$$")
          (is (and (string? script)
                (mu/posnum? (.indexOf ^String script ^String msg)))))))))


(defn diff-test
  []
  (clb-setup)
  (lb/diff lb/*db-instance*))


(deftest test-diff
  (testing "diff"
           (lb-action diff-test)))


(defn test-ns-hook []
  ;; ===== ChangeSet =====
  (test-make-changeset)
  ;; ===== DatabaseChangeLog =====
  (test-make-changelog)
  (test-defchangelog)
  ;; ===== Actions =====
  (test-update)
  (test-update-idempotency)
  (test-update-by-count)
  (test-tag)
  (test-rollback-to-tag)
  (test-rollback-to-date)
  (test-rollback-by-count)
  (test-generate-doc)
  (test-generate-sql)
  (test-diff))
