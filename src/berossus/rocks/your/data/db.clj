(ns berossus.rocks.your.data.db
  (:require [datomic.api :as d]))

(defn ensure-db
  "Given a database URI, ensures the database exists"
  [db-uri]
  (d/create-database db-uri))

(defn gen-schema
  "Schema doc given an ident, value type, and maybe cardinality
   ident is generally :namespace/attribute-name such as :person/name
   Type can be :db.type/(string|boolean|long|bigint|double|bigdec|instant|uuid|uri|bytes|ref)
   Cardinality can be one or many, such as :db.cardinality/one or :db.cardinality/many"
   ;; :db/doc "A person's name"
   ;; Future: :db/noHistory, :db/unique, :db.unique/identity,
   ;; :db/isComponent, :db/index, :db/fulltext
  ([ident type]
     (gen-schema ident type :db.cardinality/one))
  ([ident type & [cardinality & rest]]
     (let [mapped (apply hash-map rest)]
       (merge {:db/id (d/tempid :db.part/db)
               :db/ident ident
               :db/valueType type
               :db/cardinality cardinality
               :db.install/_attribute :db.part/db}
              mapped))))
