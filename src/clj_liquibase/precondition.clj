(ns clj-liquibase.precondition
  "Clojure wrappers for liquibase.change.Change implementations.
  See also:
    http://www.liquibase.org/manual/home (Available Database Refactorings)"
  (:require
    [org.bituf.clj-dbspec   :as sp]
    [clj-miscutil.core      :as mu]
    [clj-liquibase.internal :as in])
  (:import
    (liquibase.precondition      Precondition PreconditionLogic)
    (liquibase.precondition.core ChangeLogPropertyDefinedPrecondition
                                 ChangeSetExecutedPrecondition
                                 ColumnExistsPrecondition
                                 DBMSPrecondition
                                 ForeignKeyExistsPrecondition
                                 IndexExistsPrecondition
                                 PrimaryKeyExistsPrecondition
                                 RunningAsPrecondition
                                 SequenceExistsPrecondition
                                 SqlPrecondition
                                 TableExistsPrecondition
                                 ViewExistsPrecondition
                                 ;; -- containers ---
                                 AndPrecondition
                                 NotPrecondition
                                 OrPrecondition
                                 PreconditionContainer)))


(defn pre-cond?
  "Return true if given argument is a pre-condition, false otherwise."
  [x]
  (instance? Precondition x))


(defn ^ChangeLogPropertyDefinedPrecondition changelog-prop-defined
  "Change-log property defined"
  [prop value] {:post [(mu/verify-cond (instance? ChangeLogPropertyDefinedPrecondition %))]
                :pre  [(mu/verify-arg (or (keyword? prop)  (string? prop)))
                       (mu/verify-arg (or (keyword? value) (string? value)))]}
  (let [pc (ChangeLogPropertyDefinedPrecondition.)]
    (doto pc
      (.setProperty (mu/as-string prop))
      (.setValue    (mu/as-string value)))
    pc))


(defn ^ChangeSetExecutedPrecondition changeset-executed
  "Change-set executed"
  [file id author] {:post [(mu/verify-cond (instance? ChangeSetExecutedPrecondition %))]
                    :pre  [(mu/verify-arg (string? file))
                           (mu/verify-arg (mu/not-nil? id))
                           (mu/verify-arg (or (keyword? author) (string? author)))]}
  (let [pc (ChangeSetExecutedPrecondition.)]
    (doto pc
      (.setChangeLogFile ^String file)
      (.setId            (mu/as-string id))
      (.setAuthor        (mu/as-string author)))
    pc))


(defn ^ColumnExistsPrecondition column-exists
  "Specified column exists"
  [schema-name table-name column-name] {:post [(mu/verify-cond (instance? ColumnExistsPrecondition %))]
                                        :pre  [(mu/verify-arg (or (keyword? schema-name) (string? schema-name)))
                                               (mu/verify-arg (or (keyword? table-name)  (string? table-name)))
                                               (mu/verify-arg (or (keyword? column-name) (string? column-name)))]}
  (let [pc (ColumnExistsPrecondition.)]
    (doto pc
      (.setSchemaName (sp/db-iden schema-name))
      (.setTableName  (sp/db-iden table-name))
      (.setColumnName (sp/db-iden column-name)))
    pc))


;; wrapper-based custom preconditions are not supported (they need class name)


(defn ^DBMSPrecondition dbms
  "Check database type. Example:
  (dbms :mysql)"
  [db-type] {:post [(mu/verify-cond (instance? DBMSPrecondition %))]
             :pre  [(mu/verify-arg (or (keyword? db-type) (string? db-type)))]}
  (let [pc (DBMSPrecondition.)]
    (doto pc
      (.setType (mu/as-string db-type)))
    pc))


;; TODO - do we really need *both* `table-name` and `key-name` arguments?
(defn ^ForeignKeyExistsPrecondition foreign-key-exists
  "Return Precondition that asserts given Foreign key exists"
  [schema-name table-name key-name] {:post [(mu/verify-cond (instance? ForeignKeyExistsPrecondition %))]
                                     :pre  [(mu/verify-arg (or (keyword? schema-name) (string? schema-name)))
                                            (mu/verify-arg (or (keyword? table-name)  (string? table-name)))
                                            (mu/verify-arg (or (keyword? key-name)    (string? key-name)))]}
  (let [pc (ForeignKeyExistsPrecondition.)]
    (doto pc
      (.setSchemaName          (sp/db-iden schema-name))
      (.setForeignKeyTableName (sp/db-iden table-name))
      (.setForeignKeyName      (sp/db-iden key-name)))
    pc))


