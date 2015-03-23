(ns iris.logging
  (:require [clj-logging-config.log4j :as log-config]
            [iris.config :refer [LOGGING_HOST]])
  (:import (org.apache.log4j ConsoleAppender SimpleLayout PatternLayout)
           (org.apache.log4j.net SyslogAppender)))

(defn configure-console-logger!
  []
  (log-config/set-logger!
    :level :debug
    :out (ConsoleAppender. (SimpleLayout.))))

(defn setup! []
  (if-let [host LOGGING_HOST]
    (log-config/set-logger!
      :level :debug
      :out (doto (SyslogAppender.)
             (.setSyslogHost host)
             (.setFacility "LOCAL7")
             (.setFacilityPrinting false)
             (.setName "iris")
             (.setLayout (PatternLayout. "%p: (%F:%L) %x %m %n"))))

    (configure-console-logger!)))
