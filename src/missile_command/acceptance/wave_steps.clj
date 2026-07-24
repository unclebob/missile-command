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

   {:pattern #"^a wave enemy missile targeting battery <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ battery-param] example]
          (assoc world :state
                 (core/spawn-wave-enemy-targeting-battery
                  (:state world)
                  (support/example-battery example battery-param))))}

   {:pattern #"^at least one enemy missile targets a city$"
    :fn (fn [world _ _]
          (let [enemies (core/enemy-missiles (:state world))]
            (support/assert-condition
             (some #(= :city (:target-kind %)) enemies)
             "no enemy targets a city"))
          world)}

   {:pattern #"^at least one enemy missile targets a battery$"
    :fn (fn [world _ _]
          (let [enemies (core/enemy-missiles (:state world))]
            (support/assert-condition
             (some #(= :battery (:target-kind %)) enemies)
             "no enemy targets a battery"))
          world)}

   {:pattern #"^enemy missile targets include every living city$"
    :fn (fn [world _ _]
          (let [city-ids (set (map :id (core/living-cities (:state world))))
                targeted (->> (core/enemy-missiles (:state world))
                              (filter #(= :city (:target-kind %)))
                              (map :target-id)
                              set)]
            (support/assert-condition (= city-ids targeted)
                                      (str "city targets " targeted
                                           " expected " city-ids)))
          world)}

   {:pattern #"^enemy missile targets include every non-destroyed battery$"
    :fn (fn [world _ _]
          (let [bat-ids (->> (core/batteries (:state world))
                             (remove :destroyed?)
                             (map :id)
                             set)
                targeted (->> (core/enemy-missiles (:state world))
                              (filter #(= :battery (:target-kind %)))
                              (map :target-id)
                              set)]
            (support/assert-condition (= bat-ids targeted)
                                      (str "battery targets " targeted
                                           " expected " bat-ids)))
          world)}

   {:pattern #"^no enemy missile targets battery <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ battery-param] example]
          (let [battery-id (support/example-battery example battery-param)
                hit (some #(and (= :battery (:target-kind %))
                                (= battery-id (:target-id %)))
                          (core/enemy-missiles (:state world)))]
            (support/assert-condition (not hit)
                                      (str "enemy targets destroyed battery "
                                           battery-id)))
          world)}

   {:pattern #"^every enemy missile targets a living city or a non-destroyed battery$"
    :fn (fn [world _ _]
          (let [s (:state world)
                living (set (map :id (core/living-cities s)))
                bats (->> (core/batteries s)
                          (remove :destroyed?)
                          (map :id)
                          set)]
            (doseq [e (core/enemy-missiles s)]
              (case (:target-kind e)
                :city
                (support/assert-condition (contains? living (:target-id e))
                                          (str "enemy targets non-living city "
                                               (:target-id e)))
                :battery
                (support/assert-condition (contains? bats (:target-id e))
                                          (str "enemy targets destroyed battery "
                                               (:target-id e)))
                (support/fail! (str "unknown target kind " (:target-kind e))))))
          world)}
])
