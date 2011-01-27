(ns org.bituf.test-util
  (:use clojure.test))


(defn fail
  ([]
    (is false "Failed"))
  ([msg]
    (is false msg)))


(defn todo [] (is false "Not implemented yet"))
