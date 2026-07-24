(ns missile-command.sfx-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.sfx :as sfx]
            [missile-command.core :as core]))

(defspec emit-appends-and-emitted-detects
  40
  (for-all [types (gen/vector
                   (gen/elements [:sfx/launch :sfx/explosion :sfx/city-destroyed
                                  :sfx/battery-destroyed :sfx/low-ammo
                                  :sfx/wave-clear :sfx/bonus-city :sfx/the-end])
                   0 8)]
    (let [state (reduce sfx/emit {:sfx-events []} types)
          log (sfx/events state)]
      (and (= (count types) (count log))
           (every? true? (map (fn [t e] (= t (:type e))) types log))
           (every? #(sfx/emitted? state %) types)
           (or (empty? types)
               (not (sfx/emitted? state :sfx/never)))))))

(defspec launch-events-include-low-ammo-only-at-one
  40
  (for-all [battery (gen/elements [:left :center :right])
            remaining (gen/large-integer* {:min 0 :max 10})]
    (let [events (sfx/launch-events battery remaining)
          types (mapv :type events)]
      (and (= :sfx/launch (first types))
           (= battery (:battery (first events)))
           (= (= 1 remaining) (boolean (some #{:sfx/low-ammo} types)))
           (<= (count events) 2)))))

(defspec fire-logs-launch-on-stocked-battery
  25
  (for-all [battery (gen/elements [:left :center :right])]
    (let [state (core/start-game (core/new-game {:width 800 :height 600}))
          after (:state (core/handle state {:type :fire :battery battery}))]
      (and (core/sfx-emitted? after :sfx/launch)
           (= 1 (count (core/defensive-missiles after)))))))

(defspec mute-does-not-clear-sfx-log
  20
  (for-all []
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-mute true)
                    core/start-game)
          after (:state (core/handle state {:type :fire :battery :center}))]
      (and (core/mute? after)
           (core/sfx-emitted? after :sfx/launch)
           (seq (core/sfx-events after))))))

(defspec destroy-city-emits-once
  20
  (for-all [city-id (gen/elements [0 1 2 3 4 5])]
    (let [state (core/start-game (core/new-game {:width 800 :height 600}))
          once (core/destroy-city state city-id)
          twice (core/destroy-city once city-id)
          types (mapv :type (core/sfx-events twice))]
      (and (core/sfx-emitted? once :sfx/city-destroyed)
           (= 1 (count (filter #{:sfx/city-destroyed} types)))))))
