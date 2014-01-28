(ns clj-liquibase.change
  "Clojure wrappers for liquibase.change.Change implementations.
  See also:
    http://www.liquibase.org/documentation/changes/home (Available Database Refactorings)"
  (:require
    [clj-jdbcutil.core      :as sp]
    [clj-miscutil.core      :as mu]
    [clj-liquibase.internal :as in])
  (:import
    (java.math BigInteger)
    (java.util List)
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
      CreateIndexChange DropIndexChange
      ;; Custom Refactorings
      RawSQLChange SQLFileChange)
    (liquibase.statement DatabaseFunction)
    (liquibase.util      ISODateFormat)))


;; ===== schema =====


(def ^{:doc "Fallback schema-name - used only if not nil"
       :dynamic true} *schema* nil)


;; ===== Utility functions =====


(defn ^DatabaseFunction dbfn
  [^String value]
  (DatabaseFunction. value))


(defn ^java.util.Date iso-date
  "Parse date from ISO-date-format string."
  [^String date-str]
  (.parse (ISODateFormat.) date-str))


;; ===== Database refactorings =====

;; Complete list here: http://www.liquibase.org/documentation/changes/home

;; ----- Structural Refactorings -----

;; Add Column

(defn ^AddColumnChange add-columns
  "Return a Change instance that adds columns to an existing table
  (AddColumnChange).
  See also:
    http://www.liquibase.org/documentation/changes/add_column
    http://www.liquibase.org/documentation/column"
  [table-name ^List columns
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name  schema  ; String/Keyword - subject to db-iden
             ] :as opt}] {:post [(instance? AddColumnChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (coll? columns))
                                 (mu/verify-arg (mu/not-empty? columns))]}
  (let [change (AddColumnChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)]
    (.setTableName change (sp/db-iden table-name))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    (doseq [each columns]
      (.addColumn change (apply in/as-column-config each)))
    change))


;; Rename Column

(defn ^RenameColumnChange rename-column
  "Return a Change instance that renames a column in an existing table
  (RenameColumnChange).
  See also:
    http://www.liquibase.org/documentation/changes/rename_column"
  [table-name old-column-name new-column-name
   & {:keys [catalog-name     catalog   ; String/Keyword - subject to db-iden
             schema-name      schema    ; String/Keyword - subject to db-iden
             column-data-type data-type ; String/Keyword - subject to db-iden
             ] :as opt}] {:post [(instance? RenameColumnChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name     :catalog
                                                  :schema-name      :schema
                                                  :column-data-type :data-type} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (mu/not-nil? old-column-name))
                                 (mu/verify-arg (mu/not-nil? new-column-name))]}
  (let [change  (RenameColumnChange.)
        c-name (or catalog-name      catalog)
        s-name  (or schema-name      schema *schema*)
        cd-type (or column-data-type data-type)]
    (doto change
      (.setTableName     (sp/db-iden table-name))
      (.setOldColumnName (sp/db-iden old-column-name))
      (.setNewColumnName (sp/db-iden new-column-name)))
    (if c-name (.setCatalogName     change (sp/db-iden c-name)))
    (if s-name (.setSchemaName      change (sp/db-iden s-name)))
    (if cd-type (.setColumnDataType change (apply in/as-coltype
                                                  (mu/as-vector cd-type))))
    change))


;; Modify Column

(defn ^ModifyDataTypeChange modify-column
  "Return a Change instance that modifies data type of a column in an existing
  table (ModifyDataTypeChange).
  See also:
    http://www.liquibase.org/documentation/changes/modify_column"
  [table-name column-name new-data-type
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name  schema  ; String/Keyword - subject to db-iden
             ] :as opt}] {:post [(instance? ModifyDataTypeChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (mu/not-nil? column-name))
                                 (mu/verify-arg (mu/not-nil? new-data-type))]}
  (let [change (ModifyDataTypeChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)]
    (doto change
      (.setTableName   (sp/db-iden table-name))
      (.setColumnName  (sp/db-iden column-name))
      (.setNewDataType (apply in/as-coltype (mu/as-vector new-data-type))))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    change))


;; Drop Column

(defn ^DropColumnChange drop-column
  "Return Change instance that drops a column from an existing table
  (DropColumnChange).
  See also:
    http://www.liquibase.org/documentation/changes/drop_column"
  [table-name column-name
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name  schema  ; String/Keyword - subject to db-iden
             ] :as opt}] {:post [(instance? DropColumnChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (mu/not-nil? column-name))]}
  (let [change (DropColumnChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)]
    (doto change
      (.setTableName  (sp/db-iden table-name))
      (.setColumnName (sp/db-iden column-name)))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    change))


;; Alter Sequence

(defn ^AlterSequenceChange alter-sequence
  "Return a Change instance that alters a seuqence (AlterSequenceChange).
  See also:
    http://www.liquibase.org/documentation/changes/alter_sequence"
  [seq-name increment-by
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name  schema  ; string/Keyword - subject to db-iden
             max-value    max     ; number or string
             min-value    min     ; number or string
             ordered      ord     ; Boolean
             ] :as opt}] {:post [(instance? AlterSequenceChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema
                                                  :max-value    :max
                                                  :min-value    :min
                                                  :ordered      :ord} opt)
                                 (mu/verify-arg (mu/not-nil? seq-name))
                                 (mu/verify-arg (mu/not-nil? increment-by))]}
  (let [change (AlterSequenceChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)
        max-v  (or max-value    max)
        min-v  (or min-value    min)
        ord-v  (or ordered      ord)]
    (doto change
      (.setSequenceName (sp/db-iden seq-name))
      (.setIncrementBy (BigInteger. (str increment-by))))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    (if max-v  (.setMaxValue    change (BigInteger. (str max-v))))
    (if min-v  (.setMinValue    change (BigInteger. (str min-v))))
    (if ord-v  (.setOrdered     change ord-v))
    change))


