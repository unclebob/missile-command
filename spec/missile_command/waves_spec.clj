(ns missile-command.waves-spec
  (:require [speclj.core :refer :all]
            [missile-command.waves :as waves]))

(describe "wave schedule"
  (it "starts at wave one with full ammo constant"
    (should= 1 waves/initial-wave)
    (should= 10 waves/full-ammo))

  (it "schedules three ballistic attacks every wave"
    (should= 3 waves/attacks-per-wave)
    (should= 3 (waves/enemy-count 1))
    (should= 3 (waves/enemy-count 2))
    (should= 3 (waves/enemy-count 5))
    (should= 3 (waves/enemy-count 10)))

  (it "uses faster enemies on higher waves"
    ;; Wave 1 is deliberately moderate (~15s sky→ground on 600px) so players can react.
    (should= 40.0 (waves/enemy-speed 1))
    (should= 45.0 (waves/enemy-speed 2))
    (should= 50.0 (waves/enemy-speed 3))
    (should (> (waves/enemy-speed 3) (waves/enemy-speed 1))))

  (it "reports harder metrics for higher waves"
    (let [low (waves/schedule-metrics 1)
          high (waves/schedule-metrics 4)
          same (waves/schedule-metrics 2)]
      (should (waves/harder? low high))
      (should-not (waves/harder? high low))
      (should-not (waves/harder? same same))))

  (it "schedules MIRVs only from mid waves"
    (should= 0 (waves/mirv-count 1))
    (should= 0 (waves/mirv-count 2))
    (should= 0 (waves/mirv-count 3))
    (should= 1 (waves/mirv-count 4))
    (should= 1 (waves/mirv-count 5))
    (should= 2 (waves/mirv-count 6))
    (should= (:mirv-count (waves/schedule-metrics 4)) (waves/mirv-count 4)))

  (it "schedules bombers and satellites from mid waves"
    (should= 0 (waves/bomber-count 1))
    (should= 0 (waves/bomber-count 3))
    (should= 1 (waves/bomber-count 4))
    (should= 1 (waves/bomber-count 10))
    (should= 0 (waves/satellite-count 4))
    (should= 1 (waves/satellite-count 5))
    (should= 1 (waves/satellite-count 10)))

  (it "schedules smart bombs from wave 4"
    (should= 0 (waves/smart-bomb-count 1))
    (should= 0 (waves/smart-bomb-count 3))
    (should= 1 (waves/smart-bomb-count 4))
    (should= 1 (waves/smart-bomb-count 5))
    (should= 2 (waves/smart-bomb-count 6))
    (should= 3 (waves/smart-bomb-count 8))
    (should= (:smart-bomb-count (waves/schedule-metrics 4))
             (waves/smart-bomb-count 4)))

  (it "builds a wave target pool from cities and batteries"
    (let [pool (waves/target-pool [0 1 2] [:left :center])]
      (should= [[:city 0] [:city 1] [:city 2] [:battery :left] [:battery :center]]
               pool)
      (should= [[:city 0] [:city 1] [:city 2] [:battery :left] [:battery :center]
                [:city 0] [:city 1]]
               (waves/cycle-targets pool 7))
      (should= [] (waves/cycle-targets [] 3))))

  (it "raises multiplier every two waves up to six"
    (should= 1 (waves/multiplier 1))
    (should= 1 (waves/multiplier 2))
    (should= 2 (waves/multiplier 3))
    (should= 2 (waves/multiplier 4))
    (should= 3 (waves/multiplier 5))
    (should= 3 (waves/multiplier 6))
    (should= 6 (waves/multiplier 11))
    (should= 6 (waves/multiplier 12))
    (should= 6 (waves/multiplier 13))
    (should= 6 (waves/multiplier 20)))

  (it "spreads sky entry origins across the playfield"
    (let [width 800.0
          xs (mapv #(waves/sky-origin-x width % 3) [0 1 2])]
      (should= (* width (/ 0.5 3.0)) (nth xs 0))
      (should= (* width (/ 1.5 3.0)) (nth xs 1))
      (should= (* width (/ 2.5 3.0)) (nth xs 2))
      (should (every? #(and (<= 0.0 %) (< % width)) xs))
      (should= 3 (count (set xs)))
      (should= 0.0 (waves/sky-origin-x width 0 0)))))
