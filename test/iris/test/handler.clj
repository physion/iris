(ns iris.test.handler
  (:require [midje.sweet :refer :all]
            [iris.handler :as handler]
            [ring.mock.request :refer [request header content-type body]]
            [clojure.data.json :as json]))

(facts "About Iris"
  (fact "HEAD / => 200"
    (let [response (handler/app (request :head "/"))]
      (:status response) => 200))
  (fact "GET / => 200"
    (let [response (handler/app (request :get "/"))]
      (:status response) => 200))

  (fact "POST /messages => 200"
    (let [post (-> (request :post "/messages")
                 (header "X-Aws-Sqsd-Msgid" "123")
                 (header "X-Aws-Sqsd-Queue" "queue")
                 (header "X-Aws-Sqsd-First-Received-At" "12-12-12")
                 (header "X-Aws-Sqsd-Receive-Count" "1")
                 (content-type "application/json")
                 (body (json/write-str {:doc_id "doc" :doc_rev "doc-rev" :hook_id "hook" :db "dbname"})))]

      (:status (handler/app post)) => 200
      (provided
        (iris.messages/send anything) => {:id "receipt-id"})))
  )
