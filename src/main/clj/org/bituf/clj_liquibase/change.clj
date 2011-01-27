(ns org.bituf.clj-liquibase.change
  "Clojure wrappers for liquibase.change.Change implementations.
  See also:
    http://www.liquibase.org/manual/home (Available Database Refactorings)"
  (:import
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
    [org.bituf.clj-dbspec   :as sp]
    [org.bituf.clj-miscutil :as mu]
    [org.bituf.clj-liquibase.internal :as in]))


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

;; Complete list here: http://www.liquibase.org/manual/home

;; ----- Structural Refactorings -----

;; Add Column

(defn ^AddColumnChange add-columns
  "Return a Change instance that adds columns to an existing table
  (AddColumnChange).
  See also:
    http://www.liquibase.org/manual/add_column
    http://www.liquibase.org/manual/column"
  [table-name columns
   & {:keys [schema-name schema ; String
             ]}]
  (let [change (AddColumnChange.)
        s-name (or schema-name schema *schema*)]
    (.setTableName change (sp/clj-to-dbident table-name))
    (if s-name (.setSchemaName change (sp/clj-to-dbident s-name)))
    (doseq [each columns]
      (.addColumn change (apply in/as-column-config each)))
    change))


;; Rename Column

(defn ^RenameColumnChange rename-column
  "Return a Change instance that renames a column in an existing table
  (RenameColumnChange).
  See also:
    http://www.liquibase.org/manual/rename_column"
  [table-name old-column-name new-column-name
   & {:keys [schema-name      schema ; String
             column-data-type cdtype ; String
             ]}]
  (let [change  (RenameColumnChange.)
        s-name  (or schema-name      schema *schema*)
        cd-type (or column-data-type cdtype)]
    (doto change
      (.setTableName     (sp/clj-to-dbident table-name))
      (.setOldColumnName (sp/clj-to-dbident old-column-name))
      (.setNewColumnName (sp/clj-to-dbident new-column-name)))
    (if s-name  (.setSchemaName     change (sp/clj-to-dbident s-name)))
    (if cd-type (.setColumnDataType change (in/as-coltype cd-type)))
    change))


;; Modify Column

(defn ^ModifyDataTypeChange modify-column
  "Return a Change instance that modifies data type of a column in an existing
  table (ModifyDataTypeChange).
  See also:
    http://www.liquibase.org/manual/modify_column"
  [table-name column-name new-data-type
   & {:keys [schema-name schema ; String
             ]}]
  (let [change (ModifyDataTypeChange.)
        s-name (or schema-name schema *schema*)]
    (doto change
      (.setTableName   (sp/clj-to-dbident table-name))
      (.setColumnName  (sp/clj-to-dbident column-name))
      (.setNewDataType (in/as-coltype new-data-type)))
    (if s-name (.setSchemaName change (sp/clj-to-dbident s-name)))
    change))


;; Drop Column

(defn ^DropColumnChange drop-column
  "Return Change instance that drops a column from an existing table
  (DropColumnChange).
  See also:
    http://www.liquibase.org/manual/drop_column"
  [table-name column-name
   & {:keys [schema-name schema ; String
             ]}]
  (let [change (DropColumnChange.)
        s-name (or schema-name schema *schema*)]
    (doto change
      (.setTableName  (sp/clj-to-dbident table-name))
      (.setColumnName (sp/clj-to-dbident column-name)))
    (if s-name (.setSchemaName change (sp/clj-to-dbident s-name)))
    change))


;; Alter Sequence

