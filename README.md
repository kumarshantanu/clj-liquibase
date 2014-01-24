# clj-liquibase

Clj-Liquibase is a Clojure wrapper for [Liquibase](http://www.liquibase.org/)
for database change management and migration.

Supported actions:

* update
* tag
* rollback
* generate SQL for actions
* generate DB doc
* database diff


## Usage

On Clojars: https://clojars.org/clj-liquibase

Leiningen dependency: `[clj-liquibase "0.5.0"]`


### Quickstart

Create a new project e.g. `fooapp` using [Leiningen](http://leiningen.org/) and
include the following dependencies in `project.clj`:

```clojure
[clj-dbcp      "0.8.1"]  ; to create connection-pooling DataSource
[clj-liquibase "0.4.0"]  ; for this library
[oss-jdbc      "0.8.0"]  ; for Open Source JDBC drivers
```

Create a Clojure source file for managing the DB schema. Include the required
namespaces define the _change_, _changeset_ and _changelog_ objects:

```clojure
(ns fooapp.dbschema
  (:require
    [clj-dbcp.core        :as cp]
    [clj-liquibase.change :as ch]
    [clj-liquibase.cli    :as cli])
  (:use
    [clj-liquibase.core :only (defchangelog)]))

;; define the changes, changesets and the changelog

(def ct-change1 (ch/create-table :sample-table1
                  [[:id     :int          :null false :pk true :autoinc true]
                   [:name   [:varchar 40] :null false]
                   [:gender [:char 1]     :null false]]))

; recommended: one change per changeset
(def changeset-1 ["id=1" "author=shantanu" [ct-change1]])


; you can add more changesets later to the changelog
(defchangelog app-changelog "fooapp" [changeset-1])


;; keep the DataSource handy and invoke the CLI

(def ds (cp/make-datasource :mysql {:host "localhost" :database "people"
                                    :user "dbuser"    :password "s3cr3t"}))

(defn -main
  [& [cmd & args]]
  (apply cli/entry cmd {:datasource ds :changelog  app-changelog}
         args))

```

After defining the changelog, you need to apply the changes:

```bash
lein run -m fooapp.dbschema help
lein run -m fooapp.dbschema update
```

### Documentation

For more documentation please refer the file `doc/intro.md` in this repo.

## Contributors

* Shantanu Kumar (author)
* [Jonathan Rojas](https://github.com/john-roj87)

## License

Copyright © 2012-2014 Shantanu Kumar

Distributed under the Eclipse Public License, the same as Clojure.
