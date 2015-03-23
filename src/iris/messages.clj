(ns iris.messages
  (:refer-clojure :exclude [send])
  (:require [org.httpkit.client :as http]
            [iris.schema :refer [NewReceipt NewMessage Receipt]]
            [iris.couch :as couch]
            [ring.util.http-response :refer [throw!]]
            [ring.util.http-predicates :refer [created? accepted? ok? no-content?]]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.tools.logging :as logging]
            [clojure.string :as s]
            [iris.mapping :as mapping]
            [iris.logging])
  (:import (java.util UUID)))

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

(defn call-http
  "Call an HTTP method with a body to URL and record receipt or throw response exception"
  [method url body doc-id doc-rev db-name hook-id api-key]
  (let [opts {:body (json/write-str body)
              :headers {"Content-Type" "application/json"
                        "Accept" "application/json"}}
        auth-opts (if (nil? api-key) opts (assoc opts :basic-auth [api-key api-key]))
        response (method url auth-opts)]

    (if (or (ok? @response) (created? @response) (accepted? @response) (no-content? @response))
      (let [receipt {:type    "receipt"
                     :db      db-name
                     :doc_id  doc-id
                     :doc_rev doc-rev
                     :hook_id hook-id}]
        (logging/info "Recording receipt: " receipt)
        (couch/put-underworld-document (assoc receipt :_id (str "receipt-" (UUID/randomUUID)))))
      (throw! @response))))

(defn check-filter
  "Check a set of filters against doc"
  [filter-list doc]

  (if (or (nil? filter) (empty? filter-list))
    true
    (not (some nil? (map (fn [[k r]] (when ((keyword k) doc) (re-matches (re-pattern r) ((keyword k) doc)))) filter-list)))))

(defn send
  [msg]
  (let [{doc-id  :doc_id
         doc-rev :doc_rev
         db-name :db
         hook-id :hook_id} msg
        doc (couch/get-document db-name doc-id doc-rev)
        hook (couch/get-underworld-document hook-id)]

    (let [existing-receipt (couch/get-receipts doc-id doc-rev hook-id)]

      (if (seq existing-receipt)
        (do
          (logging/info "Webhook " hook-id " already called")
          existing-receipt)
        (let [raw-url (:url hook)
              mapped (map-doc doc hook)
              url (substitute-url raw-url mapped)]

          (logging/debug "Checking filter for " mapped)
          (if (check-filter (:filter hook) mapped)
            (let [method (if (:deleted msg) http/delete http/post)]
              (logging/info "Calling" url "for" mapped)
              (call-http method url mapped doc-id doc-rev db-name hook-id (:api_key hook)))
            "No matching webhooks"                                              ;; Empty result
            ))))))
