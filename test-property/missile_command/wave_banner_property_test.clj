(ns missile-command.wave-banner-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.wave-banner :as wb]
            [missile-command.core :as core]))

(defn- finish-banner
  "Tick banner until playing again (or max steps)."
  [state]
  (loop [s state n 0]
    (cond
      (core/playing? s) s
      (> n 500) s
      :else (recur (:state (core/tick s 0.05)) (inc n)))))

(defspec enter-announces-wave-and-starts-enter-phase
  30
  (for-all [wave (gen/large-integer* {:min 1 :max 20})
            w (gen/elements [800 1024])
            h (gen/elements [600 768])]
    (let [state (wb/enter {:width w :height h} w h wave)]
      (and (wb/screen? state)
           (= wave (wb/announced-wave state))
           (= (str "WAVE " wave) (wb/text state))
           (= wb/phase-enter (wb/phase state))
           (< (:x (wb/text-position state)) (/ w 2.0))))))

(defspec enter-then-exit-then-finish-returns-to-playing
  25
  (for-all [wave (gen/elements [2 5 11])]
    (let [base (core/start-game (core/new-game {:width 800 :height 600}))
          banner (wb/enter base 800 600 wave)
          finished (finish-banner banner)]
      (and (core/wave-banner? banner)
           (core/playing? finished)
           (nil? (core/wave-banner finished))))))

(defspec banner-tick-freezes-enemy-progress
  20
  (for-all []
    (let [playing (-> (core/start-game (core/new-game {:width 800 :height 600}))
                      (core/spawn-enemy-targeting-city 0))
          enemy0 (first (core/enemy-missiles playing))
          banner (wb/enter playing 800 600 2)
          after (:state (core/tick banner 0.2))
          enemy1 (first (core/enemy-missiles after))]
      (and (core/wave-banner? after)
           (= (:progress enemy0) (:progress enemy1))
           (= (:x enemy0) (:x enemy1))
           (= (:y enemy0) (:y enemy1))))))

(defspec wave-complete-opens-banner-for-next-wave
  20
  (for-all []
    (let [state (-> (core/start-game (core/new-game {:width 800 :height 600}))
                    (core/set-wave-enemies-active 1))
          ;; Destroy the single enemy via impact path: force empty enemies
          completed (loop [s state n 0]
                      (cond
                        (core/wave-banner? s) s
                        (> n 5000) s
                        :else (recur (:state (core/tick s 0.05)) (inc n))))]
      (and (core/wave-banner? completed)
           (= 2 (core/wave-banner-announced-wave completed))
           (= 2 (core/wave completed))
           (core/sfx-emitted? completed :sfx/wave)))))
