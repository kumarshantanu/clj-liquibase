(ns org.bituf.clj-liquibase.internal
  (:import
    (liquibase.database.structure Column)
    (liquibase.change      ColumnConfig ConstraintsConfig)
    (liquibase.change.core LoadDataColumnConfig)
    (liquibase.statement   DatabaseFunction)
    (liquibase.util        ISODateFormat)
    (java.util Date))
  (:require
    [clojure.string         :as sr]
    [org.bituf.clj-miscutil :as mu]
    [org.bituf.clj-dbspec   :as sp]))


(defn ^Boolean dbfn?
  [x]
  (instance? DatabaseFunction x))


(defn ^String as-coltype
  "Create column-type (string - subject to sp/clj-to-dbident).
  Examples:
    (coltype :int)        => \"INT\"
    (coltype \"BIGINT\")  => \"BIGINT\"
    (coltype :char 80)    => \"char(80)\"
    (coltype :float 17 5) => \"float(17, 5)\"
  Note: This function is called during constructing column-config.
  See also: http://www.liquibase.org/manual/column"
  [t & more]
  (str (sp/clj-to-dbident t)
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
    (.setName col (sp/clj-to-dbident colname))
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
    http://www.liquibase.org/manual/load_data"
  [colname coltype
   & {:keys [index  ; Integer
             header
             ]}]
  (let [s-coltype ^String (mu/as-string coltype)]
    (when-not (some #(.equalsIgnoreCase s-coltype %) ["STRING" "NUMERIC" "DATE" "BOOLEAN"])
      (mu/illegal-arg-value "coltype"
        "Either of \"STRING\", \"NUMERIC\", \"DATE\" or \"BOOLEAN\"" coltype))
    (let [ldcc (LoadDataColumnConfig.)]
      (doto ldcc
        (.setName (sp/clj-to-dbident colname))
        (.setType (sr/upper-case s-coltype)))
      (if index  (.setIndex  ldcc index))
      (if header (.setHeader ldcc header))
      ldcc)))


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


(defn ^ColumnConfig as-column-config
  "Create column-configuration. This function is called by change/create-table.
  Arguments:
    colname  (String/Keyword) column name - subject to clj-to-dbident
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
    :primary-key-name      |:pkname    | String/Keyword - s.t. clj-to-dbident
    :primary-key-tablespace|:pktspace  | String/Keyword - s.t. clj-to-dbident
    :references            |:refs      | String (Foreign key definition)
    :unique                |:uniq      | Boolean
    :unique-constraint-name|:ucname    | String/Keyword - s.t. clj-to-dbident
    :check                 |           | String
    :delete-cascade        |:dcascade  | Boolean
    :foreign-key-name      |:fkname    | String/Keyword - s.t. clj-to-dbident
    :initially-deferred    |:idefer    | Boolean
    :deferrable            |:defer     | Boolean
  Examples (when used inside 'change/create-table'):
    [:id         :int           :null false :pk true :autoinc true]
    [:name       [:varchar 40]  :null false]
    [:gender     [:char 1]      :null false]
    [:birth-date :date          :null false]
  See also:
    as-coltype
    http://www.liquibase.org/manual/column"
  [colname coltype ; coltype (mixed) - keyword, string, vector (1st arg: clj-to-dbident)
   & {:keys [default-value          default  ; String/Number/Date/Boolean/DatabaseFunction
             auto-increment         autoinc  ; Boolean
             remarks                         ; String
             ;; constraints
             nullable               null     ; Boolean
             primary-key            pk       ; Boolean
             primary-key-name       pkname   ; String/Keyword - s.t. clj-to-dbident
             primary-key-tablespace pktspace ; String/Keyword - s.t. clj-to-dbident
             references             refs     ; String (Foreign key definition)
             unique                 uniq     ; Boolean
             unique-constraint-name ucname   ; String/Keyword - s.t. clj-to-dbident
             check                           ; String
             delete-cascade         dcascade ; Boolean
             foreign-key-name       fkname   ; String/Keyword - s.t. clj-to-dbident
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
        c-defer    (or deferrable             defer   )
        ]
    ;; set base column properties
    (doto col
      (.setName (sp/clj-to-dbident colname))
      (.setType (apply as-coltype (mu/as-vector coltype))))
    ;; set optional column properties
    (if-nn c-default (set-default-value col c-default))
    (if-nn c-autoinc (.setAutoIncrement col ^Boolean c-autoinc))
    (if-nn c-remarks (.setRemarks       col ^String  c-remarks))
    ;; set constraints
    (if-nn c-null     (.setNullable             con ^Boolean   c-null     ))
    (if-nn c-pk       (.setPrimaryKey           con ^Boolean   c-pk       ))
    (if-nn c-pkname   (.setPrimaryKeyName       con ^String  (sp/clj-to-dbident
                                                               c-pkname  )))
    (if-nn c-pktspace (.setPrimaryKeyTablespace con ^String  (sp/clj-to-dbident
                                                               c-pktspace)))
    (if-nn c-refs     (.setReferences           con ^String    c-refs     ))
    (if-nn c-uniq     (.setUnique               con ^Boolean   c-uniq     ))
    (if-nn c-ucname   (.setUniqueConstraintName con ^String  (sp/clj-to-dbident
                                                               c-ucname  )))
    (if-nn c-check    (.setCheck                con ^String    c-check    ))
    (if-nn c-dcascade (.setDeleteCascade        con ^Boolean   c-dcascade ))
    (if-nn c-fkname   (.setForeignKeyName       con ^String  (sp/clj-to-dbident
                                                               c-fkname  )))
    (if-nn c-idefer   (.setInitiallyDeferred    con ^Boolean   c-idefer   ))
    (if-nn c-defer    (.setDeferrable           con ^Boolean   c-defer    ))
    (.setConstraints col con)
    col))


(defn ^String as-dbident-names
  "Return comma-separated name string for a given bunch of potentially
  Clojure-oriented names."
  [names]
  (mu/comma-sep-str (map sp/clj-to-dbident (mu/as-vector names))))
