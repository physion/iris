(ns iris.messages
  (:refer-clojure :exclude [send])
  (:require [org.httpkit.client :as http]
            [iris.schema :refer [NewReceipt NewMessage Receipt]]
            [iris.couch :as couch]
            [ring.util.http-response :refer [throw! get-type]]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]))


(defn send
  [msg]
  (let [{doc-id  :doc_id
         doc-rev :doc_rev
         db-name :db
         hook-id :hook_id} msg

        doc (couch/get-document db-name doc-id :rev doc-rev)
        hook (couch/get-underworld-document hook-id)]

    (let [response (http/post (:url doc) {:body (json/write-str doc)})]

      (if (.success? (get-type (:status @response)))

        {:type    "receipt"
         :db      db-name
         :doc_id  doc-id
         :doc_rev doc-rev
         :hook_id hook-id}
        (throw! @response)))))
