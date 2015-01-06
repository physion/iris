(ns iris.messages
  (:refer-clojure :exclude [send])
  (:require [org.httpkit.client :as http]
            [iris.schema :refer [NewReceipt NewMessage Receipt]]
            [iris.couch :as couch]
            [ring.util.http-response :refer [throw! get-type]]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.tools.logging :as logging]
            [clojure.string :as s]
            [iris.mapping :as mapping]))

(defn map-replace [m text]
  (reduce
    (fn [acc [k v]] (s/replace acc (str k) (str v)))
    text m))

(defn substitute-url
  [url doc]
  (map-replace doc url))

(defn substitute-map
  [doc sub]
  (into {} (map (fn [[fromkey tokey]]
                  [tokey (doc fromkey)]) sub)))

(defn map-doc
  "Take mapping from hook, if present. Otherwise, take it from mapping, if present"
  [doc hook]

  (if-let [hook-substitution (:mapping hook)]
    (substitute-map doc hook-substitution)
    (if-let [substitution (mapping/get-mapping (:type doc))]
      (substitute-map doc substitution)
      doc)))

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
        (let [raw-url (:url hook)
              mapped (map-doc doc hook)
              url (substitute-url raw-url mapped)
              response (http/post url {:body (json/write-str mapped)})]
          (if (.success? (get-type (:status @response)))
            (let [receipt {:type    "receipt"
                           :db      db-name
                           :doc_id  doc-id
                           :doc_rev doc-rev
                           :hook_id hook-id}]
              (logging/info "Recording receipt: " receipt)
              (couch/put-underworld-document (assoc receipt :_id (str "receipt-" (java.util.UUID/randomUUID)))))
            (throw! @response)))))))
