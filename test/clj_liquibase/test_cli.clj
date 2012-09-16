(ns clj-liquibase.test-cli
  (:require
    [clj-liquibase.cli :as ll])
  (:use [clojure.test]))


(deftest test-update-args
  (testing "update args"
    (let [p {}
          a {:datasource "foo.db/ds" :changelog "foo.db/cl"}]
      (is (= a (ll/parse-update-args p "--datasource=foo.db/ds" "-cfoo.db/cl")) "--datasource")
      (is (= a (ll/parse-update-args p "-dfoo.db/ds"            "-cfoo.db/cl")) "-d"))
    (let [p {:datasource :foo}
          a {:changelog "foo.db/default" :datasource :foo}]
      (is (= a (ll/parse-update-args p "--changelog=foo.db/default")) "--changelog")
      (is (= a (ll/parse-update-args p "-cfoo.db/default"))           "-c"))
    (let [p {:datasource :foo}
          a {:chs-count "5" :changelog "x" :datasource :foo}]
      (is (= a (ll/parse-update-args p "--chs-count=5"  "-cx")) "--chs-count")
      (is (= a (ll/parse-update-args p "-n5"            "-cx")) "-n"))
    (let [p {:datasource :foo}
          a {:contexts "a,b" :changelog "x" :datasource :foo}]
      (is (= a (ll/parse-update-args p "--contexts=a,b" "-cx")) "--contexts")
      (is (= a (ll/parse-update-args p "-ta,b"          "-cx")) "-t"))
    (let [p {:datasource :foo}
          a {:sql-only nil :changelog "x" :datasource :foo}]
      (is (= a (ll/parse-update-args p "--sql-only"     "-cx")) "--sql-only")
      (is (= a (ll/parse-update-args p "-s"             "-cx")) "-s"))
    (let [p {:datasource :foo}
          a {:changelog  "foo.db/default"
             :chs-count  "5"
             :contexts   "a,b"
             :sql-only   nil
             :datasource :foo}]
      (is (= a (ll/parse-update-args p
                 "--changelog=foo.db/default"
                 "--chs-count=5"
                 "--contexts=a,b"
                 "--sql-only"))      "all combined (full version)")
      (is (= a (ll/parse-update-args p
                 "-cfoo.db/default"
                 "-n5"
                 "-ta,b"
                 "-s"))              "all combined (short version)")
      (is (thrown? IllegalArgumentException (ll/parse-update-args p "--bad")))
      (is (= {:help nil} (ll/parse-update-args p "--help"))))))


(deftest test-rollback-args
  (testing "rollback args"
    (let [p {:datasource :foo}
          a {:changelog "foo.db/default" :datasource :foo}]
      (is (= a (ll/parse-rollback-args p "--changelog=foo.db/default")) "--changelog")
      (is (= a (ll/parse-rollback-args p "-cfoo.db/default"))           "-c"))
    (let [p {:datasource :foo}
          a {:chs-count "5" :changelog "x" :datasource :foo}]
      (is (= a (ll/parse-rollback-args p "--chs-count=5"     "-cx")) "--chs-count")
      (is (= a (ll/parse-rollback-args p "-n5"               "-cx")) "-n"))
    (let [p {:datasource :foo}
          a {:tag "v2.0" :changelog "x" :datasource :foo}]
      (is (= a (ll/parse-rollback-args p "--tag=v2.0"        "-cx")) "--tag")
      (is (= a (ll/parse-rollback-args p "-gv2.0"            "-cx")) "-g"))
    (let [p {:datasource :foo}
          a {:date "2011-02-26" :changelog "x" :datasource :foo}]
      (is (= a (ll/parse-rollback-args p "--date=2011-02-26" "-cx")) "--date")
      (is (= a (ll/parse-rollback-args p "-d2011-02-26"      "-cx")) "-d"))
    (let [p {:datasource :foo}
          a {:contexts "a,b" :changelog "x" :datasource :foo}]
      (is (= a (ll/parse-rollback-args p "--contexts=a,b"    "-cx")) "--contexts")
      (is (= a (ll/parse-rollback-args p "-ta,b"             "-cx")) "-t"))
    (let [p {:datasource :foo}
          a {:sql-only nil :changelog "x" :datasource :foo}]
      (is (= a (ll/parse-rollback-args p "--sql-only"        "-cx")) "--sql-only")
      (is (= a (ll/parse-rollback-args p "-s"                "-cx")) "-s"))
    (let [p {:datasource :foo}
          a {:changelog  "foo.db/default"
             :chs-count  "5"
             :contexts   "a,b"
             :sql-only   nil
             :datasource :foo}]
      (is (= a (ll/parse-rollback-args p
                 "--changelog=foo.db/default"
                 "--chs-count=5"
                 "--contexts=a,b"
                 "--sql-only"))      "all combined (full version)")
      (is (= a (ll/parse-rollback-args p
                 "-cfoo.db/default"
                 "-n5"
                 "-ta,b"
                 "-s"))              "all combined (short version)")
      (is (thrown? IllegalArgumentException (ll/parse-rollback-args p "--bad")))
      (is (= {:help nil} (ll/parse-rollback-args p "--help"))))))


