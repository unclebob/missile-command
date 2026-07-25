(ns missile-command.sfx-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.sfx :as sfx]
            [missile-command.core :as core]))

(defspec emit-appends-and-emitted-detects
  40
  (for-all [types (gen/vector
                   (gen/elements [:sfx/launch :sfx/boom :sfx/explosion
                                  :sfx/intercepted :sfx/city-destroyed
                                  :sfx/battery-destroyed :sfx/low-ammo
                                  :sfx/wave :sfx/wave-clear :sfx/bonus-city
                                  :sfx/the-end :sfx/warning])
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

(defspec take-new-is-suffix-of-log-clamped-to-bounds
  40
  (for-all [types (gen/vector
                   (gen/elements [:sfx/launch :sfx/boom :sfx/wave :sfx/warning])
                   0 12)
            from (gen/large-integer* {:min -3 :max 20})]
    (let [state (reduce sfx/emit {:sfx-events []} types)
          log (sfx/events state)
          n (count log)
          fresh (sfx/take-new state from)
          clamped (max 0 (min (long from) n))]
      (and (= fresh (subvec log clamped n))
           (= (count fresh) (- n clamped))
           (= log (into (vec (take clamped log)) fresh))))))

(defspec drain-returns-all-and-clears
  30
  (for-all [types (gen/vector
                   (gen/elements [:sfx/launch :sfx/explosion :sfx/city-destroyed])
                   0 10)]
    (let [state (reduce sfx/emit {:sfx-events []} types)
          [ev cleared] (sfx/drain state)]
      (and (= ev (sfx/events state))
           (= (count types) (count ev))
           (empty? (sfx/events cleared))
           (every? #(not (sfx/emitted? cleared %)) (set types))))))

(defspec truncate-to-keeps-prefix
  40
  (for-all [types (gen/vector
                   (gen/elements [:sfx/launch :sfx/wave :sfx/the-end])
                   0 10)
            keep (gen/large-integer* {:min -2 :max 15})]
    (let [state (reduce sfx/emit {:sfx-events []} types)
          full (sfx/events state)
          n (count full)
          kept (max 0 (long keep))
          after (sfx/truncate-to state keep)
          log (sfx/events after)]
      (and (= log (vec (take (min kept n) full)))
           (<= (count log) n)
           (<= (count log) kept)))))
