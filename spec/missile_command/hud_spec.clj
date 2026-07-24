(ns missile-command.hud-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]
            [missile-command.hud :as hud]))

(describe "HUD projection"
  (it "marks full playing HUD during playing and paused"
    (let [playing (core/start-game (core/new-game {:width 800 :height 600}))
          paused (core/pause-game playing)
          title (core/new-game {:width 800 :height 600})]
      (should (:full-playing-hud? (hud/projection playing)))
      (should (:full-playing-hud? (hud/projection paused)))
      (should-not (:full-playing-hud? (hud/projection title)))))

  (it "mirrors score wave multiplier ammo cities and reserve"
    (let [state (-> (core/start-game (core/new-game {:width 800 :height 600}))
                    (core/set-score 2500)
                    (core/set-wave 5)
                    (core/destroy-city 0)
                    (core/set-bonus-city-reserve 2))
          h (core/hud state)]
      (should= (core/score state) (:score h))
      (should= (core/wave state) (:wave h))
      (should= (core/multiplier state) (:multiplier h))
      (should= (core/bonus-cities state) (:bonus-cities h))
      (should= (count (core/living-cities state)) (:living-cities h))
      (should= (:missiles (core/battery state :left)) (:left-ammo h))
      (should= (:missiles (core/battery state :center)) (:center-ammo h))
      (should= (:missiles (core/battery state :right)) (:right-ammo h))
      (should= {:left (:left-ammo h)
                :center (:center-ammo h)
                :right (:right-ammo h)}
               (:ammo h)))))
