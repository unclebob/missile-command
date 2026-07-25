(ns missile-command.wave-schedule-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]
            [missile-command.wave-schedule :as wave-schedule]))

(describe "wave-schedule activate"
  (it "spawns ballistics MIRVs smart bombs and flyers on the final sequential attack"
    ;; Specials join attack 3 only; activate-wave-schedule starts attack 1.
    (let [state (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                    (core/set-wave 9)
                    (core/begin-wave-attack 3))
          metrics (core/wave-schedule-metrics-for state 9)]
      (should= (:enemy-count metrics)
               (count (filter #(= core/enemy-kind-ballistic (:enemy-kind %))
                              (core/enemy-missiles state))))
      (should= (:mirv-count metrics) (count (core/mirv-parents state)))
      (should= (:smart-bomb-count metrics) (count (core/smart-bombs state)))
      (should= (:bomber-count metrics) (count (core/flyers-of-kind state :bomber)))
      (should= (:satellite-count metrics) (count (core/flyers-of-kind state :satellite)))))

  (it "uses staggered flyer drop progresses for multiple drops"
    (let [one (#'wave-schedule/flyer-drop-progresses 1)
          many (#'wave-schedule/flyer-drop-progresses 3)]
      (should= 1 (count one))
      (should= 3 (count many))
      (should (< (first many) (last many)))))

  (it "cycles living city ids when requesting more than available"
    (let [ids (#'wave-schedule/cycle-living-city-ids
               [{:id 0} {:id 1}] 5)]
      (should= [0 1 0 1 0] ids)))

  (it "returns empty city ids when none live"
    (should= [] (#'wave-schedule/cycle-living-city-ids [] 3))))
