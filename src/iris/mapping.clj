(ns iris.mapping)

(def mapping {"Relation" {;; from -> to
                          :user_id   :owner
                          :rel       :rel
                          :source_id :entity_id
                          :target_id :target_id}})

(defn get-mapping
  [type]
  (mapping type))

