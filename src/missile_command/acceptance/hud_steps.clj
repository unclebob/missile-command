(ns missile-command.acceptance.hud-steps
  "Gherkin steps for the in-game HUD projection."
  (:require [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]))

(defn- batteries
  [world]
  (core/batteries (:state world)))

(defn- assert-hud-int
  [world key expected message-label]
  (let [actual (get (core/hud (:state world)) key)]
    (support/assert-condition (= expected actual)
                              (str "hud " message-label " " actual
                                   " expected " expected)))
  world)

(def handlers
  [{:pattern #"^the hud shows score <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ score-param] example]
          (assert-hud-int world :score
                          (support/example-int example score-param "score")
                          "score"))}

   {:pattern #"^the hud shows multiplier <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ mult-param] example]
          (assert-hud-int world :multiplier
                          (support/example-int example mult-param "multiplier")
                          "multiplier"))}

   {:pattern #"^the hud shows left ammo <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ ammo-param] example]
          (assert-hud-int world :left-ammo
                          (support/example-int example ammo-param "left ammo")
                          "left ammo"))}

   {:pattern #"^the hud shows center ammo <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ ammo-param] example]
          (assert-hud-int world :center-ammo
                          (support/example-int example ammo-param "center ammo")
                          "center ammo"))}

   {:pattern #"^the hud shows right ammo <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ ammo-param] example]
          (assert-hud-int world :right-ammo
                          (support/example-int example ammo-param "right ammo")
                          "right ammo"))}

   {:pattern #"^the hud shows (left|center|right) ammo <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ battery-name ammo-param] example]
          (let [battery-id (support/parse-battery-id battery-name)
                expected (support/example-int example ammo-param "ammo")
                actual (get-in (core/hud (:state world)) [:ammo battery-id])]
            (support/assert-condition (= expected actual)
                                      (str "hud " battery-name " ammo " actual
                                           " expected " expected)))
          world)}

   {:pattern #"^the hud shows <([A-Za-z0-9_]+)> ammo <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ battery-param ammo-param] example]
          (let [battery-id (support/example-battery example battery-param)
                expected (support/example-int example ammo-param "ammo")
                actual (get-in (core/hud (:state world)) [:ammo battery-id])]
            (support/assert-condition (= expected actual)
                                      (str "hud " battery-id " ammo " actual
                                           " expected " expected)))
          world)}

   {:pattern #"^the hud shows living cities <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ count-param] example]
          (assert-hud-int world :living-cities
                          (support/example-int example count-param "living cities")
                          "living cities"))}

   {:pattern #"^the hud shows bonus cities <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ count-param] example]
          (assert-hud-int world :bonus-cities
                          (support/example-int example count-param "bonus cities")
                          "bonus cities"))}

   {:pattern #"^each non-destroyed battery has matching hud ammo$"
    :fn (fn [world _ _]
          (let [hud (core/hud (:state world))]
            (doseq [b (remove :destroyed? (batteries world))]
              (let [expected (long (:missiles b))
                    actual (get-in hud [:ammo (:id b)])]
                (support/assert-condition (= expected actual)
                                          (str "hud ammo for " (:id b)
                                               " is " actual " expected "
                                               expected)))))
          world)}

   {:pattern #"^the full playing hud is not required$"
    :fn (fn [world _ _]
          (support/assert-condition
           (not (:full-playing-hud? (core/hud (:state world))))
           "full playing HUD should not be required on this screen")
          world)}])
