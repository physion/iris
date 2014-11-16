(ns iris.messages
  (:refer-clojure :exclude [send])
  (:require [org.httpkit.client :as http]
            [iris.schema :refer [NewReceipt NewMessage Receipt]]
            [iris.couch :as couch]
            [ring.util.http-response :refer [throw! get-type]]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.tools.logging :as logging]))


(defn send
  [msg]
  (let [{doc-id  :doc_id
         doc-rev :doc_rev
         db-name :db
         hook-id :hook_id} msg

        doc (couch/get-document db-name doc-id :rev doc-rev)
        hook (couch/get-underworld-document hook-id)]

    (let [existing-receipt (couch/get-receipts db-name doc-id doc-rev hook-id)]

      (if (seq existing-receipt)
        (do
          (logging/info "Webhook " hook-id " already called")
          existing-receipt)
        (let [response (http/post (:url hook) {:body (json/write-str doc)})]
          (if (.success? (get-type (:status @response)))
            (let [receipt {:type    "receipt"
                           :db      db-name
                           :doc_id  doc-id
                           :doc_rev doc-rev
                           :hook_id hook-id}]
              (logging/info "Recording receipt: " receipt)
              (couch/put-underworld-document (assoc receipt :_id (str "receipt-" (java.util.UUID/randomUUID)))))
            (throw! @response)))))))
