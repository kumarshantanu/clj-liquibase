(ns clj-liquibase.test-internal
  (:require
    [clj-liquibase.internal :as in]
    [clj-liquibase.change   :as ch])
  (:import
    (liquibase.structure.core Column)
    (liquibase.change         ColumnConfig ConstraintsConfig)
    (liquibase.change.core    LoadDataColumnConfig)
    (liquibase.statement      DatabaseFunction)
    (liquibase.util           ISODateFormat)
    (java.util Date))
  (:use clojure.test))


(deftest test-dbfn?
  (testing "dbfn?"
    (is (in/dbfn? (ch/dbfn "foo")))))


(deftest test-as-coltype
  (testing "as-coltype"
    (is (= "int"          (in/as-coltype :int)))
    (is (= "char(10)"     (in/as-coltype :char 10)))
    (is (= "float(17, 5)" (in/as-coltype :float 17 5)))))


(deftest test-if-nn
  (testing "if-nn"
    (is (= 20  (in/if-nn 10 20)))
    (is (= nil (in/if-nn nil 20)))))


(deftest test-as-column-config
  (testing "as-column-config"
    (let [cc  in/as-column-config
          cc? #(instance? ColumnConfig %)]
      (is (cc? (cc :colname :coltype)) "only required parameters")
      (is (cc? (cc :colname :char
                 :default-value          "foobar"  ; String/Number/Date/Boolean/DatabaseFunction
                 :auto-increment         true      ; Boolean
                 :remarks                "comment" ; String
                 ;; constraints
                 :nullable               false     ; Boolean
                 :primary-key            true      ; Boolean
                 :primary-key-name       "dummy"   ; String
                 :primary-key-tablespace "nonamex" ; String
                 :references             "nonamex" ; String
                 :unique                 false     ; Boolean
                 :unique-constraint-name "onlyid"  ; String
                 :check                  "check"   ; String
                 :delete-cascade         true      ; Boolean
                 :foreign-key-name       "keyname" ; String
                 :initially-deferred     false     ; Boolean
                 :deferrable             true      ; Boolean
                 )) "all optional parameters with fullname")
      (is (cc? (cc :colname :char
                 :default          "foobar"  ; String/Number/Date/Boolean/DatabaseFunction
                 :autoinc          true      ; Boolean
                 :remarks          "comment" ; String
                 ;; constraints
                 :null             false     ; Boolean
                 :pk               true      ; Boolean
                 :pkname           "dummy"   ; String - s.t. clj-to-dbident
                 :pktspace         "nonamex" ; String - s.t. clj-to-dbident
                 :refs             "nonamex" ; String
                 :uniq             false     ; Boolean
                 :ucname           "onlyid"  ; String - s.t. clj-to-dbident
                 :check            "check"   ; String
                 :dcascade         true      ; Boolean
                 :fkname           "keyname" ; String - s.t. clj-to-dbident
                 :idefer           false     ; Boolean
                 :defer            true      ; Boolean
                 )) "all optional parameters with shortnames")
      (is (cc? (cc :colname :date
                 :default (ch/dbfn "NOW"))) "Default value: DatabaseFunction")
      (is (cc? (cc :colname :int
                 :default 100)) "Default value: Number")
      (is (cc? (cc :colname [:tinyint 1]
                 :default false)) "Default value: Boolean")
      (is (cc? (cc :colname :date
                 :default (Date.))) "Default value: Date"))))


(defn test-ns-hook []
  (test-dbfn?)
  (test-as-coltype)
  (test-if-nn)
  (test-as-column-config))