;; Create Table

(defn ^CreateTableChange create-table
  "Return a Change instance that creates a table (CreateTableChange).
  See also:
    http://www.liquibase.org/documentation/changes/create_table
    http://www.liquibase.org/documentation/column"
  [table-name ^List columns
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name  schema  ; String/Keyword - subject to db-iden
             table-space  tspace  ; String/Keyword - subject to db-iden
             remarks] :as opt}] {:post [(instance? CreateTableChange %)]
                                 :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                         :schema-name  :schema
                                                         :table-space  :tspace
                                                         :remarks} opt)
                                        (mu/verify-arg (mu/not-nil?   table-name))
                                        (mu/verify-arg (coll?         columns))
                                        (mu/verify-arg (mu/not-empty? columns))]}
  (let [change  (CreateTableChange.)
        c-name  (or catalog-name catalog)
        s-name  (or schema-name  schema *schema*)
        t-space (or table-space  tspace)]
    (.setTableName change (sp/db-iden table-name))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    (if t-space (.setTablespace change (sp/db-iden t-space)))
    (if remarks (.setRemarks    change remarks))
    (doseq [each columns]
      (.addColumn change (apply in/as-column-config each)))
    change))


(defn ^CreateTableChange create-table-withid
  "Same as `create-table`, but includes an additional auto-generated primary
  key column. The primary key column is named <table-name>_id unless overriden
  with optional argument `:idcol` and ID column-name as the value. E.g. if the
  table name is :sample or \"sample\", then primary key will be \"sample_id\".
  See also:
    create-table"
  [table-name columns & args]
  (let [{:keys [idcol] :as opt} args
        idcol-name (if idcol
                     (sp/db-iden idcol)
                     (str (sp/db-iden table-name) "_id"))
        idcol-spec [idcol-name :BIGINT :null false :pk true :autoinc true]
        ct-varargs (reduce into [] (dissoc opt :idcol))]
    (apply create-table table-name (cons idcol-spec columns) ct-varargs)))


;; Rename Table

(defn ^RenameTableChange rename-table
  "Return a Change instance that renames a table (RenameTableChange).
  See also:
    http://www.liquibase.org/documentation/changes/rename_table"
  [old-table-name new-table-name
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name  schema  ; String/Keyword - subject to db-iden
             ] :as opt}] {:post [(instance? RenameTableChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema} opt)
                                 (mu/verify-arg (mu/not-nil? old-table-name))
                                 (mu/verify-arg (mu/not-nil? new-table-name))]}
  (let [change (RenameTableChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)]
    (doto change
      (.setOldTableName (sp/db-iden old-table-name))
      (.setNewTableName (sp/db-iden new-table-name)))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    change))


;; Drop Table

(defn ^DropTableChange drop-table
  "Return a Change instance that drops a table (DropTableChange).
  See also:
    http://www.liquibase.org/documentation/changes/drop_table"
  [table-name
   & {:keys [catalog-name        catalog ; String/Keyword - subject to db-iden
             schema-name         schema  ; String/Keyword - subject to db-iden
             cascade-constraints cascade ; Boolean
             ] :as opt}] {:post [(instance? DropTableChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name        :catalog
                                                  :schema-name         :schema
                                                  :cascade-constraints :cascade} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))]}
  (let [change (DropTableChange.)
        c-name (or catalog-name        catalog)
        s-name (or schema-name         schema   *schema*)
        casc-c (or cascade-constraints cascade)]
    (.setTableName change (sp/db-iden table-name))
    (if c-name (.setCatalogName        change (sp/db-iden c-name)))
    (if s-name (.setSchemaName         change (sp/db-iden s-name)))
    (if casc-c (.setCascadeConstraints change casc-c))
    change))


;; Create View

(defn ^CreateViewChange create-view
  "Return a Change instance that creates a view (CreateViewChange).
  See also:
    http://www.liquibase.org/documentation/changes/create_view"
  [view-name ^String select-query
   & {:keys [catalog-name      catalog ; String/Keyword - subject to db-iden
             schema-name       schema  ; String/Keyword - subject to db-iden
             replace-if-exists replace ; Boolean
             ] :as opt}] {:post [(instance? CreateViewChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name      :catalog
                                                  :schema-name       :schema
                                                  :replace-if-exists :replace} opt)
                                 (mu/verify-arg (mu/not-nil? view-name))
                                 (mu/verify-arg (string?     select-query))]}
  (let [change (CreateViewChange.)
        c-name (or catalog-name      catalog)
        s-name (or schema-name       schema  *schema*)
        repl-v (or replace-if-exists replace)]
    (doto change
      (.setViewName    (sp/db-iden view-name))
      (.setSelectQuery select-query))
    (if c-name (.setCatalogName     change (sp/db-iden c-name)))
    (if s-name (.setSchemaName      change (sp/db-iden s-name)))
    (if repl-v (.setReplaceIfExists change repl-v))
    change))


;; Rename View

(defn ^RenameViewChange rename-view
  "Return a Change instance that renames a view (RenameViewChange).
  See also:
    http://www.liquibase.org/documentation/changes/rename_view"
  [old-view-name new-view-name
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name  schema  ; String/Keyword - subject to db-iden
             ] :as opt}] {:post [(instance? RenameViewChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema} opt)
                                 (mu/verify-arg (mu/not-nil? old-view-name))
                                 (mu/verify-arg (mu/not-nil? new-view-name))]}
  (let [change (RenameViewChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)]
    (doto change
      (.setOldViewName (sp/db-iden old-view-name))
      (.setNewViewName (sp/db-iden new-view-name)))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    change))


