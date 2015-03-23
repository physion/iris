(ns iris.config)

(def COUCH_HOST (if-let [host (or (System/getenv "COUCH_HOST") (System/getProperty "COUCH_HOST"))]
                  host
                  "http://localhost:5995"))

(def COUCH_DATABASE (if-let [database (or (System/getenv "COUCH_DATABASE") (System/getProperty "COUCH_DATABASE"))]
                      database
                      "underworld_dev"))

(def COUCH_USER (if-let [user (or (System/getenv "COUCH_USER") (System/getProperty "COUCH_USER"))]
                  user
                  "ovation-io-dev"))

(def COUCH_PASSWORD (if-let [password (or (System/getenv "COUCH_PASSWORD") (System/getProperty "COUCH_PASSWORD"))]
                      password
                      "boom!"))

(def LOGGING_HOST (or (System/getenv "LOGGING_HOST") (System/getProperty "LOGGING_HOST")))
