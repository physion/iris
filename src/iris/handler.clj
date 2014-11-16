(ns iris.handler
  (:import (com.sun.xml.internal.bind.v2.model.core ID))
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer [ok]]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [iris.schema :refer [NewMessage]]
            [clojure.tools.logging :as logging]
            [iris.messages :as messages]))


;; --- Routes --- ;;
(defapi app
  (swaggered "iris"
    (HEAD* "/" []
      (ok ""))
    (GET* "/" []
      (ok "Iris!"))

    (POST* "/messages" []
      :body [msg NewMessage]
      :summary "Processes an update from Osiris"
      :header-params [x-aws-sqsd-msgid :- s/Str
                      x-aws-sqsd-queue :- s/Str
                      x-aws-sqsd-first-received-at :- s/Str
                      x-aws-sqsd-receive-count :- s/Str]

      (let [msg-info (-> msg
                       (assoc :sqs-msgid x-aws-sqsd-msgid)
                       (assoc :sqs-queue x-aws-sqsd-queue)
                       (assoc :sqs-first-received-at x-aws-sqsd-first-received-at)
                       (assoc :sqs-receive-count (Integer/parseInt x-aws-sqsd-receive-count)))]

        (logging/info "Sending message " msg)
        (ok (messages/send msg-info))))))
