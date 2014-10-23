(ns osiris.test.couch
  (:require [midje.sweet :refer :all]
            [osiris.couch :refer :all]
            [com.ashafa.clutch :as cl]))

(facts "About watched database state"
  (fact "gets state document for named database"
    (watched-state ...database...) => {:last-seq ...last...}
    (provided
      (couch-ready?) => true
      (cl/get-document anything ...database...) => {:_id "id",
                                                    :_rev "rev",
                                                    :last-seq ...last...}))
  (fact "sets state document for named database with existing state document"
    (set-watched-state! ...database... ...last...) => {:last-seq ...last...}
    (provided
      (couch-ready?) => true
      (cl/get-document anything ...database...) => {:_id "id",
                                                    :_rev "rev",
                                                    :last-seq ...other...}
      (cl/put-document anything (contains {:last-seq ...last...})) => {:_id "new-id"
                                               :_rev "rev-2"
                                               :last-seq ...last...}
      ))

  (fact "sets state document for named database without existing state document"
    (set-watched-state! ...database... ...last...) => {:last-seq ...last...}
    (provided
      (couch-ready?) => true
      (cl/get-document anything ...database...) => nil
      (cl/put-document anything (contains {:last-seq ...last...})) => {:_id "new-id"
                                                                       :_rev "rev-2"
                                                                       :last-seq ...last...}
      )))


(facts "About _changes feed"
  (fact "gets _changes since sequence number"
    (changes-since ...dbname... "seq") => ...changes...
    (provided
      (database ...dbname...) => ...db...
      (cl/changes ...db... :since "seq" :include_docs true) => ...changes...))
  (fact "gets _changes from sequence start"
    (changes-since ...dbname... nil) => ...all...
    (provided
      (database ...dbname...) => ...db...
      (cl/changes ...db... :include_docs true) => ...all...)))

(facts "About database creation"
  (fact "creates database when not checked"
    (ensure-db) => ...meta...
    (provided
      (couch-ready?) => false
      (cl/get-database anything) => ...meta...)))

(facts "About webhooks"
  (fact "Retrieves webhooks by [database,type]"
    (webhooks ...db... ...type...) => ...result...
    (provided
      (couch-ready?) => true
      (cl/save-view anything osiris-design-doc anything) => true
      (cl/get-view anything osiris-design-doc :webhooks {:include_docs true} {:key [...db... ...type...]}) => ...result...)))