(defn ^IndexExistsPrecondition index-exists
  [schema-name table-name column-names index-name] {:post [(mu/verify-cond (instance? IndexExistsPrecondition %))]
                                                    :pre  [(mu/verify-arg (or (keyword? schema-name)  (string? schema-name)))
                                                           (mu/verify-arg (or (keyword? table-name)   (string? table-name)))
                                                           (mu/verify-arg (or (keyword? column-names) (string? column-names)
                                                                            (and (coll? column-names)
                                                                              (every? #(or (keyword? %) (string? %))
                                                                                column-names))))
                                                           (mu/verify-arg (or (keyword? index-name)  (string? index-name)))]}
  (let [pc (IndexExistsPrecondition.)]
    (doto pc
      (.setSchemaName  (sp/db-iden schema-name))
      (.setTableName   (sp/db-iden table-name))
      (.setColumnNames (mu/comma-sep-str
                         (map sp/db-iden (mu/as-vector column-names))))
      (.setIndexName   (sp/db-iden index-name)))
    pc))


(defn ^PrimaryKeyExistsPrecondition primary-key-exists
  [schema-name table-name primary-key-name] {:post [(mu/verify-cond (instance? PrimaryKeyExistsPrecondition %))]
                                             :pre  [(mu/verify-arg (or (keyword? schema-name)      (string? schema-name)))
                                                    (mu/verify-arg (or (keyword? primary-key-name) (string? primary-key-name)))
                                                    (mu/verify-arg (or (keyword? table-name)       (string? table-name)))]}
  (let [pc (PrimaryKeyExistsPrecondition.)]
    (doto pc
      (.setSchemaName     (sp/db-iden schema-name))
      (.setPrimaryKeyName (sp/db-iden primary-key-name))
      (.setTableName      (sp/db-iden table-name)))
    pc))


(defn ^RunningAsPrecondition running-as
  "Verify database user name"
  [^String user-name] {:post [(mu/verify-cond (instance? RunningAsPrecondition %))]
                       :pre  [(mu/verify-arg (string? user-name))]}
  (let [pc (RunningAsPrecondition.)]
    (.setUsername pc user-name)
    pc))


(defn ^SequenceExistsPrecondition sequence-exists
  "Verify that given sequence exists"
  [schema-name sequence-name] {:post [(mu/verify-cond (instance? SequenceExistsPrecondition %))]
                               :pre  [(mu/verify-arg (or (keyword? schema-name)   (string? schema-name)))
                                      (mu/verify-arg (or (keyword? sequence-name) (string? sequence-name)))]}
  (let [pc (SequenceExistsPrecondition.)]
    (doto pc
      (.setSchemaName   (sp/db-iden schema-name))
      (.setSequenceName (sp/db-iden sequence-name)))
    pc))


(defn ^SqlPrecondition sql
  "SQL Check"
  [expected ^String sql-stmt] {:post [(mu/verify-cond (instance? SqlPrecondition %))]
                               :pre  [(mu/verify-arg (string? sql-stmt))]}
  (let [pc (SqlPrecondition.)]
    (doto pc
      (.setExpectedResult (or (nil? expected) nil
                            (mu/as-string expected)))
      (.setSql            sql-stmt))
    pc))


(defn ^TableExistsPrecondition table-exists
  "Verify that said table exists"
  [schema-name table-name] {:post [(mu/verify-cond (instance? TableExistsPrecondition %))]
                            :pre  [(mu/verify-arg (or (keyword? schema-name) (string? schema-name)))
                                   (mu/verify-arg (or (keyword? table-name)  (string? table-name)))]}
  (let [pc (TableExistsPrecondition.)]
    (doto pc
      (.setSchemaName (sp/db-iden schema-name))
      (.setTableName  (sp/db-iden table-name)))
    pc))


(defn ^ViewExistsPrecondition view-exists
  "Verify that view exists"
  [schema-name view-name] {:post [(mu/verify-cond (instance? ViewExistsPrecondition %))]
                           :pre  [(mu/verify-arg (or (keyword? schema-name) (string? schema-name)))
                                  (mu/verify-arg (or (keyword? view-name)   (string? view-name)))]}
  (let [pc (ViewExistsPrecondition.)]
    (doto pc
      (.setSchemaName (sp/db-iden schema-name))
      (.setViewName   (sp/db-iden view-name)))
    pc))


