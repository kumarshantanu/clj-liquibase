# clj-liquibase

Clj-Liquibase is a Clojure wrapper for Liquibase for
database change management and migrations.

Supported actions:

* update
* tag
* rollback
* generate SQL for actions
* generate DB doc
* database diff


## Usage

On Clojars: [http://clojars.org/org.bituf/clj-liquibase](http://clojars.org/org.bituf/clj-liquibase)

Leiningen dependency: `[org.bituf/clj-quibase "0.3"]`

### Quickstart

Create a new project and include the following dependencies:

```clojure
[org.bituf/clj-dbcp "0.5"]
[org.bituf/oss-jdbc "0.5"]
[org.bituf/clj-liquibase "0.3"]
```

Include the required namespace in your application and define a changelog:

```clojure
;; filename: fooapp/src/fooapp/dbchange.clj
(ns fooapp.dbchange
  (:require
    [org.bituf.clj-liquibase :as lb]
    [org.bituf.clj-liquibase.change :as ch]))
    
(def ct-change1 (ch/create-table :sample-table1
                  [[:id     :int          :null false :pk true :autoinc true]
                   [:name   [:varchar 40] :null false]
                   [:gender [:char 1]     :null false]]))
    
(def changeset-1 ["id=1" "author=shantanu" [ct-change1]])
    
(lb/defchangelog changelog [changeset-1])
```

After defining the changelog, you need to apply the changes:

```clojure
;; filename: fooapp/src/fooapp/dbmigrate.clj
(ns fooapp.dbmigrate
  (:require
    [fooapp.dbchange         :as dbch]
    [org.bituf.clj-dbcp      :as dbcp]
    [org.bituf.clj-dbspec    :as spec]
    [org.bituf.clj-liquibase :as lb]))
    
;; define datasource for supported database using Clj-DBCP
(def ds (dbcp/mysql-datasource "localhost" "dbname" "user" "pass"))
    
(defn do-lb-action "Wrap f using DBSpec middleware and execute it"
  [f]
  (let [g (spec/wrap-dbspec (spec/make-dbspec ds)
            (lb/wrap-lb-init f))]
            (g)))
    
(defn do-update "Invoke this function to update the database"
  []
  (do-lb-action #(lb/update dbch/changelog)))
```


Once you are done with these, you can invoke `fooapp.dbmigrate/do-update` to
carry out the changes.


## License

Copyright © 2012 Shantanu Kumar

Distributed under the Eclipse Public License, the same as Clojure.
