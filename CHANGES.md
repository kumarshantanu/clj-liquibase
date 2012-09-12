# Changes and TODO

- Move to Github (from Bitbucket)
- Move to Leiningen builds (from Maven)
- Move to Liquibase 2.0.5 (from 2.0.2)
- [TODO] Force user to specify _logical file-name_ (logical schema name)
- [TODO] Expose Liquibase 2.0.5 features
- [TODO] Improve documentation
- [TODO] Liquibase Functionality (commands)
  # Diff Changelog
  # Generate Changelog
- [TODO] Column type vars/inference functions
  # Database-independent columns: http://www.liquibase.org/manual/column
  # Infer from java.sql.Types instance (use liquibase.database.structure.Column)
  # By example: (example "Joe Backer") or (eg 269.8)
- [TODO] 'Change' implementations:
  # Custom Refactorings
    * Custom SQL
    * Custom SQL File
    * Custom Refactoring Class
    * Execute Shell Command


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