(defn ^AlterSequenceChange alter-sequence
  "Return a Change instance that alters a seuqence (AlterSequenceChange).
  See also:
    http://www.liquibase.org/manual/alter_sequence"
  [seq-name increment-by
   & {:keys [schema-name schema ; string
             max-value   max    ; number or string
             min-value   min    ; number of string
             ordered     ord    ; Boolean
             ]}]
  (let [change (AlterSequenceChange.)
        s-name (or schema-name schema *schema*)
        max-v  (or max-value   max)
        min-v  (or min-value   min)
        ord-v  (or ordered     ord)]
    (doto change
      (.setSequenceName (sp/clj-to-dbident seq-name))
      (.setIncrementBy (bigint increment-by)))
    (if s-name (.setSchemaName change (sp/clj-to-dbident s-name)))
    (if max-v  (.setMaxValue   change (bigint max-v)))
    (if min-v  (.setMinValue   change (bigint min-v)))
    (if ord-v  (.setOrdered    change ord-v))
    change))


;; Create Table

(defn ^CreateTableChange create-table
  "Return a Change instance that creates a table (CreateTableChange).
  See also:
    http://www.liquibase.org/manual/create_table
    http://www.liquibase.org/manual/column"
  [table-name columns
   & {:keys [schema-name schema ; String
             table-space tspace ; String
             remarks]}]
  (let [change  (CreateTableChange.)
        s-name  (or schema-name schema *schema*)
        t-space (or table-space tspace)]
    (.setTableName change (sp/clj-to-dbident table-name))
    (if s-name  (.setSchemaName change (sp/clj-to-dbident s-name)))
    (if t-space (.setTablespace change (sp/clj-to-dbident t-space)))
    (if remarks (.setRemarks    change remarks))
    (doseq [each columns]
      (.addColumn change (apply in/as-column-config each)))
    change))


;; Rename Table

(defn ^RenameTableChange rename-table
  "Return a Change instance that renames a table (RenameTableChange).
  See also:
    http://www.liquibase.org/manual/rename_table"
  [old-table-name new-table-name
   & {:keys [schema-name schema ; String
             ]}]
  (let [change (RenameTableChange.)
        s-name (or schema-name schema *schema*)]
    (doto change
      (.setOldTableName (sp/clj-to-dbident old-table-name))
      (.setNewTableName (sp/clj-to-dbident new-table-name)))
    (if s-name (.setSchemaName change (sp/clj-to-dbident s-name)))
    change))


;; Drop Table

(defn ^DropTableChange drop-table
  "Return a Change instance that drops a table ().
  See also:
    http://www.liquibase.org/manual/drop_table"
  [table-name
   & {:keys [schema-name         schema   ; String
             cascade-constraints casc-con ; Boolean
             ]}]
  (let [change (DropTableChange.)
        s-name (or schema-name         schema   *schema*)
        casc-c (or cascade-constraints casc-con)]
    (.setTableName change (sp/clj-to-dbident table-name))
    (if s-name (.setSchemaName         change (sp/clj-to-dbident s-name)))
    (if casc-c (.setCascadeConstraints change casc-c))
    change))


;; Create View

(defn ^CreateViewChange create-view
  "Return a Change instance that creates a view (CreateViewChange).
  See also:
    http://www.liquibase.org/manual/create_view"
  [view-name ^String select-query
   & {:keys [schema-name       schema  ; String
             replace-if-exists replace ; Boolean
             ]}]
  (let [change (CreateViewChange.)
        s-name (or schema-name       schema  *schema*)
        repl-v (or replace-if-exists replace)]
    (doto change
      (.setViewName    (sp/clj-to-dbident view-name))
      (.setSelectQuery select-query))
    (if s-name (.setSchemaName      change (sp/clj-to-dbident s-name)))
    (if repl-v (.setReplaceIfExists change repl-v))
    change))


;; Rename View

(defn ^RenameViewChange rename-view
  "Return a Change instance that renames a view (RenameViewChange).
  See also:
    http://www.liquibase.org/manual/rename_view"
  [old-view-name new-view-name
   & {:keys [schema-name schema  ; String
             ]}]
  (let [change (RenameViewChange.)
        s-name (or schema-name schema *schema*)]
    (doto change
      (.setOldViewName (sp/clj-to-dbident old-view-name))
      (.setNewViewName (sp/clj-to-dbident new-view-name)))
    (if s-name (.setSchemaName change (sp/clj-to-dbident s-name)))
    change))


