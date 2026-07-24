(ns missile-command.acceptance.city-steps
  "Gherkin steps for city living/destroyed state."
  (:require [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]))

(def handlers
  [
   {:pattern #"^city (\d+) has been destroyed$"
    :fn (fn [world [_ city-text] _]
          (assoc world :state
                 (core/destroy-city
                  (:state world)
                  (support/parse-int city-text "city"))))}

   {:pattern #"^city <([A-Za-z0-9_]+)> has been destroyed$"
    :fn (fn [world [_ city-param] example]
          (assoc world :state
                 (core/destroy-city
                  (:state world)
                  (support/example-int example city-param "city"))))}

   {:pattern #"^city (\d+) is living$"
    :fn (fn [world [_ city-text] _]
          (let [city-id (support/parse-int city-text "city")]
            (support/assert-condition (core/living-city? (:state world) city-id)
                              (str "city " city-id " is not living")))
          world)}

   {:pattern #"^city <([A-Za-z0-9_]+)> is living$"
    :fn (fn [world [_ city-param] example]
          (let [city-id (support/example-int example city-param "city")]
            (support/assert-condition (core/living-city? (:state world) city-id)
                              (str "city " city-id " is not living")))
          world)}

   {:pattern #"^city (\d+) is not living$"
    :fn (fn [world [_ city-text] _]
          (let [city-id (support/parse-int city-text "city")
                city (core/city (:state world) city-id)]
            (support/assert-condition city (str "city " city-id " does not exist"))
            (support/assert-condition (not (:alive? city))
                              (str "city " city-id " is still living")))
          world)}

   {:pattern #"^city <([A-Za-z0-9_]+)> is not living$"
    :fn (fn [world [_ city-param] example]
          (let [city-id (support/example-int example city-param "city")
                city (core/city (:state world) city-id)]
            (support/assert-condition city (str "city " city-id " does not exist"))
            (support/assert-condition (not (:alive? city))
                              (str "city " city-id " is still living")))
          world)}
])
