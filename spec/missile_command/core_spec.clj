(ns missile-command.core-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]))

(describe "new-game"
  (it "records the playfield width and height"
    (let [state (core/new-game {:width 800 :height 600})]
      (should= 800 (core/playfield-width state))
      (should= 600 (core/playfield-height state))))

  (it "records other playfield sizes"
    (let [state (core/new-game {:width 1920 :height 1080})]
      (should= 1920 (core/playfield-width state))
      (should= 1080 (core/playfield-height state))))

  (it "starts with six living cities and three full batteries"
    (let [state (core/new-game {:width 800 :height 600})]
      (should= 6 (count (core/living-cities state)))
      (should= 3 (count (core/batteries state)))
      (should= #{:left :center :right}
               (set (map :id (core/batteries state))))
      (doseq [b (core/batteries state)]
        (should-not (:destroyed? b))
        (should= 10 (:missiles b)))
      (doseq [c (core/cities state)]
        (should (core/on-ground? state c)))
      (doseq [b (core/batteries state)]
        (should (core/on-ground? state b)))))

  (it "starts with score zero, one times multiplier, and a crosshair on the playfield"
    (let [state (core/new-game {:width 800 :height 600})
          crosshair (core/crosshair state)]
      (should= 0 (core/score state))
      (should= 1 (core/multiplier state))
      (should= 0 (:score (core/hud state)))
      (should= 1 (:multiplier (core/hud state)))
      (should= 0 (:next-entity-id state))
      (should= [] (core/defensive-missiles state))
      (should (<= 0 (:x crosshair)))
      (should (< (:x crosshair) 800))
      (should (<= 0 (:y crosshair)))
      (should (< (:y crosshair) 600)))))

(describe "aim"
  (it "moves the crosshair to an in-bounds point"
    (let [state (core/new-game {:width 800 :height 600})
          result (core/handle state {:type :aim :x 100 :y 200})]
      (should= {:x 100 :y 200} (core/crosshair (:state result)))
      (should= [] (:events result))))

  (it "clamps aim points outside the playfield"
    (let [state (core/new-game {:width 800 :height 600})]
      (should= {:x 0 :y 100}
               (core/crosshair (:state (core/handle state {:type :aim :x -10 :y 100}))))
      (should= {:x 799 :y 100}
               (core/crosshair (:state (core/handle state {:type :aim :x 900 :y 100}))))
      (should= {:x 100 :y 0}
               (core/crosshair (:state (core/handle state {:type :aim :x 100 :y -5}))))
      (should= {:x 100 :y 599}
               (core/crosshair (:state (core/handle state {:type :aim :x 100 :y 700}))))
      (should= {:x 799 :y 599}
               (core/crosshair (:state (core/handle state {:type :aim :x 9999 :y 9999}))))))

  (it "does not change cities batteries ammo or score when aiming"
    (let [before (core/new-game {:width 800 :height 600})
          after (:state (core/handle before {:type :aim :x 250 :y 150}))]
      (should= (core/cities before) (core/cities after))
      (should= (core/batteries before) (core/batteries after))
      (should= (core/score before) (core/score after))
      (should= {:x 250 :y 150} (core/crosshair after))))

  (it "rejects unsupported commands"
    (let [state (core/new-game {:width 800 :height 600})]
      (should-throw Exception #"unsupported command: :dance"
        (core/handle state {:type :dance})))))