;; Drop View

(defn ^DropViewChange drop-view
  "Return a Change instance that drops a view (DropViewChange).
  See also:
    http://www.liquibase.org/manual/drop_view"
  [view-name
   & {:keys [schema-name schema  ; String
             ]}]
  (let [change (DropViewChange.)
        s-name (or schema-name schema *schema*)]
    (.setViewName change (sp/clj-to-dbident view-name))
    (if s-name (.setSchemaName change (sp/clj-to-dbident s-name)))
    change))


;; Merge Columns

(defn ^MergeColumnChange merge-columns
  "Return a Change instance that merges columns (MergeColumnChange).
  See also:
    http://www.liquibase.org/manual/merge_columns"
  [table-name column1-name ^String join-string
   column2-name final-column-name final-column-type
   & {:keys [schema-name schema  ; String
             ]}]
  (let [change (MergeColumnChange.)
        s-name (or schema-name schema *schema*)]
    (doto change
      (.setTableName       (sp/clj-to-dbident table-name))
      (.setColumn1Name     (sp/clj-to-dbident column1-name))
      (.setJoinString      join-string)
      (.setColumn2Name     (sp/clj-to-dbident column2-name))
      (.setFinalColumnName (sp/clj-to-dbident final-column-name))
      (.setFinalColumnType (in/as-coltype final-column-type)))
    (if s-name (.setSchemaName change (sp/clj-to-dbident s-name)))
    change))


;; Create Stored Procedure

(defn ^CreateProcedureChange create-stored-procedure
  "Return a Change instance that creates a stored procedure
  (CreateProcedureChange).
  See also:
    http://www.liquibase.org/manual/create_stored_procedure"
  [^String procedure-body
   & {:keys [comments  ; String
             ]}]
  (let [change (CreateProcedureChange.)]
    (.setProcedureBody change procedure-body)
    (if comments (.setComments change comments))
    change))


;; ----- Data Quality Refactorings -----

;; Add Lookup Table

(defn ^AddLookupTableChange add-lookup-table
  "Return a Change instance that adds a lookup table (AddLookupTableChange).
  See also:
    http://www.liquibase.org/manual/add_lookup_table"
  [existing-table-name existing-column-name
   new-table-name      new-column-name
   constraint-name
   & {:keys [existing-table-schema-name existing-schema ; String
             new-table-schema-name      new-schema      ; String
             new-column-data-type       new-data-type   ; String
             ]}]
  (let [change  (AddLookupTableChange.)
        exs-name (or existing-table-schema-name existing-schema)
        nws-name (or new-table-schema-name      new-schema)
        nwd-type (or new-column-data-type       new-data-type)]
    (doto change
      (.setExistingTableName  (sp/clj-to-dbident existing-table-name))
      (.setExistingColumnName (sp/clj-to-dbident existing-column-name))
      (.setNewTableName       (sp/clj-to-dbident new-table-name))
      (.setNewColumnName      (sp/clj-to-dbident new-column-name))
      (.setConstraintName     (sp/clj-to-dbident constraint-name)))
    (if exs-name (.setExistingTableSchemaName change (sp/clj-to-dbident exs-name)))
    (if nws-name (.setNewTableSchemaName      change (sp/clj-to-dbident nws-name)))
    (if nwd-type (.setNewColumnDataType       change (in/as-coltype nwd-type)))
    change))


;; Add Not-Null Constraint

(defn ^AddNotNullConstraintChange add-not-null-constraint
  "Return a Change instance that adds a NOT NULL constraint
  (AddNotNullConstraintChange).
  See also:
    http://www.liquibase.org/manual/add_not-null_constraint"
  [table-name column-name column-data-type
   & {:keys [schema-name        schema  ; String
             default-null-value default ; String
             ]}]
  (let [change (AddNotNullConstraintChange.)
        s-name  (or schema-name        schema  *schema*)
        n-value (or default-null-value default)]
    (doto change
      (.setTableName      (sp/clj-to-dbident table-name))
      (.setColumnName     (sp/clj-to-dbident column-name))
      (.setColumnDataType (in/as-coltype column-data-type)))
    (if s-name  (.setSchemaName       change (sp/clj-to-dbident s-name)))
    (if n-value (.setDefaultNullValue change n-value))
    change))


