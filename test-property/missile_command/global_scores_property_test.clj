(ns missile-command.global-scores-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.global-scores :as global]))

(defspec submit-skip-status-matches-readiness
  40
  (for-all [enabled? gen/boolean
            read-succeeded? gen/boolean]
    (= (global/submit-skip-status {:enabled? enabled?
                                   :read-succeeded? read-succeeded?})
       (cond
         (not enabled?) :skipped_disabled
         (not read-succeeded?) :skipped_no_read
         :else nil))))
