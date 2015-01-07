(ns iris.couch
  (:require [com.ashafa.clutch :as cl]
            [iris.config :as config]
            [clojure.tools.logging :as logging]))


(defn database
  "Constructs a database URL for the given database name."
  [db-name & {:keys [username password]
              :or {username config/COUCH_USER
                   password config/COUCH_PASSWORD}}]

  (assoc (cemerick.url/url config/COUCH_HOST db-name)
    :username username
    :password password))

(defonce ^{:private true} db (atom (database config/COUCH_DATABASE)))


(def iris-design-doc "iris")

(defn check-db
  "Creates db (a cemerick.url/url) if it doesn't exist already." ;; TODO eventually, this should become a macro wrapper with-db
  [database]
  (logging/debug "Checking database" (dissoc database :username :password))
  (cl/get-database database))


(defn receipts-view!
  []
  (check-db @db)
  (logging/debug "Creating webhooks view")
  (cl/save-view @db iris-design-doc
    (cl/view-server-fns :javascript
      {:receipts {:map
                  "function(doc) {
                    if(doc.type && doc.type==='receipt') {
                      emit([doc.doc_id, doc.doc_rev, doc.hook_id], null);
                    }
                  }"}})))

(defn get-document
  [db-name doc-id &{:keys [rev]}]
  (if rev
    (cl/get-document (database db-name) doc-id {:rev rev})
    (cl/get-document (database db-name) doc-id)))

(defn put-underworld-document
  [doc]
  (cl/put-document @db doc))


(defn get-underworld-document
  [doc-id]
  (cl/get-document @db doc-id))

(defn get-receipts
  "Checks if a receipt is present for a webhook call"
  [doc-id doc-rev hook-id]
  (receipts-view!)
  (cl/get-view @db iris-design-doc :receipts {:include_docs false} {:key [doc-id doc-rev hook-id]}))
