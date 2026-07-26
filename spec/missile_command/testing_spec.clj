(ns missile-command.testing-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]
            [missile-command.testing :as testing]))

(describe "testing staging helpers"
  (it "owns static fireball staging while core keeps a compatibility wrapper"
    (let [base (core/new-game {:width 800 :height 600})
          staged (testing/add-static-fireball base 100 120 25.0)
          compat (core/add-static-fireball base 100 120 25.0)]
      (should= (core/fireballs staged) (core/fireballs compat))
      (should= 1 (count (core/fireballs staged)))))

  (it "owns score and wave staging while core keeps compatibility wrappers"
    (let [base (core/new-game {:width 800 :height 600})
          staged (-> base
                     (testing/set-score 2500)
                     (testing/set-wave 4))
          compat (-> base
                     (core/set-score 2500)
                     (core/set-wave 4))]
      (should= (core/score staged) (core/score compat))
      (should= (core/wave staged) (core/wave compat))))

  (it "stages enemy and target helpers directly"
    (let [base (core/new-game {:width 800 :height 600})
          target-state (testing/add-destroyable-target base 111 222)
          city-state (testing/spawn-enemy-targeting-city-from base 12 0 0)
          battery-state (testing/spawn-enemy-targeting-battery-from base 34 0 :left)
          many-state (testing/spawn-enemies-targeting-distinct-cities base 2)
          target (first (:destroyable-targets target-state))
          city-enemy (first (core/enemy-missiles city-state))
          battery-enemy (first (core/enemy-missiles battery-state))]
      (should= 111 (:x target))
      (should= 222 (:y target))
      (should= :city (:target-kind city-enemy))
      (should= 0 (:target-id city-enemy))
      (should= 12 (:x0 city-enemy))
      (should= :battery (:target-kind battery-enemy))
      (should= :left (:target-id battery-enemy))
      (should= 34 (:x0 battery-enemy))
      (should= 2 (count (core/enemy-missiles many-state)))))

  (it "rejects unknown explicit enemy targets"
    (let [base (core/new-game {:width 800 :height 600})]
      (should-throw Exception
        (testing/spawn-enemy-targeting-city-from base 0 0 99))
      (should-throw Exception
        (testing/spawn-enemy-targeting-battery-from base 0 0 :missing))))

  (it "stages special enemies and retargets them through fireballs"
    (let [base (core/new-game {:width 800 :height 600})
          smart (testing/spawn-smart-bomb-targeting-city base 0)
          centered (testing/route-smart-bomb-centered-in-fireball smart 300 200 10)
          edge (testing/route-smart-bomb-edge-band-in-fireball smart 300 200 20 40)
          mirv (testing/spawn-mirv-targeting-city base 0 3 0.5)
          routed-mirv (testing/route-first-mirv-child-through-point mirv 123 45)
          smart-enemy (first (core/enemy-missiles centered))
          edge-enemy (first (core/enemy-missiles edge))
          mirv-enemy (first (core/enemy-missiles routed-mirv))]
      (should= :smart (:enemy-kind smart-enemy))
      (should= 300 (:x0 smart-enemy))
      (should= 200 (:y0 smart-enemy))
      (should= 330.0 (:x0 edge-enemy))
      (should= 200.0 (:y0 edge-enemy))
      (should= :mirv (:enemy-kind mirv-enemy))
      (should= 3 (:child-count mirv-enemy))
      (should= 0.5 (:split-progress mirv-enemy))
      (should= (core/enemy-missiles mirv)
               (core/enemy-missiles routed-mirv))))

  (it "leaves enemy and flyer retargeting unchanged when none exist"
    (let [base (core/new-game {:width 800 :height 600})]
      (should= [] (core/enemy-missiles
                   (testing/route-enemy-through-point base 1 2)))
      (should= [] (:flyers
                   (testing/route-flyer-through-point base 1 2)))
      (should= [] (:flyers
                   (testing/set-flyer-drops base [{:id 0}])))))

  (it "stages flyers and drop schedules"
    (let [base (core/new-game {:width 800 :height 600})
          with-flyer (testing/spawn-flyer base :bomber 0 80 800 80 100)
          routed (testing/route-flyer-through-point with-flyer 50 60)
          explicit (testing/set-flyer-drop-targeting-city routed 1 0.25)
          cycled (testing/set-flyer-drops-toward-living-cities routed 8 0.5)
          flyer (first (:flyers routed))
          explicit-drop (first (:drops (first (:flyers explicit))))
          cycled-drops (:drops (first (:flyers cycled)))]
      (should (:wave-had-enemies? with-flyer))
      (should-not (:wave-complete? with-flyer))
      (should= 50.0 (:x0 flyer))
      (should= 60.0 (:y0 flyer))
      (should= 0.0 (:progress flyer))
      (should= :bomber (:kind (first (testing/flyers-of-kind routed :bomber))))
      (should= [:city 1] (:target explicit-drop))
      (should= 0.25 (:at-progress explicit-drop))
      (should= 8 (count cycled-drops))
      (should= [:city 0] (:target (first cycled-drops)))
      (should= [:city 1] (:target (second cycled-drops)))))

  (it "stages scheduled wave attacks"
    (let [base (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
          battery-targeted (testing/spawn-wave-enemy-targeting-battery base :left)
          active (testing/set-wave-enemies-active base 2)
          attack (testing/begin-wave-attack base 1)
          alias-attack (testing/start-wave-attack base 1)
          activated (testing/activate-wave-schedule base)]
      (should= :battery (:target-kind (first (core/enemy-missiles battery-targeted))))
      (should= :left (:target-id (first (core/enemy-missiles battery-targeted))))
      (should= 2 (count (core/enemy-missiles active)))
      (should (seq (core/enemy-missiles attack)))
      (should= (count (core/enemy-missiles attack))
               (count (core/enemy-missiles alias-attack)))
      (should= (count (core/enemy-missiles attack))
               (count (core/enemy-missiles activated))))))