(describe "fire battery"
  (it "launches a defensive missile toward the crosshair and spends ammo"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/handle {:type :aim :x 400 :y 200})
                    :state)
          result (core/handle state {:type :fire :battery :left})
          after (:state result)
          missiles (core/defensive-missiles after)
          missile (first missiles)]
      (should= 9 (:missiles (core/battery after :left)))
      (should= 1 (count missiles))
      (should= 0 (:id missile))
      (should= 1 (:next-entity-id after))
      (should= :left (:battery missile))
      (should= 400 (:x1 missile))
      (should= 200 (:y1 missile))
      (should= (:x (core/battery state :left)) (:x0 missile))
      (should= (:y (core/battery state :left)) (:y0 missile))
      (should= (:missile-speed (core/battery state :left)) (:speed missile))))

  (it "assigns increasing entity ids to successive launches"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/handle {:type :aim :x 400 :y 200})
                    :state)
          after (->> [:left :center :right]
                     (reduce (fn [s id]
                               (:state (core/handle s {:type :fire :battery id})))
                             state))
          ids (mapv :id (core/defensive-missiles after))]
      (should= [0 1 2] ids)
      (should= 3 (:next-entity-id after))))

  (it "starts entity ids from zero when next-entity-id is missing"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (dissoc :next-entity-id)
                    (core/handle {:type :aim :x 10 :y 20})
                    :state)
          after (:state (core/handle state {:type :fire :battery :left}))
          missile (first (core/defensive-missiles after))]
      (should= 0 (:id missile))
      (should= 1 (:next-entity-id after))))

  (it "does not spend ammo on other batteries"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/handle {:type :aim :x 400 :y 200})
                    :state
                    (#(:state (core/handle % {:type :fire :battery :center}))))]
      (should= 9 (:missiles (core/battery state :center)))
      (should= 10 (:missiles (core/battery state :left)))
      (should= 10 (:missiles (core/battery state :right)))))

  (it "does nothing when the battery is empty"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-battery-ammo :left 0))
          after (:state (core/handle state {:type :fire :battery :left}))]
      (should= 0 (:missiles (core/battery after :left)))
      (should= 0 (count (core/defensive-missiles after)))))

  (it "does nothing when the battery is destroyed"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/destroy-battery :center))
          after (:state (core/handle state {:type :fire :battery :center}))]
      (should= 0 (count (core/defensive-missiles after)))
      (should= 10 (:missiles (core/battery after :center)))
      (should (:destroyed? (core/battery after :center)))))

  (it "gives center missiles higher speed than side missiles"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/handle {:type :aim :x 400 :y 100})
                    :state)
          after (->> [:left :center :right]
                     (reduce (fn [s id]
                               (:state (core/handle s {:type :fire :battery id})))
                             state))
          by-battery (into {} (map (juxt :battery identity)
                                   (core/defensive-missiles after)))]
      (should= 3 (count by-battery))
      (should (> (:speed (by-battery :center))
                 (:speed (by-battery :left))))
      (should (> (:speed (by-battery :center))
                 (:speed (by-battery :right)))))))

(describe "click zone fire"
  (it "maps horizontal thirds to batteries and aims at the click"
    (let [state (core/new-game {:width 900 :height 600})
          left (:state (core/handle state {:type :click :x 0 :y 100}))
          center (:state (core/handle state {:type :click :x 300 :y 100}))
          right (:state (core/handle state {:type :click :x 600 :y 100}))]
      (should= :left (:battery (first (core/defensive-missiles left))))
      (should= {:x 0 :y 100} (core/crosshair left))
      (should= :center (:battery (first (core/defensive-missiles center))))
      (should= :right (:battery (first (core/defensive-missiles right))))))

  (it "falls back along the zone order when preferred battery cannot fire"
    (let [state (-> (core/new-game {:width 900 :height 600})
                    (core/set-battery-ammo :left 0))
          after (:state (core/handle state {:type :click :x 100 :y 100}))]
      (should= :center (:battery (first (core/defensive-missiles after))))
      (should= 9 (:missiles (core/battery after :center)))))

  (it "updates the crosshair but launches nothing when no battery can fire"
    (let [state (-> (core/new-game {:width 900 :height 600})
                    (core/set-battery-ammo :left 0)
                    (core/set-battery-ammo :center 0)
                    (core/set-battery-ammo :right 0))
          after (:state (core/handle state {:type :click :x 450 :y 100}))]
      (should= 0 (count (core/defensive-missiles after)))
      (should= {:x 450 :y 100} (core/crosshair after))))

  (it "keeps key fire as a no-op for empty batteries without fallback"
    (let [state (-> (core/new-game {:width 900 :height 600})
                    (core/set-battery-ammo :left 0)
                    (core/handle {:type :aim :x 100 :y 100})
                    :state)
          after (:state (core/handle state {:type :fire :battery :left}))]
      (should= 0 (count (core/defensive-missiles after)))))

  (it "recomputes zones after resize"
    (let [state (-> (core/new-game {:width 900 :height 600})
                    (core/resize 1800 600))
          after (:state (core/handle state {:type :click :x 500 :y 100}))]
      (should= :left (:battery (first (core/defensive-missiles after)))))))

