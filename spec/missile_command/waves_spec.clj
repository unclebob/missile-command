(ns missile-command.waves-spec
  (:require [speclj.core :refer :all]
            [missile-command.waves :as waves]))

(describe "wave schedule"
  (it "starts at wave one with full ammo constant"
    (should= 1 waves/initial-wave)
    (should= 10 waves/full-ammo))

  (it "schedules more enemies on higher waves"
    (should (> (waves/enemy-count 3) (waves/enemy-count 1)))
    (should= 3 (waves/enemy-count 1))
    (should= 5 (waves/enemy-count 3)))

  (it "uses faster enemies on higher waves"
    (should= 100.0 (waves/enemy-speed 1))
    (should= 125.0 (waves/enemy-speed 2))
    (should= 150.0 (waves/enemy-speed 3))
    (should (> (waves/enemy-speed 3) (waves/enemy-speed 1))))

  (it "reports harder metrics for higher waves"
    (let [low (waves/schedule-metrics 1)
          high (waves/schedule-metrics 4)
          same (waves/schedule-metrics 2)]
      (should (waves/harder? low high))
      (should-not (waves/harder? high low))
      (should-not (waves/harder? same same)))))
