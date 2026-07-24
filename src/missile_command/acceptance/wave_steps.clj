(ns missile-command.acceptance.wave-steps
  "Gherkin steps for wave schedule, rearm, and hardness."
  (:require [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]))

(def handlers
  [
   {:pattern #"^the wave number is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param] example]
          (let [expected (support/example-int example wave-param "wave")
                actual (core/wave (:state world))]
            (support/assert-condition (= expected actual)
                              (str "wave " actual " expected " expected)))
          world)}

   {:pattern #"^the hud shows wave <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param] example]
          (let [expected (support/example-int example wave-param "wave")
                actual (:wave (core/hud (:state world)))]
            (support/assert-condition (= expected actual)
                              (str "hud wave " actual " expected " expected)))
          world)}

   {:pattern #"^each non-destroyed battery has <([A-Za-z0-9_]+)> missiles$"
    :fn (fn [world [_ ammo-param] example]
          (let [ammo (support/example-int example ammo-param "ammo")]
            (doseq [b (remove :destroyed? (core/batteries (:state world)))]
              (support/assert-condition (= ammo (:missiles b))
                                (str "battery " (:id b) " missiles "
                                     (:missiles b) " expected " ammo))))
          world)}

   {:pattern #"^the current wave has <([A-Za-z0-9_]+)> scheduled enemies still active$"
    :fn (fn [world [_ rem-param] example]
          (assoc world :state
                 (core/set-wave-enemies-active
                  (:state world)
                  (support/example-int example rem-param "remaining"))))}

   {:pattern #"^the current wave has (\d+) scheduled enemies still active$"
    :fn (fn [world [_ rem-text] _]
          (assoc world :state
                 (core/set-wave-enemies-active
                  (:state world)
                  (support/parse-int rem-text "remaining"))))}

   {:pattern #"^the wave is not complete$"
    :fn (fn [world _ _]
          (support/assert-condition (not (core/wave-complete? (:state world)))
                            "wave is complete but should not be")
          world)}

   {:pattern #"^the wave is complete$"
    :fn (fn [world _ _]
          (support/assert-condition (core/wave-complete? (:state world))
                            "wave is not complete")
          world)}

   {:pattern #"^time advances until all wave enemies are destroyed or have impacted$"
    :fn (fn [world _ _]
          (support/advance-until world
                                 (fn [s]
                                   (and (core/wave-complete? s)
                                        (empty? (core/enemy-missiles s))))
                                 core/tick
                                 0.05
                                 10000
                                 "wave enemies never finished"))}

   {:pattern #"^every non-destroyed battery has <([A-Za-z0-9_]+)> missiles$"
    :fn (fn [world [_ ammo-param] example]
          (assoc world :state
                 (core/set-non-destroyed-battery-ammo
                  (:state world)
                  (support/example-int example ammo-param "spent ammo"))))}

   {:pattern #"^the next wave starts$"
    :fn (fn [world _ _]
          (assoc world :state (core/start-next-wave (:state world))))}

   {:pattern #"^the <([A-Za-z0-9_]+)> battery has been destroyed$"
    :fn (fn [world [_ battery-param] example]
          (assoc world :state
                 (core/destroy-battery
                  (:state world)
                  (support/example-battery example battery-param))))}

   {:pattern #"^wave <([A-Za-z0-9_]+)> enemy schedule metrics are recorded$"
    :fn (fn [world [_ wave-param] example]
          (let [w (support/example-int example wave-param "wave")]
            (assoc world :recorded-wave-metrics
                   (core/wave-schedule-metrics w)
                   :recorded-low-wave w)))}

   {:pattern #"^the game is at wave <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param] example]
          (let [w (support/example-int example wave-param "wave")]
            (assoc world
                   :state (core/set-wave (:state world) w)
                   :high-wave w
                   :high-wave-metrics (core/wave-schedule-metrics w))))}

   {:pattern #"^wave <([A-Za-z0-9_]+)> is harder than wave <([A-Za-z0-9_]+)> by enemy count or enemy speed$"
    :fn (fn [world [_ high-param low-param] example]
          (let [high (support/example-int example high-param "high wave")
                low (support/example-int example low-param "low wave")
                low-m (or (:recorded-wave-metrics world)
                          (core/wave-schedule-metrics low))
                high-m (or (:high-wave-metrics world)
                           (core/wave-schedule-metrics high))]
            (support/assert-condition (core/harder-wave? low-m high-m)
                              (str "wave " high " not harder than " low
                                   " metrics " low-m " vs " high-m)))
          world)}

   {:pattern #"^wave <([A-Za-z0-9_]+)> has enemy count <([A-Za-z0-9_]+)> and speed <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param count-param speed-param] example]
          (let [wave (support/example-int example wave-param "wave")
                expected-count (support/example-int example count-param "enemy count")
                expected-speed (support/example-double example speed-param "enemy speed")
                metrics (core/wave-schedule-metrics wave)]
            (support/assert-condition (= expected-count (:enemy-count metrics))
                                      (str "wave " wave " enemy count "
                                           (:enemy-count metrics) " expected " expected-count))
            (support/assert-condition (= expected-speed (double (:enemy-speed metrics)))
                                      (str "wave " wave " enemy speed "
                                           (:enemy-speed metrics) " expected " expected-speed)))
          world)}
])
