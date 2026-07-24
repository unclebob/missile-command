(ns missile-command.scoring-spec
  (:require [speclj.core :refer :all]
            [missile-command.scoring :as scoring]))

(describe "scoring tables"
  (it "uses classic base point values"
    (should= 25 scoring/points-enemy-missile)
    (should= 5 scoring/points-unused-missile)
    (should= 100 scoring/points-surviving-city))

  (it "scales enemy kills by multiplier"
    (should= 25 (scoring/enemy-kill-points 1))
    (should= 50 (scoring/enemy-kill-points 2))
    (should= 75 (scoring/enemy-kill-points 3))
    (should= 150 (scoring/enemy-kill-points 6)))

  (it "computes wave-end bonuses for ammo and cities"
    ;; 10 ammo * 5 + 5 cities * 100 = 550 at 1x; 1100 at 2x
    (should= 550 (scoring/wave-end-points 10 5 1))
    (should= 1100 (scoring/wave-end-points 10 5 2))
    (should= 500 (scoring/wave-end-points 0 5 1))
    (should= 50 (scoring/wave-end-points 10 0 1))))
