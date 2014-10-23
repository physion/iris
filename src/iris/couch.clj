(ns iris.couch
  (:require [com.ashafa.clutch :as cl]
            [iris.config :as config]
            [clojure.tools.logging :as logging]))


(defn database
  "Constructs a database URL for the given database name. Other parameters are pulled from config."
  [db-name]
  (assoc (cemerick.url/url config/COUCH_HOST db-name)
    :username config/COUCH_USER
    :password config/COUCH_PASSWORD))

(defonce ^{:private true} db (atom (database config/COUCH_DATABASE)))


(def iris-design-doc "iris")

(defn check-db
  "Creates db (a cemerick.url/url) if it doesn't exist already." ;; TODO eventually, this should become a macro wrapper with-db
  [database]
  (logging/debug "Checking database" (dissoc database :username :password))
  (cl/get-database database))


(defn ensure-webhooks
  []
  (check-db @db)
  (logging/debug "Creating webhooks view")
  (cl/save-view @db iris-design-doc
    (cl/view-server-fns :javascript
      {:webhooks {:map
                  "function(doc) {
                    if(doc.type && doc.type==='webhook') {
                      emit([doc.db, doc.trigger_type], null);
                    }
                  }"
                  }})))

(defn get-document
  [db-name doc-id &{:keys [rev]}]
  (if rev
    (cl/get-document (database db-name) doc-id {:rev rev})
    (cl/get-document (database db-name) doc-id)))

(defn get-underworld-document
  [doc-id]
  (cl/get-document @db doc-id))
