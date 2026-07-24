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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T15:58:55.631202-05:00", :module-hash "187776858", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "-1245219576"} {:id "defn-/batteries", :kind "defn-", :line 6, :end-line 8, :hash "2112668418"} {:id "defn-/assert-hud-int", :kind "defn-", :line 10, :end-line 16, :hash "-1910884135"} {:id "def/handlers", :kind "def", :line 18, :end-line 88, :hash "-337373597"}]}
;; clj-mutate-manifest-end
