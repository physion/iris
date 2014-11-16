(ns iris.test.couch
  (:require [midje.sweet :refer :all]
            [iris.couch :refer :all]
            [com.ashafa.clutch :as cl]))



(facts "About database creation"
  (fact "creates database when not checked"
    (let [db (database ...dbname...)]
      (check-db db) => ...meta...
      (provided
        (cl/get-database db) => ...meta...))))

(facts "About receipts"
       (fact "Retrieves receipt by [db,doc_id,doc_rev,hook_id]"
             (get-receipts ...db...  ...doc-id... ...doc-rev... ...hook-id...) => ...result...
             (provided
               (check-db anything) => true
               (cl/save-view anything iris-design-doc anything) => true
               (cl/get-view anything "iris"  :receipts {:include_docs false} {:key [...db... ...doc-id... ...doc-rev... ...hook-id...]}) => ...result...)))