;; Drop View

(defn ^DropViewChange drop-view
  "Return a Change instance that drops a view (DropViewChange).
  See also:
    http://www.liquibase.org/documentation/changes/drop_view"
  [view-name
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name  schema  ; String/Keyword - subject to db-iden
             ] :as opt}] {:post [(instance? DropViewChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema} opt)
                                 (mu/verify-arg (mu/not-nil?   view-name))]}
  (let [change (DropViewChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)]
    (.setViewName change (sp/db-iden view-name))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    change))


;; Merge Columns

(defn ^MergeColumnChange merge-columns
  "Return a Change instance that merges columns (MergeColumnChange).
  See also:
    http://www.liquibase.org/documentation/changes/merge_columns"
  [table-name column1-name ^String join-string
   column2-name final-column-name final-column-type
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name  schema  ; String/Keyword - subject to db-iden
             ] :as opt}] {:post [(instance? MergeColumnChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (mu/not-nil? column1-name))
                                 (mu/verify-arg (string?     join-string))
                                 (mu/verify-arg (mu/not-nil? column2-name))
                                 (mu/verify-arg (mu/not-nil? final-column-name))
                                 (mu/verify-arg (mu/not-nil? final-column-type))]}
  (let [change (MergeColumnChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)]
    (doto change
      (.setTableName       (sp/db-iden table-name))
      (.setColumn1Name     (sp/db-iden column1-name))
      (.setJoinString      join-string)
      (.setColumn2Name     (sp/db-iden column2-name))
      (.setFinalColumnName (sp/db-iden final-column-name))
      (.setFinalColumnType (apply in/as-coltype
                                  (mu/as-vector final-column-type))))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    change))


;; Create Stored Procedure

(defn ^CreateProcedureChange create-stored-procedure
  "Return a Change instance that creates a stored procedure
  (CreateProcedureChange).
  See also:
    http://www.liquibase.org/documentation/changes/create_stored_procedure"
  [^String procedure-body
   & {:keys [comments  ; String
             ] :as opt}] {:post [(instance? CreateProcedureChange %)]
                          :pre  [(mu/verify-opt #{:comments} opt)
                                 (mu/verify-arg (string? procedure-body))]}
  (let [change (CreateProcedureChange.)]
    (.setProcedureBody change procedure-body)
    (if comments (.setComments change comments))
    change))


;; ----- Data Quality Refactorings -----

;; Add Lookup Table

(defn ^AddLookupTableChange add-lookup-table
  "Return a Change instance that adds a lookup table (AddLookupTableChange).
  See also:
    http://www.liquibase.org/documentation/changes/add_lookup_table"
  [existing-table-name existing-column-name
   new-table-name      new-column-name
   constraint-name
   & {:keys [existing-table-schema-name existing-schema ; String/Keyword - subject to db-iden
             new-table-schema-name      new-schema      ; String/Keyword - subject to db-iden
             new-column-data-type       new-data-type   ; String/vector  - subject to as-coltype
             ] :as opt}] {:post [(instance? AddLookupTableChange %)]
                          :pre  [(mu/verify-opt #{:existing-table-schema-name :existing-schema
                                                  :new-table-schema-name      :new-schema
                                                  :new-column-data-type       :new-data-type} opt)
                                 (mu/verify-arg (mu/not-nil? existing-table-name))
                                 (mu/verify-arg (mu/not-nil? existing-column-name))
                                 (mu/verify-arg (mu/not-nil? new-table-name))
                                 (mu/verify-arg (mu/not-nil? new-column-name))
                                 (mu/verify-arg (mu/not-nil? constraint-name))]}
  (let [change  (AddLookupTableChange.)
        exs-name (or existing-table-schema-name existing-schema)
        nws-name (or new-table-schema-name      new-schema)
        nwd-type (or new-column-data-type       new-data-type)]
    (doto change
      (.setExistingTableName  (sp/db-iden existing-table-name))
      (.setExistingColumnName (sp/db-iden existing-column-name))
      (.setNewTableName       (sp/db-iden new-table-name))
      (.setNewColumnName      (sp/db-iden new-column-name))
      (.setConstraintName     (sp/db-iden constraint-name)))
    (if exs-name (.setExistingTableSchemaName change (sp/db-iden exs-name)))
    (if nws-name (.setNewTableSchemaName      change (sp/db-iden nws-name)))
    (if nwd-type (.setNewColumnDataType       change (apply in/as-coltype
                                                            (mu/as-vector nwd-type))))
    change))


;; Add Not-Null Constraint

(defn ^AddNotNullConstraintChange add-not-null-constraint
  "Return a Change instance that adds a NOT NULL constraint
  (AddNotNullConstraintChange).
  See also:
    http://www.liquibase.org/documentation/changes/add_not-null_constraint"
  [table-name column-name column-data-type
   & {:keys [catalog-name       catalog ; String/Keyword - subject to db-iden
             schema-name        schema  ; String/Keyword - subject to db-iden
             default-null-value default ; String
             ] :as opt}] {:post [(instance? AddNotNullConstraintChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name       :catalog
                                                  :schema-name        :schema
                                                  :default-null-value :default} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (mu/not-nil? column-name))
                                 (mu/verify-arg (mu/not-nil? column-data-type))]}
  (let [change (AddNotNullConstraintChange.)
        c-name (or catalog-name        catalog)
        s-name  (or schema-name        schema  *schema*)
        n-value (or default-null-value default)]
    (doto change
      (.setTableName      (sp/db-iden table-name))
      (.setColumnName     (sp/db-iden column-name))
      (.setColumnDataType (apply in/as-coltype
                                 (mu/as-vector column-data-type))))
    (if c-name (.setCatalogName       change (sp/db-iden c-name)))
    (if s-name (.setSchemaName        change (sp/db-iden s-name)))
    (if n-value (.setDefaultNullValue change n-value))
    change))


