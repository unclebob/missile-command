(ns missile-command.options-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.options :as opts]
            [missile-command.waves :as waves]
            [missile-command.core :as core]))

(def difficulty-gen
  (gen/elements [:arcade :normal :easy "arcade" "NORMAL" "Easy"]))

(defspec difficulty-factor-is-positive-and-at-most-one
  40
  (for-all [d difficulty-gen]
    (let [f (opts/difficulty-factor d)]
      (and (pos? f) (<= f 1.0)))))

(defspec scale-enemy-count-never-exceeds-arcade-and-keeps-positive
  50
  (for-all [count (gen/large-integer* {:min 0 :max 40})
            factor (gen/elements [1.0 0.85 0.7 0.5])]
    (let [scaled (opts/scale-enemy-count count factor)]
      (and (<= scaled count)
           (if (pos? count)
             (pos? scaled)
             (zero? scaled))))))

(defspec scale-enemy-speed-is-non-negative-and-monotone-in-factor
  40
  (for-all [speed (gen/double* {:min 0.0 :max 500.0 :NaN? false :infinite? false})
            f-lo (gen/elements [0.5 0.7])
            f-hi (gen/elements [0.85 1.0])]
    (let [lo (opts/scale-enemy-speed speed f-lo)
          hi (opts/scale-enemy-speed speed f-hi)]
      (and (<= 0.0 lo)
           (<= lo hi)
           (<= hi (* speed 1.0000001))))))

(defspec schedule-metrics-respect-difficulty-factor
  40
  (for-all [wave (gen/large-integer* {:min 1 :max 20})
            diff (gen/elements [:arcade :normal :easy])]
    (let [arcade (waves/schedule-metrics wave :arcade)
          scaled (waves/schedule-metrics wave diff)
          factor (opts/difficulty-factor diff)]
      (and (= wave (:wave scaled))
           (= (opts/scale-enemy-count (:enemy-count arcade) factor)
              (:enemy-count scaled))
           (= (opts/scale-enemy-speed (:enemy-speed arcade) factor)
              (:enemy-speed scaled))
           (= (:mirv-count arcade) (:mirv-count scaled))
           (= (:multiplier arcade) (:multiplier scaled))))))

(defspec open-and-leave-options-from-title
  20
  (for-all []
    (let [title (core/new-game {:width 800 :height 600})
          opts-screen (core/open-options title)
          back (core/leave-options opts-screen)]
      (and (core/title? title)
           (core/options? opts-screen)
           (core/title? back)
           (not (core/mute? title))
           (= :arcade (core/difficulty title))))))

(defspec options-carry-across-start-game
  25
  (for-all [mute? gen/boolean
            diff (gen/elements [:arcade :normal :easy])]
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-mute mute?)
                    (core/set-difficulty diff)
                    core/start-game)]
      (and (core/playing? state)
           (= mute? (core/mute? state))
           (= diff (core/difficulty state))))))

(defspec remapped-fire-key-fires-when-playing
  20
  (for-all [key (gen/elements ["q" "a" "m"])]
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/bind-fire-key :left key)
                    core/start-game)
          after (:state (core/press-key state key))]
      (and (core/fire-key-includes? state :left key)
           (= 1 (count (core/defensive-missiles after)))
           (= 9 (:missiles (core/battery after :left)))))))

(defspec options-tick-advances-clock-only
  15
  (for-all [dt (gen/elements [0.016 0.1 0.5])]
    (let [state (core/open-options (core/new-game {:width 800 :height 600}))
          t0 (core/sim-time state)
          after (:state (core/tick state dt))]
      (and (core/options? after)
           (> (core/sim-time after) t0)
           (empty? (core/enemy-missiles after))
           (empty? (core/defensive-missiles after))))))
