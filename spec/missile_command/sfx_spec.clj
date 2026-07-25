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

  (it "emits warning once while on the title screen"
    (let [state (core/new-game {:width 800 :height 600})
          t1 (:state (core/tick state 0.05))
          t2 (:state (core/tick t1 0.05))
          playing (core/start-game t2)
          ;; Fresh playing shell has no title flag; returning to title re-emits.
          back-to-title (assoc playing :screen :title)
          t3 (:state (core/tick back-to-title 0.05))
          t4 (:state (core/tick t3 0.05))]
      (should (core/title? t1))
      (should (core/sfx-emitted? t1 :sfx/warning))
      ;; second tick on title does not double-emit
      (should= 1 (count (filter #(= :sfx/warning (:type %)) (core/sfx-events t2))))
      (should-not (core/title? playing))
      (should (core/title? t3))
      (should (core/sfx-emitted? t3 :sfx/warning))
      (should= 1 (count (filter #(= :sfx/warning (:type %)) (core/sfx-events t3))))
      (should= 1 (count (filter #(= :sfx/warning (:type %)) (core/sfx-events t4))))))

  (it "emits boom when a defensive missile arrives and becomes a fireball"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    core/start-game
                    (core/set-battery-ammo :center 10))
          fired (:state (core/handle state {:type :fire :battery :center}))
          end (loop [s fired n 0]
                (cond
                  (seq (core/fireballs s)) s
                  (> n 5000) s
                  :else (recur (:state (core/tick s 0.05)) (inc n))))]
      (should (seq (core/fireballs end)))
      (should (core/sfx-emitted? end :sfx/boom))))

  (it "emits intercepted when a fireball destroys an enemy"
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
      (should (core/sfx-emitted? end :sfx/intercepted))))

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

  (it "emits wave when the wave banner is shown"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    core/start-game
                    (core/set-wave-enemies-active 1))
          end (loop [s state n 0]
                (cond
                  (core/wave-banner? s) s
                  (core/wave-complete? s) s
                  (> n 10000) s
                  :else (recur (:state (core/tick s 0.05)) (inc n))))]
      (should (or (core/wave-banner? end) (core/wave-complete? end)))
      (should (core/sfx-emitted? end :sfx/wave))))

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