;; Remove/drop Not-Null Constraint

(defn ^DropNotNullConstraintChange drop-not-null-constraint
  "Return a Change instance that drops a NOT NULL constraint
  (DropNotNullConstraintChange).
  See also:
    http://www.liquibase.org/documentation/changes/remove_not-null_constraint"
  [table-name column-name
   & {:keys [catalog-name     catalog   ; String/Keyword - subject to db-iden
             schema-name      schema    ; String/Keyword - subject to db-iden
             column-data-type data-type ; String/vector - subject to as-coltype
             ] :as opt}] {:post [(instance? DropNotNullConstraintChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name     :catalog
                                                  :schema-name      :schema
                                                  :column-data-type :data-type} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (mu/not-nil? column-name))]}
  (let [change (DropNotNullConstraintChange.)
        c-name (or catalog-name     catalog)
        s-name (or schema-name      schema    *schema*)
        d-type (or column-data-type data-type)]
    (doto change
      (.setTableName  (sp/db-iden table-name))
      (.setColumnName (sp/db-iden column-name)))
    (if c-name (.setCatalogName    change (sp/db-iden c-name)))
    (if s-name (.setSchemaName     change (sp/db-iden s-name)))
    (if d-type (.setColumnDataType change (apply in/as-coltype
                                                 (mu/as-vector d-type))))
    change))


;; Add Unique Constraint

(defn ^AddUniqueConstraintChange add-unique-constraint
  "Return a Change instance that adds a UNIQUE constraint
  (AddUniqueConstraintChange).
  See also:
    http://www.liquibase.org/documentation/changes/add_unique_constraint"
  [table-name column-names constraint-name
   & {:keys [catalog-name       catalog ; String/Keyword - subject to db-iden
             schema-name        schema  ; String/Keyword - subject to db-iden
             table-space        tspace  ; String/Keyword - subject to db-iden
             deferrable         defer   ; Boolean
             initially-deferred idefer  ; Boolean
             disabled                   ; Boolean
             ] :as opt}] {:post [(instance? AddUniqueConstraintChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name       :catalog
                                                  :schema-name        :schema
                                                  :table-space        :tspace
                                                  :deferrable         :defer
                                                  :initially-deferred :idefer
                                                  :disabled} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (mu/not-nil? column-names))
                                 (mu/verify-arg (mu/not-nil? constraint-name))]}
  (let [change (AddUniqueConstraintChange.)
        c-name (or catalog-name       catalog)
        s-name (or schema-name        schema *schema*)
        t-name (or table-space        tspace)
        df-val (or deferrable         defer)
        id-val (or initially-deferred idefer)
        di-val disabled]
    (doto change
      (.setTableName      (sp/db-iden table-name))
      (.setColumnNames    (in/as-dbident-names column-names))
      (.setConstraintName (sp/db-iden constraint-name)))
    (if c-name (.setCatalogName       change (sp/db-iden c-name)))
    (if s-name (.setSchemaName        change (sp/db-iden s-name)))
    (if t-name (.setTablespace        change (sp/db-iden t-name)))
    (if df-val (.setDeferrable        change df-val))
    (if id-val (.setInitiallyDeferred change id-val))
    (if di-val (.setDisabled          change di-val))
    change))


;; Drop Unique Constraint

(defn ^DropUniqueConstraintChange drop-unique-constraint
  "Return a Change instance that drops a UNIQUE constraint
  (DropUniqueConstraintChange).
  See also:
    http://www.liquibase.org/documentation/changes/drop_unique_constraint"
  [table-name constraint-name
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name schema   ; String/Keyword - subject to db-iden
             ] :as opt}] {:post [(instance? DropUniqueConstraintChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (mu/not-nil? constraint-name))]}
  (let [change (DropUniqueConstraintChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)]
    (doto change
      (.setTableName      (sp/db-iden table-name))
      (.setConstraintName (sp/db-iden constraint-name)))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    change))


;; Create Sequence

(defn ^CreateSequenceChange create-sequence
  "Return a Change instance that creates a sequence (CreateSequenceChange).
  See also:
    http://www.liquibase.org/documentation/changes/create_sequence"
  [sequence-name
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name  schema  ; String/Keyword - subject to db-iden
             start-value  start   ; BigInteger
             increment-by incby   ; BigInteger
             max-value    max     ; BigInteger
             min-value    min     ; BigInteger
             ordered      ord     ; Boolean
             cycle        cyc     ; Boolean
             ] :as opt}] {:post [(instance? CreateSequenceChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema
                                                  :start-value  :start
                                                  :increment-by :incby
                                                  :max-value    :max
                                                  :min-value    :min
                                                  :ordered      :ord
                                                  :cycle        :cyc} opt)
                                 (mu/verify-arg (mu/not-nil? sequence-name))]}
  (let [change (CreateSequenceChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)
        str-v  (or start-value  start)
        inc-v  (or increment-by incby)
        max-v  (or max-value    max)
        min-v  (or min-value    min)
        ord-v  (or ordered      ord)
        cyc-v  (or cycle        cyc)]
    (.setSequenceName change (sp/db-iden sequence-name))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    (if str-v  (.setStartValue  change (BigInteger. (str str-v))))
    (if inc-v  (.setIncrementBy change (BigInteger. (str inc-v))))
    (if max-v  (.setMaxValue    change (BigInteger. (str max-v))))
    (if min-v  (.setMinValue    change (BigInteger. (str min-v))))
    (if ord-v  (.setOrdered     change ord-v))
    (if cyc-v  (.setCycle       change cyc-v))
    change))


