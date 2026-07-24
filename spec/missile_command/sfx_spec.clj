(ns missile-command.sfx-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]))

(describe "sfx events"
  (it "emits launch when firing"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    core/start-game)
          result (core/handle state {:type :fire :battery :left})]
      (should (core/sfx-emitted? (:state result) :sfx/launch))
      (should (some #(= :sfx/launch (:type %)) (:events result)))))

  (it "emits low-ammo when firing leaves one missile"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    core/start-game
                    (core/set-battery-ammo :left 2))
          result (core/handle state {:type :fire :battery :left})]
      (should (core/sfx-emitted? (:state result) :sfx/launch))
      (should (core/sfx-emitted? (:state result) :sfx/low-ammo))
      (should= 1 (:missiles (core/battery (:state result) :left)))))

  (it "emits explosion when a fireball destroys an enemy"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    core/start-game
                    (core/spawn-enemy-targeting-city 1)
                    (core/add-static-fireball 400 250 40)
                    (core/route-enemy-through-point 400 250))
          end (loop [s state n 0]
                (cond
                  (empty? (core/enemy-missiles s)) s
                  (> n 5000) s
                  :else (recur (:state (core/tick s 0.05)) (inc n))))]
      (should= :fireball (core/last-enemy-fate end))
      (should (core/sfx-emitted? end :sfx/explosion))))

  (it "emits city-destroyed on city impact"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    core/start-game
                    (core/spawn-enemy-targeting-city 0))
          end (loop [s state n 0]
                (cond
                  (empty? (core/enemy-missiles s)) s
                  (> n 5000) s
                  :else (recur (:state (core/tick s 0.05)) (inc n))))]
      (should-not (core/living-city? end 0))
      (should (core/sfx-emitted? end :sfx/city-destroyed))))

  (it "emits battery-destroyed on battery impact"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    core/start-game
                    (core/spawn-enemy-targeting-battery :left))
          end (loop [s state n 0]
                (cond
                  (empty? (core/enemy-missiles s)) s
                  (> n 5000) s
                  :else (recur (:state (core/tick s 0.05)) (inc n))))]
      (should (:destroyed? (core/battery end :left)))
      (should (core/sfx-emitted? end :sfx/battery-destroyed))))

  (it "emits wave-clear when the wave completes"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    core/start-game
                    (core/set-wave-enemies-active 1))
          end (loop [s state n 0]
                (cond
                  (core/wave-complete? s) s
                  (> n 10000) s
                  :else (recur (:state (core/tick s 0.05)) (inc n))))]
      (should (core/wave-complete? end))
      (should (core/sfx-emitted? end :sfx/wave-clear))))

  (it "emits bonus-city when score crosses a threshold"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    core/start-game
                    (core/set-score 10000))]
      (should (core/sfx-emitted? state :sfx/bonus-city))
      (should= 1 (core/bonus-city-earned-events state))))

  (it "emits the-end when game over is entered"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    core/start-game
                    (#(reduce core/destroy-city % (map :id (core/cities %))))
                    (core/set-bonus-city-reserve 0)
                    core/evaluate-game-over)]
      (should (core/the-end? state))
      (should (core/sfx-emitted? state :sfx/the-end))))

  (it "still logs launch when muted"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    core/start-game
                    (core/set-mute true))
          result (core/handle state {:type :fire :battery :left})]
      (should (core/mute? (:state result)))
      (should (core/sfx-emitted? (:state result) :sfx/launch)))))