;; Remove/drop Not-Null Constraint

(defn ^DropNotNullConstraintChange drop-not-null-constraint
  "Return a Change instance that drops a NOT NULL constraint
  (DropNotNullConstraintChange).
  See also:
    http://www.liquibase.org/manual/remove_not-null_constraint"
  [table-name column-name
   & {:keys [schema-name      schema    ; String
             column-data-type data-type ; String
             ]}]
  (let [change (DropNotNullConstraintChange.)
        s-name (or schema-name      schema    *schema*)
        d-type (or column-data-type data-type)]
    (doto change
      (.setTableName  (sp/clj-to-dbident table-name))
      (.setColumnName (sp/clj-to-dbident column-name)))
    (if s-name (.setSchemaName     change (sp/clj-to-dbident s-name)))
    (if d-type (.setColumnDataType change (in/as-coltype d-type)))
    change))


;; Add Unique Constraint

(defn ^AddUniqueConstraintChange add-unique-constraint
  "Return a Change instance that adds a UNIQUE constraint
  (AddUniqueConstraintChange).
  See also:
    http://www.liquibase.org/manual/add_unique_constraint"
  [table-name column-names constraint-name
   & {:keys [schema-name        schema ; String
             tablespace         tspace ; String
             deferrable         defer  ; Boolean
             initially-deferred idefer ; Boolean
             disabled                  ; Boolean
             ]}]
  (let [change (AddUniqueConstraintChange.)
        s-name (or schema-name        schema *schema*)
        t-name (or tablespace         tspace)
        df-val (or deferrable         defer)
        id-val (or initially-deferred idefer)
        di-val disabled]
    (doto change
      (.setTableName      (sp/clj-to-dbident table-name))
      (.setColumnNames    (in/as-dbident-names column-names))
      (.setConstraintName (sp/clj-to-dbident constraint-name)))
    (if s-name (.setSchemaName        change (sp/clj-to-dbident s-name)))
    (if t-name (.setTablespace        change (sp/clj-to-dbident t-name)))
    (if df-val (.setDeferrable        change df-val))
    (if id-val (.setInitiallyDeferred change id-val))
    (if di-val (.setDisabled          change di-val))
    change))


;; Drop Unique Constraint

(defn ^DropUniqueConstraintChange drop-unique-constraint
  "Return a Change instance that drops a UNIQUE constraint
  (DropUniqueConstraintChange).
  See also:
    http://www.liquibase.org/manual/drop_unique_constraint"
  [table-name constraint-name
   & {:keys [schema-name schema  ; String
             ]}]
  (let [change (DropUniqueConstraintChange.)
        s-name (or schema-name schema *schema*)]
    (doto change
      (.setTableName      (sp/clj-to-dbident table-name))
      (.setConstraintName (sp/clj-to-dbident constraint-name)))
    (if s-name (.setSchemaName change (sp/clj-to-dbident s-name)))
    change))


;; Create Sequence

