(ns clj-liquibase.core
  "Expose functions from the Liquibase library.
  See also:
    http://www.liquibase.org/manual/home"
  (:require
    [clojure.string    :as sr]
    [clj-jdbcutil.core :as sp]
    [clj-miscutil.core :as mu]
    [clj-liquibase.internal     :as in]
    [clj-liquibase.change       :as ch]
    [clj-liquibase.precondition :as pc]
    [clj-liquibase.sql-visitor  :as vis])
  (:import
    (java.io                           File IOException Writer)
    (java.sql                          Connection)
    (java.text                         DateFormat)
    (java.util                         Date List)
    (javax.sql                         DataSource)
    (clj_liquibase                     CustomDBDocVisitor)
    (liquibase.changelog               ChangeLogIterator ChangeSet ChangeLogParameters
                                       DatabaseChangeLog)
    (liquibase.change                  Change)
    (liquibase.change.core             CreateTableChange)
    (liquibase.changelog.filter        AfterTagChangeSetFilter      AlreadyRanChangeSetFilter
                                       ChangeSetFilter              ContextChangeSetFilter
                                       CountChangeSetFilter         DbmsChangeSetFilter
                                       ExecutedAfterChangeSetFilter ShouldRunChangeSetFilter)
    (liquibase.changelog.visitor       DBDocVisitor RollbackVisitor UpdateVisitor)
    (liquibase.database                Database DatabaseFactory)
    (liquibase.database.jvm            JdbcConnection)
    (liquibase.executor                Executor ExecutorService LoggingExecutor)
    (liquibase.exception               LiquibaseException LockException)
    (liquibase.integration.commandline CommandLineUtils)
    (liquibase.lockservice             LockService)
    (liquibase.logging                 LogFactory Logger)
    (liquibase.precondition            Precondition)
    (liquibase.precondition.core       PreconditionContainer)
    (liquibase.sql                     Sql)
    (liquibase.sql.visitor             SqlVisitor)
    (liquibase.sqlgenerator            SqlGeneratorFactory)
    (liquibase.statement               SqlStatement)
    (liquibase.util                    LiquibaseUtil)))


(def ^{:doc "Clj-Liquibase version"}
      version [0 4 0])


;; ===== Dynamic vars for Integration =====


(def ^{:doc "Logical filepath use by ChangeSet and ChangeLog instances."
       :tag String
       :dynamic true}
      *logical-filepath* nil)


(def ^{:doc "Database (liquibase.database.Database) instance."
       :tag Database
       :dynamic true}
      *db-instance* nil)


(def ^{:doc "Changelog params (liquibase.changelog.ChangeLogParameters) instance."
       :tag ChangeLogParameters
       :dynamic true}
      *changelog-params* nil)


(defn verify-valid-logical-filepath
  "Verify whether the *logical-filepath* var has a valid value. Return true if
  all OK, throw IllegalStateException otherwise."
  []
  (when (not (string? *logical-filepath*))
    (throw (IllegalStateException.
             ^String (format
                       "Expected %s but found %s - not wrapped in 'defchangelog'?"
                       "var *logical-filepath* to be string"
                       (mu/val-dump *logical-filepath*)))))
  true)


;; ===== ChangeSet =====


(defn changeset?
  "Return true if specified argument is a liquibase.changelog.ChangeSet
  instance, false otherwise."
  [x]
  (instance? ChangeSet x))