(describe "waves and rearm"
  (it "starts at wave one with full ammo and incomplete wave flags"
    (let [state (core/new-game {:width 800 :height 600})]
      (should= false core/wave-flag-off)
      (should= true core/wave-flag-on)
      (should= false core/wave-starts-complete?)
      (should= false core/wave-starts-with-enemies?)
      (should= false core/target-starts-destroyed?)
      (should= 0 core/initial-entity-id)
      (should= 0 core/clamp-lo)
      (should= {:x 0 :y 0} core/default-crosshair)
      (should= 1 (core/wave state))
      (should= 1 (:wave (core/hud state)))
      (should-not (core/wave-complete? state))
      (should-not (:wave-had-enemies? state))
      (should= false (:wave-complete? state))
      (should= false (:wave-had-enemies? state))
      (should= 0 (:next-entity-id state))
      (doseq [b (core/batteries state)]
        (should= 10 (:missiles b)))))

  (it "marks that enemies have been seen and wave is not complete after spawn"
    (let [state (core/spawn-enemy-targeting-city
                 (core/new-game {:width 800 :height 600}) 0)]
      (should (:wave-had-enemies? state))
      (should= true (:wave-had-enemies? state))
      (should-not (core/wave-complete? state))
      (should= false (:wave-complete? state))
      (should= 1 (count (core/enemy-missiles state)))))

  (it "completes a wave when enemies are cleared and advances the number"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-wave-enemies-active 1))
          after (loop [s state n 0]
                  (if (or (core/wave-complete? s) (> n 5000))
                    s
                    (recur (:state (core/tick s 0.05)) (inc n))))]
      (should (core/wave-complete? after))
      (should= true (:wave-complete? after))
      (should= false (:wave-had-enemies? after))
      (should= 2 (core/wave after))
      (should (empty? (core/enemy-missiles after)))))

  (it "does not complete a wave while enemies remain"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-wave-enemies-active 1))
          after (:state (core/tick state 0.05))]
      (should-not (core/wave-complete? after))
      (should (:wave-had-enemies? after))
      (should= 1 (core/wave after))
      (should= 1 (count (core/enemy-missiles after)))))

  (it "does not complete when enemies never appeared"
    (let [state (core/new-game {:width 800 :height 600})
          after (:state (core/tick state 0.1))]
      (should-not (core/wave-complete? after))
      (should= 1 (core/wave after))))

  (it "set-wave-enemies-active with zero does not mark enemies seen"
    (let [state (core/set-wave-enemies-active
                 (core/new-game {:width 800 :height 600}) 0)]
      (should= false (:wave-had-enemies? state))
      (should= false (:wave-complete? state))
      (should (empty? (core/enemy-missiles state)))))

  (it "set-wave clears enemies and wave flags"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-wave-enemies-active 2)
                    (core/set-wave 4))]
      (should= 4 (core/wave state))
      (should= false (:wave-complete? state))
      (should= false (:wave-had-enemies? state))
      (should (empty? (core/enemy-missiles state)))))

  (it "start-next-wave rearms and clears complete flags"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-non-destroyed-battery-ammo 1)
                    (assoc :wave-complete? true
                           :wave-had-enemies? true
                           :wave 2)
                    (core/start-next-wave))]
      (should= false (:wave-complete? state))
      (should= false (:wave-had-enemies? state))
      (should= 2 (core/wave state))
      (doseq [b (core/batteries state)]
        (should= 10 (:missiles b)))))

  (it "rearms surviving batteries but not destroyed ones"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-battery-ammo :left 3)
                    (core/destroy-battery :left)
                    (core/set-non-destroyed-battery-ammo 2)
                    (core/rearm-surviving-batteries))]
      (should (:destroyed? (core/battery state :left)))
      (should= 3 (:missiles (core/battery state :left)))
      (should= 10 (:missiles (core/battery state :center)))
      (should= 10 (:missiles (core/battery state :right)))))

  (it "exposes wave schedule metrics and harder-wave?"
    (let [low (core/wave-schedule-metrics 1)
          high (core/wave-schedule-metrics 3)]
      (should (core/harder-wave? low high))
      (should-not (core/harder-wave? high low))))

  (it "throws on unknown city or battery spawn targets"
    (let [state (core/new-game {:width 800 :height 600})]
      (should-throw (core/spawn-enemy-targeting-city state 99))
      (should-throw (core/spawn-enemy-targeting-battery state :missing))))

  (it "set-wave-enemies-active with no living cities leaves no enemies"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (assoc :cities [])
                    (core/set-wave-enemies-active 2))]
      (should (empty? (core/enemy-missiles state)))
      (should (:wave-had-enemies? state))))

  (it "resize reclamps when crosshair is missing"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (dissoc :crosshair)
                    (core/resize 400 300))
          ch (core/crosshair state)]
      (should (<= 0 (:x ch)))
      (should (<= 0 (:y ch)))
      (should (< (:x ch) 400))
      (should (< (:y ch) 300))))

  (it "add-destroyable-target starts not destroyed"
    (let [state (core/add-destroyable-target
                 (core/new-game {:width 800 :height 600}) 10 20)
          t (first (core/destroyable-targets state))]
      (should= false (:destroyed? t))
      (should= 10 (:x t))
      (should= 20 (:y t)))))

