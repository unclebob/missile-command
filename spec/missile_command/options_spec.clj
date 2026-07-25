(ns missile-command.options-spec
  (:require [speclj.core :refer :all]
            [missile-command.options :as opts]
            [missile-command.waves :as waves]
            [missile-command.core :as core]))

(describe "options defaults and mute"
  (it "defaults to unmuted arcade with dual fire keys and pause p/escape"
    (let [o opts/default-options]
      (should-not (opts/mute? o))
      (should= :arcade (opts/difficulty o))
      (should (opts/fire-key-includes? o :left "z"))
      (should (opts/fire-key-includes? o :left "1"))
      (should (opts/fire-key-includes? o :center "x"))
      (should (opts/fire-key-includes? o :center "2"))
      (should (opts/fire-key-includes? o :right "c"))
      (should (opts/fire-key-includes? o :right "3"))
      (should (opts/pause-key-includes? o "p"))
      (should (opts/pause-key-includes? o "escape"))))

  (it "toggles mute"
    (should (opts/mute? (opts/set-mute opts/default-options true)))
    (should-not (opts/mute? (opts/set-mute opts/default-options false)))))

(describe "difficulty scaling"
  (it "uses arcade 1.0, normal 0.85, easy 0.7"
    (should= 1.0 (opts/difficulty-factor :arcade))
    (should= 0.85 (opts/difficulty-factor :normal))
    (should= 0.7 (opts/difficulty-factor :easy)))

  (it "scales wave 1 count and speed for each preset"
    (let [base-c (waves/enemy-count 1)
          base-s (waves/enemy-speed 1)]
      (should= 3 base-c)
      (should= 40.0 base-s)
      (should= 3 (opts/scale-enemy-count base-c 1.0))
      (should= 2 (opts/scale-enemy-count base-c 0.85))
      (should= 2 (opts/scale-enemy-count base-c 0.7))
      (should= 40.0 (opts/scale-enemy-speed base-s 1.0))
      (should= 34.0 (opts/scale-enemy-speed base-s 0.85))
      (should= 28.0 (opts/scale-enemy-speed base-s 0.7))))

  (it "scales schedule metrics through waves/schedule-metrics"
    (let [easy (waves/schedule-metrics 1 :easy)
          normal (waves/schedule-metrics 3 :normal)
          arcade (waves/schedule-metrics 3 :arcade)]
      (should= 2 (:enemy-count easy))
      (should= 28.0 (:enemy-speed easy))
      (should= 3 (:enemy-count arcade))
      (should= 50.0 (:enemy-speed arcade))
      (should= 2 (opts/scale-enemy-count 3 0.85))
      (should= 42.5 (opts/scale-enemy-speed 50.0 0.85))
      (should= (:enemy-count (waves/schedule-metrics 3 :normal))
               (:enemy-count normal)))))

(describe "key remapping"
  (it "binds a fire key and resolves presses"
    (let [o (opts/bind-fire-key opts/default-options :left "q")]
      (should (opts/fire-key-includes? o :left "q"))
      (should-not (opts/fire-key-includes? o :left "z"))
      (should= :left (opts/key->battery o "q"))
      (should= :center (opts/key->battery o "x"))
      (should-be-nil (opts/key->battery o "z")))))

(describe "options screens"
  (it "opens options from title and leaves back to title"
    (let [state (core/new-game {:width 800 :height 600})
          opened (core/open-options state)
          left (core/leave-options opened)]
      (should (core/title? state))
      (should (core/options? opened))
      (should (core/title? left))))

  (it "stores mute across leave and reopen"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    core/open-options
                    (core/set-mute true)
                    core/leave-options
                    core/open-options)]
      (should (core/mute? state))))

  (it "carries remapped fire keys into a started game"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    core/open-options
                    (core/bind-fire-key :left "q")
                    core/leave-options
                    core/start-game)
          aimed (:state (core/handle state {:type :aim :x 400 :y 200}))
          fired (:state (core/press-key aimed "q"))]
      (should= 1 (count (core/defensive-missiles fired)))
      (should= :left (:battery (first (core/defensive-missiles fired))))
      (should= 9 (:missiles (core/battery fired :left)))))

  (it "applies difficulty to wave metrics after start"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    core/open-options
                    (core/set-difficulty :easy)
                    core/leave-options
                    core/start-game)
          metrics (core/wave-schedule-metrics-for state 1)]
      (should= :easy (core/difficulty state))
      (should= 2 (:enemy-count metrics))
      (should= 28.0 (:enemy-speed metrics)))))
