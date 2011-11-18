(ns org.bituf.clj-liquibase.sql-visitor
  (:import
    (java.util.regex       Pattern)
    (liquibase.sql.visitor SqlVisitor
                           AppendSqlVisitor PrependSqlVisitor
                           RegExpReplaceSqlVisitor ReplaceSqlVisitor))
  (:require [org.bituf.clj-miscutil :as mu]))


(defn visitor?
  "Return true if `v` is an SqlVisitor, false otherwise."
  [v]
  (instance? SqlVisitor v))


(defn ^AppendSqlVisitor make-append-visitor
  "Return visitor that appends `text` to generated SQL."
  [^String text] {:post [(visitor? %)]
                  :pre  [(mu/verify-arg (mu/not-nil? text))]}
  (doto (AppendSqlVisitor.)
    (.setValue (mu/as-string text))))


(defn ^PrependSqlVisitor make-prepend-visitor
  "Return visitor that prefixes generated SQL with `text`."
  [^String text] {:post [(visitor? %)]
                  :pre  [(mu/verify-arg (mu/not-nil? text))]}
  (doto (PrependSqlVisitor.)
    (.setValue (mu/as-string text))))


(defn ^SqlVisitor make-replace-visitor
  "Return visitor that replaces `needle` with `text` in generated SQL. Note that
  `needle` can be either string or regex (java.util.regex.Pattern instance.)"
  [needle ^String new-text] {:post [(visitor? %)]
                             :pre  [(mu/verify-arg (mu/not-nil? needle))
                                    (mu/verify-arg (mu/not-nil? new-text))]}
  (let [regex?  (instance? Pattern needle)
        replace (if regex? (.pattern ^Pattern needle) (mu/as-string needle))]
    (if regex?
      (doto (RegExpReplaceSqlVisitor.)
        (.setReplace ^String replace)
        (.setWith ^String (mu/as-string new-text)))
      (doto (ReplaceSqlVisitor.)
        (.setReplace ^String replace)
        (.setWith ^String (mu/as-string new-text))))))


(defn for-dbms!
  "Restrict a visitor (that applies to all DBMS by default) to specified DBMS
  list `dbms`."
  [dbms ^SqlVisitor visitor]
  (doto visitor
    (.setApplicableDbms (set (map mu/as-string (mu/as-vector dbms))))))


(defn apply-to-rollback!
  "Specify whether a visitor should be applied to rollbacks. By default a
  visitor is not applied to rollbacks."
  [apply? ^SqlVisitor visitor]
  (doto visitor
    (.setApplyToRollback apply?)))


(defn for-contexts!
  "Restrict visitor to specified `contexts` only. By default a visitor applies
  to all contexts."
  [contexts ^SqlVisitor visitor]
  (doto visitor
    (.setContexts (set (map mu/as-string (mu/as-vector contexts))))))


(defn make-visitors
  "Return a list of visitors from a DSL-like fluent list of arguments.
  Example:
    (make-visitors :include (map (partial for-dbms! :mysql)
                                 (make-visitors :append \"engine=InnoDB\"))
                   :append  \" -- creating table\n\"
                   :replace [:integer :bigint]
                   :replace {:string \"VARCHAR(256)\"
                             #\"varchar*\" \"VARCHAR2\"}
                   :prepend \"IF NOT EXIST\")"
  [k v & args] {:post [(coll? %)]
                :pre  [(mu/verify-arg (even? (count args)))]}
  (let [pairs (partition 2 (into [k v] args))
        makev (fn [k v]
                (case k
                  :include (mu/as-vector v)
                  :append  [(make-append-visitor  v)]
                  :prepend [(make-prepend-visitor v)]
                  :replace (if (map? v)
                             (map (fn [[ik iv]] (make-replace-visitor ik iv)) v)
                             (if (and (coll? v) (= 2 (count v)))
                               [(apply make-replace-visitor v)]
                               (mu/illegal-argval
                                 'v "list of 2 arguments (needle, new-text)"
                                 v)))
                  (mu/illegal-argval
                    'k ":include/:append/:prepend/:replace" k)))]
    (into [] (reduce concat (map (fn [[k v]] (makev k v)) pairs)))))