(describe "enemy missiles"
  (it "spawns city-bound enemies from the top of the sky"
    (let [state (core/spawn-enemy-targeting-city
                 (core/new-game {:width 800 :height 600}) 0)
          m (first (core/enemy-missiles state))]
      (should= 0.0 (double (:y0 m)))
      (should= 0.0 (double (:y m)))
      (should= 0 (:id (first (filter #(= 0 (:id %)) (core/cities state)))))))

  (it "spawns battery-bound enemies from the top of the sky"
    (let [state (core/spawn-enemy-targeting-battery
                 (core/new-game {:width 800 :height 600}) :left)
          m (first (core/enemy-missiles state))]
      (should= 0.0 (double (:y0 m)))
      (should= 0.0 (double (:y m)))
      (should= :battery (:target-kind m))
      (should= :left (:target-id m))))

  (it "spawns a city-bound enemy from an explicit angled sky origin"
    (let [state (core/spawn-enemy-targeting-city-from
                 (core/new-game {:width 800 :height 600}) 50 0 0)
          city (core/city state 0)
          m (first (core/enemy-missiles state))]
      (should= 50.0 (double (:x0 m)))
      (should= 0.0 (double (:y0 m)))
      (should= 50.0 (double (:x m)))
      (should= 0.0 (double (:y m)))
      (should= (double (:x city)) (double (:x1 m)))
      (should-not= (double (:x0 m)) (double (:x1 m)))))

  (it "moves an angled enemy toward its target on both axes"
    (let [state (core/spawn-enemy-targeting-city-from
                 (core/new-game {:width 800 :height 600}) 50 0 0)
          before (first (core/enemy-missiles state))
          after (:state (core/tick state 0.1))
          m (first (core/enemy-missiles after))]
      (should (< (double (:x before)) (double (:x m)) (double (:x1 before))))
      (should (< (double (:y before)) (double (:y m)) (double (:y1 before))))
      (should (pos? (double (:progress m))))))

  (it "destroys a city when an angled enemy impacts unintercepted"
    (let [state (core/spawn-enemy-targeting-city-from
                 (core/new-game {:width 800 :height 600}) 50 0 0)
          after (loop [s state n 0]
                  (if (or (empty? (core/enemy-missiles s)) (> n 5000))
                    s
                    (recur (:state (core/tick s 0.05)) (inc n))))]
      (should-not (core/living-city? after 0))
      (should= 5 (count (core/living-cities after)))
      (should= :impact (core/last-enemy-fate after))
      (should (empty? (core/enemy-missiles after)))))

  (it "destroys a battery when an angled enemy impacts unintercepted"
    (let [state (core/spawn-enemy-targeting-battery-from
                 (core/new-game {:width 800 :height 600}) 200 0 :left)
          after (loop [s state n 0]
                  (if (or (empty? (core/enemy-missiles s)) (> n 5000))
                    s
                    (recur (:state (core/tick s 0.05)) (inc n))))]
      (should (:destroyed? (core/battery after :left)))
      (should= :impact (core/last-enemy-fate after))
      (should (empty? (core/enemy-missiles after)))))

  (it "wave schedule uses varied sky origins not locked to targets"
    (let [state (core/set-wave-enemies-active
                 (core/new-game {:width 800 :height 600}) 3)
          enemies (core/enemy-missiles state)
          origin-xs (mapv #(double (:x0 %)) enemies)
          width (core/playfield-width state)]
      (should= 3 (count enemies))
      (should (every? #(= 0.0 (double (:y0 %))) enemies))
      (should (every? #(and (<= 0 %) (< % width)) origin-xs))
      (should (< 1 (count (set origin-xs))))
      (should (some #(not= (double (:x0 %)) (double (:x1 %))) enemies))))

  (it "route-enemy-through-point is a no-op without enemies"
    (let [state (core/new-game {:width 800 :height 600})
          after (core/route-enemy-through-point state 10 20)]
      (should= [] (core/enemy-missiles after))))

  (it "destroys a city on impact"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/spawn-enemy-targeting-city 0))
          after (loop [s state n 0]
                  (if (or (empty? (core/enemy-missiles s)) (> n 2000))
                    s
                    (recur (:state (core/tick s 0.05)) (inc n))))]
      (should-not (core/living-city? after 0))
      (should= 5 (count (core/living-cities after)))
      (should= :impact (core/last-enemy-fate after))
      (should (pos? (count (core/fireballs after))))))

  (it "destroys a battery on impact"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/spawn-enemy-targeting-battery :left))
          after (loop [s state n 0]
                  (if (or (empty? (core/enemy-missiles s)) (> n 2000))
                    s
                    (recur (:state (core/tick s 0.05)) (inc n))))]
      (should (:destroyed? (core/battery after :left)))
      (should= :impact (core/last-enemy-fate after))
      (should (pos? (count (core/fireballs after))))))

  (it "is destroyed by a fireball without impacting its target"
    (let [city (first (core/cities (core/new-game {:width 800 :height 600})))
          state (-> (core/new-game {:width 800 :height 600})
                    (core/spawn-enemy-targeting-city (:id city))
                    (core/add-static-fireball (:x city) 200 50)
                    (core/route-enemy-through-point (:x city) 200))
          after (loop [s state n 0]
                  (if (or (empty? (core/enemy-missiles s))
                          (= :fireball (core/last-enemy-fate s))
                          (> n 2000))
                    s
                    (recur (:state (core/tick s 0.01)) (inc n))))]
      (should= :fireball (core/last-enemy-fate after))
      (should (core/living-city? after (:id city)))
      (should= 0 (count (core/enemy-missiles after)))
      ;; Last enemy: kill points plus wave-end bonuses (ammo + cities × mult).
      (should= (+ 25 (* 30 5) (* 6 100)) (core/score after)))))

(describe "scoring and multiplier"
  (it "tracks multiplier from the current wave"
    (should= 1 (core/multiplier (core/set-wave (core/new-game {:width 800 :height 600}) 1)))
    (should= 2 (core/multiplier (core/set-wave (core/new-game {:width 800 :height 600}) 3)))
    (should= 6 (core/multiplier (core/set-wave (core/new-game {:width 800 :height 600}) 11)))
    (should= 6 (core/multiplier (core/set-wave (core/new-game {:width 800 :height 600}) 20))))

  (it "awards twenty five times multiplier for a fireball kill without completing the wave"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-wave 3)
                    (core/set-wave-enemies-active 2)
                    (core/add-static-fireball 400 250 40)
                    (core/route-enemy-through-point 400 250))
          after (loop [s state n 0]
                  (if (or (= :fireball (core/last-enemy-fate s))
                          (> n 5000))
                    s
                    (recur (:state (core/tick s 0.01)) (inc n))))]
      (should= :fireball (core/last-enemy-fate after))
      (should= 1 (count (core/enemy-missiles after)))
      (should-not (core/wave-complete? after))
      (should= 2 (core/multiplier after))
      (should= 50 (core/score after))))

  (it "awards unused ammo and living cities at wave end times multiplier"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-wave 1)
                    (core/set-non-destroyed-battery-ammo 10)
                    (core/set-wave-enemies-active 1))
          after (loop [s state n 0]
                  (if (or (core/wave-complete? s) (> n 5000))
                    s
                    (recur (:state (core/tick s 0.05)) (inc n))))]
      (should (core/wave-complete? after))
      (should= 5 (count (core/living-cities after)))
      (should= 650 (core/score after))))

  (it "does not decrease score when aiming after a kill"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-wave-enemies-active 2)
                    (core/add-static-fireball 400 250 40)
                    (core/route-enemy-through-point 400 250))
          after-kill (loop [s state n 0]
                       (if (or (= :fireball (core/last-enemy-fate s))
                               (> n 5000))
                         s
                         (recur (:state (core/tick s 0.01)) (inc n))))
          after-aim (:state (core/handle after-kill {:type :aim :x 100 :y 100}))]
      (should= 25 (core/score after-kill))
      (should= 25 (core/score after-aim)))))

