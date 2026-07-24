(ns missile-command.acceptance.sfx-steps
  "Gherkin steps for core SFX event logging."
  (:require [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]))

(def handlers
  [{:pattern #"^an sfx event <([A-Za-z0-9_/]+)> was emitted$"
    :fn (fn [world [_ event-param] example]
          (let [event (str (support/require-value example event-param))]
            (support/assert-condition
             (core/sfx-emitted? (:state world) event)
             (str "sfx event " event " not emitted; log="
                  (core/sfx-events (:state world)))))
          world)}

   {:pattern #"^an sfx event ([A-Za-z0-9_/]+) was emitted$"
    :fn (fn [world [_ event] _]
          (support/assert-condition
           (core/sfx-emitted? (:state world) event)
           (str "sfx event " event " not emitted; log="
                (core/sfx-events (:state world))))
          world)}])
