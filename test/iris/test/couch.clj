(ns iris.test.couch
  (:require [midje.sweet :refer :all]
            [iris.couch :refer :all]
            [com.ashafa.clutch :as cl]))



(facts "About database creation"
  (fact "creates database when not checked"
    (let [db (database ...dbname...)]
      (check-db db) => ...meta...
      (provided
        (couch-ready?) => false
        (update-view!) => true
        (cl/get-database db) => ...meta...))))

(facts "About receipts"
       (fact "Retrieves receipt by [doc_id,doc_rev,hook_id]"
             (get-receipts ...doc-id... ...doc-rev... ...hook-id...) => ...result...
             (provided
               (couch-ready?) => true
               (cl/get-view anything "iris"  :receipts {:include_docs false :key [...doc-id... ...doc-rev... ...hook-id...]}) => ...result...)))