(describe "smart bombs"
  (it "advances toward its city target"
    (let [state (core/spawn-smart-bomb-targeting-city
                 (core/new-game {:width 800 :height 600}) 0)
          after (:state (core/tick state 0.1))
          b (first (core/smart-bombs after))]
      (should= 1 (count (core/smart-bombs after)))
      (should (pos? (double (:progress b))))
      (should= core/enemy-kind-smart (:enemy-kind b))))

  (it "is destroyed by a well-centered fireball for smart-bomb points"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/spawn-smart-bomb-targeting-city 1)
                    (core/add-static-fireball 400 250 40)
                    (core/route-smart-bomb-centered-in-fireball 400 250 15))
          after (loop [s state n 0]
                  (if (or (= :fireball (core/last-enemy-fate s))
                          (empty? (core/smart-bombs s))
                          (> n 5000))
                    s
                    (recur (:state (core/tick s 0.01)) (inc n))))]
      (should= :fireball (core/last-enemy-fate after))
      (should (empty? (core/enemy-missiles after)))
      ;; Last threat: smart-bomb kill points plus wave-end bonuses.
      (should= (+ 125 (* 30 5) (* 6 100)) (core/score after))
      (should= 6 (count (core/living-cities after)))))

  (it "evades an edge-of-blast fireball once and stays a threat"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/spawn-smart-bomb-targeting-city 1)
                    (core/add-static-fireball 400 250 40)
                    (core/route-smart-bomb-edge-band-in-fireball 400 250 25 40))
          after (loop [s state n 0]
                  (let [b (first (core/smart-bombs s))]
                    (if (or (and b (:smart-evaded? b))
                            (> n 5000))
                      s
                      (recur (:state (core/tick s 0.01)) (inc n)))))
          b (first (core/smart-bombs after))]
      (should b)
      (should (:smart-evaded? b))
      (should-not= :fireball (core/last-enemy-fate after))
      (should= 6 (count (core/living-cities after)))))

  (it "destroys its city when unintercepted"
    (let [state (core/spawn-smart-bomb-targeting-city
                 (core/new-game {:width 800 :height 600}) 0)
          after (loop [s state n 0]
                  (if (or (empty? (core/enemy-missiles s)) (> n 10000))
                    s
                    (recur (:state (core/tick s 0.05)) (inc n))))]
      (should-not (core/living-city? after 0))
      (should= 5 (count (core/living-cities after))))))

