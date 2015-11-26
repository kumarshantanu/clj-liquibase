# Changes and TODO

* [TODO] Liquibase Functionality (commands)
  * Diff Changelog
  * Generate Changelog (i.e. reverse-engineer DB as a changelog)
* [TODO] Column type vars/inference functions
  * Database-independent columns: http://www.liquibase.org/manual/column
  * Infer from java.sql.Types instance (use liquibase.database.structure.Column)
  * By example: (example "Joe Backer") or (eg 269.8)
* [TODO] 'Change' implementations:
  * Custom Refactorings
    * Custom Refactoring Class
    * Execute Shell Command


## 0.6.0 / 2015-Nov-26

* Support for parsing changelog files
* Include EDN changelog parser
* Deprecate changelog/changeset DSL API


## 0.5.3 / 2015-Aug-04

* Exclude `clojure.core/update` to avoid shadow warning in Clojure 1.7 (by Jake McCrary, @jakemcc)
* Fix type hints
* Drop support for Clojure 1.2


## 0.5.2 / 2014-Sep-04

* Make CLI command accessible (by Christopher Mark Gore, @cgore)


## 0.5.1 / 2014-Jan-28

* Custom Refactorings
  * SQL File


## 0.5.0 / 2014-Jan-24

* Use Liquibase 3.0.8 (by Jonathan Rojas, @john-roj87)
* Custom Refactorings
  * Custom/Raw SQL (by Jonathan Rojas, @john-roj87)
* Upgrade test dependencies
  * clj-dbcp from 0.8.0 to 0.8.1


## 0.4.0 / 2012-Sep-25

* Move to Github (from Bitbucket)
* Move to Leiningen build (from Maven)
* Move to Liquibase 2.0.5 (from 2.0.2)
* Introduce required argument _logical-schema_ in `defchangelog`
* Support for CLI integration (pulled from Lein-LB)
* Upgrade dependencies
  * clj-miscutil from 0.3 to 0.4.1
  * Drop clj-dbspec, use clj-jdbcutil 0.1.0
* Upgrade test dependencies
  * OSS-JDBC from 0.5 to 0.8.0
  * clj-dbcp from 0.5 to 0.8.0
* Improve documentation


## 0.3 / 2011-Nov-20

* Use Clj-DBSpec 0.3
* Liquibase Functionality (commands)
  # Diff (Regular database diff - output to STDOUT)
* `make-changeset` now accepts SQL-visitors as :visitors optional argument
  # defaults `create-table` changes to InnoDB for MySQL unless overridden
* 'Change' implementations
  # Create table: `create-table-withid` (updated)
    * Allow user to specify ID column name via an optional argument :idcol
  # Custom Refactorings (new implementation)
    * Modifying Generated SQL (Append, Prepend, Replace SQL visitors)


## 0.2 / 2011-Apr-01

- Use Clj-DBSpec 0.2
- Remove dependency on Clojure-contrib
- Argument verification in functions/macros - action, change etc.
- Pre-conditions
- 'Change' implementations
  # `create-table` variant that auto-includes a bigint (auto-incr) primary key
- Liquibase actions
  # Generate SQL statements for individual Change instances
  # DBDoc
  # SQL Output


## 0.1 / 2011-Mar-06

- Liquibase Functionality (commands)
  # Update
  # Tagging
  # Rollback
- Dynamic vars - DataSource/Connection, schema(name), etc.
- Clojuresque schema/table/column names, data types, attributes and constraints
- Building Change-Logs
  # <DatabaseChangeLog>
  # <ChangeSet>
  # Contexts
  # ChangeLog Parameters
- Liquibase 'Change' implementations:
  # Structural Refactorings
    * Add Column
    * Rename Column
    * Modify Column
    * Drop Column
    * Alter Sequence
    * Create Table
    * Rename Table
    * Drop Table
    * Create View
    * Rename View
    * Drop View
    * Merge Columns
    * Create Stored Procedure
  # Data Quality Refactorings
    * Add Lookup Table
    * Add Not-Null Constraint
    * Remove Not-Null Constraint
    * Add Unique Constraint
    * Drop Unique Constraint
    * Create Sequence
    * Drop Sequence
    * Add Auto-Increment
    * Add Default Value
    * Drop Default Value
  # Referential Integrity Refactorings
    * Add Foreign Key Constraint
    * Drop Foreign Key Constraint
    * Drop All Foreign Key Constraints
    * Add Primary Key Constraint
    * Drop Primary Key Constraint
  # Non-Refactoring Transformations
    * Insert Data
    * Load Data
    * Load Update Data
    * Update Data
    * Delete Data
    * Tag Database
    * Stop
  # Architectural Refactorings
    * Create Index
    * Drop Index
