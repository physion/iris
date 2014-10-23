(ns iris.messages
  (:refer-clojure :exclude [send])
  (:require [org.httpkit.client :as http]
            [schema.core :as s]
            [iris.schema :refer [NewReceipt NewMessage]]
            [iris.couch :as couch]))


(s/defn send :- Receipt
  [msg :- NewMessage]
  (let [{doc-id  :doc_id
         doc-rev :doc_rev
         db-name :db
         hook-id :hook_id} msg

        doc (couch/get-document db-name doc-id :rev doc-rev)
        hook (couch/get-underworld-document hook-id)]

    {:type    "receipt"
     :db      db-name
     :doc_id  doc-id
     :doc_rev doc-rev
     :hook_id hook-id}))