;; Drop Sequence

(defn ^DropSequenceChange drop-sequence
  "Return a Change instance that drops a sequence (DropSequenceChange).
  See also:
    http://www.liquibase.org/documentation/changes/drop_sequence"
  [sequence-name
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name schema   ; String/Keyword - subject to db-iden
             ] :as opt}] {:post [(instance? DropSequenceChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema} opt)
                                 (mu/verify-arg (mu/not-nil? sequence-name))]}
  (let [change (DropSequenceChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)]
    (.setSequenceName change (sp/db-iden sequence-name))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    change))


;; Add Auto-Increment

(defn ^AddAutoIncrementChange add-auto-increment
  "Return a Change instance that converts an existing column to be an
  auto-increment column (AddAutoIncrementChange).
  See also:
    http://www.liquibase.org/documentation/changes/add_auto-increment"
  [table-name column-name column-data-type
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name schema   ; String/Keyword - subject to db-iden
             ] :as opt}] {:post [(instance? AddAutoIncrementChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (mu/not-nil? column-name))
                                 (mu/verify-arg (mu/not-nil? column-data-type))]}
  (let [change (AddAutoIncrementChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)]
    (doto change
      (.setTableName      (sp/db-iden table-name))
      (.setColumnName     (sp/db-iden column-name))
      (.setColumnDataType (apply in/as-coltype
                                 (mu/as-vector column-data-type))))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    change))


;; Add Default Value

(defn ^AddDefaultValueChange add-default-value
  "Return a Change instance that adds a default value to the database definition
  for the specified column (AddDefaultValueChange).
  See also:
    http://www.liquibase.org/documentation/changes/add_default_value"
  [table-name column-name default-value
   & {:keys [catalog-name     catalog   ; String/Keyword - subject to db-iden
             schema-name      schema    ; String/Keyword - subject to db-iden
             column-data-type data-type
             ] :as opt}] {:post [(instance? AddDefaultValueChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name     :catalog
                                                  :schema-name      :schema
                                                  :column-data-type :data-type} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (mu/not-nil? column-name))]}
  (let [change (AddDefaultValueChange.)
        c-name (or catalog-name     catalog)
        s-name (or schema-name      schema    *schema*)
        d-type (or column-data-type data-type)]
    (doto change
      (.setTableName  (sp/db-iden table-name))
      (.setColumnName (sp/db-iden column-name)))
    (in/add-default-value change default-value)
    (if c-name (.setCatalogName    change (sp/db-iden c-name)))
    (if s-name (.setSchemaName     change (sp/db-iden s-name)))
    (if d-type (.setColumnDataType change (apply in/as-coltype
                                                 (mu/as-vector d-type))))
    change))


;; Drop Default Value

(defn ^DropDefaultValueChange drop-default-value
  "Return a Change instance that removes a database default value for a column
  (DropDefaultValueChange).
  See also:
    http://www.liquibase.org/documentation/changes/drop_default_value"
  [table-name column-name
   & {:keys [catalog-name     catalog ; String/Keyword - subject to db-iden
             schema-name      schema  ; String/Keyword - subject to db-iden
             column-data-type data-type
             ] :as opt}] {:post [(instance? DropDefaultValueChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name     :catalog
                                                  :schema-name      :schema
                                                  :column-data-type :data-type} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (mu/not-nil? column-name))]}
  (let [change (DropDefaultValueChange.)
        c-name (or catalog-name     catalog)
        s-name (or schema-name      schema    *schema*)
        d-type (or column-data-type data-type)]
    (doto change
      (.setTableName  (sp/db-iden table-name))
      (.setColumnName (sp/db-iden column-name)))
    (if c-name (.setCatalogName    change (sp/db-iden c-name)))
    (if s-name (.setSchemaName     change (sp/db-iden s-name)))
    (if d-type (.setColumnDataType change (apply in/as-coltype
                                                 (mu/as-vector d-type))))
    change))


;; ----- Referential Integrity Refactorings -----

;; Add Foreign Key Constraint