(describe "MIRV warheads"
  (it "flies as a single parent before the split progress"
    (let [state (core/spawn-mirv-targeting-city
                 (core/new-game {:width 800 :height 600}) 0 3 0.5)
          after (:state (core/tick state 0.05))
          m (first (core/enemy-missiles after))]
      (should= 1 (count (core/enemy-missiles after)))
      (should= 1 (count (core/mirv-parents after)))
      (should= core/enemy-kind-mirv (:enemy-kind m))
      (should (< (double (:progress m)) 0.5))))

  (it "splits into child warheads with independent city targets"
    (let [state (core/spawn-mirv-targeting-city
                 (core/new-game {:width 800 :height 600}) 0 3 0.5)
          after (loop [s state n 0]
                  (if (or (and (empty? (core/mirv-parents s))
                               (seq (core/mirv-children s)))
                          (> n 5000))
                    s
                    (recur (:state (core/tick s 0.05)) (inc n))))
          children (core/mirv-children after)
          targets (set (map :target-id children))]
      (should= 3 (count children))
      (should (empty? (core/mirv-parents after)))
      (should (< 1 (count targets)))
      (should (every? #(= core/enemy-kind-mirv-child (:enemy-kind %)) children))))

  (it "destroys a MIRV parent with a fireball before children appear"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/spawn-mirv-targeting-city 1 3 0.5)
                    (core/add-static-fireball 400 100 40)
                    (core/route-enemy-through-point 400 100))
          after (loop [s state n 0]
                  (if (or (= :fireball (core/last-enemy-fate s))
                          (> n 5000))
                    s
                    (recur (:state (core/tick s 0.01)) (inc n))))]
      (should= :fireball (core/last-enemy-fate after))
      (should (empty? (core/enemy-missiles after)))
      (should= 6 (count (core/living-cities after)))))

  (it "lets unintercepted children destroy their target cities"
    (let [state (core/spawn-mirv-targeting-city
                 (core/new-game {:width 800 :height 600}) 0 2 0.5)
          after (loop [s state n 0]
                  (if (or (and (empty? (core/enemy-missiles s))
                               (core/wave-complete? s))
                          (and (empty? (core/enemy-missiles s)) (> n 100))
                          (> n 10000))
                    s
                    (recur (:state (core/tick s 0.05)) (inc n))))]
      (should (empty? (core/enemy-missiles after)))
      (should= 4 (count (core/living-cities after)))))

  (it "route-first-mirv-child-through-point retargets only the first child"
    (let [state (core/spawn-mirv-targeting-city
                 (core/new-game {:width 800 :height 600}) 0 3 0.4)
          after-split (loop [s state n 0]
                        (if (or (and (empty? (core/mirv-parents s))
                                     (seq (core/mirv-children s)))
                                (> n 5000))
                          s
                          (recur (:state (core/tick s 0.05)) (inc n))))
          children (core/mirv-children after-split)
          routed (core/route-first-mirv-child-through-point after-split 111 222)
          first-child (first (core/mirv-children routed))
          others (rest (core/mirv-children routed))]
      (should= 3 (count children))
      (should= 111.0 (double (:x0 first-child)))
      (should= 222.0 (double (:y0 first-child)))
      (should= core/enemy-kind-mirv-child (:enemy-kind first-child))
      (should (every? #(not= 111.0 (double (:x0 %))) others))))

  (it "route-first-mirv-child-through-point is a no-op without MIRV children"
    (let [state (core/spawn-enemy-targeting-city
                 (core/new-game {:width 800 :height 600}) 0)
          after (core/route-first-mirv-child-through-point state 10 20)
          m (first (core/enemy-missiles after))]
      (should= 1 (count (core/enemy-missiles after)))
      (should-not= 10.0 (double (:x0 m))))))

(describe "bonus cities"
  (it "starts with empty reserve and default threshold"
    (let [state (core/new-game {:width 800 :height 600})]
      (should= 0 (core/bonus-cities state))
      (should= 10000 (core/bonus-city-threshold state))
      (should= 0 (core/bonus-city-earned-events state))
      (should= 0 (:bonus-cities (core/hud state)))))

  (it "awards reserve when score crosses the threshold and keeps six living cities"
    (let [under (core/set-score (core/new-game {:width 800 :height 600}) 9999)
          at (core/set-score (core/new-game {:width 800 :height 600}) 10000)
          multi (core/set-score (core/new-game {:width 800 :height 600}) 30000)]
      (should= 0 (core/bonus-cities under))
      (should= 0 (core/bonus-city-earned-events under))
      (should= 1 (core/bonus-cities at))
      (should= 1 (core/bonus-city-earned-events at))
      (should= 6 (count (core/living-cities at)))
      (should= 3 (core/bonus-cities multi))
      (should= 3 (core/bonus-city-earned-events multi))))

  (it "restores the lowest destroyed city when a bonus is earned"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/destroy-city 0)
                    (core/set-score 10000))]
      (should (core/living-city? state 0))
      (should= 6 (count (core/living-cities state)))
      (should= 0 (core/bonus-cities state))))

  (it "never places more than six living cities"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/destroy-city 0)
                    (core/destroy-city 1)
                    (core/set-score 30000))]
      (should= 6 (count (core/living-cities state)))
      (should= 1 (core/bonus-cities state))))

  (it "applies remaining reserve after wave resolution"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/destroy-city 0)
                    (core/destroy-city 1)
                    (core/set-bonus-city-reserve 3)
                    (core/apply-bonus-cities-from-reserve))]
      (should= 6 (count (core/living-cities state)))
      (should= 1 (core/bonus-cities state)))))