(defn ^CreateSequenceChange create-sequence
  "Return a Change instance that creates a sequence (CreateSequenceChange).
  See also:
    http://www.liquibase.org/manual/create_sequence"
  [sequence-name
   & {:keys [schema-name  schema ; String
             start-value  start  ; BigInteger
             increment-by incby  ; BigInteger
             max-value    max    ; BigInteger
             min-value    min    ; BigInteger
             ordered      ord    ; Boolean
             cycle        cyc    ; Boolean
             ]}]
  (let [change (CreateSequenceChange.)
        s-name (or schema-name  schema *schema*)
        str-v  (or start-value  start)
        inc-v  (or increment-by incby)
        max-v  (or max-value    max)
        min-v  (or min-value    min)
        ord-v  (or ordered      ord)
        cyc-v  (or cycle        cyc)]
    (.setSequenceName change (sp/clj-to-dbident sequence-name))
    (if s-name (.setSchemaName  change (sp/clj-to-dbident s-name)))
    (if str-v  (.setStartValue  change (bigint str-v)))
    (if inc-v  (.setIncrementBy change (bigint inc-v)))
    (if max-v  (.setMaxValue    change (bigint max-v)))
    (if min-v  (.setMinValue    change (bigint min-v)))
    (if ord-v  (.setOrdered     change ord-v))
    (if cyc-v  (.setCycle       change cyc-v))
    change))


;; Drop Sequence

(defn ^DropSequenceChange drop-sequence
  "Return a Change instance that drops a sequence (DropSequenceChange)."
  [sequence-name
   & {:keys [schema-name schema  ; String
             ]}]
  (let [change (DropSequenceChange.)
        s-name (or schema-name schema *schema*)]
    (.setSequenceName change (sp/clj-to-dbident sequence-name))
    (if s-name (.setSchemaName change (sp/clj-to-dbident s-name)))
    change))


;; Add Auto-Increment

(defn ^AddAutoIncrementChange add-auto-increment
  "Return a Change instance that converts an existing column to be an
  auto-increment column (AddAutoIncrementChange).
  See also:
    http://www.liquibase.org/manual/add_auto-increment"
  [^String table-name ^String column-name ^String column-data-type
   & {:keys [schema-name schema  ; String
             ]}]
  (let [change (AddAutoIncrementChange.)
        s-name (or schema-name schema *schema*)]
    (doto change
      (.setTableName      (sp/clj-to-dbident table-name))
      (.setColumnName     (sp/clj-to-dbident column-name))
      (.setColumnDataType (in/as-coltype column-data-type)))
    (if s-name (.setSchemaName change (sp/clj-to-dbident s-name)))
    change))


;; Add Default Value

(defn ^AddDefaultValueChange add-default-value
  "Return a Change instance that adds a default value to the database definition
  for the specified column (AddDefaultValueChange).
  See also:
    http://www.liquibase.org/manual/add_default_value"
  [table-name column-name default-value
   & {:keys [schema-name      schema  ; String
             column-data-type data-type
             ]}]
  (let [change (AddDefaultValueChange.)
        s-name (or schema-name      schema    *schema*)
        d-type (or column-data-type data-type)]
    (doto change
      (.setTableName  (sp/clj-to-dbident table-name))
      (.setColumnName (sp/clj-to-dbident column-name)))
    (in/set-default-value change default-value)
    (if s-name (.setSchemaName     change (sp/clj-to-dbident s-name)))
    (if d-type (.setColumnDataType change (apply in/as-coltype
                                            (mu/as-vector d-type))))
    change))


;; Drop Default Value

(defn ^DropDefaultValueChange drop-default-value
  "Return a Change instance that removes a database default value for a column
  (DropDefaultValueChange).
  See also:
    http://www.liquibase.org/manual/drop_default_value"
  [table-name column-name
   & {:keys [schema-name      schema  ; String
             column-data-type data-type
             ]}]
  (let [change (DropDefaultValueChange.)
        s-name (or schema-name      schema    *schema*)
        d-type (or column-data-type data-type)]
    (doto change
      (.setTableName  (sp/clj-to-dbident table-name))
      (.setColumnName (sp/clj-to-dbident column-name)))
    (if s-name (.setSchemaName     change (sp/clj-to-dbident s-name)))
    (if d-type (.setColumnDataType change (apply in/as-coltype
                                            (mu/as-vector d-type))))
    change))


;; ----- Referential Integrity Refactorings -----

;; Add Foreign Key Constraint

