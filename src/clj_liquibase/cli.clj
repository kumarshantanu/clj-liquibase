(ns clj-liquibase.cli
  (:require
    [clojure.java.io :as io]
    [clojure.string  :as sr]
    [clojure.pprint  :as pp]
    [clj-miscutil.core    :as mu]
    [org.bituf.clj-dbspec :as sp]
    [clj-liquibase.core   :as lb]
    [clj-liquibase.change :as ch])
  (:import
    (java.util.regex Pattern)
    (javax.sql       DataSource)))


(defn help
  []
  (println "The following commands are available
help       - Shows this help screen
version    - Shows version information
update     - Updates the database
rollback   - Rolls back database
tag        - Tags the database
dbdoc      - Generates documentation for database/changelogs
diff       - Reports differences between two database instances

For help on individual command, append with `--help`, e.g.:
<command> update --help
"))


(defn ^String as-string
  [s]
  (if (keyword? s) (name s)
    (str s)))


(defn opt?
  [^String s] {:pre [(string? s)]}
  (some #(re-matches % s) [(re-pattern "--.+")
                           (re-pattern "-.+")]))


(defn opt-string
  ([^String elem] {:post [(string? %)]
                   :pre  [(string? elem)]}
    (format (if (> (count elem) 1)
              "--%s"
              "-%s")
      elem))
  ([^String elem ^String value]
    (format (if (> (count elem) 1)
              "--%s=%s"
              "-%s%s")
      elem value)))


(defn opt-pattern
  [^String elem] {:post [(instance? Pattern %)]
                  :pre  [(string? elem)]}
  (re-pattern (opt-string elem "(.*)")))


(defn opt-match-value
  "Return option value
  Example:
    (opt-match-value (opt-pattern \"foo\") \"--foo=bar\")
    => returns \"bar\"
  See also: opt-pattern"
  [^Pattern re ^String arg]
  (second (re-matches re arg)))


(defn noarg-pattern
  [^String elem] {:pre [(string? elem)]}
  (-> (if (> (count elem) 1) "--%s" "-%s")
    (format elem)
    re-pattern))


(def arg-types #{:with-arg :opt-arg :no-arg})


(defn print-usage
  "Print command usage"
  [cmd-prefix spec]
  (println "Usage: " cmd-prefix "<options>\n")
  (mu/print-table
    ["Option" "Must" "Description"]
    (map (fn [row]
           (let [[desc opt-type & keywds] row
                  takes-arg (contains? #{:with-arg :opt-arg} opt-type)
                  ks (map #(if takes-arg
                             (opt-string (as-string %) "<Val>")
                             (opt-string (as-string %)))
                       keywds)]
              [(mu/comma-sep-str ks)
               (if (= :with-arg opt-type) "Yes" "...")
               desc]))
      spec))
  (println))


(defn assert-spec
  "Assert spec as a spec"
  [spec-coll]
  (assert (or (nil? spec-coll) (coll? spec-coll)))
  (doseq [each spec-coll]
    (assert (coll? each))
    (let [[docstr argtype] each]
      (assert (string? docstr))
      (assert (arg-types argtype))))
  true)


(defn parse-opts
  "Spec can be:
    [[docstring :opt-arg :datasource :d]
     [docstring :no-arg  :sql-only   :s]
     [docstring :with-arg :a]]
  `args` is a collection of argument bodies:
    \"--foo=bar\" \"-fbar\" \"--simulate\" \"-s\"
  Note: Evaluated every time"
  [opts cmd-prefix args & spec]
  {:post [(map? %)]
   :pre  [(map? opts)
          (assert-spec spec)]}
  (let [spec-opts  (map #(map as-string (drop 2 %)) spec)
        rev-opts   (->> spec-opts
                     (map (fn [opt-row]
                            (let [sentinel (keyword (first opt-row))]
                              (map #(array-map % sentinel) opt-row))))
                     flatten
                     (reduce into {}))
        with-arg (map (partial drop 2) (filter #(= (second %) :with-arg) spec))
        opt-arg  (map (partial drop 2) (filter #(= (second %) :opt-arg)  spec))
        no-arg   (map (partial drop 2) (filter #(= (second %) :no-arg)   spec))
        ;; fn to convert arg into map entries
        get-opts (fn [acc arg] {:post [(map? %)]
                                :pre  [(map? acc)
                                       (string? arg)]}
                   (or
                     ;; with-arg and opt-arg
                     (some (fn [row]
                             (some #(let [v (-> (as-string %)
                                              opt-pattern
                                              (opt-match-value arg))]
                                      (and v
                                        (into acc
                                          {(get rev-opts (as-string %)) v})))
                               row))
                       (into with-arg opt-arg))
                     ;; no-arg
                     (some (fn [row]
                             (some (fn [opt]
                                     (if (opt? arg)
                                       (if (re-matches (noarg-pattern
                                                         (as-string opt)) arg)
                                         (into acc
                                           {(get rev-opts (as-string opt)) nil}))
                                       (if (contains? acc :more)
                                         {:more [arg]})))
                               row))
                       no-arg)
                     ;; special or bad args
                     (if (some #(= arg %) ["--help" "-h" "/?"])
                       (do (print-usage cmd-prefix spec)
                         {:help nil})
                       (into acc {:more (cons arg (:more acc))}))
                     (throw (IllegalArgumentException.
                              (str "Illegal option: " arg)))))]
    (let [opt-map   (reduce get-opts {} args)
          with-arg? (fn []
                      (-> (fn [row]
                            (or (contains? opts (first row))
                                (-> (fn [opt]
                                      (-> #(re-matches
                                             (opt-pattern (as-string opt))
                                             (as-string %))
                                        (some args)))
                                  (some row))))
                        (every? with-arg)))]
      (cond
        ;; ignore validations if help was sought
        (contains?
          opt-map :help)  opt-map
        ;; ensure that `with-arg` options are supplied
        (not (with-arg?)) (let [optfn #(let [x (as-string %)]
                                         (if (> (count x) 1)
                                           (str "--" x) (str "-" x)))
                                optsr #(format "Either of %s\n"
                                         (mu/comma-sep-str (map optfn %)))]
                            (throw (IllegalArgumentException.
                                     (str "Must supply the following:\n"
                                       (apply str (map optsr with-arg))))))
        :else             (merge opts opt-map)))))


(defn resolve-var
  "Given a qualified/un-qualified var name (string), resolve and return value.
  Throw NullPointerException if var cannot be resolved."
  [^String var-name] {:pre [(string? var-name)]}
  @(let [tokens (sr/split var-name #"/")
         var-ns (first tokens)]
     (when (and (> (count tokens) 1)
             (not (find-ns (symbol var-ns))))
       (require (symbol var-ns)))
     (resolve (symbol var-name))))


(defn opt-value
  [k opt & opts] {:pre [(map? opt)
                        (every? #(or (map? %) (nil? %)) opts)]}
  (-> #(when (contains? % k)
         [(get % k)])
    (some (concat [opt] opts))
    first))


(defn opt-datasource
  [opt & opts]
  (when-let [ds (apply opt-value :datasource opt opts)]
    (cond (string? ds) (resolve-var ds)
          (symbol? ds) (resolve-var (name ds))
          :otherwise   ds)))


(defn ctx-list
  "Generate context list from a given comma-separated context list (string)"
  [contexts] {:post [(vector? %)]
              :pre  [(or (nil? contexts)
                       (string? contexts))]}
  (if contexts
    (sr/split contexts #",")
    []))


(defn parse-update-args
  [opts & args]
  (parse-opts opts "update"
    args
    ["JDBC Datasource"                        :with-arg :datasource :d]
    ["Changelog var name to apply update on"  :with-arg :changelog  :c]
    ["How many Changesets to apply update on" :opt-arg  :chs-count  :n]
    ["Contexts (comma separated)"             :opt-arg  :contexts   :t]
    ["Only generate SQL, do not commit"       :no-arg   :sql-only   :s]))


(defn update
  [opts & args] {:pre [(map? opts)]}
  (let [opt (apply parse-update-args opts args)]
    (when-not (contains? opt :help)
      (let [changelog  (resolve-var (:changelog opt))
            chs-count  (:chs-count opt)
            contexts   (:contexts  opt)
            sql-only   (contains? opt :sql-only)
            datasource (opt-datasource opts opt)]
        (sp/with-dbspec {:datasource datasource}
          (lb/with-lb
            (if chs-count
              (let [chs-num (Integer/parseInt chs-count)]
                (if sql-only
                  (lb/update-by-count changelog chs-num (ctx-list contexts) *out*)
                  (lb/update-by-count changelog chs-num (ctx-list contexts))))
              (if sql-only
                (lb/update changelog (ctx-list contexts) *out*)
                (lb/update changelog (ctx-list contexts))))))))))


(defn parse-rollback-args
  [opts  & args]
  (parse-opts opts "rollback"
    args
    ["JDBC Datasource"                           :with-arg :datasource :d]
    ["Changelog var name to apply rollback on"   :with-arg :changelog  :c]
    ["How many Changesets to rollback"           :opt-arg  :chs-count  :n]
    ["Which tag to rollback to"                  :opt-arg  :tag        :g]
    ["Rollback ISO-date (yyyy-MM-dd'T'HH:mm:ss)" :opt-arg  :date       :d]
    ["Contexts (comma separated)"                :opt-arg  :contexts   :t]
    ["Only generate SQL, do not commit"          :no-arg   :sql-only   :s]))


(defn rollback
  [opts & args]
  (let [opt (apply parse-rollback-args opts args)]
    (when-not (contains? opt :help)
      (let [changelog  (resolve-var (:changelog opt))
            chs-count  (:chs-count opt)
            tag        (:tag       opt)
            date       (:date      opt)
            c-t-d      [chs-count tag date] ; either of 3 is required
            contexts   (:contexts  opt)
            sql-only   (contains? opt :sql-only)
            datasource (opt-datasource opts opt)]
        (when (not (= 1 (count (filter identity c-t-d))))
          (throw
            (IllegalArgumentException.
              (format
                "Expected only either of --chs-count/-n, --tag/-g and --date/-d
arguments, but found %s"
                (with-out-str (pp/pprint args))))))
        (sp/with-dbspec {:datasource datasource}
          (lb/with-lb
            (cond
              chs-count (let [chs-num (Integer/parseInt chs-count)]
                          (if sql-only
                            (lb/rollback-by-count changelog chs-num (ctx-list contexts) *out*)
                            (lb/rollback-by-count changelog chs-num (ctx-list contexts))))
              tag       (if sql-only
                          (lb/rollback-to-tag changelog tag (ctx-list contexts) *out*)
                          (lb/rollback-to-tag changelog tag (ctx-list contexts)))
              date      (if sql-only
                          (lb/rollback-to-date changelog (ch/iso-date date) (ctx-list contexts) *out*)
                          (lb/rollback-to-date changelog (ch/iso-date date) (ctx-list contexts)))
              :else     (throw
                          (IllegalStateException.
                            (format
                              "Neither of changeset-count, tag and date found to
roll back to: %s"
                              (with-out-str (pp/pprint args))))))))))))


(defn parse-tag-args
  [opts  & args]
  (parse-opts opts  "tag"
    args
    ["JDBC Datasource"   :with-arg :datasource     :d]
    ["Tag name to apply" :with-arg :tag        :g]))


(defn tag
  "Tag the database manually (recommended: create a Change object of type tag)"
  [opts  & args]
  (let [opt (apply parse-tag-args opts args)]
    (when-not (contains? opt :help)
      (let [tag        (:tag opt)
            datasource (opt-datasource opts opt)]
        (sp/with-dbspec {:datasource datasource}
          (lb/with-lb
            (lb/tag tag)))))))


(defn parse-dbdoc-args
  "Parse arguments for `dbdoc` command."
  [opts & args]
  (parse-opts opts "dbdoc"
    args
    ["JDBC Datasource"                             :with-arg :datasource :d]
    ["Changelog var name to apply tag on"          :with-arg :changelog  :c]
    ["Output directory to generate doc files into" :with-arg :output-dir :o]
    ["Contexts (comma separated)"                  :opt-arg  :contexts   :t]))


(defn dbdoc
  "Generate database/changelog documentation"
  [opts & args]
  (let [opt (apply parse-dbdoc-args opts args)]
    (when-not (contains? opt :help)
      (let [changelog  (resolve-var (:changelog opt))
            out-dir    (:output-dir opt)
            contexts   (:contexts   opt)
            datasource (opt-datasource opts opt)]
        (sp/with-dbspec {:datasource datasource}
          (lb/with-lb
            (lb/generate-doc changelog out-dir (ctx-list contexts))))))))


(defn parse-diff-args
  "Parse arguments for `diff` command."
  [opts & args]
  (parse-opts opts "diff"
    args
    ["JDBC Datasource"           :with-arg :datasource     :d]
    ["Reference JDBC Datasource" :with-arg :ref-datasource :r]))


(defn opt-ref-datasource
  [opt & opts]
  (when-let [ds (apply opt-value :ref-datasource opt opts)]
    (cond (string? ds) (resolve-var ds)
          (symbol? ds) (resolve-var (name ds))
          :otherwise   ds)))


(defn diff
  "Report differences between two database instances"
  [opts & args]
  (let [opt (apply parse-diff-args opts args)]
    (when-not (contains? opt :help)
      (let [ref-datasource (opt-ref-datasource opts opt)]
        ;; begin with reference DB profile
        (sp/with-dbspec
          {:datasource ref-datasource}
          (sp/with-connection
            sp/*dbspec*
            (let [ref-db     (lb/make-db-instance (:connection sp/*dbspec*))
                  datasource (opt-datasource opts opt)]
              ;; go on to target DB profile
              (sp/with-dbspec {:datasource datasource}
                              (lb/with-lb
                                (lb/diff ref-db))))))))))


(defn call*
  "Invoke f after parsing/normalizing args"
  [opts args f ks] {:pre [(map? opts)]}
  (let []
    (mu/! (apply f opts (-> (fn [[k v]]
                              (opt-string (as-string k) v))
                          (map (select-keys opts ks))
                          (into args))))))


(defn entry
  "Entry point for clj-liquibase CLI"
  [opts & [cmd & args]]
  (let [argc (count args)
        call (partial call* opts args)]
    ;; check for commands
    (case cmd
      nil          (help)
      ""           (help)
      "help"       (help)
      "version"    (println (format "clj-liquibase version %s"
                              (apply str (interpose "." lb/version))))
      "update"     (call update   [:datasource :changelog :chs-count :contexts :sql-only])
      "rollback"   (call rollback [:datasource :changelog :chs-count :tag :date :contexts :sql-only])
      "tag"        (call tag      [:datasource :tag])
      "dbdoc"      (call dbdoc    [:datasource :changelog :output-dir :contexts])
      "diff"       (call diff     [:datasource :ref-datasource])
      (do
        (println (format "Invalid command: %s" cmd))
        (help)))))