(defn ^AddForeignKeyConstraintChange add-foreign-key-constraint
  "Return a Change instance that adds a foreign key constraint to an existing
  column (AddForeignKeyConstraintChange).
  See also:
    http://www.liquibase.org/documentation/changes/add_foreign_key_constraint"
  [constraint-name base-table-name base-column-names
   referenced-table-name referenced-column-names
   & {:keys [base-table-schema-name       base-schema ; String
             referenced-table-schema-name ref-schema  ; String
             deferrable                   defer  ; Boolean
             initially-deferred           idefer ; Boolean
             on-delete                    ondel  ; String
             on-update                    onupd  ; String
             ] :as opt}] {:post [(instance? AddForeignKeyConstraintChange %)]
                          :pre  [(mu/verify-opt #{:base-table-schema-name       :base-schema
                                                  :referenced-table-schema-name :ref-schema
                                                  :deferrable                   :defer
                                                  :initially-deferred           :idefer
                                                  :on-delete                    :ondel
                                                  :on-update                    :onupd} opt)
                                 (mu/verify-arg (mu/not-nil? constraint-name))
                                 (mu/verify-arg (mu/not-nil? base-table-name))
                                 (mu/verify-arg (mu/not-nil? base-column-names))
                                 (mu/verify-arg (mu/not-nil? referenced-table-name))
                                 (mu/verify-arg (mu/not-nil? referenced-column-names))]}
  (let [change (AddForeignKeyConstraintChange.)
        bs-name (or base-table-schema-name       base-schema)
        rs-name (or referenced-table-schema-name ref-schema)
        df-v    (or deferrable         defer)
        id-v    (or initially-deferred idefer)
        od-v    (or on-delete          ondel)
        ou-v    (or on-update          onupd)]
    (doto change
      (.setConstraintName        (sp/db-iden constraint-name))
      (.setBaseTableName         (sp/db-iden base-table-name))
      (.setBaseColumnNames       (in/as-dbident-names base-column-names))
      (.setReferencedTableName   (sp/db-iden referenced-table-name))
      (.setReferencedColumnNames (in/as-dbident-names referenced-column-names)))
    (if bs-name (.setBaseTableSchemaName       change (sp/db-iden bs-name)))
    (if rs-name (.setReferencedTableSchemaName change (sp/db-iden rs-name)))
    (if df-v    (.setDeferrable                change df-v))
    (if id-v    (.setInitiallyDeferred         change id-v))
    (if od-v    (.setOnDelete                  change ^String od-v))
    (if ou-v    (.setOnUpdate                  change ^String ou-v))
    change))


;; Drop Foreign Key Constraint

(defn ^DropForeignKeyConstraintChange drop-foreign-key-constraint
  "Return a Change instance that drops an existing foreign key
  (DropForeignKeyConstraintChange).
  See also:
    http://www.liquibase.org/documentation/changes/drop_foreign_key_constraint"
  [constraint-name base-table-name
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name schema   ; String/Keyword - subject to db-iden
             ] :as opt}] {:post [(instance? DropForeignKeyConstraintChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema} opt)
                                 (mu/verify-arg (mu/not-nil? constraint-name))
                                 (mu/verify-arg (mu/not-nil? base-table-name))]}
  (let [change (DropForeignKeyConstraintChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)]
    (doto change
      (.setConstraintName (sp/db-iden constraint-name))
      (.setBaseTableName  (sp/db-iden base-table-name)))
    (if c-name (.setBaseTableCatalogName change (sp/db-iden c-name)))
    (if s-name (.setBaseTableSchemaName  change (sp/db-iden s-name)))
    change))


;; Add Primary Key Constraint

(defn ^AddPrimaryKeyChange add-primary-key
  "Return a Change instance that adds creates a primary key out of an existing
  column or set of columns (AddPrimaryKeyChange).
  See also:
    http://www.liquibase.org/documentation/changes/add_primary_key_constraint"
  [table-name column-names constraint-name
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name schema   ; String/Keyword - subject to db-iden
             table-space tspace   ; String/Keyword - subject to db-iden
             ] :as opt}] {:post [(instance? AddPrimaryKeyChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema
                                                  :table-space  :tspace} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (mu/not-nil? column-names))
                                 (mu/verify-arg (mu/not-nil? constraint-name))]}
  (let [change (AddPrimaryKeyChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)
        t-name (or table-space  tspace)]
    (doto change
      (.setTableName      (sp/db-iden table-name))
      (.setColumnNames    (in/as-dbident-names column-names))
      (.setConstraintName (sp/db-iden constraint-name)))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    (if t-name (.setTablespace  change (sp/db-iden t-name)))
    change))


;; Drop Primary Key Constraint - DropPrimaryKeyChange

(defn ^DropPrimaryKeyChange drop-primary-key
  "Return a Change instance that drops an existing primary key
  (DropPrimaryKeyChange).
  See also:
    http://www.liquibase.org/documentation/changes/drop_primary_key_constraint"
  [table-name
   & {:keys [catalog-name catalog   ; String/Keyword - subject to db-iden
             schema-name     schema ; String/Keyword - subject to db-iden
             constraint-name constr ; String/Keyword - subject to db-iden
             ] :as opt}] {:post [(instance? DropPrimaryKeyChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name    :catalog
                                                  :schema-name     :schema
                                                  :constraint-name :constr} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))]}
  (let [change    (DropPrimaryKeyChange.)
        c-name    (or catalog-name    catalog)
        s-name    (or schema-name     schema *schema*)
        cons-name (or constraint-name constr)]
    (.setTableName change (sp/db-iden table-name))
    (if c-name (.setCatalogName       change (sp/db-iden c-name)))
    (if s-name (.setSchemaName        change (sp/db-iden s-name)))
    (if cons-name (.setConstraintName change (sp/db-iden cons-name)))
    change))


;; ----- Non-Refactoring Transformations -----

;; Insert Data - InsertDataChange

(defn ^InsertDataChange insert-data
  "Return a Change instance that inserts data into an existing table
  (InsertDataChange).
  See also:
    http://www.liquibase.org/documentation/changes/insert_data"
  [table-name column-value-map
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name  schema  ; String/Keyword - subject to db-iden
             ] :as opt}] {:post [(instance? InsertDataChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (map?        column-value-map))]}
  (let [change (InsertDataChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)
        cols-v (map (fn [[n v]] (in/new-column-value n v)) column-value-map)]
    (.setTableName change (sp/db-iden table-name))
    (doseq [each cols-v]
      (.addColumn change ^ColumnConfig each))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    change))


;; Load Data - LoadDataChange

