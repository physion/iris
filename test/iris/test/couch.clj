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

;(facts "About webhooks"
;  (fact "Retrieves webhooks by [database,type]"
;    (webhooks ...db... ...type...) => ...result...
;    (provided
;      (couch-ready?) => true
;      (cl/save-view anything iris-design-doc anything) => true
;      (cl/get-view anything iris-design-doc :webhooks {:include_docs true} {:key [...db... ...type...]}) => ...result...)))
