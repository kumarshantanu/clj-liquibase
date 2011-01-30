(ns org.bituf.clj-liquibase.test-change
  (:import
    (java.util Date)
    (liquibase.change.core
      ;; Structural Refactorings
      AddColumnChange           RenameColumnChange         ModifyDataTypeChange
      DropColumnChange          AlterSequenceChange        CreateTableChange
      RenameTableChange         DropTableChange            CreateViewChange
      RenameViewChange          DropViewChange             MergeColumnChange
      CreateProcedureChange
      ;; Data Quality Refactorings
      AddLookupTableChange      AddNotNullConstraintChange DropNotNullConstraintChange
      AddUniqueConstraintChange DropUniqueConstraintChange CreateSequenceChange
      DropSequenceChange        AddAutoIncrementChange     AddDefaultValueChange
      DropDefaultValueChange
      ;; Referential Integrity Refactorings
      AddForeignKeyConstraintChange DropForeignKeyConstraintChange
      AddPrimaryKeyChange           DropPrimaryKeyChange
      ;; Non-Refactoring Transformations
      InsertDataChange LoadDataChange    LoadUpdateDataChange UpdateDataChange
      DeleteDataChange TagDatabaseChange StopChange
      ;; Architectural Refactorings
      CreateIndexChange DropIndexChange)
    (liquibase.statement DatabaseFunction)
    (liquibase.util      ISODateFormat))
  (:require
    [org.bituf.clj-liquibase.change :as change])
  (:use org.bituf.test-util)
  (:use clojure.test))


