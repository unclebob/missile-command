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
        (should= 10 (:missiles b)))))

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
      (should= {:x 250 :y 150} (core/crosshair after)))))

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
                   (mapv :x (core/cities after))))))
