(ns org.bituf.test-util
  (:require
    [org.bituf.clj-miscutil :as mu])
  (:use clojure.test))


(defn fail
  ([]
    (is false "Failed"))
  ([msg]
    (is false msg)))


(defn todo [] (is false "Not implemented yet"))
