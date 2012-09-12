(ns test
  (:require
    [clojure.pprint         :as pp]
    [org.bituf.clj-miscutil :as mu]
    [clj-liquibase.core     :as lb]
    [clj-liquibase.change   :as ch]
    [org.bituf.clj-dbcp     :as dbcp]
    [org.bituf.clj-dbspec   :as spec]))


(def ds (dbcp/h2-memory-datasource))

;(def ds (dbcp/mysql-datasource "localhost" "bituf" "root" "root"))


(def dbspec (spec/make-dbspec ds))


(def ct-change1 (mu/! (ch/create-table "sampletable1"
                        [[:id     :int          :null false :pk true :autoinc true]
                         [:name   [:varchar 40] :null false]
                         [:gender [:char 1]     :null false]])))

(def ct-change2 (mu/! (ch/create-table "sampletable2"
                        [[:id     :int          :null false :pk true :autoinc true]
                         [:name   [:varchar 40] :null false]
                         [:gender [:char 1]     :null false]])))

(def changeset-1 ["id=1" "author=shantanu" [ct-change1]])


(def changeset-2 ["id=2" "author=shantanu" [ct-change2]])


(lb/defchangelog changelog-1 [changeset-1])

(lb/defchangelog changelog-2 [changeset-1 changeset-2])


;; ----- execute


(defn update-1
  []
  ((spec/wrap-dbspec dbspec
     (lb/wrap-lb-init (fn []
                        (lb/update changelog-1)
                        (lb/tag "tag1"))))))

(defn update-2
  []
  ((spec/wrap-dbspec dbspec
     (lb/wrap-lb-init (fn []
                        (lb/update changelog-2)
                        (lb/tag "tag2"))))))

(defn rollback-to-1
  []
  ((spec/wrap-dbspec dbspec
     (lb/wrap-lb-init (fn []
                        (lb/rollback-to-tag changelog-2 "tag1" []))))))


;(mu/! (update-1))

;(mu/! (update-2))

;(mu/! (rollback-to-1))