(deftest test-tag-args
  (testing "rollback args"
    (let [p {}
          a {:tag "y" :datasource "foo/bar"}]
      (is (= a (ll/parse-tag-args p "--datasource=foo/bar" "-gy")) "--datasource")
      (is (= a (ll/parse-tag-args p "-dfoo/bar"            "-gy")) "-d"))
    (let [p {:datasource :foo}
          a {:tag "v2.0" :datasource :foo}]
      (is (= a (ll/parse-tag-args p "--tag=v2.0")) "--tag")
      (is (= a (ll/parse-tag-args p "-gv2.0"))     "-g"))
    (let [p {:datasource :foo}
          a {:tag        "foo"
             :datasource :foo}]
      (is (= a (ll/parse-tag-args p
                 "--tag=foo"))      "all combined (full version)")
      (is (= a (ll/parse-tag-args p
                 "-gfoo"))          "all combined (short version)")
      (is (thrown? IllegalArgumentException (ll/parse-tag-args p "--bad")))
      (is (= {:help nil} (ll/parse-tag-args p "--help"))))))


(deftest test-dbdoc-args
  (testing "dbdoc args"
    (let [p {}
          a {:datasource "foo/bar" :output-dir "y" :changelog "x"}]
      (is (= a (ll/parse-dbdoc-args p "--datasource=foo/bar" "-cx" "-oy")) "--datasource")
      (is (= a (ll/parse-dbdoc-args p "-dfoo/bar"            "-cx" "-oy")) "-d"))
    (let [p {:datasource :foo}
          a {:output-dir "y" :changelog "foo.db/default" :datasource :foo}]
      (is (= a (ll/parse-dbdoc-args p "--changelog=foo.db/default" "-oy")) "--changelog")
      (is (= a (ll/parse-dbdoc-args p "-cfoo.db/default"           "-oy")) "-c"))
    (let [p {:datasource :foo}
          a {:output-dir "foo/bar" :changelog "x" :datasource :foo}]
      (is (= a (ll/parse-dbdoc-args p "--output-dir=foo/bar" "-cx")) "--output-dir")
      (is (= a (ll/parse-dbdoc-args p "-ofoo/bar"            "-cx")) "-o"))
    (let [p {:datasource :foo}
          a {:datasource :foo
             :changelog  "foo.db/default"
             :output-dir "foo"}]
      (is (= a (ll/parse-dbdoc-args p
                 "--changelog=foo.db/default"
                 "--output-dir=foo"))         "all combined (full version)")
      (is (= a (ll/parse-dbdoc-args p
                 "-cfoo.db/default"
                 "-ofoo"))                    "all combined (short version)")
      (is (thrown? IllegalArgumentException (ll/parse-dbdoc-args p "--bad")))
      (is (= {:help nil} (ll/parse-dbdoc-args p "--help"))))))


(deftest test-diff-args
  (testing "diff args"
    (let [p {}
          a {:datasource     "foo/bar"
             :ref-datasource "bar/baz"}]
      (is (= a (ll/parse-diff-args p "--datasource=foo/bar" "-rbar/baz")) "--datasource")
      (is (= a (ll/parse-diff-args p "-dfoo/bar"            "-rbar/baz")) "-d")
      (is (= a (ll/parse-diff-args p "-dfoo/bar" "--ref-datasource=bar/baz")) "--ref-datasource")
      (is (= a (ll/parse-diff-args p "-dfoo/bar" "-rbar/baz"))                "-r"))
    (let [p {:datasource :foo}
          a {:datasource "bar" :ref-datasource "baz"}]
      (is (= a (ll/parse-diff-args p "-dbar" "-rbaz")) "override defaults via CLI arg"))
    (is (= {:help nil} (ll/parse-diff-args {} "--help")))))


(defn test-ns-hook
  []
  (test-update-args)
  (test-rollback-args)
  (test-tag-args)
  (test-dbdoc-args)
  (test-diff-args))