(ns clj-liquibase.test-util
  (:require
    [clj-jdbcutil.core :as sp]
    [clj-miscutil.core :as mu])
  (:import
    (java.util List)
    (java.sql Connection Statement))
  (:use clojure.test))


(defn fail
  ([]
    (is false "Failed"))
  ([msg]
    (is false msg)))


(defn todo [] (is false "Not implemented yet"))


(defn ^List query
  [^String sql]
  (with-open [st ^Statement (.createStatement
                              ^Connection (:connection sp/*dbspec*))]
    (sp/row-seq (.executeQuery st sql))))


(defn query-value
  "Execute SQL query and return the first column value."
  [^String sql]
  (let [res (query sql)]
    ((first res))))


(defn as-vector
  [v]
  (if (coll? v) (vec v)
    [v]))