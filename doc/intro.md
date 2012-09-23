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
a database change. However a _change_ cannot be tracked as it is; hence it must
be wrapped in a _changeset_ (instance of class `liquibase.changelog.ChangeSet`.)
A _changeset_ can contain several _change_ objects in the desired order, and is
marked with `id` and `author` attributes.

A _changelog_ (instance of the class `liquibase.changelog.DatabaseChangeLog`)
contains **all** changesets in the desired order meant for a database schema.
The database may actually be up to date with, or behind the _changelog_ at a
certain point of time. A Clj-Liquibase _command_ lets you apply the changelog
to the database in the intended way.

Important points to note:

* The _change_ and _changeset_ definition (in code) cannot be modified in any
  way once applied to the database.
* A _changelog_ definition (in code) cannot be modified after being applied to
  the database. However, you can add more _changeset_ objects to it later.


### _Change_

The _change_ objects can be constructed by using the factory functions in the
`clj-liquibase.change` namespace, which are described in the sub-sections below:

Note that you can use the following short names for corresponding keyword args:

| Keyword arg                   | Short name         | Value type            |
|-------------------------------|--------------------|-----------------------|
| `:schema-name`                | `:schema`          | string/keyword        |
| `:existing-table-schema-name` | `:existing-schema` | string/keyword        |
| `:new-table-schema-name`      | `:new-schema`      | string/keyword        |
| `:column-data-type`           | `:data-type`       | string/keyword/vector |
| `:new-column-data-type`       | `:new-data-type`   | string/keyword/vector |
| `:max-value`                  | `:max`             | number or string      |
| `:min-value`                  | `:min`             | number or string      |
| `:ordered`                    | `:ord`             | true or false         |
| `:table-space`                | `:tspace`          | string/keyword        |
| `:cascade-constraints`        | `:cascade`         | logical boolean       |
| `:replace-if-exists`          | `:replace`         | logical boolean       |
| `:default-null-value`         | `:default`         | string                |
| `:deferrable`                 | `:defer`           | logical boolean       |
| `:initially-deferred`         | `:idefer`          | logical boolean       |
| `:start-value`                | `:start`           | coerced as BigInteger |
| `:increment-by`               | `:incby`           | coerced as BigInteger |
| `:cycle`                      | `:cyc`             | logical boolean       |

base-table-schema-name       base-schema ; String
             referenced-table-schema-name ref-schema  ; String
             deferrable                   defer  ; Boolean
             initially-deferred           idefer ; Boolean
             on-delete                    ondel  ; String
             on-update                    onupd  ; String

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

TODO

#### Architectural Refactorings

TODO

### Constructing Changeset objects

TODO

### Defining Changelog

TODO

## Command Line Interface (CLI) integration

TODO

## Core functions

TODO
