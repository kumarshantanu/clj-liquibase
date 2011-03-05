(ns org.bituf.clj-liquibase
  "Expose functions from the Liquibase library.
  See also:
    http://www.liquibase.org/manual/home"
  (:import
    (java.io                     IOException Writer)
    (java.sql                    Connection)
    (java.text                   DateFormat)
    (java.util                   Date List)
    (javax.sql                   DataSource)
    (liquibase.changelog         ChangeLogIterator ChangeSet ChangeLogParameters
                                 DatabaseChangeLog)
    (liquibase.change            Change)
    (liquibase.changelog.filter  AfterTagChangeSetFilter      AlreadyRanChangeSetFilter
                                 ChangeSetFilter              ContextChangeSetFilter
                                 CountChangeSetFilter         DbmsChangeSetFilter
                                 ExecutedAfterChangeSetFilter ShouldRunChangeSetFilter)
    (liquibase.changelog.visitor RollbackVisitor UpdateVisitor)
    (liquibase.database          Database DatabaseFactory)
    (liquibase.database.jvm      JdbcConnection)
    (liquibase.executor          Executor ExecutorService LoggingExecutor)
    (liquibase.exception         LiquibaseException LockException)
    (liquibase.lockservice       LockService)
    (liquibase.logging           LogFactory Logger)
    (liquibase.precondition.core PreconditionContainer)
    (liquibase.util              LiquibaseUtil))
  (:require
    [clojure.string         :as sr]
    [org.bituf.clj-dbspec   :as sp]
    [org.bituf.clj-miscutil :as mu]
    [org.bituf.clj-liquibase.internal :as in]))


(def ^{:doc "Clj-Liquibase version (only major and minor)"}
      version 0.1)


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
             (format "Expected %s but found %s - not wrapped in 'defchangelog'?"
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
  "Return a ChangeSet instance.
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
    :pre-conditions     :pre-cond   ; vector
    :valid-checksum     :valid-csum ; String
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
                                                  :valid-checksum     :valid-csum} opt)
                                 (mu/verify-arg (string?       id))
                                 (mu/verify-arg (string?       author))
                                 (mu/verify-arg (coll?         changes))
                                 (mu/verify-arg (mu/not-empty? changes))]}
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
            (mu/verify-arg (mu/boolean? b-in-txn)))
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
    (if v-pre-cond (.setPreconditions c-set v-pre-cond)) ; TODO convert v-pre-cond
    (if v-rollback (doseq [each (mu/as-vector v-rollback)]
                     (if (string? each) (.addRollBackSQL c-set ^String each)
                       (.addRollbackChange c-set ^Change each))))
    (if s-val-csum (doseq [each (mu/as-vector s-val-csum)]
                     (.addValidCheckSum c-set each)))
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


(defn wrap-lb-init
  "Initialize global settings and wrap f with that context."
  [f] {:post [(fn? %)]
       :pre  [(fn? f)]}
  (fn [& args]
    (if (mu/not-nil? *db-instance*) (apply f args)
      (let [g (sp/wrap-connection
                #(binding [*db-instance* (make-db-instance
                                           (:connection sp/*dbspec*))
                           *changelog-params* (make-changelog-params
                                                *db-instance*)]
                   (apply f args)))]
        (g)))))


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
        v-pre-cond (or pre-conditions pre-cond)]
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
      (.setPreconditions dbcl ^PreconditionContainer v-pre-cond)) ; TODO convert v-pre-cond
    dbcl))


(defmacro defchangelog
  "Define a function that when executed with no arguments, returns a database
  changelog (DatabaseChangeLog instance). Do so in the context where
  *logical-filepath* is bound to *file* i.e. name of the current file.
  See also:
    make-changelog"
  [var-name change-sets & var-args]
  `(def ~var-name
     (partial make-changelog (or (and *logical-filepath* (mu/java-filepath
                                                           *logical-filepath*))
                               (mu/pick-filename *file*))
       ~change-sets ~@var-args)))


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


(defn update
  "Run the Liquibase Update command.
  See also:
    liquibase.Liquibase/update
    make-db-instance
    http://www.liquibase.org/manual/update"
  ([changelog-fn]
    (update changelog-fn []))
  ([changelog-fn ^List contexts]
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
    (.setContexts *changelog-params* contexts)
    (with-writer output
      (output-header "Update Database Script")
      (update changelog-fn contexts))))


(defn update-by-count
  "Run Liquibase Update command restricting number of changes to howmany-changesets.
  See also:
    liquibase.Liquibase/update
    make-db-instance
    http://www.liquibase.org/manual/update"
  ([changelog-fn ^Integer howmany-changesets]
    (update-by-count changelog-fn howmany-changesets []))
  ([changelog-fn ^Integer howmany-changesets ^List contexts]
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
      (update-by-count changelog-fn howmany-changesets contexts))))


(defn tag
  "Tag the database schema with specified tag (coerced as string)."
  [the-tag]
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
                              (DbmsChangeSetFilter. *db-instance*)
                              ])]
          (.run changelog-it (RollbackVisitor. *db-instance*) *db-instance*)))))
  ([changelog-fn ^String tag ^List contexts ^Writer output]
    (.setContexts *changelog-params* contexts)
    (with-writer output
      (output-header (str "Rollback to '" tag "' Script"))
      (rollback-to-tag changelog-fn tag contexts))))


(defn rollback-to-date
  "Rollback schema to specified date.
  See also:
    liquibase.Liquibase/rollback
    http://www.liquibase.org/manual/rollback"
  ([changelog-fn ^Date date]
    (rollback-to-date changelog-fn date []))
  ([changelog-fn ^Date date ^List contexts]
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
                              (DbmsChangeSetFilter. *db-instance*)
                              ])]
          (.run changelog-it (RollbackVisitor. *db-instance*) *db-instance*)))))
  ([changelog-fn ^Date date contexts ^Writer output]
    (.setContexts *changelog-params* contexts)
    (with-writer output
      (output-header (str "Rollback to " date " Script"))
      (rollback-to-date changelog-fn date contexts))))


(defn rollback-by-count
  "Rollback schema by specified count of changes.
  See also:
    liquibase.Liquibase/rollback
    http://www.liquibase.org/manual/rollback"
  ([changelog-fn ^Integer howmany-changesets]
    (rollback-by-count changelog-fn ^Integer howmany-changesets []))
  ([changelog-fn ^Integer howmany-changesets ^List contexts]
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
                              (CountChangeSetFilter. howmany-changesets)
                              ])]
          (.run changelog-it (RollbackVisitor. *db-instance*) *db-instance*)))))
  ([changelog-fn ^Integer howmany-changesets ^List contexts ^Writer output]
    (.setContexts *changelog-params* contexts)
    (with-writer output
      (output-header (str "Rollback to " howmany-changesets " Change-sets Script"))
      (rollback-by-count changelog-fn howmany-changesets contexts))))
