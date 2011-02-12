(ns org.bituf.test-util
  (:import
    (java.util List)
    (java.sql Connection Statement))
  (:require
    [org.bituf.clj-miscutil :as mu]
    [org.bituf.clj-dbspec   :as sp])
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