(defn ^AddForeignKeyConstraintChange add-foreign-key-constraint
  "Return a Change instance that adds a foreign key constraint to an existing
  column (AddForeignKeyConstraintChange).
  See also:
    http://www.liquibase.org/manual/add_foreign_key_constraint"
  [constraint-name base-table-name base-column-names
   referenced-table-name referenced-column-names
   & {:keys [base-table-schema-name       base-schema ; String
             referenced-table-schema-name ref-schema  ; String
             deferrable                   defer  ; Boolean
             initially-deferred           idefer ; Boolean
             on-delete                    ondel  ; String
             on-update                    onupd  ; String
             ]}]
  (let [change (AddForeignKeyConstraintChange.)
        bs-name (or base-table-schema-name       base-schema)
        rs-name (or referenced-table-schema-name ref-schema)
        df-v    (or deferrable         defer)
        id-v    (or initially-deferred idefer)
        od-v    (or on-delete          ondel)
        ou-v    (or on-update          onupd)]
    (doto change
      (.setConstraintName        (sp/clj-to-dbident constraint-name))
      (.setBaseTableName         (sp/clj-to-dbident base-table-name))
      (.setBaseColumnNames       (in/as-dbident-names base-column-names))
      (.setReferencedTableName   (sp/clj-to-dbident referenced-table-name))
      (.setReferencedColumnNames (in/as-dbident-names referenced-column-names)))
    (if bs-name (.setBaseTableSchemaName       change (sp/clj-to-dbident bs-name)))
    (if rs-name (.setReferencedTableSchemaName change (sp/clj-to-dbident rs-name)))
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
    http://www.liquibase.org/manual/drop_foreign_key_constraint"
  [constraint-name base-table-name
   & {:keys [schema-name schema  ; String
             ]}]
  (let [change (DropForeignKeyConstraintChange.)
        s-name (or schema-name schema *schema*)]
    (doto change
      (.setConstraintName (sp/clj-to-dbident constraint-name))
      (.setBaseTableName  (sp/clj-to-dbident base-table-name)))
    (if s-name (.setBaseTableSchemaName change (sp/clj-to-dbident s-name)))
    change))


;; Add Primary Key Constraint

(defn ^AddPrimaryKeyChange add-primary-key
  "Return a Change instance that adds creates a primary key out of an existing
  column or set of columns (AddPrimaryKeyChange).
  See also:
    http://www.liquibase.org/manual/add_primary_key_constraint"
  [table-name column-names constraint-name
   & {:keys [schema-name schema ; String
             table-space tspace ; String
             ]}]
  (let [change (AddPrimaryKeyChange.)
        s-name (or schema-name schema *schema*)
        t-name (or table-space tspace)]
    (doto change
      (.setTableName      (sp/clj-to-dbident table-name))
      (.setColumnNames    (in/as-dbident-names column-names))
      (.setConstraintName (sp/clj-to-dbident constraint-name)))
    (if s-name (.setSchemaName change (sp/clj-to-dbident s-name)))
    (if t-name (.setTablespace change (sp/clj-to-dbident t-name)))
    change))


;; Drop Primary Key Constraint - DropPrimaryKeyChange

(defn ^DropPrimaryKeyChange drop-primary-key
  "Return a Change instance that drops an existing primary key
  (DropPrimaryKeyChange).
  See also:
    http://www.liquibase.org/manual/drop_primary_key_constraint"
  [table-name
   & {:keys [schema-name     schema ; String
             constraint-name constr ; String
             ]}]
  (let [change (DropPrimaryKeyChange.)
        s-name (or schema-name     schema *schema*)
        c-name (or constraint-name constr)]
    (.setTableName change (sp/clj-to-dbident table-name))
    (if s-name (.setSchemaName     change (sp/clj-to-dbident s-name)))
    (if c-name (.setConstraintName change (sp/clj-to-dbident c-name)))
    change))


;; ----- Non-Refactoring Transformations -----

;; Insert Data - InsertDataChange

(defn ^InsertDataChange insert-data
  "Return a Change instance that inserts data into an existing table
  (InsertDataChange).
  See also:
    http://www.liquibase.org/manual/insert_data"
  [^String table-name column-value-map
   & {:keys [schema-name schema ; String
             ]}]
  (let [change (InsertDataChange.)
        s-name (or schema-name schema *schema*)
        cols-v (map (fn [[n v]] (in/new-column-value n v)) column-value-map)]
    (.setTableName change (sp/clj-to-dbident table-name))
    (doseq [each cols-v]
      (.addColumn change ^ColumnConfig each))
    (if s-name (.setSchemaName change (sp/clj-to-dbident s-name)))
    change))


;; Load Data - LoadDataChange

(defn ^LoadDataChange load-data
  "Return a Change instance that Loads data from a CSV file into an existing
  table (LoadDataChange). A value of NULL in a cell will be converted to a
  database NULL rather than the string “NULL”.
  Arguments:
    table-name   (keyword/String) table name
    csv-filename (String)         CSV file name
    columns-spec (map)            key => name, value => type
                 (coll)           list of lists, each list is a col spec
  Optional arguments:
    :schema   (keyword/String) schema name - defaults to the default schema
    :encoding (String)         encoding of the CSV file - defaults to UTF-8
  See also:
    http://www.liquibase.org/manual/load_data"
  [table-name ^String csv-filename columns-spec
   & {:keys [schema-name schema ; String
             encoding    enc    ; String
             ]}]
  (let [change (LoadDataChange.)
        s-name (or schema-name schema *schema*)
        enc-v  (or encoding    enc)]
    (doto change
      (.setTableName (sp/clj-to-dbident table-name))
      (.setFile      csv-filename))
    (doseq [each columns-spec]
      (.addColumn change (apply in/load-data-column-config each)))
    (if s-name (.setSchemaName change (sp/clj-to-dbident s-name)))
    (if enc-v  (.setEncoding   change (mu/as-string enc-v)))
    change))


;; Load Update Data - LoadUpdateDataChange

(defn ^LoadUpdateDataChange load-update-data
  "Return a Change instance that loads or updates data from a CSV file into an
  existing table (LoadUpdateDataChange). Differs from loadData by issuing a SQL
  batch that checks for the existence of a record. If found, the record is
  UPDATEd, else the record is INSERTed. Also, generates DELETE statements for a
  rollback. A value of NULL in a cell will be converted to a database NULL
  rather than the string “NULL”.
  Arguments:
    table-name   (keyword/String) table name
    csv-filename (String)         CSV file name
    primary-key  (String)         Comma delimited list of the columns for the primary key
    columns-spec (map)            key => name, value => type
                 (coll)           list of lists, each list is a col spec
  Optional arguments:
    :schema   (keyword/String) schema name - defaults to the default schema
    :encoding (String)         encoding of the CSV file - defaults to UTF-8
  See also:
    http://www.liquibase.org/manual/load_update_data"
  [table-name ^String csv-filename primary-key columns-spec
   & {:keys [schema-name schema ; String
             encoding    enc    ; String
             ]}]
  (let [change (LoadUpdateDataChange.)
        s-name (or schema-name schema *schema*)
        enc-v  (or encoding    enc)]
    (doto change
      (.setTableName  (sp/clj-to-dbident table-name))
      (.setFile       csv-filename)
      (.setPrimaryKey (in/as-dbident-names primary-key)))
    (doseq [each columns-spec]
      (.addColumn change (apply in/load-data-column-config each)))
    (if s-name (.setSchemaName change (sp/clj-to-dbident s-name)))
    (if enc-v  (.setEncoding   change (mu/as-string enc-v)))
    change))


;; Update Data - UpdateDataChange

(defn ^UpdateDataChange update-data
  "Return a Change instance that updates data in an existing table
  (UpdateDataChange).
  See also:
    http://www.liquibase.org/manual/update_data"
  [table-name column-name-value-map
   & {:keys [schema-name  schema ; String
             where-clause where  ; String
             ]}]
  (let [change   (UpdateDataChange.)
        s-name   (or schema-name  schema *schema*)
        w-clause (or where-clause where)]
    (.setTableName change (sp/clj-to-dbident table-name))
    (doseq [[n v] column-name-value-map]
      (.addColumn change (in/new-column-value n v)))
    (if s-name   (.setSchemaName change (sp/clj-to-dbident s-name)))
    (if w-clause (.setWhereClause change w-clause))
    change))


;; Delete Data - DeleteDataChange

(defn ^DeleteDataChange delete-data
  "Return a Change instance that deletes data from an existing table
  (DeleteDataChange).
  See also:
    http://www.liquibase.org/manual/delete_data"
  [table-name
   & {:keys [schema-name  schema ; String
             where-clause where  ; String
             ]}]
  (let [change   (DeleteDataChange.)
        s-name   (or schema-name  schema *schema*)
        w-clause (or where-clause where)]
    (.setTableName change (sp/clj-to-dbident table-name))
    (if s-name   (.setWhereClause change (sp/clj-to-dbident s-name)))
    (if w-clause (.setSchemaName  change w-clause))
    change))


;; Tag Database - TagDatabaseChange

(defn ^TagDatabaseChange tag-database
  "Return a Change instance that applies a tag to the database for future
  rollback (TagDatabaseChange).
  See also:
    http://www.liquibase.org/manual/tag_database"
  [tag]
  (let [change (TagDatabaseChange.)]
    (.setTag change (mu/as-string tag))
    change))


;; Stop - StopChange

(defn ^StopChange stop
  "Return a Change instance that stops LiquiBase execution with a message
  (StopChange). Mainly useful for debugging and stepping through a changelog.
  See also:
    http://www.liquibase.org/manual/stop"
  ([] (StopChange.))
  ([^String message]
    (let [change (StopChange.)]
      (.setMessage change message)
      change)))


;; ----- Architectural Refactorings -----

;; Create Index - CreateIndexChange

(defn ^CreateIndexChange create-index
  "Return a Change instance that creates an index on an existing column or set
  of columns (CreateIndexChange).
  See also:
    http://www.liquibase.org/manual/create_index"
  [table-name column-names
   & {:keys [schema-name  schema ; String
             index-name   index  ; String
             unique       uniq   ; Boolean
             table-space  tspace ; String
             ]}]
  (let [change  (CreateIndexChange.)
        s-name  (or schema-name  schema *schema*)
        i-name  (or index-name   index)
        uniq-v  (or unique       uniq)
        t-space (or table-space  tspace)]
    (.setTableName change (sp/clj-to-dbident table-name))
    (doseq [each column-names]
      (.addColumn change (in/new-column-value each "")))
    (if s-name  (.setSchemaName change (sp/clj-to-dbident s-name)))
    (if i-name  (.setIndexName  change (sp/clj-to-dbident i-name)))
    (if uniq-v  (.setUnique     change uniq-v))
    (if t-space (.setTablespace change t-space))
    change))


;; Drop Index - DropIndexChange

(defn ^DropIndexChange drop-index
  "Return a Change instance that drops an existing index (DropIndexChange).
  See also:
    http://www.liquibase.org/manual/drop_index"
  [index-name table-name
   & {:keys [schema-name  schema ; String
             ]}]
  (let [change (DropIndexChange.)
        s-name (or schema-name  schema *schema*)]
    (doto change
      (.setIndexName (sp/clj-to-dbident index-name))
      (.setTableName (sp/clj-to-dbident table-name)))
    (if s-name (.setSchemaName change (sp/clj-to-dbident s-name)))
    change))


;; ----- Custom Refactorings -----

;; Modifying Generated SQL
;; TODO

;; Custom SQL
;; TODO

;; Custom SQL File
;; TODO

;; Custom Refactoring Class
;; TODO

;; Execute Shell Command
;; TODO
