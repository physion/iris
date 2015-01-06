(ns iris.test.messages
  (:require [midje.sweet :refer :all]
            [org.httpkit.client :as http]
            [org.httpkit.fake :refer [with-fake-http]]
            [iris.messages :as msg]
            [iris.couch :as couch]
            [iris.mapping :as mapping]))

(facts "About Message sending" clj-stacktrace.utils
       (fact "calls message url and saves receipt"
             (let [url "https://ovation.io/callback"]
               (with-fake-http [{:url url :method :post} {:status 201 :body "ok"}]
                               (msg/send {:doc_id ...id... :doc_rev ...rev... :db ...db... :hook_id ...hook-id...})) => ...receipt-doc...
               (provided
                 (couch/get-document ...db... ...id... :rev ...rev...) => ...doc...
                 (couch/get-underworld-document ...hook-id...) => {:url url}
                 (couch/get-receipts ...db... ...id... ...rev... ...hook-id...) => '()
                 (couch/put-underworld-document anything) => ...receipt-doc...
                 ...receipt... =contains=> {:hook_id ...hook-id... :doc_id ...id... :doc_rev ...rev... :db ...db... :type "receipt"})))

       (fact "throws exception if http call fails"
             (let [url "https://ovation.io/callback"
                   response {:status 400 :body "crap!"}]
               (with-fake-http [{:url url :method :post} response]
                               (msg/send {:doc_id ...id... :doc_rev ...rev... :db ...db... :hook_id ...hook-id...})) => (throws anything)
               (provided
                 (couch/get-document ...db... ...id... :rev ...rev...) => ...doc...
                 (couch/get-underworld-document ...hook-id...) => {:url url})))

       (fact "substitutes target URL from mapped document"
             (let [url-raw "https://ovation.io/callback/:project_id/update"
                   project_id "123abc"
                   url (str "https://ovation.io/callback/" project_id "/update")
                   doc {:project_id project_id}]
               (with-fake-http [{:url url :method :post} {:status 201 :body "ok"}]
                               (msg/send {:doc_id ...id...
                                          :doc_rev ...rev...
                                          :db ...db...
                                          :hook_id ...hook-id...})) => ...receipt-doc...
               (provided
                 (couch/get-document ...db... ...id... :rev ...rev...) => doc
                 (couch/get-underworld-document ...hook-id...) => {:url url-raw}
                 (couch/get-receipts ...db... ...id... ...rev... ...hook-id...) => '()
                 (couch/put-underworld-document anything) => ...receipt-doc...)))

       (fact "Maps doc from mapping"
             (let [url-raw "https://ovation.io/callback/:mapped_id/update"
                   project_id "123abc"
                   url (str "https://ovation.io/callback/" project_id "/update")
                   type "MyType"
                   doc {:project_id project_id
                        :type type}]

               (with-fake-http [{:url url :method :post} {:status 201 :body "ok"}]
                               (msg/send {:doc_id ...id...
                                          :doc_rev ...rev...
                                          :db ...db...
                                          :hook_id ...hook-id...})) => ...receipt-doc...
               (provided
                 (mapping/get-mapping type) => {:project_id :mapped_id}
                 (couch/get-document ...db... ...id... :rev ...rev...) => doc
                 (couch/get-underworld-document ...hook-id...) => {:url url-raw}
                 (couch/get-receipts ...db... ...id... ...rev... ...hook-id...) => '()
                 (couch/put-underworld-document anything) => ...receipt-doc...)))


       (fact "Maps doc from hook document"
             (let [url-raw "https://ovation.io/callback/:hook_id/update"
                   project_id "123abc"
                   url (str "https://ovation.io/callback/" project_id "/update")
                   type "MyType"
                   doc {:project_id project_id
                        :type type}]

               (with-fake-http [{:url url :method :post} {:status 201 :body "ok"}]
                               (msg/send {:doc_id ...id...
                                          :doc_rev ...rev...
                                          :db ...db...
                                          :hook_id ...hook-id...})) => ...receipt-doc...
               (provided
                 (couch/get-document ...db... ...id... :rev ...rev...) => doc
                 (couch/get-underworld-document ...hook-id...) => {:url url-raw :mapping {:project_id :hook_id}}
                 (couch/get-receipts ...db... ...id... ...rev... ...hook-id...) => '()
                 (couch/put-underworld-document anything) => ...receipt-doc...)))

       (fact "Does not call url if receipt already in underworld database"
             (let [url "https://ovation.io/callback"]
               (with-fake-http [{:url url :method :post} {:status 201 :body "ok"}]
                               (msg/send {:doc_id ...id... :doc_rev ...rev... :db ...db... :hook_id ...hook-id...})) => '(...receipt...)
               (provided
                 (couch/get-document ...db... ...id... :rev ...rev...) => ...doc...
                 (couch/get-underworld-document ...hook-id...) => {:url url}
                 (couch/get-receipts ...db... ...id... ...rev... ...hook-id...) => '(...receipt...)))))