(defn ^ChangeSet make-changeset
  "Return a ChangeSet instance. Use MySQL InnoDB for `create-table` changes by
  default (unless overridden by :visitors argument.)
  Arguments:
    id      (String)     Author-assigned ID, which can be sequential
    author  (String)     Author name (must be kept same across changesets)
    changes (collection) List of Change objects
  Optional arguments:
    :dbms                          ; String/Keyword/vector-of-multiple
    :run-always         :always    ; Boolean
    :run-on-change      :on-change ; Boolean
    :context            :ctx       ; String
    :run-in-transaction :in-txn    ; Boolean (true by default)
    :fail-on-error      :fail-err  ; Boolean
    ;; sub tags
    :comment                        ; String
    :pre-conditions     :pre-cond   ; list of Precondition objects, or PreconditionContainer object
    :valid-checksum     :valid-csum ; String
    :visitors                       ; list of SqlVisitor objects
  See also:
    http://www.liquibase.org/manual/changeset"
  [^String id ^String author ^List changes
   & {:keys [logical-filepath   filepath
             dbms
             run-always         always
             run-on-change      on-change
             context            ctx
             run-in-transaction in-txn
             fail-on-error      fail-err
             comment
             pre-conditions     pre-cond
             rollback-changes   rollback
             valid-checksum     valid-csum
             visitors
             ] :as opt}] {:post [(instance? ChangeSet %)]
                          :pre  [(mu/verify-opt #{:logical-filepath   :filepath
                                                  :dbms
                                                  :run-always         :always
                                                  :run-on-change      :on-change
                                                  :context            :ctx
                                                  :run-in-transaction :in-txn
                                                  :fail-on-error      :fail-err
                                                  :comment
                                                  :pre-conditions     :pre-cond
                                                  :rollback-changes   :rollback
                                                  :valid-checksum     :valid-csum
                                                  :visitors} opt)
                                 (mu/verify-arg (string?       id))
                                 (mu/verify-arg (string?       author))
                                 (mu/verify-arg (coll?         changes))
                                 (mu/verify-arg (mu/not-empty? changes))
                                 (mu/verify-arg (every? vis/visitor? visitors))]}
  (when-not (or logical-filepath filepath)
    (verify-valid-logical-filepath))
  (let [s-filepath (or logical-filepath filepath *logical-filepath*)
        s-dbms     (in/as-dbident-names dbms)
        b-always   (or run-always       always    false)
        b-change   (or run-on-change    on-change false)
        s-contxt   (or context          ctx)
        b-in-txn   (let [x (or run-in-transaction in-txn)]
                     (if (nil? x) true (or x false)))
        b-fail-err (or fail-on-error    fail-err  false)
        ;; sub tags
        s-comment  comment
        v-pre-cond (or pre-conditions     pre-cond)
        v-rollback (or rollback-changes   rollback)
        s-val-csum (or valid-checksum     valid-csum)
        v-visitors (or visitors (if (every? #(instance? CreateTableChange %)
                                            changes)
                                  [vis/mysql-innodb]
                                  []))
        _ (do
            (mu/verify-arg (string?       id))
            (mu/verify-arg (string?       author))
            (mu/verify-arg (string?       s-filepath))
            (mu/verify-arg (mu/not-empty? changes))
            (doseq [each changes]
              (mu/verify-arg (instance? Change each)))
            (mu/verify-arg (mu/boolean? b-always))
            (mu/verify-arg (mu/boolean? b-change))
            (mu/verify-arg (or (nil? s-contxt) (string? s-contxt)))
            (mu/verify-arg (string?     s-dbms))
            (mu/verify-arg (mu/boolean? b-in-txn))
            (mu/verify-arg (or (nil? v-pre-cond)
                             (instance? PreconditionContainer v-pre-cond)
                             (and (coll? v-pre-cond)
                               (every? #(instance? Precondition %) v-pre-cond)))))
        ;; String id, String author, boolean alwaysRun, boolean runOnChange,
        ;; String filePath, String contextList, String dbmsList, boolean runInTransaction
        c-set (ChangeSet.
                ^String id ^String author   ^Boolean b-always ^Boolean b-change
                ^String (mu/java-filepath s-filepath)
                ^String s-contxt ^String  s-dbms   ^Boolean b-in-txn)]
    (doseq [each changes]
      (.addChange c-set each))
    (if b-fail-err (.setFailOnError   c-set b-fail-err))
    (if s-comment  (.setComments      c-set s-comment))
    (if v-pre-cond (.setPreconditions c-set (if (coll? v-pre-cond)
                                              (pc/pre-cond v-pre-cond)
                                              v-pre-cond)))
    (if v-rollback (doseq [each (mu/as-vector v-rollback)]
                     (if (string? each) (.addRollBackSQL c-set ^String each)
                       (.addRollbackChange c-set ^Change each))))
    (if s-val-csum (doseq [each (mu/as-vector s-val-csum)]
                     (.addValidCheckSum c-set each)))
    (doseq [each v-visitors]
      (.addSqlVisitor c-set ^SqlVisitor each))
    c-set))


;; ===== DatabaseChangeLog helpers =====


(defn ^Database make-db-instance
  "Return a Database instance for current connection."
  [^Connection conn]
  ;; DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn))
  (.findCorrectDatabaseImplementation (DatabaseFactory/getInstance)
    (JdbcConnection. conn)))


(defn ^ChangeLogParameters make-changelog-params
  "Return a ChangeLogParameters instance."
  [^Database db-instance
   & {:keys [contexts ; list of string
             ]}]
  (let [clp (ChangeLogParameters. db-instance)]
    (doseq [each (mu/as-vector contexts)]
      (.addContext clp (mu/as-string each)))
    clp))


;; ===== Integration =====


(defmacro with-lb
  "Execute body of code in the context of initialized Liquibase settings."
  [& body]
  `(do (assert (:connection sp/*dbspec*))
     (if (mu/not-nil? *db-instance*) (do ~@body)
       (binding [*db-instance* (make-db-instance (:connection sp/*dbspec*))
                 *changelog-params* (make-changelog-params *db-instance*)]
         ~@body))))


;; ===== DatabaseChangeLog =====


(defn changelog?
  "Return true if specified argument is a liquibase.changelog.DatabaseChangeLog
  instance, false otherwise."
  [x]
  (instance? DatabaseChangeLog x))


(defn ^DatabaseChangeLog make-changelog
  "Return a DatabaseChangeLog instance.
  Arguments:
    change-sets  (collection/list) List of ChangeSet instances, or
                                   List of arg-lists (for 'make-changeset' fn)
  Optional args:
    :pre-conditions :pre-cond  ; PreconditionContainer object, or list of Precondition objects
  See also:
    http://www.liquibase.org/manual/databasechangelog
    make-changelog-params"
  [^String filepath ^List change-sets
   & {:keys [pre-conditions     pre-cond   ; vector
             ] :as opt}] {:post [(instance? DatabaseChangeLog %)]
                          :pre  [(mu/verify-opt #{:pre-conditions   :pre-cond} opt)
                                 (mu/verify-arg (string?       filepath))
                                 (mu/verify-arg (coll?         change-sets))
                                 (mu/verify-arg (mu/not-empty? change-sets))]}
  (let [dbcl       (DatabaseChangeLog.)
        v-pre-cond (or pre-conditions pre-cond)
        _          (mu/verify-arg
                     (or (nil? v-pre-cond)
                       (instance? PreconditionContainer v-pre-cond)
                       (and (coll? v-pre-cond)
                         (every? #(instance? Precondition %) v-pre-cond))))]
    (doto dbcl
      (.setLogicalFilePath (mu/java-filepath filepath))
      (.setChangeLogParameters ^ChangeLogParameters *changelog-params*))
    (doseq [each change-sets]
      (binding [*logical-filepath* (mu/java-filepath filepath)]
        (cond
          (changeset? each)    (.addChangeSet dbcl ^ChangeSet each)
          (and (coll? each)
            (not (map? each))) (.addChangeSet dbcl
                                 ^ChangeSet (apply make-changeset each))
          :else
          (mu/illegal-argval "change-sets#element"
            "ChangeSet object or arg-lists for 'make-changeset' fn"
            each))))
    (if v-pre-cond
      (.setPreconditions dbcl
        ^PreconditionContainer (if (coll? v-pre-cond) (pc/pre-cond v-pre-cond)
                                 v-pre-cond)))
    dbcl))


(defmacro defchangelog
  "Define a function that when executed with no arguments, returns a database
  changelog (DatabaseChangeLog instance) while binding *logical-filepath* to
  `logical-schema`.
  See also:
    make-changelog"
  [var-name logical-schema change-sets & var-args] {:pre [(symbol? var-name)]}
  `(def ~var-name
     (partial make-changelog ~logical-schema ~change-sets ~@var-args)))


;; ===== Actions helpers =====


(def ^{:doc "Liquibase logger"
       :tag Logger}
      log (LogFactory/getLogger))

(defn check-database-changelog-table
  "Check database changelog table.
  See also:
    liquibase.Liquibase/checkDatabaseChangeLogTable"
  [^Database db ^Boolean update-existing-null-checksums
   ^DatabaseChangeLog db-changelog contexts]
  (when (and update-existing-null-checksums (nil? db-changelog))
    (throw
      (LiquibaseException.
        "'db-changelog' parameter is required if updating existing checksums")))
  (.checkDatabaseChangeLogTable db
    update-existing-null-checksums db-changelog (into-array String contexts))
  (when (not (.hasChangeLogLock (LockService/getInstance db)))
    (.checkDatabaseChangeLogLockTable db)))


(defn ^ChangeLogIterator make-changelog-iterator
  "Return a ChangeLogIterator instance.
  See also:
    liquibase.Liquibase/getStandardChangelogIterator"
  ([^DatabaseChangeLog changelog ^List changeset-filters]
    (ChangeLogIterator. changelog
      (into-array ChangeSetFilter changeset-filters)))
  ([^List ran-changesets ^DatabaseChangeLog changelog ^List changeset-filters]
    (ChangeLogIterator. ran-changesets changelog
      (into-array ChangeSetFilter
        changeset-filters))))


(defn ^Executor get-db-executor
  []
  (let [ex (ExecutorService/getInstance)]
    (.getExecutor ex *db-instance*)))


(defn output-header
  [^String message]
  (let [ex (get-db-executor)]
    (doto ex
      (.comment "*********************************************************************")
      (.comment message)
      (.comment "*********************************************************************")
      (.comment (format "Change Log: %s" *file*)) ; TODO get the logical filename
      (.comment (format "Ran at: %s" (.format (DateFormat/getDateTimeInstance
                                                DateFormat/SHORT DateFormat/SHORT)
                                       (Date.))))
      (.comment (format "Against: %s@%s"
                  (-> *db-instance* .getConnection .getConnectionUserName)
                  (-> *db-instance* .getConnection .getURL)))
      (.comment (format "Liquibase version: %s"
                  (LiquibaseUtil/getBuildVersion)))
      (.comment "*********************************************************************"))))


(defmacro with-writer
  [^Writer output & body]
  `(let [old-template# (get-db-executor)
         log-executor# (LoggingExecutor.
                         (get-db-executor) ~output *db-instance*)]
     (.setExecutor (ExecutorService/getInstance) *db-instance* log-executor#)
     ~@body
     (try
       (.flush ~output)
       (catch IOException e#
         (throw (LiquibaseException. e#))))
     (.setExecutor (ExecutorService/getInstance) *db-instance* old-template#)))


(defmacro do-locked
  "Acquire lock and execute body of code in that context. Make sure the lock is
  released (or log an error if it can't be) before exit."
  [& body]
  `(let [ls# (LockService/getInstance *db-instance*)]
     (.waitForLock ls#)
     (try ~@body
       (finally
         (try (.releaseLock ls#)
           (catch LockException e#
             (.severe log "Could not release lock" e#)))))))


;; ===== Actions =====


(defn ^List change-sql
  "Return a list of SQL statements (string) that would be required to execute
  the given Change object instantly for current database without versioning."
  [^Change change] {:post [(mu/verify-cond (vector? %))
                           (mu/verify-cond (every? string? %))]
                    :pre  [(mu/verify-arg  (instance? Change change))
                           (mu/verify-cond (instance? Database *db-instance*))]}
  (let [sgf (SqlGeneratorFactory/getInstance)
        sql (map (fn [^SqlStatement stmt]
                   (map (fn [^Sql sql]
                          ^String (.toSql sql))
                     (.generateSql sgf stmt *db-instance*)))
              (.generateStatements change *db-instance*))]
    (into [] (flatten sql))))


(defmacro with-writable
  "Set spec with :read-only? as false and execute body of code in that context."
  [& body]
  `(sp/with-connection (sp/assoc-readonly sp/*dbspec* false)
     ~@body))


(defn update
  "Run the Liquibase Update command.
  See also:
    liquibase.Liquibase/update
    make-db-instance
    http://www.liquibase.org/manual/update"
  ([changelog-fn]
    (update changelog-fn []))
  ([changelog-fn ^List contexts] {:pre [(mu/verify-arg (fn? changelog-fn))
                                        (mu/verify-arg (coll? contexts))]}
    (sp/verify-writable)
    (do-locked
      (.setContexts *changelog-params* contexts)
      (let [changelog ^DatabaseChangeLog (changelog-fn)]
        (check-database-changelog-table *db-instance* true changelog contexts)
        (.validate changelog *db-instance* (into-array String contexts))
        (let [changelog-it (make-changelog-iterator changelog
                             [(ShouldRunChangeSetFilter. *db-instance*)
                              (ContextChangeSetFilter.
                                (into-array String
                                  [(mu/comma-sep-str contexts)]))
                              (DbmsChangeSetFilter. *db-instance*)
                              ])]
          (.run changelog-it (UpdateVisitor. *db-instance*) *db-instance*)))))
  ([changelog-fn ^List contexts ^Writer output]
    {:pre [(mu/verify-arg (instance? Writer output))]}
    (.setContexts *changelog-params* contexts)
    (with-writer output
      (output-header "Update Database Script")
      (with-writable
        (update changelog-fn contexts)))))


(defn update-by-count
  "Run Liquibase Update command restricting number of changes to howmany-changesets.
  See also:
    liquibase.Liquibase/update
    make-db-instance
    http://www.liquibase.org/manual/update"
  ([changelog-fn ^Integer howmany-changesets]
    (update-by-count changelog-fn howmany-changesets []))
  ([changelog-fn ^Integer howmany-changesets ^List contexts]
    {:pre [(mu/verify-arg (fn? changelog-fn))
           (mu/verify-arg (mu/posnum? howmany-changesets))
           (mu/verify-arg (coll? contexts))]}
    (sp/verify-writable)
    (.setContexts *changelog-params* contexts)
    (do-locked
      (let [changelog ^DatabaseChangeLog (changelog-fn)]
        (check-database-changelog-table *db-instance* true changelog contexts)
        (.validate changelog *db-instance* (into-array String contexts))
        (let [changelog-it (make-changelog-iterator changelog
                             [(ShouldRunChangeSetFilter. *db-instance*)
                              (ContextChangeSetFilter.
                                (into-array String
                                  [(mu/comma-sep-str contexts)]))
                              (DbmsChangeSetFilter. *db-instance*)
                              (CountChangeSetFilter. howmany-changesets)
                              ])]
          (.run changelog-it (UpdateVisitor. *db-instance*) *db-instance*)))))
  ([changelog-fn ^Integer howmany-changesets ^List contexts ^Writer output]
    (.setContexts *changelog-params* contexts)
    (with-writer output
      (output-header (str "Update " howmany-changesets " Change-sets Database Script"))
      (with-writable
        (update-by-count changelog-fn howmany-changesets contexts)))))


(defn tag
  "Tag the database schema with specified tag (coerced as string)."
  [the-tag] {:pre [(mu/verify-arg (or (string? the-tag) (keyword? the-tag)))]}
  (sp/verify-writable)
  (do-locked
    (check-database-changelog-table *db-instance* false nil nil)
    (.tag *db-instance* (mu/as-string the-tag))))


(defn rollback-to-tag
  "Rollback schema to specified tag.
  See also:
    liquibase.Liquibase/rollback
    http://www.liquibase.org/manual/rollback"
  ([changelog-fn ^String tag]
    (rollback-to-tag changelog-fn tag []))
  ([changelog-fn ^String tag ^List contexts]
    {:pre [(mu/verify-arg (fn? changelog-fn))
           (mu/verify-arg (coll? contexts))]}
    (sp/verify-writable)
    (do-locked
      (.setContexts *changelog-params* contexts)
      (let [changelog ^DatabaseChangeLog (changelog-fn)]
        (check-database-changelog-table *db-instance* false changelog contexts)
        (.validate changelog *db-instance* (into-array String contexts))
        (let [ran-changesets (.getRanChangeSetList *db-instance*)
              changelog-it (make-changelog-iterator ran-changesets changelog
                             [(AfterTagChangeSetFilter. tag ran-changesets)
                              (AlreadyRanChangeSetFilter. ran-changesets)
                              (ContextChangeSetFilter.
                                (into-array String
                                  [(mu/comma-sep-str contexts)]))
                              (DbmsChangeSetFilter. *db-instance*)])]
          (.run changelog-it (RollbackVisitor. *db-instance*) *db-instance*)))))
  ([changelog-fn ^String tag ^List contexts ^Writer output]
    {:pre [(mu/verify-arg (instance? Writer output))]}
    (.setContexts *changelog-params* contexts)
    (with-writer output
      (output-header (str "Rollback to '" tag "' Script"))
      (with-writable
        (rollback-to-tag changelog-fn tag contexts)))))


(defn rollback-to-date
  "Rollback schema to specified date.
  See also:
    liquibase.Liquibase/rollback
    http://www.liquibase.org/manual/rollback"
  ([changelog-fn ^Date date]
    (rollback-to-date changelog-fn date []))
  ([changelog-fn ^Date date ^List contexts]
    {:pre [(mu/verify-arg (fn? changelog-fn))
           (mu/verify-arg (mu/date? date))
           (mu/verify-arg (coll? contexts))]}
    (sp/verify-writable)
    (do-locked
      (.setContexts *changelog-params* contexts)
      (let [changelog ^DatabaseChangeLog (changelog-fn)]
        (check-database-changelog-table *db-instance* false changelog contexts)
        (.validate changelog *db-instance* (into-array String contexts))
        (let [ran-changesets (.getRanChangeSetList *db-instance*)
              changelog-it (make-changelog-iterator ran-changesets changelog
                             [(ExecutedAfterChangeSetFilter. date ran-changesets)
                              (AlreadyRanChangeSetFilter. ran-changesets)
                              (ContextChangeSetFilter.
                                (into-array String
                                  [(mu/comma-sep-str contexts)]))
                              (DbmsChangeSetFilter. *db-instance*)])]
          (.run changelog-it (RollbackVisitor. *db-instance*) *db-instance*)))))
  ([changelog-fn ^Date date contexts ^Writer output]
    {:pre [(mu/verify-arg (instance? Writer output))]}
    (.setContexts *changelog-params* contexts)
    (with-writer output
      (output-header (str "Rollback to " date " Script"))
      (with-writable
        (rollback-to-date changelog-fn date contexts)))))


(defn rollback-by-count
  "Rollback schema by specified count of changes.
  See also:
    liquibase.Liquibase/rollback
    http://www.liquibase.org/manual/rollback"
  ([changelog-fn ^Integer howmany-changesets]
    (rollback-by-count changelog-fn ^Integer howmany-changesets []))
  ([changelog-fn ^Integer howmany-changesets ^List contexts]
    {:pre [(mu/verify-arg (fn? changelog-fn))
           (mu/verify-arg (mu/posnum? howmany-changesets))
           (mu/verify-arg (coll? contexts))]}
    (sp/verify-writable)
    (.setContexts *changelog-params* contexts)
    (do-locked
      (let [changelog ^DatabaseChangeLog (changelog-fn)]
        (check-database-changelog-table *db-instance* false changelog contexts)
        (.validate changelog *db-instance* (into-array String contexts))
        (let [ran-changesets (.getRanChangeSetList *db-instance*)
              changelog-it (make-changelog-iterator ran-changesets changelog
                             [(AlreadyRanChangeSetFilter. ran-changesets)
                              (ContextChangeSetFilter.
                                (into-array String
                                  [(mu/comma-sep-str contexts)]))
                              (DbmsChangeSetFilter. *db-instance*)
                              (CountChangeSetFilter. howmany-changesets)])]
          (.run changelog-it (RollbackVisitor. *db-instance*) *db-instance*)))))
  ([changelog-fn ^Integer howmany-changesets ^List contexts ^Writer output]
    {:pre [(mu/verify-arg (instance? Writer output))]}
    (.setContexts *changelog-params* contexts)
    (with-writer output
      (output-header (str "Rollback to " howmany-changesets " Change-sets Script"))
      (with-writable
        (rollback-by-count changelog-fn howmany-changesets contexts)))))


(defn generate-doc
  "Generate documentation for changelog.
  See also:
    http://www.liquibase.org/manual/dbdoc
    http://www.liquibase.org/dbdoc/index.html"
  ([changelog-fn ^String output-dir ^List contexts]
    {:pre [(mu/verify-arg (fn? changelog-fn))
           (mu/verify-arg (string? output-dir))
           (mu/verify-arg (coll? contexts))
           (mu/verify-cond (mu/not-nil? *db-instance*))]}
    (.setContexts *changelog-params* contexts)
    (do-locked
      (let [changelog ^DatabaseChangeLog (changelog-fn)]
        (check-database-changelog-table *db-instance* false changelog nil)
        (.validate changelog *db-instance* (into-array String contexts))
        (let [changelog-it (make-changelog-iterator changelog
                             [(DbmsChangeSetFilter. *db-instance*)])
              dbdoc-visitor (DBDocVisitor. *db-instance*)]
          (.run changelog-it dbdoc-visitor *db-instance*)
          (.writeHTML (CustomDBDocVisitor. *db-instance*)
            (File. output-dir) nil)))))
  ([changelog-fn ^String output-dir]
    (generate-doc changelog-fn output-dir [])))


(defn diff
  "Report a description of the differences between two databases to standard out.
  See also:
    http://www.liquibase.org/manual/diff"
  [^Database ref-db-instance]
  (CommandLineUtils/doDiff ref-db-instance *db-instance*))
