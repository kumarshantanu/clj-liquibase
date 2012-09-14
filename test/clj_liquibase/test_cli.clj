(ns clj-liquibase.test-cli
  (:require
    [clj-liquibase.cli :as ll])
  (:use [clojure.test]))


(deftest test-update-args
  (testing "update args"
    (let [a {:changelog "foo.db/default"}]
      (is (= a (ll/parse-update-args "--changelog=foo.db/default")) "--changelog")
      (is (= a (ll/parse-update-args "-cfoo.db/default"))           "-c"))
    (let [a {:chs-count "5" :changelog "x"}]
      (is (= a (ll/parse-update-args "--chs-count=5"  "-cx")) "--chs-count")
      (is (= a (ll/parse-update-args "-n5"            "-cx")) "-n"))
    (let [a {:contexts "a,b" :changelog "x"}]
      (is (= a (ll/parse-update-args "--contexts=a,b" "-cx")) "--contexts")
      (is (= a (ll/parse-update-args "-ta,b"          "-cx")) "-t"))
    (let [a {:sql-only nil :changelog "x"}]
      (is (= a (ll/parse-update-args "--sql-only"     "-cx")) "--sql-only")
      (is (= a (ll/parse-update-args "-s"             "-cx")) "-s"))
    (let [a {:changelog "foo.db/default"
             :chs-count "5"
             :contexts  "a,b"
             :sql-only  nil}]
      (is (= a (ll/parse-update-args
                 "--changelog=foo.db/default"
                 "--chs-count=5"
                 "--contexts=a,b"
                 "--sql-only"))      "all combined (full version)")
      (is (= a (ll/parse-update-args
                 "-cfoo.db/default"
                 "-n5"
                 "-ta,b"
                 "-s"))              "all combined (short version)")
      (is (thrown? IllegalArgumentException (ll/parse-update-args "--bad")))
      (is (= {:help nil} (ll/parse-update-args "--help"))))))


(deftest test-rollback-args
  (testing "rollback args"
    (let [a {:changelog "foo.db/default"}]
      (is (= a (ll/parse-rollback-args "--changelog=foo.db/default")) "--changelog")
      (is (= a (ll/parse-rollback-args "-cfoo.db/default"))           "-c"))
    (let [a {:chs-count "5" :changelog "x"}]
      (is (= a (ll/parse-rollback-args "--chs-count=5"     "-cx")) "--chs-count")
      (is (= a (ll/parse-rollback-args "-n5"               "-cx")) "-n"))
    (let [a {:tag "v2.0" :changelog "x"}]
      (is (= a (ll/parse-rollback-args "--tag=v2.0"        "-cx")) "--tag")
      (is (= a (ll/parse-rollback-args "-gv2.0"            "-cx")) "-g"))
    (let [a {:date "2011-02-26" :changelog "x"}]
      (is (= a (ll/parse-rollback-args "--date=2011-02-26" "-cx")) "--date")
      (is (= a (ll/parse-rollback-args "-d2011-02-26"      "-cx")) "-d"))
    (let [a {:contexts "a,b" :changelog "x"}]
      (is (= a (ll/parse-rollback-args "--contexts=a,b"    "-cx")) "--contexts")
      (is (= a (ll/parse-rollback-args "-ta,b"             "-cx")) "-t"))
    (let [a {:sql-only nil :changelog "x"}]
      (is (= a (ll/parse-rollback-args "--sql-only"        "-cx")) "--sql-only")
      (is (= a (ll/parse-rollback-args "-s"                "-cx")) "-s"))
    (let [a {:changelog "foo.db/default"
             :chs-count "5"
             :contexts  "a,b"
             :sql-only  nil}]
      (is (= a (ll/parse-rollback-args
                 "--changelog=foo.db/default"
                 "--chs-count=5"
                 "--contexts=a,b"
                 "--sql-only"))      "all combined (full version)")
      (is (= a (ll/parse-rollback-args
                 "-cfoo.db/default"
                 "-n5"
                 "-ta,b"
                 "-s"))              "all combined (short version)")
      (is (thrown? IllegalArgumentException (ll/parse-rollback-args "--bad")))
      (is (= {:help nil} (ll/parse-rollback-args "--help"))))))


(deftest test-tag-args
  (testing "rollback args"
    (let [a {:profile "pp" :tag "y"}]
      (is (= a (ll/parse-tag-args "--profile=pp"      "-gy")) "--profile")
      (is (= a (ll/parse-tag-args "-ppp"              "-gy")) "-p"))
    (let [a {:tag "v2.0"}]
      (is (= a (ll/parse-tag-args "--tag=v2.0")) "--tag")
      (is (= a (ll/parse-tag-args "-gv2.0"))     "-g"))
    (let [a {:profile   "pp"
             :tag       "foo"}]
      (is (= a (ll/parse-tag-args
                 "--profile=pp"
                 "--tag=foo"))      "all combined (full version)")
      (is (= a (ll/parse-tag-args
                 "-ppp"
                 "-gfoo"))          "all combined (short version)")
      (is (thrown? IllegalArgumentException (ll/parse-tag-args "--bad")))
      (is (= {:help nil} (ll/parse-tag-args "--help"))))))


(deftest test-dbdoc-args
  (testing "dbdoc args"
    (let [a {:profile "pp" :output-dir "y" :changelog "x"}]
      (is (= a (ll/parse-dbdoc-args "--profile=pp"      "-cx" "-oy")) "--profile")
      (is (= a (ll/parse-dbdoc-args "-ppp"              "-cx" "-oy")) "-p"))
    (let [a {:output-dir "y" :changelog "foo.db/default"}]
      (is (= a (ll/parse-dbdoc-args "--changelog=foo.db/default" "-oy")) "--changelog")
      (is (= a (ll/parse-dbdoc-args "-cfoo.db/default"           "-oy")) "-c"))
    (let [a {:output-dir "foo/bar" :changelog "x"}]
      (is (= a (ll/parse-dbdoc-args "--output-dir=foo/bar" "-cx")) "--output-dir")
      (is (= a (ll/parse-dbdoc-args "-ofoo/bar"            "-cx")) "-o"))
    (let [a {:profile    "pp"
             :changelog  "foo.db/default"
             :output-dir "foo"}]
      (is (= a (ll/parse-dbdoc-args
                 "--profile=pp"
                 "--changelog=foo.db/default"
                 "--output-dir=foo"))         "all combined (full version)")
      (is (= a (ll/parse-dbdoc-args
                 "-ppp"
                 "-cfoo.db/default"
                 "-ofoo"))                    "all combined (short version)")
      (is (thrown? IllegalArgumentException (ll/parse-dbdoc-args "--bad")))
      (is (= {:help nil} (ll/parse-dbdoc-args "--help"))))))


(deftest test-diff-args
  (testing "diff args"
           (is (= {:help nil} (ll/parse-diff-args "--help")))))