(defn ^LoadDataChange load-data
  "Return a Change instance that Loads data from a CSV file into an existing
  table (LoadDataChange). A value of NULL in a cell will be converted to a
  database NULL rather than the string NULL.
  Arguments:
    table-name   (keyword/String) table name
    csv-filename (String) CSV file name
    columns-spec (map)    key => name, value => type (:string :numeric :date :boolean)
                 (coll)   list of lists, each list is a col spec
  Optional arguments:
    :schema   (keyword/String) schema name - defaults to the default schema
    :encoding (String)         encoding of the CSV file - defaults to UTF-8
  See also:
    http://www.liquibase.org/documentation/changes/load_data"
  [table-name ^String csv-filename columns-spec
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name  schema  ; String/Keyword - subject to db-iden
             encoding     enc     ; String
             ] :as opt}] {:post [(instance? LoadDataChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name :schema
                                                  :encoding    :enc} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (string?     csv-filename))
                                 (mu/verify-arg (coll?       columns-spec))]}
  (let [change (LoadDataChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)
        enc-v  (or encoding     enc)]
    (doto change
      (.setTableName (sp/db-iden table-name))
      (.setFile      csv-filename))
    (doseq [each columns-spec]
      (.addColumn change (apply in/load-data-column-config each)))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    (if enc-v  (.setEncoding    change (mu/as-string enc-v)))
    change))


;; Load Update Data - LoadUpdateDataChange

(defn ^LoadUpdateDataChange load-update-data
  "Return a Change instance that loads or updates data from a CSV file into an
  existing table (LoadUpdateDataChange). Differs from loadData by issuing a SQL
  batch that checks for the existence of a record. If found, the record is
  UPDATEd, else the record is INSERTed. Also, generates DELETE statements for a
  rollback. A value of NULL in a cell will be converted to a database NULL
  rather than the string NULL.
  Arguments:
    table-name       (keyword/String) table name
    csv-filename     (String) CSV file name
    primary-key-cols (String) Comma delimited list of the columns for the primary key
    columns-spec     (map)    key => name, value => type (:string :numeric :date :boolean)
                     (coll)   list of lists, each list is a col spec
  Optional arguments:
    :schema   (keyword/String) schema name - defaults to the default schema
    :encoding (String)         encoding of the CSV file - defaults to UTF-8
  See also:
    http://www.liquibase.org/documentation/changes/load_update_data"
  [table-name ^String csv-filename primary-key-cols columns-spec
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name schema   ; String/Keyword - subject to db-iden
             encoding    enc      ; String
             ] :as opt}] {:post [(instance? LoadUpdateDataChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name :schema
                                                  :encoding    :enc} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (string?     csv-filename))
                                 (mu/verify-arg (mu/not-nil? primary-key-cols))
                                 (mu/verify-arg (coll?       columns-spec))]}
  (let [change (LoadUpdateDataChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)
        enc-v  (or encoding    enc)]
    (doto change
      (.setTableName  (sp/db-iden table-name))
      (.setFile       csv-filename)
      (.setPrimaryKey (in/as-dbident-names primary-key-cols)))
    (doseq [each columns-spec]
      (.addColumn change (apply in/load-data-column-config each)))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    (if enc-v  (.setEncoding    change (mu/as-string enc-v)))
    change))


;; Update Data - UpdateDataChange

(defn ^UpdateDataChange update-data
  "Return a Change instance that updates data in an existing table
  (UpdateDataChange).
  See also:
    http://www.liquibase.org/documentation/changes/update_data"
  [table-name column-name-value-map
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name  schema  ; String/Keyword - subject to db-iden
             where-clause where   ; String
             ] :as opt}] {:post [(instance? UpdateDataChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema
                                                  :where-clause :where} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (map?        column-name-value-map))]}
  (let [change   (UpdateDataChange.)
        c-name   (or catalog-name catalog)
        s-name   (or schema-name  schema *schema*)
        w-clause (or where-clause where)]
    (.setTableName change (sp/db-iden table-name))
    (doseq [[n v] column-name-value-map]
      (.addColumn change (in/new-column-value n v)))
    (if c-name   (.setCatalogName change (sp/db-iden c-name)))
    (if s-name   (.setSchemaName  change (sp/db-iden s-name)))
    (if w-clause (.setWhereClause change w-clause))
    change))


;; Delete Data - DeleteDataChange

(defn ^DeleteDataChange delete-data
  "Return a Change instance that deletes data from an existing table
  (DeleteDataChange).
  See also:
    http://www.liquibase.org/documentation/changes/delete_data"
  [table-name
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name  schema  ; String/Keyword - subject to db-iden
             where-clause where   ; String
             ] :as opt}] {:post [(instance? DeleteDataChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema
                                                  :where-clause :where} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))]}
  (let [change   (DeleteDataChange.)
        c-name   (or catalog-name catalog)
        s-name   (or schema-name  schema *schema*)
        w-clause (or where-clause where)]
    (.setTableName change (sp/db-iden table-name))
    (if c-name   (.setCatalogName change (sp/db-iden c-name)))
    (if s-name   (.setSchemaName  change (sp/db-iden s-name)))
    (if w-clause (.setSchemaName  change w-clause))
    change))


;; Tag Database - TagDatabaseChange

(defn ^TagDatabaseChange tag-database
  "Return a Change instance that applies a tag to the database for future
  rollback (TagDatabaseChange).
  See also:
    http://www.liquibase.org/documentation/changes/tag_database"
  [tag] {:post [(instance? TagDatabaseChange %)]
         :pre  [(mu/verify-arg (mu/not-nil? tag))]}
  (let [change (TagDatabaseChange.)]
    (.setTag change (mu/as-string tag))
    change))


;; Stop - StopChange

