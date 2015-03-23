(ns iris.test.messages
  (:require [midje.sweet :refer :all]
            [ring.util.http-predicates :as predicates]
            [org.httpkit.fake :refer [with-fake-http]]
            [iris.messages :as msg]
            [iris.couch :as couch]
            [iris.mapping :as mapping]
            [iris.messages :as messages])
  (:import (java.util UUID)))

(facts "About Message sending" clj-stacktrace.utils
       (fact "POSTs to message url and saves receipt after succesful filter"
             (let [url "https://ovation.io/callback"]
               (with-fake-http [{:url url :method :post} {:status 201 :body "ok"}]
                               (msg/send {:doc_id ...id... :doc_rev ...rev... :db ...db... :hook_id ...hook-id...})) => ...receipt-doc...
               (provided
                 (couch/get-document ...db... ...id... ...rev...) => {:foo "bar" :baz "yes!"}
                 (couch/get-underworld-document ...hook-id...) => {:url url :filter [[:baz "yes!"]]}
                 (couch/get-receipts ...id... ...rev... ...hook-id...) => '()
                 (couch/put-underworld-document anything) => ...receipt-doc...
                 (predicates/created? anything) => true
                 ...receipt... =contains=> {:hook_id ...hook-id... :doc_id ...id... :doc_rev ...rev... :db ...db... :type "receipt"})))

       (fact "DELETEs message url for deleted document "
             (let [url "https://ovation.io/callback"]
               (with-fake-http [{:url url :method :delete} {:status 201 :body "ok"}]
                               (msg/send {:doc_id ...id... :doc_rev ...rev... :db ...db... :hook_id ...hook-id... :deleted true})) => ...receipt-doc...
               (provided
                 (couch/get-document ...db... ...id... ...rev...) => {:foo "bar" :baz "yes!"}
                 (couch/get-underworld-document ...hook-id...) => {:url url :filter [[:baz "yes!"]]}
                 (couch/get-receipts ...id... ...rev... ...hook-id...) => '()
                 (couch/put-underworld-document anything) => ...receipt-doc...
                 ...receipt... =contains=> {:hook_id ...hook-id... :doc_id ...id... :doc_rev ...rev... :db ...db... :type "receipt"})))

       (fact "POSTS to message url and saves receipt"
             (let [url "https://ovation.io/callback"]
               (with-fake-http [{:url url :method :post} {:status 201 :body "ok"}]
                               (msg/send {:doc_id ...id... :doc_rev ...rev... :db ...db... :hook_id ...hook-id...})) => ...receipt-doc...
               (provided
                 (couch/get-document ...db... ...id... ...rev...) => ...doc...
                 (couch/get-underworld-document ...hook-id...) => {:url url}
                 (couch/get-receipts ...id... ...rev... ...hook-id...) => '()
                 (couch/put-underworld-document anything) => ...receipt-doc...
                 ...receipt... =contains=> {:hook_id ...hook-id... :doc_id ...id... :doc_rev ...rev... :db ...db... :type "receipt"})))

       (fact "uses hook api_key for basic auth when present"
             (let [url "https://ovation.io/callback"
                   api-key "secrect!"]
               (with-fake-http [{:url url :method :post :basic-auth [api-key api-key]} {:status 201 :body "ok"}]
                               (msg/send {:doc_id ...id... :doc_rev ...rev... :db ...db... :hook_id ...hook-id...})) => ...receipt-doc...
               (provided
                 (couch/get-document ...db... ...id... ...rev...) => ...doc...
                 (couch/get-underworld-document ...hook-id...) => {:url url :api_key api-key}
                 (couch/get-receipts ...id... ...rev... ...hook-id...) => '()
                 (couch/put-underworld-document anything) => ...receipt-doc...
                 ...receipt... =contains=> {:hook_id ...hook-id... :doc_id ...id... :doc_rev ...rev... :db ...db... :type "receipt"})))

       (fact "throws exception if http call fails"
             (let [url "https://ovation.io/callback"
                   response {:status 400 :body "crap!"}]
               (with-fake-http [{:url url :method :post} response]
                               (msg/send {:doc_id ...id... :doc_rev ...rev... :db ...db... :hook_id ...hook-id...})) => (throws anything)
               (provided
                 (couch/get-document ...db... ...id... ...rev...) => ...doc...
                 (couch/get-underworld-document ...hook-id...) => {:url url})))

       (fact "substitutes target URL from mapped document"
             (let [url-raw "https://ovation.io/callback/:project_id/update"
                   project_id "123abc"
                   url (str "https://ovation.io/callback/" project_id "/update")
                   doc {:project_id project_id}]
               (with-fake-http [{:url url :method :post} {:status 201 :body "ok"}]
                               (msg/send {:doc_id  ...id...
                                          :doc_rev ...rev...
                                          :db      ...db...
                                          :hook_id ...hook-id...})) => ...receipt-doc...
               (provided
                 (couch/get-document ...db... ...id... ...rev...) => doc
                 (couch/get-underworld-document ...hook-id...) => {:url url-raw}
                 (couch/get-receipts ...id... ...rev... ...hook-id...) => '()
                 (couch/put-underworld-document anything) => ...receipt-doc...)))

       (fact "maps doc from mapping"
             (let [url-raw "https://ovation.io/callback/:mapped_id/update"
                   project_id "123abc"
                   url (str "https://ovation.io/callback/" project_id "/update")
                   type "MyType"
                   doc {:project_id project_id
                        :type       type}]

               (with-fake-http [{:url url :method :post} {:status 201 :body "ok"}]
                               (msg/send {:doc_id  ...id...
                                          :doc_rev ...rev...
                                          :db      ...db...
                                          :hook_id ...hook-id...})) => ...receipt-doc...
               (provided
                 (mapping/get-mapping type) => {:project_id :mapped_id}
                 (couch/get-document ...db... ...id... ...rev...) => doc
                 (couch/get-underworld-document ...hook-id...) => {:url url-raw}
                 (couch/get-receipts ...id... ...rev... ...hook-id...) => '()
                 (couch/put-underworld-document anything) => ...receipt-doc...)))


       (fact "maps doc from hook document"
             (let [url-raw "https://ovation.io/callback/:hook_id/update"
                   project_id "123abc"
                   url (str "https://ovation.io/callback/" project_id "/update")
                   type "MyType"
                   doc {:project_id project_id
                        :type       type}]

               (with-fake-http [{:url url :method :post} {:status 201 :body "ok"}]
                               (msg/send {:doc_id  ...id...
                                          :doc_rev ...rev...
                                          :db      ...db...
                                          :hook_id ...hook-id...})) => ...receipt-doc...
               (provided
                 (couch/get-document ...db... ...id... ...rev...) => doc
                 (couch/get-underworld-document ...hook-id...) => {:url url-raw :mapping {:project_id :hook_id}}
                 (couch/get-receipts ...id... ...rev... ...hook-id...) => '()
                 (couch/put-underworld-document anything) => ...receipt-doc...)))

       (fact "does not call url if receipt already in underworld database"
             (let [url "https://ovation.io/callback"]
               (with-fake-http [{:url url :method :post} {:status 201 :body "ok"}]
                               (msg/send {:doc_id ...id... :doc_rev ...rev... :db ...db... :hook_id ...hook-id...})) => '(...receipt...)
               (provided
                 (couch/get-document ...db... ...id... ...rev...) => ...doc...
                 (couch/get-underworld-document ...hook-id...) => {:url url}
                 (couch/get-receipts ...id... ...rev... ...hook-id...) => '(...receipt...)))))


(facts "About message mapping"
       (fact "maps Relation to ovation.io update"
             (let [entity-id (str (UUID/randomUUID))
                   owner-id (str (UUID/randomUUID))
                   target-id (str (UUID/randomUUID))
                   doc {:_id         entity-id
                        :_rev        "1-43492685ecdaafd5aa89458b1b577dc8",
                        :rel         "experiments",
                        :inverse_rel "projects"
                        :target_id   target-id,
                        :links       {"_collaboration_roots" ["0f1ad537-3868-456a-805e-9b2f9cc7499a"]}
                        :source_id   entity-id
                        :type        "Relation"
                        :user_id     owner-id}

                   expected {:rel         "experiments"
                             :inverse_rel "projects"
                             :owner_id    owner-id
                             :entity_id   entity-id
                             :target_id   target-id
                             }]

               (messages/map-doc doc {}) => expected)))


(facts "About message filtering"
       (fact "passes nil filter"
             (msg/check-filter nil ...doc...) => true)

       (fact "passes empty filter"
             (msg/check-filter [] ...doc...) => true)

       (fact "passes matching filter"
             (msg/check-filter [[:foo "yes"]] {:bar "baz" :foo "yes"}) => true)

       (fact "rejects non-matching filter"
             (msg/check-filter [[:foo "no"]] {:bar "baz" :foo "yes"}) => false)

       (fact "rejects message send for rejecting filter"
             (let [url "https://ovation.io/callback"]
               (msg/send {:doc_id ...id... :doc_rev ...rev... :db ...db... :hook_id ...hook-id...}) => '()
               (provided
                 (couch/get-receipts ...id... ...rev... ...hook-id...) => '()
                 (couch/get-document ...db... ...id... ...rev...) => {:foo "bar" :baz "yes!"}
                 (couch/get-underworld-document ...hook-id...) => {:url url :filter [[:baz "no!"]]}))))