(describe "tick defensive missiles and fireballs"
  (it "advances defensive missiles toward the aim point"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/handle {:type :aim :x 400 :y 100})
                    :state
                    (#(:state (core/handle % {:type :fire :battery :center})))
                    (#(:state (core/tick % 0.05))))
          m (first (core/defensive-missiles state))]
      (should (seq (core/defensive-missiles state)))
      (should (< 0 (:progress m)))
      (should= 0.05 (core/last-applied-dt state))))

  (it "clamps large time steps so a missile does not instantly arrive"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/handle {:type :aim :x 400 :y 100})
                    :state
                    (#(:state (core/handle % {:type :fire :battery :center})))
                    (#(:state (core/tick % 5.0))))
          m (first (core/defensive-missiles state))]
      (should= 0.05 (core/last-applied-dt state))
      (should (seq (core/defensive-missiles state)))
      (should (< (:progress m) 1.0))))

  (it "turns arrived missiles into fireballs at the aim point"
    (let [state (loop [s (-> (core/new-game {:width 800 :height 600})
                             (core/handle {:type :aim :x 400 :y 100})
                             :state
                             (#(:state (core/handle % {:type :fire :battery :center}))))
                       n 0]
                  (if (or (empty? (core/defensive-missiles s)) (> n 1000))
                    s
                    (recur (:state (core/tick s 0.05)) (inc n))))
          fb (first (core/fireballs state))]
      (should= 0 (count (core/defensive-missiles state)))
      (should= 1 (count (core/fireballs state)))
      (should= 400 (:x fb))
      (should= 100 (:y fb))))

  (it "destroys targets inside a fireball and leaves distant targets alone"
    (let [base (-> (core/new-game {:width 800 :height 600})
                   (core/handle {:type :aim :x 400 :y 200})
                   :state
                   (#(:state (core/handle % {:type :fire :battery :center})))
                   (core/add-destroyable-target 400 200)
                   (core/add-destroyable-target 50 50))
          arrived (loop [s base n 0]
                    (if (or (empty? (core/defensive-missiles s)) (> n 1000))
                      s
                      (recur (:state (core/tick s 0.05)) (inc n))))
          peaked (loop [s arrived n 0]
                   (let [r (if (seq (core/fireballs s))
                             (apply max (map :radius (core/fireballs s)))
                             0.0)]
                     (if (or (>= r (core/max-fireball-radius s)) (> n 1000))
                       s
                       (recur (:state (core/tick s 0.05)) (inc n)))))
          targets (core/destroyable-targets peaked)
          by-pos (group-by (juxt :x :y) targets)]
      (should (:destroyed? (first (by-pos [400 200]))))
      (should-not (:destroyed? (first (by-pos [50 50])))))))

(describe "resize"
  (it "updates playfield size and reflows cities and batteries"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/resize 1920 1080))
          city-xs (mapv :x (core/cities state))
          left (core/battery state :left)
          right (core/battery state :right)]
      (should= 1920 (core/playfield-width state))
      (should= 1080 (core/playfield-height state))
      (should= 6 (count (core/living-cities state)))
      (should (every? #(and (<= 0 %) (< % 1920)) city-xs))
      (should (every? #(core/city-on-ground? state %) (core/cities state)))
      (should (< (:x left) (/ 1920 3.0)))
      (should (> (:x right) (* 1920 (/ 2.0 3))))
      (doseq [b (core/batteries state)]
        (should= 10 (:missiles b)))))

  (it "does not leave city positions from the previous size"
    (let [before (core/new-game {:width 800 :height 600})
          after (core/resize before 1600 600)]
      (should-not= (mapv :x (core/cities before))
                   (mapv :x (core/cities after)))))

  (it "preserves city alive flags and battery ammo across resize"
    (let [before (-> (core/new-game {:width 800 :height 600})
                     (update :cities (fn [cs]
                                       (mapv #(if (zero? (:id %))
                                                (assoc % :alive? false)
                                                %)
                                             cs)))
                     (update :batteries (fn [bs]
                                          (mapv #(if (= :left (:id %))
                                                   (assoc % :missiles 4 :destroyed? true)
                                                   %)
                                                bs))))
          after (core/resize before 1920 1080)
          left (core/battery after :left)]
      (should= 5 (count (core/living-cities after)))
      (should-not (:alive? (first (filter #(zero? (:id %)) (core/cities after)))))
      (should (:destroyed? left))
      (should= 4 (:missiles left))
      (should (every? #(core/city-on-ground? after %) (core/cities after)))))

  (it "reclamps the crosshair into the new playfield"
    (let [before (:state (core/handle (core/new-game {:width 800 :height 600})
                                      {:type :aim :x 790 :y 590}))
          after (core/resize before 400 300)
          crosshair (core/crosshair after)]
      (should= {:x 399 :y 299} crosshair)))

  (it "uses a zero origin when reclamp finds no crosshair"
    (let [before (dissoc (core/new-game {:width 800 :height 600}) :crosshair)
          after (core/resize before 800 600)]
      (should= {:x 0 :y 0} (core/crosshair after)))))
