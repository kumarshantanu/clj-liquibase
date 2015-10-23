(ns clj-liquibase.test-parser
  (:require
    [clj-liquibase.core      :as lb]
    [clj-liquibase.test-core :as tc])
  (:use clojure.test))


(lb/defparser p "example.edn")


(deftest test-parse-changelog
  (testing "parse-changelog"
    (is (lb/changelog? (lb/parse-changelog "example.edn"))))
  (testing "defparser"
    (is (lb/changelog? (p))))
  (testing "update on parsed changelog"
    (tc/lb-action
      #(do
         (tc/clb-setup)
         (lb/update p)))))
