(ns iris.mapping)

(def mapping {"Relation" {:user_id :owner
                          :rel :rel
                          :source_id :entity_id}})

(defn get-mapping
  [type]
  (mapping type))