(defn ^StopChange stop
  "Return a Change instance that stops LiquiBase execution with a message
  (StopChange). Mainly useful for debugging and stepping through a changelog.
  See also:
    http://www.liquibase.org/documentation/changes/stop"
  ([] {:post [(instance? StopChange %)]}
   (StopChange.))
  ([^String message] {:post [(instance? StopChange %)]
                      :pre  [(mu/verify-arg (string? message))]}
   (let [change (StopChange.)]
     (.setMessage change message)
     change)))


;; ----- Architectural Refactorings -----

;; Create Index - CreateIndexChange

(defn ^CreateIndexChange create-index
  "Return a Change instance that creates an index on an existing column or set
  of columns (CreateIndexChange).
  See also:
    http://www.liquibase.org/documentation/changes/create_index"
  [table-name ^List column-names
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name  schema  ; String/Keyword - subject to db-iden
             index-name   index   ; String/Keyword - subject to db-iden
             unique       uniq    ; Boolean
             table-space  tspace  ; String
             ] :as opt}] {:post [(instance? CreateIndexChange %)]
                          :pre  [(mu/verify-opt #{:catalog-name :catalog
                                                  :schema-name  :schema
                                                  :index-name   :index
                                                  :unique       :uniq
                                                  :table-space :tspace} opt)
                                 (mu/verify-arg (mu/not-nil? table-name))
                                 (mu/verify-arg (coll? column-names))]}
  (let [change  (CreateIndexChange.)
        c-name  (or catalog-name catalog)
        s-name  (or schema-name  schema *schema*)
        i-name  (or index-name   index)
        uniq-v  (or unique       uniq)
        t-space (or table-space  tspace)]
    (.setTableName change (sp/db-iden table-name))
    (doseq [each column-names]
      (.addColumn change (in/new-column-value each "")))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    (if i-name  (.setIndexName  change (sp/db-iden i-name)))
    (if uniq-v  (.setUnique     change uniq-v))
    (if t-space (.setTablespace change t-space))
    change))


;; Drop Index - DropIndexChange

(defn ^DropIndexChange drop-index
  "Return a Change instance that drops an existing index (DropIndexChange).
  See also:
    http://www.liquibase.org/documentation/changes/drop_index"
  [index-name table-name
   & {:keys [catalog-name catalog ; String/Keyword - subject to db-iden
             schema-name  schema  ; String/Keyword - subject to db-iden
             ] :as opt}] {:post [(instance? DropIndexChange %)]
                          :pre  [(mu/verify-opt #{:schema-name :schema} opt)
                                 (mu/verify-arg (mu/not-nil? index-name))
                                 (mu/verify-arg (mu/not-nil? table-name))]}
  (let [change (DropIndexChange.)
        c-name (or catalog-name catalog)
        s-name (or schema-name  schema *schema*)]
    (doto change
      (.setIndexName (sp/db-iden index-name))
      (.setTableName (sp/db-iden table-name)))
    (if c-name (.setCatalogName change (sp/db-iden c-name)))
    (if s-name (.setSchemaName  change (sp/db-iden s-name)))
    change))


;; ----- Custom Refactorings -----

;; Modifying Generated SQL
;; TODO


;; Custom SQL - RawSQLChange

(defn ^RawSQLChange sql
  "Return a Change instance that executes arbitrary SQL (RawSQLChange). May be
  useful when desired change types don't exist, or are buggy/inflexible.
  See also:
    http://www.liquibase.org/documentation/changes/sql"
  [sql
   & {:keys [comment
             dbms
             end-delimiter
             split-statements
             strip-comments
             ] :as opt}] {:post [(instance? RawSQLChange %)]
                          :pre  [(mu/verify-opt #{:comment
                                                  :dbms
                                                  :end-delimiter
                                                  :split-statements
                                                  :strip-comments} opt)
                                 (mu/verify-arg (mu/not-nil? sql))]}
  (let [change (RawSQLChange.)]
    (doto change
      (.setSql sql))
    (if comment          (.setComment         change comment))
    (if dbms             (.setDbms            change (if (or (coll? dbms) (seq? dbms))
                                                       (mu/comma-sep-str dbms)
                                                       dbms)))
    (if end-delimiter    (.setEndDelimiter    change end-delimiter))
    (if split-statements (.setSplitStatements change split-statements))
    (if strip-comments   (.setStripComments   change strip-comments))
    change))


;; Custom SQL File - SQLFileChange

(defn ^SQLFileChange sql-file
  "Return a Change instance that executes SQL after reading it from a file
  (SQLFileChange). Useful to integrate with legacy projects having SQL files for
  DDL, or to decouple DDL from the project in general.
  See also:
    http://www.liquibase.org/documentation/changes/sql_file"
  [sql-filepath
   & {:keys [dbms
             encoding
             end-delimiter
             split-statements
             strip-comments
             ] :as opt}] {:post (instance? SQLFileChange %)
                          :pre [(mu/verify-opt #{:dbms
                                                 :encoding
                                                 :end-delimiter
                                                 :split-statements
                                                 :strip-comments} opt)]}
  (let [change (SQLFileChange.)]
    (doto change
      (.setPath sql-filepath))
    (if dbms             (.setDbms         change (if (or (coll? dbms) (seq? dbms))
                                                    (mu/comma-sep-str dbms)
                                                    dbms)))
    (if encoding         (.setEncoding     change encoding))
    (if end-delimiter    (.setEndDelimiter change end-delimiter))
    (if split-statements (.setSplitStatements change split-statements))
    (if strip-comments   (.setStripComments   change strip-comments))
    change))


;; Custom Refactoring Class
;; TODO

;; Execute Shell Command ExecuteShellCommandChange.
;; TODO
