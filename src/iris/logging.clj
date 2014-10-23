(ns osiris.logging
  (:require [clj-logging-config.log4j :as log-config]))

(defn setup! []
  (if-let [log-host osiris.config/LOGGING_HOST]
    (do
      (log-config/set-logger!
        :level :debug
        :out (org.apache.log4j.net.SyslogAppender.
               (org.apache.log4j.PatternLayout. "%p: (%F:%L) %x %m %n")
               log-host
               org.apache.log4j.net.SyslogAppender/LOG_LOCAL7)))))