(defn  ^AndPrecondition pc-and
  "Verify that ALL immediate nested preconditions are met"
  [pc & more] {:post [(mu/verify-cond (instance? AndPrecondition %))]
               :pre  [(mu/verify-arg (every? #(instance? Precondition %)
                                       (into [pc] more)))]}
  (let [pl (AndPrecondition.)]
    (doseq [each (into [pc] more)]
      (.addNestedPrecondition pl each))
    pl))


(defn  ^NotPrecondition pc-not
  "Verify that NONE OF immediate nested preconditions is met"
  [pc & more] {:post [(mu/verify-cond (instance? NotPrecondition %))]
               :pre  [(mu/verify-arg (every? #(instance? Precondition %)
                                       (into [pc] more)))]}
  (let [pl (NotPrecondition.)]
    (doseq [each (into [pc] more)]
      (.addNestedPrecondition pl each))
    pl))


(defn  ^OrPrecondition pc-or
  "Verify that ANY OF immediate nested preconditions is met"
  [pc & more] {:post [(mu/verify-cond (instance? OrPrecondition %))]
               :pre  [(mu/verify-arg (every? #(instance? Precondition %)
                                       (into [pc] more)))]}
  (let [pl (OrPrecondition.)]
    (doseq [each (into [pc] more)]
      (.addNestedPrecondition pl each))
    pl))


(def on-fail-error-values
  {:halt     "HALT"     ; Immediately halt execution of entire change log [default]
   :continue "CONTINUE" ; Skip over change set. Execution of change set will be attempted again on the next update. Continue with change log.
   :mark-ran "MARK_RAN" ; Skip over change set, but mark it as ran. Continue with change log
   :warn     "WARN"     ; Output warning and continue executing change set as normal.
   })


(def on-update-sql-values
  {:ignore "IGNORE" ; Ignore the preCondition in updateSQL mode
   :test   "TEST"   ; Test the changeSet in updateSQL mode
   :fail   "FAIL"   ; Fail the preCondition in updateSQL mode
   })


(defn ^PreconditionContainer pre-cond
  "Return a PreconditionContainer that verifies that ALL immediate nested
  preconditions are met.
  Optional args:
    :on-fail       What to do when preconditions fail; either of
                     :halt (default), :continue, :mark-ran, :warn
    :on-fail-msg   Custom message (string) to output when preconditions fail
    :on-error      What to do when preconditions error; either of
                     :halt (default), :continue, :mark-ran, :warn
    :on-error-msg  Custom message (string) to output when preconditions fail
    :on-update-sql What to do in updateSQL mode; either of
                     :run, :fail, :ignore
  See also:
    http://www.liquibase.org/manual/preconditions"
  [pre-cond-list
   & {:keys [on-fail       ; onFail         -- What to do when preconditions fail
             on-fail-msg   ; onFailMessage  -- Custom message to output when preconditions fail
             on-error      ; onError        -- What to do when preconditions error
             on-error-msg  ; onErrorMessage -- Custom message to output when preconditions fail
             on-update-sql ; onUpdateSQL    -- What to do in updateSQL mode
             ]
      :or {on-fail       nil
           on-fail-msg   nil
           on-error      nil
           on-error-msg  nil
           on-update-sql nil}
      :as opt}]
  {:post [(mu/verify-cond (instance? PreconditionContainer %))]
   :pre  [(mu/verify-opt #{:on-fail  :on-fail-msg :on-error :on-error-msg
                           :on-update-sql} opt)
          (mu/verify-arg (and (coll? pre-cond-list)
                           (every? #(instance? Precondition %) pre-cond-list)))
          (mu/verify-arg (or (nil? on-fail)       (contains? on-fail-error-values on-fail)))
          (mu/verify-arg (or (nil? on-error)      (contains? on-fail-error-values on-error)))
          (mu/verify-arg (or (nil? on-update-sql) (contains? on-update-sql-values on-update-sql)))
          (mu/verify-arg (or (nil? on-fail-msg)   (string? on-fail-msg)))
          (mu/verify-arg (or (nil? on-error-msg)  (string? on-error-msg)))]}
  (let [pc (PreconditionContainer.)]
    (doseq [each pre-cond-list]
      (.addNestedPrecondition pc each))
    (when on-fail       (.setOnFail  pc (on-fail  on-fail-error-values)))
    (when on-error      (.setOnError pc (on-error on-fail-error-values)))
    (when on-update-sql (.setOnSqlOutput pc ^String (on-update-sql on-update-sql-values)))
    (when on-fail-msg   (.setOnFailMessage  pc on-fail-msg))
    (when on-error-msg  (.setOnErrorMessage pc on-error-msg))
    pc))