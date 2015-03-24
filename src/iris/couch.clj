(ns iris.couch
  (:require [com.ashafa.clutch :as cl]
            [iris.config :as config]
            [clojure.tools.logging :as logging]
            [iris.logging]))


(defn database
  "Constructs a database URL for the given database name."
  [db-name & {:keys [username password]
              :or {username config/COUCH_USER
                   password config/COUCH_PASSWORD}}]

  (assoc (cemerick.url/url config/COUCH_HOST db-name)
    :username username
    :password password))

(defonce ^{:private true} db (atom (database config/COUCH_DATABASE)))
(defonce ^{:private true} token (atom false))

(defn couch-ready?
  "True if the defined db has been checked/created"
  []
  @token)

(def iris-design-doc "iris")
(def view-fns (cl/view-server-fns :javascript
                                  {:receipts {:map
                                              "function(doc) {
                                                if(doc.type && doc.type==='receipt') {
                                                  emit([doc.doc_id, doc.doc_rev, doc.hook_id], null);
                                                }
                                              }"}}))

(defn update-view! []
  (cl/save-view @db iris-design-doc view-fns))

(defn check-db
  "Creates db (a cemerick.url/url) if it doesn't exist already." ;; TODO eventually, this should become a macro wrapper with-db
  [database]
  (when (not (couch-ready?))
    (do
      (logging/debug "Checking database" (dissoc database :username :password))
      (let [meta (cl/get-database database)
            view (update-view!)]
        (swap! token not)
        (and view meta)))))


(defn receipts-view!
  []
  (when (not (couch-ready?))
    (do
      (check-db @db)
      (logging/debug "Creating webhooks view")
      (update-view!))))

(defn get-document
  ([db-name doc-id]
    (cl/get-document (database db-name) doc-id))
  ([db-name doc-id rev]
    (cl/get-document (database db-name) doc-id :rev rev)))

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
  (cl/get-view @db iris-design-doc :receipts {:include_docs false :key [doc-id doc-rev hook-id]}))
