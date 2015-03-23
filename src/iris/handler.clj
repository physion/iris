(ns iris.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer [ok]]
            [schema.core :as s]
            [iris.schema :refer [NewMessage]]
            [clojure.tools.logging :as logging]
            [iris.messages :as messages]
            [iris.logging]))

(iris.logging/setup!)
(logging/info "Starting Iris handler")

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

                            (logging/debug "Osiris update received by Iris" msg-info)

                            (let [result (messages/send msg-info)]
                              (logging/debug "Webhook result:" result)
                              (ok {:receipts result}))))))
