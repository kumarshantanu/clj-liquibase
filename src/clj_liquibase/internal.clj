(ns clj-liquibase.internal
  (:require
    [clojure.string    :as sr]
    [clj-jdbcutil.core :as sp]
    [clj-miscutil.core :as mu])
  (:import
    (liquibase.structure.core Column)
    (liquibase.change      ColumnConfig ConstraintsConfig)
    (liquibase.change.core LoadDataColumnConfig)
    (liquibase.statement   DatabaseFunction)
    (liquibase.util        ISODateFormat)
    (java.util Date)))


(defn dbfn?
  ^Boolean [x]
  (instance? DatabaseFunction x))


(defn as-coltype
  "Create column-type (string - subject to sp/db-iden).
  Examples:
    (coltype :int)        => \"INT\"
    (coltype \"BIGINT\")  => \"BIGINT\"
    (coltype :char 80)    => \"char(80)\"
    (coltype :float 17 5) => \"float(17, 5)\"
  Note: This function is called during constructing column-config.
  See also: http://www.liquibase.org/documentation/column"
  ^String [t & more]
  (str (sp/db-iden t)
    (if (mu/not-empty? more) (apply str "(" (mu/comma-sep-str more) ")"))))


(defmacro if-nn
  "Execute body if arg not nil"
  [arg & body]
  `(if (mu/not-nil? ~arg)
     (do ~@body)))


(defmacro set-column-value
  "Set column value for a container object.
  Arguments:
    cont  The container object (e.g. ColumnConfig)
    value The column value"
  [cont value]
  `(cond
     (string?     ~value) (.setValue         ~cont ~(with-meta value {:tag 'String}))
     (number?     ~value) (.setValueNumeric  ~cont ~(with-meta value {:tag 'Number}))
     (mu/boolean? ~value) (.setValueBoolean  ~cont ~(with-meta value {:tag 'Boolean}))
     (mu/date?    ~value) (.setValueDate     ~cont ~(with-meta value {:tag 'java.util.Date}))
     (dbfn?       ~value) (.setValueComputed ~cont ~(with-meta value {:tag 'DatabaseFunction}))
     :else (mu/illegal-arg "Bad column value: " ~value (type ~value)
             ", allowed types are: String, Number, Boolean, java.util.Date"
             ", liquibase.statement.DatabaseFunction (see 'dbfn' function)")))


(defn new-column-value
  "Return a ColumnConfig instance with column name and value set."
  [colname value]
  (let [col (ColumnConfig.)]
    (.setName col (sp/db-iden colname))
    (set-column-value col value)
    col))


(defn load-data-column-config
  "Return a new LoadDataColumnConfig instance from supplied arguments.
  Arguments:
    colname (keyword/String) column name
    coltype (keyword/String) either of STRING, NUMERIC, DATE, BOOLEAN
  Optional arguments:
    :index  (Number)
    :header (Keyword/String)
  See also:
    http://www.liquibase.org/documentation/changes/load_data"
  [colname coltype
   & {:keys [index  ; Integer
             header
             ]}]
  (let [s-coltype ^String (mu/as-string coltype)]
    (when-not (some #(.equalsIgnoreCase s-coltype %) ["STRING" "NUMERIC" "DATE" "BOOLEAN"])
      (mu/illegal-argval "coltype"
        "Either of \"STRING\", \"NUMERIC\", \"DATE\" or \"BOOLEAN\"" coltype))
    (let [ldcc (LoadDataColumnConfig.)]
      (doto ldcc
        (.setName (sp/db-iden colname))
        (.setType (sr/upper-case s-coltype)))
      (if index  (.setIndex  ldcc index))
      (if header (.setHeader ldcc header))
      ldcc)))


(defn- dbfn
  ^DatabaseFunction [^String value]
  (DatabaseFunction. value))


(defn- iso-date
  "Parse date from ISO-date-format string."
  ^java.util.Date [^String date-str]
  (.parse (ISODateFormat.) date-str))


(defmacro iso-date-str
  "Generate ISO-Date string from the following types:
  java.sql.Date
  java.sql.Time
  java.sql.Timestamp"
  ^String [date]
  `(let [idf# ^ISODateFormat (ISODateFormat.)
         sd# (cond
               (instance? java.sql.Date ~date)      (.format idf# ~(with-meta date {:tag 'java.sql.Date}))
               (instance? java.sql.Time ~date)      (.format idf# ~(with-meta date {:tag 'java.sql.Time}))
               (instance? java.sql.Timestamp ~date) (.format idf# ~(with-meta date {:tag 'java.sql.Timestamp}))
               :else (mu/illegal-arg
                       "Allowed types: java.sql.Date, java.sql.Time, java.sql.Timestamp"
                       " -- Found: " (class ~date)))]
     (str \" sd# \")))


(defn any-sqldate?
  [d]
  (or
    (instance? java.sql.Timestamp d)
    (instance? java.sql.Date d)
    (instance? java.sql.Time d)))


(defn sqldate
  [^Date d]
  (java.sql.Date.
    (.getTime d)))


(defmacro add-default-value
  "Add default value for a container object.
  (Meant for add-default-value change.)
  Arguments:
    cont    The container object
    default The default value"
  [cont default]
  `(cond
     (string?      ~default) (let [s-default#
                                   (str \" ~default \")]  (.setDefaultValue        ~cont s-default#))
     (number?      ~default) (let [n-default#
                                   (str ~default)]        (.setDefaultValueNumeric ~cont n-default#))
     (mu/boolean?  ~default) (.setDefaultValueBoolean
                               ~cont ~(with-meta default {:tag 'Boolean}))
     (mu/date?     ~default) (let [d-default#
                                   (iso-date-str
                                     (sqldate ~default))] (.setDefaultValueDate    ~cont d-default#))
     (any-sqldate? ~default) (let [d-default#
                                   (iso-date-str
                                     ~default)]           (.setDefaultValueDate    ~cont d-default#))
     (dbfn?        ~default) (.setDefaultValueComputed
                               ~cont ~(with-meta default {:tag 'DatabaseFunction}))
     :else (mu/illegal-arg "Bad default value: " ~default (type ~default)
             ", allowed types are: String, Number, Boolean"
             ", java.util.Date/java.sql.Date/java.sql.Time/java.sql.Timestamp"
             ", liquibase.statement.DatabaseFunction (see 'dbfn' function)")))


(defmacro set-default-value
  "Set default value for a container object.
  Arguments:
    cont    The container object
    default The default value"
  [cont default]
  `(cond
     (string?     ~default) (.setDefaultValue         ~cont ~(with-meta default {:tag 'String}))
     (number?     ~default) (.setDefaultValueNumeric  ~cont ~(with-meta default {:tag 'Number}))
     (mu/boolean? ~default) (.setDefaultValueBoolean  ~cont ~(with-meta default {:tag 'Boolean}))
     (mu/date?    ~default) (.setDefaultValueDate     ~cont ~(with-meta default {:tag 'java.util.Date}))
     (dbfn?       ~default) (.setDefaultValueComputed ~cont ~(with-meta default {:tag 'DatabaseFunction}))
     :else (mu/illegal-arg "Bad default value: " ~default (type ~default)
             ", allowed types are: String, Number, Boolean, java.util.Date"
             ", liquibase.statement.DatabaseFunction (see 'dbfn' function)")))


(defn as-column-config
  "Create column-configuration. This function is called by change/create-table.
  Arguments:
    colname  (String/Keyword) column name - subject to db-iden
    coltype  (String/Keyword/Vector) column type
  Optional arguments (can use either long/short name):
    Long name              |Short name |Allowed types
    -----------------------|-----------|------------------------
    :default-value         |:default   | String/Number/java.util.Date/Boolean/DatabaseFunction
    :auto-increment        |:autoinc   | Boolean
    :remarks               |           | String
    ;; constraints (s.t. = subject to)
    :nullable              |:null      | Boolean
    :primary-key           |:pk        | Boolean
    :primary-key-name      |:pkname    | String/Keyword - s.t. db-iden
    :primary-key-tablespace|:pktspace  | String/Keyword - s.t. db-iden
    :references            |:refs      | String (Foreign key definition)
    :unique                |:uniq      | Boolean
    :unique-constraint-name|:ucname    | String/Keyword - s.t. db-iden
    :check                 |           | String
    :delete-cascade        |:dcascade  | Boolean
    :foreign-key-name      |:fkname    | String/Keyword - s.t. db-iden
    :initially-deferred    |:idefer    | Boolean
    :deferrable            |:defer     | Boolean
  Examples (when used inside 'change/create-table'):
    [:id         :int           :null false :pk true :autoinc true]
    [:name       [:varchar 40]  :null false]
    [:gender     [:char 1]      :null false]
    [:birth-date :date          :null false]
  See also:
    as-coltype
    http://www.liquibase.org/documentation/column"
  ^ColumnConfig [colname coltype ; coltype (mixed) - keyword, string, vector (1st arg: db-iden)
   & {:keys [default-value          default  ; String/Number/Date/Boolean/DatabaseFunction
             auto-increment         autoinc  ; Boolean
             remarks                         ; String
             ;; constraints
             nullable               null     ; Boolean
             primary-key            pk       ; Boolean
             primary-key-name       pkname   ; String/Keyword - s.t. db-iden
             primary-key-tablespace pktspace ; String/Keyword - s.t. db-iden
             references             refs     ; String (Foreign key definition)
             unique                 uniq     ; Boolean
             unique-constraint-name ucname   ; String/Keyword - s.t. db-iden
             check                           ; String
             delete-cascade         dcascade ; Boolean
             foreign-key-name       fkname   ; String/Keyword - s.t. db-iden
             initially-deferred     idefer   ; Boolean
             deferrable             defer    ; Boolean
             ]}]
  (let [col (ColumnConfig.)
        con (ConstraintsConfig.)
        ;; optional column properties
        c-default  (or default-value          default )
        c-autoinc  (or auto-increment         autoinc )
        c-remarks   remarks
        ;; constraints
        c-null     (or nullable               null    )
        c-pk       (or primary-key            pk      )
        c-pkname   (or primary-key-name       pkname  )
        c-pktspace (or primary-key-tablespace pktspace)
        c-refs     (or references             refs    )
        c-uniq     (or unique                 uniq    )
        c-ucname   (or unique-constraint-name ucname  )
        c-check    check
        c-dcascade (or delete-cascade         dcascade)
        c-fkname   (or foreign-key-name       fkname  )
        c-idefer   (or initially-deferred     idefer  )
        c-defer    (or deferrable             defer   )]
    ;; set base column properties
    (doto col
      (.setName (sp/db-iden colname))
      (.setType (apply as-coltype (mu/as-vector coltype))))
    ;; set optional column properties
    (if-nn c-default (set-default-value col c-default))
    (if-nn c-autoinc (.setAutoIncrement col ^Boolean c-autoinc))
    (if-nn c-remarks (.setRemarks       col ^String  c-remarks))
    ;; set constraints
    (if-nn c-null     (.setNullable             con ^Boolean   c-null     ))
    (if-nn c-pk       (.setPrimaryKey           con ^Boolean   c-pk       ))
    (if-nn c-pkname   (.setPrimaryKeyName       con ^String  (sp/db-iden
                                                               c-pkname  )))
    (if-nn c-pktspace (.setPrimaryKeyTablespace con ^String  (sp/db-iden
                                                               c-pktspace)))
    (if-nn c-refs     (.setReferences           con ^String    c-refs     ))
    (if-nn c-uniq     (.setUnique               con ^Boolean   c-uniq     ))
    (if-nn c-ucname   (.setUniqueConstraintName con ^String  (sp/db-iden
                                                               c-ucname  )))
    (if-nn c-check    (.setCheckConstraint      con ^String    c-check    ))
    (if-nn c-dcascade (.setDeleteCascade        con ^Boolean   c-dcascade ))
    (if-nn c-fkname   (.setForeignKeyName       con ^String  (sp/db-iden
                                                               c-fkname  )))
    (if-nn c-idefer   (.setInitiallyDeferred    con ^Boolean   c-idefer   ))
    (if-nn c-defer    (.setDeferrable           con ^Boolean   c-defer    ))
    (.setConstraints col con)
    col))


(defn as-dbident-names
  "Return comma-separated name string for a given bunch of potentially
  Clojure-oriented names."
  ^String [names]
  (mu/comma-sep-str (map sp/db-iden (mu/as-vector names))))
