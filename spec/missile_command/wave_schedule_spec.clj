(ns missile-command.wave-schedule-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]
            [missile-command.wave-schedule :as wave-schedule]))

(describe "wave-schedule activate"
  (it "attack 1 is ballistic-only; final attack adds specials on a late wave"
    (let [first-attack (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                           (core/set-wave 9)
                           core/activate-wave-schedule)
          final (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                    (core/set-wave 9)
                    (core/begin-wave-attack 3))
          metrics (core/wave-schedule-metrics-for final 9)]
      (should= 1 (:wave-attack first-attack))
      (should= (:enemy-count metrics)
               (count (filter #(= core/enemy-kind-ballistic (:enemy-kind %))
                              (core/enemy-missiles first-attack))))
      (should= 0 (count (core/mirv-parents first-attack)))
      (should= 3 (:wave-attack final))
      (should= (:mirv-count metrics) (count (core/mirv-parents final)))
      (should= (:smart-bomb-count metrics) (count (core/smart-bombs final)))
      (should= (:bomber-count metrics) (count (core/flyers-of-kind final :bomber)))
      (should= (:satellite-count metrics) (count (core/flyers-of-kind final :satellite)))))

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

(describe "wave-schedule ensure attack start"
  (it "needs-attack-start? when sky clear, nil attack, not complete"
    (let [state (assoc (core/new-game {:width 800 :height 600}) :screen :playing)]
      (should (wave-schedule/needs-attack-start? state))
      (should-not (wave-schedule/needs-attack-start?
                   (assoc state :wave-attack 1)))
      (should-not (wave-schedule/needs-attack-start?
                   (assoc state :wave-complete? true)))
      (should-not (wave-schedule/needs-attack-start?
                   (assoc state :enemy-missiles [{:id 0}])))))

  (it "ensure-attack-started invokes begin-fn only when needed"
    (let [calls (atom 0)
          begin (fn [s] (swap! calls inc) (assoc s :started true))
          base (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
          started (wave-schedule/ensure-attack-started base begin)
          again (wave-schedule/ensure-attack-started
                 (assoc base :wave-attack 1) begin)]
      (should= 1 @calls)
      (should (:started started))
      (should-not (:started again)))))
