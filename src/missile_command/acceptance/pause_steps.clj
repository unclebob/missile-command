(ns missile-command.acceptance.pause-steps
  "Gherkin steps for pause and resume."
  (:require [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]))

(def handlers
  [{:pattern #"^the screen is paused$"
    :fn (fn [world _ _]
          (support/assert-condition (core/paused? (:state world))
                                    (str "screen is " (core/screen (:state world))
                                         " expected paused"))
          world)}

   {:pattern #"^the player pauses the game$"
    :fn (fn [world _ _]
          (assoc world :state (:state (core/handle (:state world) {:type :pause}))))}

   {:pattern #"^the player resumes the game$"
    :fn (fn [world _ _]
          (assoc world :state (:state (core/handle (:state world) {:type :resume}))))}

   {:pattern #"^the first enemy missile progress is recorded$"
    :fn (fn [world _ _]
          (let [m (first (core/enemy-missiles (:state world)))]
            (support/assert-condition m "missing enemy missile")
            (assoc world :recorded-enemy-progress (double (:progress m 0.0)))))}

   {:pattern #"^the first enemy missile progress equals the recorded progress$"
    :fn (fn [world _ _]
          (let [m (first (core/enemy-missiles (:state world)))
                expected (double (:recorded-enemy-progress world))
                actual (double (:progress m 0.0))]
            (support/assert-condition m "missing enemy missile")
            (support/assert-condition (< (Math/abs (- actual expected)) 1.0e-9)
                                      (str "progress " actual " expected recorded "
                                           expected)))
          world)}

   {:pattern #"^the first enemy missile progress is greater than the recorded progress$"
    :fn (fn [world _ _]
          (let [m (first (core/enemy-missiles (:state world)))
                expected (double (:recorded-enemy-progress world))
                actual (double (:progress m 0.0))]
            (support/assert-condition m "missing enemy missile")
            (support/assert-gt actual expected
                               (str "progress " actual " not greater than recorded "
                                    expected)))
          world)}])
