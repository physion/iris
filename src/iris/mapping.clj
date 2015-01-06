(ns iris.mapping)

(def mapping {"Relation" {:user_id :owner}})

(defn get-mapping
  [type]
  (mapping type))