(defn test-change
  "Assert Change instance by running 'pch' partial function through arg colls.
  Each arg coll is a list where the first element is test description (string)."
  [^Class ch-class pch & argcolls]
  (let [ch? #(instance? ch-class %)]
    (doseq [each argcolls]
      (let [desc (first each)
            args (rest each)]
        (is (ch? (apply pch args)) desc)))))


(def min-args ["Minimum args"])
(def with-schema-name  ["With :schema-name argument" :schema-name :sample])
(def with-schema       ["With :schema argument"      :schema      :sample])
(def with-where-clause ["With :where-clause value" :where-clause "grade < 5"])
(def with-where        ["With :where value"        :where        "code NOT NULL"])

(def long-names  "Optional args with long-names")
(def short-names "Optional args with short-names")


;; ----- Structural Refactorings -----


(deftest test-add-columns
  (testing "add-columns with required args"
    (let [ch  change/add-columns
          pch (partial ch :table [[:emp :char]])]
      (is (thrown? IllegalArgumentException (ch :table []))        "Empty columndef-list")
      (is (thrown? IllegalArgumentException (ch :table [[:name]])) "Insufficient arguments")
      (test-change AddColumnChange (partial ch :table [[:name :type]])
        min-args)
      (test-change AddColumnChange
        (partial ch :table [[:name :type][:name2 :type2]]) ["Multiple columndefs"])
      (test-change AddColumnChange
        (partial ch :table [[:name :type :default true]]) ["With optional argument"])
      (test-change AddColumnChange pch with-schema-name with-schema))))


(deftest test-rename-column
  (testing "rename-column"
    (test-change RenameColumnChange
      (partial change/rename-column :table :oldname :newname)
      min-args
      with-schema-name
      with-schema
      ["With :column-data-type argument" :column-data-type :char]
      ["With :data-type argument"        :data-type        :char])))


(deftest test-modify-column
  (testing "modify-column"
    (test-change ModifyDataTypeChange
      (partial change/modify-column :table :colname [:float 10 7])
      min-args
      with-schema-name
      with-schema)))


(deftest test-drop-column
  (testing "drop-column"
    (test-change DropColumnChange
      (partial change/drop-column :table :colname)
      min-args
      with-schema-name
      with-schema)))


(deftest test-alter-sequence
  (testing "alter-sequence"
    (test-change AlterSequenceChange
      (partial change/alter-sequence :seq-name 500)
      min-args
      with-schema-name
      with-schema
      [long-names
       :max-value   6779    ; number or string
       :min-value   2000    ; number or string
       :ordered     true    ; Boolean
       ]
      [short-names
       :max    5697     ; number or string
       :min    "56"     ; number or string
       :ord    true     ; Boolean
       ])))


(deftest test-create-table
  (testing "create-table"
    (let [ct  change/create-table
          pch (partial ct :emp [[:c1 :int] [:c2 [:varchar 30]]])]
      (is (thrown? IllegalArgumentException
            (ct :tbl-name []))        "No column-defs")
      (test-change CreateTableChange
        (partial ct :tbl-name [[:c1 :int]]) min-args)
      (test-change CreateTableChange pch
        with-schema-name
        with-schema
        [long-names
         :table-space :hello  ; string/Keyword - s.t. clj-to-dbident
         :remarks     "Later" ; string
         ]
        [short-names
         :tspace  :go-air  ; string/Keyword - s.t. clj-to-dbident
         :remarks "Notnow" ; string
         ]))))


(deftest test-rename-table
  (testing "rename-table"
    (test-change RenameTableChange
      (partial change/rename-table :old-name :new-name)
      min-args
      with-schema-name
      with-schema)))


(deftest test-drop-table
  (testing "drop-table"
    (test-change DropTableChange
      (partial change/drop-table :table-name)
      min-args
      with-schema-name
      with-schema
      [long-names  :cascade-constraints false]
      [short-names :cascade             true])))


(deftest test-create-view
  (testing "create-view"
    (test-change CreateViewChange
      (partial change/create-view :view-name "SELECT * FROM emp;")
      min-args
      with-schema-name
      with-schema
      [long-names  :replace-if-exists false]
      [short-names :replace           true])))


(deftest test-rename-view
  (testing "rename-view"
    (test-change RenameViewChange
      (partial change/rename-view :old-name :new-name)
      min-args
      with-schema-name
      with-schema)))


(deftest test-drop-view
  (testing "drop-view"
    (test-change DropViewChange
      (partial change/drop-view :view-name)
      min-args
      with-schema-name
      with-schema)))


(deftest test-merge-columns
  (testing "merge-columns"
    (test-change MergeColumnChange
      (partial change/merge-columns 
        :table-name :column1-name "|" :column2-name
        :final-column-name [:char 100])
      min-args
      with-schema-name
      with-schema)))


(deftest test-create-stored-procedure
  (testing "create-stored-procedure"
    (test-change CreateProcedureChange
      (partial change/create-stored-procedure
        "CREATE OR REPLACE PROCEDURE testHello
        IS
        BEGIN
          DBMS_OUTPUT.PUT_LINE('Hello From The Database!');
        END;")
      min-args
      ["With :comments argument" :comments "foobar"])))


;; ----- Data Quality Refactorings -----


(deftest test-add-lookup-table
  (testing "add-lookup-table"
    (test-change AddLookupTableChange
      (partial change/add-lookup-table
        :existing-table-name :existing-column-name
        :new-table-name      :new-column-name
        :constraint-name)
      min-args
      [long-names
       :existing-table-schema-name :sample  ; String/Keyword - s.t. clj-to-dbident
       :new-table-schema-name      :another ; String/Keyword - s.t. clj-to-dbident
       :new-column-data-type       :int     ; String/vector - s.t. as-coltype
       ]
      [short-names
       :existing-schema :sample    ; String/Keyword - s.t. clj-to-dbident
       :new-schema      :another   ; String/Keyword - s.t. clj-to-dbident
       :new-data-type   [:char 10] ; String/vector - s.t. as-coltype
       ])))


(deftest test-add-not-null-constraint
  (testing "add-not-null-constraint"
    (test-change AddNotNullConstraintChange
      (partial change/add-not-null-constraint
        :table-name :column-name [:char 100])
      min-args
      with-schema-name
      with-schema
      [long-names  :default-null-value "default"]
      [short-names :default            "default"])))


(deftest test-drop-not-null-constraint
  (testing "drop-not-null-constraint"
    (test-change DropNotNullConstraintChange
      (partial change/drop-not-null-constraint :table-name :column-name)
      min-args
      with-schema-name
      with-schema
      [long-names  :column-data-type [:char 100]]
      [short-names :data-type        [:char 100]])))


(deftest test-add-unique-constraint
  (testing "add-unique-constraint"
    (test-change AddUniqueConstraintChange
      (partial change/add-unique-constraint
        :table-name [:col1 :col2] :constraint-name)
      min-args
      with-schema-name
      with-schema
      [long-names
       :table-space        :tspace ; String/Keyword - s.t. clj-to-dbident
       :deferrable         false   ; Boolean
       :initially-deferred true    ; Boolean
       :disabled           false   ; Boolean
       ]
      [short-names
       :tspace   :tspace ; String/Keyword - s.t. clj-to-dbident
       :defer    false   ; Boolean
       :idefer   true    ; Boolean
       :disabled true    ; Boolean
       ])))


(deftest test-drop-unique-constraint
  (testing "drop-unique-constraint"
    (test-change DropUniqueConstraintChange
      (partial change/drop-unique-constraint :table-name :constraint-name)
      min-args
      with-schema-name
      with-schema)))


(deftest test-create-sequence
  (testing "create-sequence"
    (test-change CreateSequenceChange
      (partial change/create-sequence :sequence-name)
      min-args
      with-schema-name
      with-schema
      [long-names
       :start-value  1000 ; BigInteger
       :increment-by 1    ; BigInteger
       :max-value    9999 ; BigInteger
       :min-value    1000 ; BigInteger
       :ordered      true ; Boolean
       :cycle        true ; Boolean
       ]
      [short-names
       :start 1000 ; BigInteger
       :incby 1    ; BigInteger
       :max   9999 ; BigInteger
       :min   1000 ; BigInteger
       :ord   true ; Boolean
       :cyc   true ; Boolean
       ])))


(deftest test-drop-sequence
  (testing "drop-sequence"
    (test-change DropSequenceChange
      (partial change/drop-sequence :sequence-name)
      min-args
      with-schema-name
      with-schema)))


(deftest test-add-auto-increment
  (testing "add-auto-increment"
    (test-change AddAutoIncrementChange
      (partial change/add-auto-increment
        :table-name :column-name :column-data-type)
      min-args
      with-schema-name
      with-schema)))


(deftest test-add-default-value
  (testing "add-default-value"
    (doseq [each ["default-value" 100 (Date.) true (change/dbfn "NOW")]]
      (test-change AddDefaultValueChange
        (partial change/add-default-value :table-name :column-name each)
        min-args
        with-schema-name
        with-schema))))


(deftest test-drop-default-value
  (testing "drop-default-value"
    (test-change DropDefaultValueChange
      (partial change/drop-default-value :table-name :column-name)
      min-args
      with-schema-name
      with-schema
      ["With :column-data-type value" :column-data-type [:char 100]]
      ["With :data-type value" :data-type [:float 12 5]])))


;; ----- Referential Integrity Refactorings -----


(deftest test-add-foreign-key-constraint
  (testing "add-foreign-key-constraint"
    (test-change AddForeignKeyConstraintChange
      (partial change/add-foreign-key-constraint
        :constraint-name :base-table-name [:base-column1 :base-column2]
        :referenced-table-name [:referenced-column1 :referenced-column2])
      min-args
      [long-names
       :base-table-schema-name       :base  ; String
       :referenced-table-schema-name :ref   ; String
       :deferrable                   false  ; Boolean
       :initially-deferred           true   ; Boolean
       :on-delete                    "none" ; String
       :on-update                    "none" ; String
       ]
      [short-names
       :base-schema :base  ; String
       :ref-schema  :ref   ; String
       :defer       false  ; Boolean
       :idefer      true   ; Boolean
       :ondel       "none" ; String
       :onupd       "none" ; String
       ])))


(deftest test-drop-foreign-key-constraint
  (testing "drop-foreign-key-constraint"
    (test-change DropForeignKeyConstraintChange
      (partial change/drop-foreign-key-constraint :constraint-name :base-table)
      min-args
      with-schema-name
      with-schema)))


(deftest test-add-primary-key
  (testing "add-primary-key"
    (test-change AddPrimaryKeyChange
      (partial change/add-primary-key :table-name [:col1 :col2] :constraint-name)
      min-args
      with-schema-name
      with-schema
      ["With :table-space value" :table-space :space]
      ["With :tspace value"      :tspace      :space])))


(deftest test-drop-primary-key
  (testing "drop-primary-key"
    (test-change DropPrimaryKeyChange
      (partial change/drop-primary-key :table-name)
      min-args
      with-schema-name
      with-schema
      ["With :constraint-name value" :constraint-name :constraint]
      ["With :constr value"          :constr          :constraint])))


;; ----- Non-Refactoring Transformations -----


(deftest test-insert-data
  (testing "insert-data"
    (test-change InsertDataChange
      (partial change/insert-data :table-name
        {:name "Abraham" :age 30 :male true :joined (Date.) :now (change/dbfn "NOW")})
      min-args
      with-schema-name
      with-schema)))


(deftest test-load-data
  (testing "load-data"
    (test-change LoadDataChange
      (partial change/load-data :table-name "filename.csv"
        (array-map :col1 :string :col2 :numeric :col3 :date :col4 :boolean))
      min-args
      with-schema-name
      with-schema
      ["With :encoding value" :encoding "UTF-16"]
      ["With :enc value"      :enc      "UTF-8"])))


(deftest test-load-update-data
  (testing "load-update-data"
    (test-change LoadUpdateDataChange
      (partial change/load-update-data :table-name "filename.csv"
        [:pk-col1 :pk-col2]
        (array-map :col1 :string :col2 :numeric :col3 :date :col4 :boolean))
      min-args
      with-schema-name
      with-schema
      ["With :encoding value" :encoding "UTF-16"]
      ["With :enc value"      :enc      "UTF-8"])))


(deftest test-update-data
  (testing "update-data"
    (test-change UpdateDataChange
      (partial change/update-data :table-name
        {:name "Abraham" :age 30 :male true :joined (Date.) :now (change/dbfn "NOW")})
      min-args
      with-schema-name
      with-schema
      with-where-clause
      with-where)))


(deftest test-delete-data
  (testing "delete-data"
    (test-change DeleteDataChange
      (partial change/delete-data :table-name)
      min-args
      with-schema-name
      with-schema
      with-where-clause
      with-where)))


(deftest test-tag-database
  (testing "tag-database"
    (test-change TagDatabaseChange
      (partial change/tag-database :tag)
      min-args)))


(deftest test-stop
  (testing "stop"
    (test-change StopChange change/stop min-args)
    (test-change StopChange (partial change/stop "Some message") min-args)))


;; ----- Architectural Refactorings -----


(deftest test-create-index
  (testing "create-index"
    (test-change CreateIndexChange
      (partial change/create-index :table-name [:col1 :col2])
      min-args
      with-schema-name
      with-schema
      [long-names
       :index-name   "fooindex" ; String
       :unique       true       ; Boolean
       :table-space  "space"    ; String
       ]
      [short-names
       :index  "fooindex" ; String
       :uniq   false      ; Boolean
       :tspace "space"    ; String
       ])))


(deftest test-drop-index
  (testing "drop-index"
    (test-change DropIndexChange
      (partial change/drop-index :index-name :table-name)
      min-args
      with-schema-name
      with-schema)))


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
