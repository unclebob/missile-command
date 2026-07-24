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
        (should= 10 (:missiles b))))))

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
      (should (every? #(core/city-on-ground? after %) (core/cities after))))))
