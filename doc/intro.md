# Introduction to clj-liquibase

TODO: write [great documentation](http://jacobian.org/writing/great-documentation/what-to-write/)

[Liquibase](http://liquibase.org/) is an Open Source (Apache 2 license) database
change management library. Clj-Liquibase provides a way to write Liquibase
changesets and changelogs in Clojure, while inheriting other attributes of
Liquibase.

In order to work with Clj-Liquibase, you need to know the Liquibase abstractions
_change_, _changeset_ and _changelog_ objects, which constitute the changes that
can be tracked and applied to JDBC databases via various _commands_. These terms
are described in the sections below.

Supported commands:

* Update
* Tag
* Rollback
  * By tag
  * By date
  * By changeset count
* DB documentation
* Diff

## Understanding change, changeset, changelog

A _change_ (instance of class `liquibase.change.Change`) is the smallest unit of
a database change. A _change_ cannot be tracked as it is; hence it must be
wrapped in a _changeset_ (instance of class `liquibase.changelog.ChangeSet`.) A
_changeset_ can contain several _change_ objects in the desired order, and is
marked with `id` and `author` attributes.

A _changelog_ (instance of the class `liquibase.changelog.DatabaseChangeLog`)
contains **all** changesets in the desired order meant for a database schema.
The database may actually be up to date with, or behind the _changelog_ at a
certain point of time. A Clj-Liquibase _command_ lets you apply the changelog
to the database in the intended way.

Important points to note:

* The _change_ and _changeset_ definition (in code) cannot be modified once
  applied to the database.
* A _changelog_ definition (in code) cannot be modified after being applied to
  the database. However, you can add more _changeset_ objects to it later.


### Change

_Change_ objects can be constructed by using the factory functions in the
`clj-liquibase.change` namespace, which are described in the sub-sections below:


#### Structural Refactorings

| Function name             | Required args       | Optional kwargs        | Description |
|---------------------------|---------------------|------------------------|-------------|
| `add-columns`             | `table-name`        | `:schema-name`         | [Add columns to an existing table](http://www.liquibase.org/manual/add_column) |
|                           | `columns`           |                        | [Column definition](http://www.liquibase.org/manual/column) |
| `rename-column`           | `table-name`        | `:schema-name`         | [Rename column in an existing table](http://www.liquibase.org/manual/rename_column) |
|                           | `old-column-name`   | `:column-data-type`    ||
|                           | `new-column-name`   |||
| `modify-column`           | `table-name`        | `:schema-name`         | [Modify data type of a column in an existing table](http://www.liquibase.org/manual/modify_column) |
|                           | `column-name`       |                        ||
|                           | `new-data-type`     |||
| `drop-column`             | `table-name`        | `:schema-name`         | [Drop specified column from an existing table](http://www.liquibase.org/manual/drop_column) |
|                           | `column-name`       |                        ||
| `alter-sequence`          | `seq-name`          | `:schema-name`         | [Modifies a database sequence](http://www.liquibase.org/manual/alter_sequence) |
|                           | `increment-by`      | `:max-value`           ||
|                           |                     | `:min-value`           ||
|                           |                     | `:ordered`             ||
| `create-table`            | `table-name`        | `:schema-name`         | [Create a new table](http://www.liquibase.org/manual/create_table) |
|                           | `columns`           | `:table-space`         | [Column definition](http://www.liquibase.org/manual/column) |
|                           |                     | `:remarks`             ||
| `create-table-withid`     | same as above       | same as above          | Same as above, except it creates auto-incremented ID column |
|                           |                     | `:idcol`               ||
| `rename-table`            | `old-table-name`    | `:schema-name`         | [Rename an existing table](http://www.liquibase.org/manual/rename_table) |
|                           | `new-table-name`    |                        ||
| `drop-table`              | `table-name`        | `:schema-name`         | [Drop an existing table](http://www.liquibase.org/manual/drop_table) |
|                           |                     | `:cascade-constraints` ||
| `create-view`             | `view-name`         | `:schema-name`         | [Create a database view](http://www.liquibase.org/manual/create_view) |
|                           | `select-query`      | `:replace-if-exists`   ||
| `rename-view`             | `old-view-name`     | `:schema-name`         | [Rename an existing database view](http://www.liquibase.org/manual/rename_view) |
|                           | `new-view-name`     |                        ||
| `drop-view`               | `view-name`         | `:schema-name`         | [Drop an existing database view](http://www.liquibase.org/manual/drop_view) |
| `merge-columns`           | `table-name`        | `:schema-name`         | [Merge two columns of the same table into one](http://www.liquibase.org/manual/merge_columns) |
|                           | `column1-name`      |                        ||
|                           | `join-string`       |||
|                           | `column2-name`      |||
|                           | `final-column-name` |||
|                           | `final-column-type` |||
| `create-stored-procedure` | `procedure-body`    | `:comments`            | [Create database stored procedure](http://www.liquibase.org/manual/create_stored_procedure) |


##### Column config

The functions `add-columns`, `create-table` and `create-table-withid` accept a
`columns` argument, which is a collection of
[_column-config_](http://www.liquibase.org/manual/column) elements. Each
column-config is a vector of 2 required args followed by optional keyword args.

Required args: `column-name`, `column-type`
Optional kwargs:

| Long name                 | Short name  |Allowed types           |
|---------------------------|-------------|------------------------|
| `:default-value`          | `:default`  | String/Number/java.util.Date/Boolean/DatabaseFunction |
| `:auto-increment`         | `:autoinc   | Boolean        |
| `:remarks`                |             | String         |
| `:nullable`               | `:null`     | Boolean        |
| `:primary-key`            | `:pk`       | Boolean        |
| `:primary-key-name`       | `:pkname`   | String/Keyword |
| `:primary-key-tablespace` | `:pktspace` | String/Keyword |
| `:references`             | `:refs`     | String (Foreign key definition) |
| `:unique`                 | `:uniq`     | Boolean        |
| `:unique-constraint-name` | `:ucname`   | String/Keyword |
| `:check`                  |             | String         |
| `:delete-cascade`         | `:dcascade` | Boolean        |
| `:foreign-key-name`       | `:fkname`   | String/Keyword |
| `:initially-deferred`     | `:idefer`   | Boolean        |
| `:deferrable`             | `:defer`    | Boolean        |

##### Example

Example of creating a _change_ object:

```clojure
(clj-liquibase.change/create-table "sampletable1"
                                   [[:id     :int          :null false :pk true :autoinc true]
                                    [:name   [:varchar 40] :null false]
                                    [:gender [:char 1]     :null false]])
```


#### Data Quality Refactorings

| Function name              | Required args          | Optional kwargs               | Description |
|----------------------------|------------------------|-------------------------------|-------------|
| `add-lookup-table`         | `existing-table-name`  | `:existing-table-schema-name` | [Add a lookup table](http://www.liquibase.org/manual/add_lookup_table) |
|                            | `existing-column-name` | `:new-table-schema-name`      ||
|                            | `new-table-name`       | `:new-column-data-type`       ||
|                            | `new-column-name`      |||
|                            | `constraint-name`      |||
| `add-not-null-constraint`  | `table-name`           | `:schema-name`                | [Add NOT NULL constraint on specified column in a table](http://www.liquibase.org/manual/add_not-null_constraint) |
|                            | `column-name`          | `:default-null-value`         ||
|                            | `column-data-type`     |||
| `drop-not-null-constraint` | `table-name`           | `:schema-name`                | [Drop NOT NULL constraint for specified column](http://www.liquibase.org/manual/remove_not-null_constraint) |
|                            | `column-name`          | `:column-data-type`           ||
| `add-unique-constraint`    | `table-name`           | `:schema-name`                | [Add UNIQUE constraint for specified columns](http://www.liquibase.org/manual/add_unique_constraint) |
|                            | `column-names`         | `:table-space`                ||
|                            | `constraint-name`      | `:deferrable`                 ||
|                            |                        | `:initially-deferred`         ||
|                            |                        | `:disabled`                   ||
| `drop-unique-constraint`   | `table-name`           | `:schema-name`                | [Drop specified UNIQUE constraint](http://www.liquibase.org/manual/drop_unique_constraint) |
|                            | `constraint-name`      |||
| `create-sequence`          | `sequence-name`        | `:schema-name`                | [Create a database sequence](http://www.liquibase.org/manual/create_sequence) |
|                            |                        | `:start-value`                ||
|                            |                        | `:increment-by`               ||
|                            |                        | `:max-value`                  ||
|                            |                        | `:min-value`                  ||
|                            |                        | `:ordered`                    ||
|                            |                        | `:cycle`                      ||
| `drop-sequence`            | `sequence-name`        | `:schema-name`                | [Drop specified database sequence](http://www.liquibase.org/manual/drop_sequence) |
| `add-auto-increment`       | `table-name`           | `:schema-name`                | [Convert an existing column to auto-increment type](http://www.liquibase.org/manual/add_auto-increment) |
|                            | `column-name`          |||
|                            | `column-data-type`     |||
| `add-default-value`        | `table-name`           | `:schema-name`                | [Add default value for specified column](http://www.liquibase.org/manual/add_default_value) |
|                            | `column-name`          | `:column-data-type`           ||
|                            | `default-value`        |||
| `drop-default-value`       | `table-name`           | `:schema-name`                | [Drop default value for specified column](http://www.liquibase.org/manual/drop_default_value) |
|                            | `column-name`          | `:column-data-type`           ||


#### Referential Integrity Refactorings

| Function name                 | Required args             | Optional kwargs                 | Description |
|-------------------------------|---------------------------|---------------------------------|-------------|
| `add-foreign-key-constraint`  | `constraint-name`         | `:base-table-schema-name`       | [Add foreign key constraint to an existing column](http://www.liquibase.org/manual/add_foreign_key_constraint) |
|                               | `base-table-name`         | `:referenced-table-schema-name` ||
|                               | `base-column-names`       | `:deferrable`                   ||
|                               | `referenced-table-name`   | `:initially-deferred`           ||
|                               | `referenced-column-names` | `:on-delete`                    ||
|                               |                           | `:on-update`                    ||
| `drop-foreign-key-constraint` | `constraint-name`         | `:schema-name`                  | [Drop a foreign key constraint](http://www.liquibase.org/manual/drop_foreign_key_constraint) |
|                               | `base-table-name`         |||
| `add-primary-key`             | `table-name`              | `:schema-name`                  | [Add primary key from one or more columns](http://www.liquibase.org/manual/add_primary_key_constraint) |
|                               | `column-names`            | `:table-space`                  ||
|                               | `constraint-name`         |||
| `drop-primary-key`            | `table-name`              | `:schema-name`                  | [Drop an existing primary key](http://www.liquibase.org/manual/drop_primary_key_constraint) |
|                               |                           | `:constraint-name`              ||


#### Non-Refactoring Transformations

| Function name                 | Required args           | Type     | Optional kwargs                 | Description |
|-------------------------------|-------------------------|----------|---------------------------------|-------------|
| `insert-data`                 | `table-name`            | str/kw   | `:schema-name`                  | [Insert data into specified table](http://www.liquibase.org/manual/insert_data) |
|                               | `column-value-map`      | map      |||
| `load-data`                   | `table-name`            | str/kw   | `:schema-name`                  | [Load data from CSV file into specified table](http://www.liquibase.org/manual/load_data) |
|                               | `csv-filename`          | string   | `:encoding`                     ||
|                               | `columns-spec`          | coll/map |||
| `load-update-data`            | `table-name`            | str/kw   | `:schema-name`                  | [Load and save (insert/update) data from CSV file into specified table](http://www.liquibase.org/manual/load_update_data) |
|                               | `csv-filename`          | string   | `:encoding`                     ||
|                               | `primary-key-cols`      |          |                                 ||
|                               | `columns-spec`          | coll/map |                                 ||
| `update-data`                 | `table-name`            |          | `:schema-name`                  | [Update data in existing table](http://www.liquibase.org/manual/update_data) |
|                               | `column-name-value-map` |          | `:where-clause`                 ||
| `delete-data`                 | `table-name`            |          | `:schema-name`                  | [Delete data from specified table](http://www.liquibase.org/manual/delete_data) |
|                               |                         |          | `:where-clause`                 ||
| `tag-database`                | `tag`                   |          |                                 | [Tag the database with specified tag](http://www.liquibase.org/manual/tag_database) |
| `stop`                        |                         |          |                                 | [Stop Liquibase execution immediately, useful for debugging](http://www.liquibase.org/manual/stop) |

##### Columns config for loading data

Loading data from CSV files into the database requires translation rules. The
functions `load-data` and `load-update-data` accept an argument `columns-spec`
that is a collection of column-config elements. Every column-config is a
collection of 2 required arguments followed by optional keyword args:

Required arguments:

  * First element: `colname` (keyword/string)
  * Second element: `coltype` (either of "STRING", "NUMERIC", "DATE", "BOOLEAN")

Optional keyword args with corresponding values:

  `:index` (number)
  `:header` (Keyword/String)

#### Architectural Refactorings

| Function name                 | Required args           | Type       | Optional kwargs                 | Description |
|-------------------------------|-------------------------|------------|---------------------------------|-------------|
| `create-index`                | `table-name`            | stringable | `:schema-name`                  | [Create index with specified column names](http://www.liquibase.org/manual/create_index) |
|                               | `column-names`          | collection | `:index-name`                   ||
|                               |                         |            | `:unique`                       ||
|                               |                         |            | `:table-space`                  ||
| `drop-index`                  | `index-name`            | stringable | `:schema-name`                  | [Drop an existing index](http://www.liquibase.org/manual/drop_index) |
|                               | `table-name`            | stringable |||


#### Short names for keyword args

Note that you can use the following short names for corresponding keyword args:

| Keyword arg (long name)         | Short name         | Value type            | Default |
|---------------------------------|--------------------|-----------------------|---------|
| `:schema-name`                  | `:schema`          | string/keyword        ||
| `:existing-table-schema-name`   | `:existing-schema` | string/keyword        ||
| `:new-table-schema-name`        | `:new-schema`      | string/keyword        ||
| `:column-data-type`             | `:data-type`       | string/keyword/vector ||
| `:new-column-data-type`         | `:new-data-type`   | string/keyword/vector ||
| `:max-value`                    | `:max`             | number or string      ||
| `:min-value`                    | `:min`             | number or string      ||
| `:ordered`                      | `:ord`             | true or false         ||
| `:table-space`                  | `:tspace`          | string/keyword        ||
| `:cascade-constraints`          | `:cascade`         | logical boolean       ||
| `:replace-if-exists`            | `:replace`         | logical boolean       ||
| `:default-null-value`           | `:default`         | string                ||
| `:deferrable`                   | `:defer`           | logical boolean       ||
| `:initially-deferred`           | `:idefer`          | logical boolean       ||
| `:start-value`                  | `:start`           | coerced as BigInteger ||
| `:increment-by`                 | `:incby`           | coerced as BigInteger ||
| `:cycle`                        | `:cyc`             | logical boolean       ||
| `:encoding`                     | `:enc`             | string                | "UTF-8" |
| `:base-table-schema-name`       | `:base-schema`     | string                ||
| `:referenced-table-schema-name` | `:ref-schema`      | string                ||
| `:on-delete`                    | `:ondel`           | string                ||
| `:on-update`                    | `:onupd`           | string                ||
| `:where-clause`                 | `:where`           | string                ||
| `:index-name`                   | `:index`           | string                ||
| `:unique`                       | `:uniq`            | logical boolean       ||


### Constructing Changeset objects

A [_changeset_](http://www.liquibase.org/manual/changeset) can be constructed
using the function `clj-liquibase.core/make-changeset`.
Required args: `id` (string),  `author` (string), `changes` (collection of _change_ objects)
Optional kwargs:

| Long name             | Short name    | Type         |
|-----------------------|---------------|--------------|
| `:dbms`               |               | String/Keyword/vector-of-multiple |
| `:run-always`         | `:always`     | Boolean                           |
| `:run-on-change`      | `:on-change`  | Boolean                           |
| `:context`            | `:ctx`        | String                            |
| `:run-in-transaction` | `:in-txn`     | Boolean (true by default)         |
| `:fail-on-error`      | `:fail-err`   | Boolean                           |
| `:comment`            |               | String                            |
| `:pre-conditions`     | `:pre-cond`   | list of Precondition objects, or PreconditionContainer object |
| `:valid-checksum`     | `:valid-csum` | String                            |
| `:visitors`           |               | collection of SqlVisitor objects  |

An example changeset-construction look like this:

```clojure
;; assume `ch1` is a change object
(clj-liquibase.core/make-changeset "id=1" "author=shantanu" [ch1])
```

A shorter way to define a _changeset_ for use in a _changelog_ is to only store
the arguments in a vector -- `defchangelog` automatically creates a _changeset_
from the arguments in the vector:

```clojure
(def ch-set1 ["id=1" "author=shantanu" [ch1]])
```

The recommended way to create a changeset is to wrap only one _change_ object,
the main reason being pre-conditions and SQL-visitors can be applied only at the
changeset level. Since changesets cannot be modified after being applied to the
database, it would be impossible to go back and refactor the changesets.
However, one can add conditional SQL-visitor to a changeset later to modify the
generated SQL statement a little to suit a different database type.

#### Precondition

TODO

#### SQL Visitor

TODO

### Defining Changelog

A _changelog_ can be defined using the `defchangelog` macro, which essentially
defines a partially applied function such that when executed with no args it
returns a `liquibase.changelog.DatabaseChangeLog` object.

```clojure
(clj-liquibase.core/defchangelog changelog-name
  "logical-schema-name" [changeset-1 changeset-2 changeset-3])
```

Alternatively, you can also create a changelog using the factory function
`clj-liquibase.core/make-changelog`. The macro `defchangelog` returns a higher
order function that calls `make-changelog`.

The function `make-changelog` and (hence) the `defchangelog` macro accept an
optional keyword argument `:pre-conditions` (short name `:pre-cond`) to specify
the pre-condition checks for the changelog.

A changelog definition cannot be modified once applied to the database; however,
you can incrementally add changesets to a changelog as time goes.

## Command Line Interface (CLI) integration

The _Command-Line Interface_ is the easiest integration option for applications
that want to use Clj-Liquibase. The `clj-liquibase.cli` namespace has a built-in
command-line argument parser that knows about the CLI commands and their various
switches respectively.

An application simply needs to collect user-provided command line arguments and
invoke:

```clojure
(clj-liquibase.cli/entry cmd opts & args)
```

The `clj-liquibase.cli/entry` arguments are described below:

| Argument | Description |
|----------|-------------|
| `cmd`    | any of "help" "version" "update" "rollback" "tag" "dbdoc" "diff" |
| `opts`   | default options         |
| `args`   | user provided arguments |

The various switches for their respective commands are listed below:

| Command    | Required          | Optional     | Opt no-value | Description |
|------------|-------------------|--------------|--------------|-------------|
| `help`     |                   |              |              | Show help text |
| `version`  |                   |              |              | Show Clj-Liquibase version |
| `update`   | `:datasource`     | `:chs-count` | `:sql-only`  | [Update database to specified changelog](http://www.liquibase.org/manual/update) |
|            | `:changelog`      | `:contexts`  |||
| `rollback` | `:datasource`     | `:chs-count` | `:sql-only`  | [Rollback database to specified changeset-count/tag/ISO-date](http://www.liquibase.org/manual/rollback) |
|            | `:changelog`      | `:tag`       |||
|            |                   | `:date`      |||
|            |                   | `:contexts`  |||
| `tag`      | `:datasource`     |              |              | Tag the database on _ad hoc_ basis |
|            | `:tag`            ||||
| `dbdoc`    | `:datasource`     | `:contexts`  |              | [Generate database/changelog documentation](http://www.liquibase.org/manual/dbdoc) |
|            | `:changelog`      ||||
|            | `:output-dir`     ||||
| `diff`     | `:datasource`     |              |              | [Report difference between 2 database instances](http://www.liquibase.org/manual/diff) |
|            | `:ref-datasource` ||||

The switches listed above may either be provided as part of the `opts` map, or
as command-line arguments in `args` as follows:

| Switch            | Long-name example           | Short-name example |
|-------------------|-----------------------------|--------------------|
| `:changelog`      | `"--changelog=a.schema/cl"` | `"-c=a.schema/cl"` |
| `:chs-count`      | `"--chs-count=10"`          | `"-n10"`           |
| `:contexts`       | `"--contexts=foo,bar"`      | `"-tfoo,bar"`      |
| `:datasource`     | `"--datasource=foo.bar/ds"` | `"-dfoo.bar/ds"`   |
| `:date`           | `"--date=2012-09-16"`       | `"-e2012-09-16"`   |
| `:output-dir`     | `"--output-dir=target/doc"` | `"-otarget/doc"`   |
| `:ref-datasource` | `"--ref-datasource=foo/ds"` | `"-rfoo/ds"`       |
| `:sql-only`       | `"--sql-only"` (no value)   | `"-s"` (no value)  |
| `:tag`            | `"--tag=v0.1.0"`            | `"-gv0.1.0"`       |

Please note that `:datasource`, `:changelog` and `:ref-datasource` may point to
var names that would be resolved at runtime to obtain the corresponding values.

### Integrating in an app

The following example shows how to integrate an app with the Clj-Liquibase CLI:

```clojure
(ns foo.schema
  (:require
    [foo.globals :as globals]
    [clj-liquibase.change :as ch]
    [clj-liquibase.core   :as lb]
    [clj-liquibase.cli    :as cli]))

;; assuming that globals/ds is bound to a DataSource

(defchangelog ch-log "foo" [..change-sets..])

(defn -main
  [& [cmd & args]]
  (apply cli/entry cmd {:datasource globals/ds :changelog ch-log} args))
```


You can run this example as follows:

```bash
$ lein run -m foo.schema update
$ lein run -m foo.schema tag --tag=v0.1.0
```

## Core functions

The CLI commands shown above are implemented via corresponding functions in the
`clj-liquibase.core` namespace listed below:

* `update` `update-by-count`
* `tag`
* `rollback-to-tag` `rollback-to-date` `rollback-by-count`
* `generate-doc`
* `diff`

The core functions that implement the commands are supposed to be invoked in
a context where certain dynamic vars are bound to appropriate values. Feel
encouraged to inspect the source code in the namespace `clj-liquibase.core`.
