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

  (it "starts with score zero and a crosshair on the playfield"
    (let [state (core/new-game {:width 800 :height 600})
          crosshair (core/crosshair state)]
      (should= 0 (core/score state))
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
      (should= :left (:battery missile))
      (should= 400 (:x1 missile))
      (should= 200 (:y1 missile))
      (should= (:x (core/battery state :left)) (:x0 missile))
      (should= (:y (core/battery state :left)) (:y0 missile))
      (should= (:missile-speed (core/battery state :left)) (:speed missile))))

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
      (should= {:x 399 :y 299} crosshair))))
