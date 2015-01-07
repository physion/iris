(ns iris.schema
  (:require [schema.core :as s]))

;; --- Schema --- ;;

(s/defschema NewMessage {:doc_id  s/Str
                         :doc_rev s/Str
                         :db      s/Str
                         :hook_id s/Str})

(s/defschema MessageInfo (-> NewMessage
                             (assoc :sqs-msgid s/Str)
                             (assoc :sqs-queue s/Str)
                             (assoc :sqs-first-received-at s/Str)
                             (assoc :sqs-receive-count s/Str)))


;; NB Shared with Osiris; we should factor this into a common library
(s/defschema Webhook {:_id                      s/Str
                      :_rev                     s/Str
                      :type                     "webhook"
                      (s/optional-key :user)    s/Uuid      ;; User Id
                      :trigger_type             s/Str       ;; Entity type
                      (s/optional-key :db)      s/Str       ;; User database
                      :url                      s/Str       ;; Webhook URL
                      (s/optional-key :api_key) s/Str       ;; URL API Key
                      (s/optional-key :filter)  [s/Keyword s/Str] ;; filter field key and regex
                      })


(s/defschema NewReceipt {:type    "receipt"
                         :hook_id s/Str
                         :doc     s/Str
                         :doc_rev s/Str
                         })
(s/defschema Receipt (assoc NewReceipt :_id s/Str :_rev s/Str))
