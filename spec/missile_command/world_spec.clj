(ns missile-command.world-spec
  (:require [speclj.core :refer :all]
            [missile-command.world :as world]))

(describe "ground band"
  (it "occupies the bottom tenth of the playfield"
    (let [{:keys [top bottom]} (world/ground-band 600)]
      (should= 540 top)
      (should= 600 bottom)))

  (it "reports whether a y is in the band"
    (should (world/in-ground-band? 540 600))
    (should (world/in-ground-band? 590 600))
    (should-not (world/in-ground-band? 539 600))
    (should-not (world/in-ground-band? 601 600))))

(describe "city layout"
  (it "places six cities with increasing x"
    (let [xs (mapv :x (world/layout-cities 800 600))]
      (should= 6 (count xs))
      (should (apply < xs))))

  (it "keeps every city in playfield and on the ground"
    (doseq [city (world/layout-cities 1920 1080)]
      (should (<= 0 (:x city)))
      (should (< (:x city) 1920))
      (should (world/in-ground-band? (:y city) 1080))
      (should (:alive? city))))

  (it "spans more than half the width but less than full width"
    (let [xs (mapv :x (world/layout-cities 800 600))
          span (- (apply max xs) (apply min xs))]
      (should (> span 400))
      (should (< span 800))))

  (it "scales city positions with width"
    (let [narrow (mapv :x (world/layout-cities 800 600))
          wide (mapv :x (world/layout-cities 1600 600))]
      (should (> (- (last wide) (first wide))
                 (- (last narrow) (first narrow)))))))

(describe "battery layout"
  (it "places left center and right batteries left-to-right on the ground"
    (let [batteries (world/layout-batteries 800 600)
          by-id (into {} (map (juxt :id identity) batteries))]
      (should= [:left :center :right] (mapv :id batteries))
      (should (< (get-in by-id [:left :x]) (get-in by-id [:center :x])))
      (should (< (get-in by-id [:center :x]) (get-in by-id [:right :x])))
      (should (< (get-in by-id [:left :x]) (/ 800 3.0)))
      (should (< (/ 800 3.0) (get-in by-id [:center :x]) (* 800 (/ 2.0 3))))
      (should (> (get-in by-id [:right :x]) (* 800 (/ 2.0 3))))
      (doseq [b batteries]
        (should (world/in-ground-band? (:y b) 600))
        (should-not (:destroyed? b))
        (should= 10 (:missiles b)))))

  (it "gives the center battery a higher missile speed"
    (let [batteries (world/layout-batteries 800 600)
          by-id (into {} (map (juxt :id identity) batteries))]
      (should (> (get-in by-id [:center :missile-speed])
                 (get-in by-id [:left :missile-speed])))
      (should (> (get-in by-id [:center :missile-speed])
                 (get-in by-id [:right :missile-speed]))))))
