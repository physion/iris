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

; Web hooks are
; {
;   "type": "webhook",
;   "trigger_type": <doc type>,
;   "db": <db>
; }
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
                  }}))
  )

;(defn watched-state
;  [database-name]
;  (ensure-db)
;  (-> (cl/get-document @db database-name)
;    (cl/dissoc-meta)))
;
;(defn set-watched-state!
;  [database-name last-seq]
;  (ensure-db)
;  (-> (if-let [doc (cl/get-document @db database-name)]
;        (cl/put-document @db (assoc doc :last-seq last-seq))
;        (cl/put-document @db {:_id database-name :last-seq last-seq}))
;    (cl/dissoc-meta)))
;
;(defn changes-since
;  "Returns all database changes since the given sequence (a string) for the database db"
;  [db-name since]
;  (let [url (database db-name)]
;    (if (nil? since)
;      (cl/changes url :include_docs true)
;      (cl/changes url :since since :include_docs true))))
;
;(defn webhooks
;  "Gets all webhooks for the given database for updated documents with the given type"
;  [database                                                 ; :- s/Str
;   type]                                                    ; :- document "type" value
;  (ensure-webhooks)
;  (cl/get-view @db osiris-design-doc :webhooks {:include_docs true} {:key [database type]}))
