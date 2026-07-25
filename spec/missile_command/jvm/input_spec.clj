(ns missile-command.jvm.input-spec
  (:require [speclj.core :refer :all]
            [missile-command.jvm.input :as input]
            [missile-command.core :as core]
            [clojure.string :as str]))

(describe "key-char->battery"
  (it "maps left fire keys"
    (should= :left (input/key-char->battery \z))
    (should= :left (input/key-char->battery \Z))
    (should= :left (input/key-char->battery \1)))

  (it "maps center fire keys"
    (should= :center (input/key-char->battery \x))
    (should= :center (input/key-char->battery \2)))

  (it "maps right fire keys"
    (should= :right (input/key-char->battery \c))
    (should= :right (input/key-char->battery \3)))

  (it "returns nil for unrelated keys"
    (should-be-nil (input/key-char->battery \p))
    (should-be-nil (input/key-char->battery nil))))

(describe "key-char->command"
  (it "builds fire commands for default keys"
    (should= {:type :fire :battery :left} (input/key-char->command \1))
    (should= {:type :fire :battery :center} (input/key-char->command \x)))

  (it "returns nil when the key is not a fire key"
    (should-be-nil (input/key-char->command \q))))

(describe "aim-command and click-command"
  (it "builds aim and click commands"
    (should= {:type :aim :x 10 :y 20} (input/aim-command 10 20))
    (should= {:type :click :x 30 :y 40} (input/click-command 30 40))))

(describe "escape-key?"
  (it "detects Esc"
    (should (input/escape-key? (char 27)))
    (should-not (input/escape-key? \q))))

(describe "parse-cli-args"
  (it "uses defaults when args are empty"
    (let [opts (input/parse-cli-args [] 800 600)]
      (should= 800 (:width opts))
      (should= 600 (:height opts))
      (should-not (:qa-telemetry? opts))
      (should= [] (:destroy-batteries opts))))

  (it "uses default size arity when only args are given"
    (let [opts (input/parse-cli-args ["1024" "768"])]
      (should= 1024 (:width opts))
      (should= 768 (:height opts))))

  (it "parses width height and switches"
    (let [opts (input/parse-cli-args
                ["1280" "720" "--qa-telemetry" "--destroy-batteries" "left,center"
                 "--qa-events" "tmp/events.txt" "--qa-target" "400,200"
                 "--qa-enemy" "city:0" "--qa-fireball" "10,20,30"
                 "--qa-speed" "8"]
                800 600)]
      (should= 1280 (:width opts))
      (should= 720 (:height opts))
      (should (:qa-telemetry? opts))
      (should= 8.0 (:qa-speed opts))
      (should= [:left :center] (:destroy-batteries opts))
      (should= "tmp/events.txt" (:qa-events opts))
      (should= [{:x 400 :y 200}] (:qa-targets opts))
      (should= [{:kind :city :id 0}] (:qa-enemies opts))
      (should= [{:x 10 :y 20 :radius 30.0}] (:qa-fireballs opts))))

  (it "assigns first size token to width and second to height only"
    (let [opts (input/parse-cli-args ["900" "500"] 800 600)]
      (should= 900 (:width opts))
      (should= 500 (:height opts))
      (should-not= (:width opts) (:height opts))))

  (it "rejects a third bare size token"
    (should-throw Exception
      (input/parse-cli-args ["800" "600" "1024"] 800 600)))

  (it "defaults qa-speed to 1.0"
    (let [opts (input/parse-cli-args [] 800 600)]
      (should= 1.0 (:qa-speed opts))))

  (it "accepts --qa as telemetry alias and --qa-scenario"
    (let [opts (input/parse-cli-args
                ["--qa" "--qa-scenario" "tmp/s.edn" "--qa-speed" "10.5"
                 "--scores-file" "tmp/scores.edn"]
                800 600)]
      (should (:qa-telemetry? opts))
      (should= "tmp/s.edn" (:qa-scenario opts))
      (should= 10.5 (:qa-speed opts))
      (should= "tmp/scores.edn" (:scores-file opts))))

  (it "rejects non-positive qa-speed"
    (should-throw Exception
      (input/parse-cli-args ["--qa-speed" "0"] 800 600))
    (should-throw Exception
      (input/parse-cli-args ["--qa-speed" "-2"] 800 600)))

  (it "ignores a bare -- separator"
    (let [opts (input/parse-cli-args ["--" "--qa-telemetry"] 800 600)]
      (should (:qa-telemetry? opts))))

  (it "rejects unknown arguments"
    (should-throw Exception
      (input/parse-cli-args ["--nope"] 800 600))))

(describe "parse-window-size"
  (it "parses numeric size args"
    (should= [1280 720] (input/parse-window-size ["1280" "720"] 800 600))))

(describe "resize-if-needed"
  (it "reflows when dimensions change"
    (let [state (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
          next (input/resize-if-needed state 1024 768
                                       core/resize
                                       core/playfield-width
                                       core/playfield-height)]
      (should= 1024 (core/playfield-width next))
      (should= 768 (core/playfield-height next))))

  (it "keeps the same state object when size is unchanged"
    (let [state (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
          next (input/resize-if-needed state 800 600
                                       core/resize
                                       core/playfield-width
                                       core/playfield-height)]
      (should (identical? state next)))))

(describe "format-telemetry-line"
  (it "reports battery none with zero missiles"
    (let [state (assoc (core/new-game {:width 900 :height 600}) :screen :playing)
          line (input/format-telemetry-line {:state state :events []})]
      (should (str/includes? line "battery=none"))
      (should (str/includes? line "missiles_in_flight=0"))))

  (it "sim telemetry includes score and multiplier"
    (let [state (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
          line (input/format-sim-telemetry-line state)]
      (should (str/includes? line "score=0"))
      (should (str/includes? line "multiplier=1"))))

  (it "includes flight vectors after a click fire"
    (let [state (assoc (core/new-game {:width 900 :height 600}) :screen :playing)
          result (core/handle state {:type :click :x 100 :y 150})
          line (input/format-telemetry-line result)]
      (should (str/includes? line "battery=left"))
      (should (str/includes? line "missiles_in_flight=1"))
      (should (str/includes? line "target_x=100"))
      (should (str/includes? line "target_y=150"))
      (should (str/includes? line "origin_x=")))))

(describe "parse-qa-event-line"
  (it "parses click aim key wait and quit"
    (should= {:type :click :x 10 :y 20} (input/parse-qa-event-line "click 10 20"))
    (should= {:type :aim :x 30 :y 40} (input/parse-qa-event-line "aim 30 40"))
    (should= {:type :key :ch \z} (input/parse-qa-event-line "key z"))
    (should= {:type :wait :seconds 2.5} (input/parse-qa-event-line "wait 2.5"))
    (should= {:type :enemy :spec {:kind :city :id 1}}
             (input/parse-qa-event-line "enemy city:1"))
    (should= {:type :fireball :spec {:x 1 :y 2 :radius 3.0}}
             (input/parse-qa-event-line "fireball 1,2,3"))
    (should= {:type :quit} (input/parse-qa-event-line "quit")))

  (it "parses high-score events"
    (should= {:type :open-high-scores}
             (input/parse-qa-event-line "open-high-scores"))
    (should= {:type :close-high-scores}
             (input/parse-qa-event-line "close-high-scores"))
    (should= {:type :submit-high-score :initials "BOB"}
             (input/parse-qa-event-line "initials BOB"))
    (should= {:type :confirm} (input/parse-qa-event-line "confirm"))
    (should= {:type :start} (input/parse-qa-event-line "start")))

  (it "ignores blank lines"
    (should-be-nil (input/parse-qa-event-line "   ")))

  (it "rejects unknown event ops"
    (should-throw Exception
      (input/parse-qa-event-line "warp 1 2"))))

(describe "fireball phase detection"
  (it "emits start then max/shrink transitions"
    (let [fb0 {:id 1 :x 10 :y 20 :age 0.0 :radius 0.0
               :expand-seconds 0.4 :contract-seconds 0.4 :max-radius 40.0}
          fb1 (assoc fb0 :age 0.1 :radius 10.0)
          fb2 (assoc fb0 :age 0.45 :radius 35.0)
          [e0 m0] (input/detect-fireball-phase-events {} [fb0])
          [e1 m1] (input/detect-fireball-phase-events m0 [fb1])
          [e2 _] (input/detect-fireball-phase-events m1 [fb2])]
      (should= :start (:phase (first e0)))
      (should (empty? e1)) ; still expanding -> still start
      (should= [:max :shrink] (mapv :phase e2)))))

(describe "parse-enemy-spec"
  (it "parses city and battery specs"
    (should= {:kind :city :id 2} (input/parse-enemy-spec "city:2"))
    (should= {:kind :battery :id :left} (input/parse-enemy-spec "battery:left")))

  (it "rejects unknown kinds"
    (should-throw Exception
      (input/parse-enemy-spec "moon:1"))))

(describe "format-sim-telemetry-line"
  (it "includes wave, ammo, and enemy fields for a seeded scenario"
    (let [state (input/apply-scenario
                 (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                 {:wave 2
                  :batteries {:left {:ammo 3}}
                  :enemies [{:origin [50 0] :target [:city 0]}
                            {:target [:battery :center]}]
                  :targets [{:x 100 :y 200}]})
          line (input/format-sim-telemetry-line state)]
      (should (str/includes? line "qa-sim t="))
      (should (str/includes? line "wave=2"))
      (should (str/includes? line "wave_complete="))
      (should (str/includes? line "wave_enemy_count="))
      (should (str/includes? line "enemy_missiles=2"))
      (should (str/includes? line "enemy_origin_x=50"))
      (should (str/includes? line "enemy_target=city:0"))
      (should (str/includes? line "battery_left_ammo=3"))
      (should (str/includes? line "target_x=100"))
      (should (str/includes? line "cities_alive="))))

  (it "reports ammo 0 when battery missiles are missing"
    (let [base (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
          left (dissoc (core/battery base :left) :missiles)
          state (assoc-in base [:batteries]
                          (mapv (fn [b]
                                  (if (= :left (:id b)) left b))
                                (core/batteries base)))
          line (input/format-sim-telemetry-line state)]
      (should (str/includes? line "battery_left_ammo=0"))
      (should-not (str/includes? line "battery_left_ammo=1"))))

  (it "includes high-score telemetry fields"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-high-score-capacity 3)
                    (core/add-high-score-entry "AAA" 1000)
                    (core/add-high-score-entry "BOB" 500)
                    (assoc :initials-draft "AB"))
          line (input/format-sim-telemetry-line state)]
      (should (str/includes? line "high_score_count=2"))
      (should (str/includes? line "high_score_capacity=3"))
      (should (str/includes? line "hs_rank1_initials=AAA"))
      (should (str/includes? line "hs_rank1_score=1000"))
      (should (str/includes? line "hs_rank2_initials=BOB"))
      (should (str/includes? line "pending_high_score=none"))
      (should (str/includes? line "initials_draft=AB"))))

  (it "includes fireball geometry and last-enemy-fate when present"
    (let [base (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
          with-fb (core/add-static-fireball base 111 222 12.0)
          with-fate (assoc with-fb :last-enemy-fate :fireball)
          line (input/format-sim-telemetry-line with-fate)]
      (should (str/includes? line "center_x=111"))
      (should (str/includes? line "center_y=222"))
      (should (str/includes? line "radius="))
      (should (str/includes? line "last_enemy_fate=fireball")))))
(describe "load-scenario-edn"
  (it "reads an EDN map from a path"
    (let [path "tmp/hardender-scenario.edn"
          _ (spit path "{:wave 4 :enemies []}")
          scenario (input/load-scenario-edn path)]
      (should= 4 (:wave scenario))
      (should= [] (:enemies scenario))))

  (it "returns nil when path is nil"
    (should-be-nil (input/load-scenario-edn nil))))

(describe "format-fireball-phase-line"
  (it "includes id phase time and geometry"
    (let [state (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
          fb {:id 9 :x 1 :y 2 :radius 3.5}
          line (input/format-fireball-phase-line state fb :start)]
      (should (str/includes? line "qa-fireball id=9"))
      (should (str/includes? line "phase=start"))
      (should (str/includes? line "t="))
      (should (str/includes? line "center_x=1"))
      (should (str/includes? line "center_y=2"))
      (should (str/includes? line "radius=3.5")))))

(describe "detect-fireball-phase-events end"
  (it "emits end when a previously tracked fireball disappears"
    (let [prev {7 :shrink}
          [events next] (input/detect-fireball-phase-events prev [])
          fb (:fireball (first events))]
      (should= 1 (count events))
      (should= :end (:phase (first events)))
      (should= 7 (:id (first events)))
      (should= 0 (:x fb))
      (should= 0 (:y fb))
      (should= 0.0 (:radius fb))
      (should= :end (get next 7))))

  (it "does not re-end a fireball already at end"
    (let [prev {7 :end}
          [events _] (input/detect-fireball-phase-events prev [])]
      (should (empty? events)))))

(describe "fireball-report-phase"
  (it "classifies start shrink and end by age boundaries"
    (let [fb {:age 0.0 :expand-seconds 0.4 :contract-seconds 0.4}
          at-expand (assoc fb :age 0.4)
          mid-shrink (assoc fb :age 0.5)
          at-end (assoc fb :age 0.8)
          past-end (assoc fb :age 0.9)]
      (should= :start (input/fireball-report-phase fb))
      (should= :shrink (input/fireball-report-phase at-expand))
      (should= :shrink (input/fireball-report-phase mid-shrink))
      (should= :end (input/fireball-report-phase at-end))
      (should= :end (input/fireball-report-phase past-end)))))

(describe "load-qa-events"
  (it "loads non-blank event lines from a file"
    (let [path "tmp/hardender-events.txt"
          _ (spit path "aim 1 2\n\nquit\n")
          events (input/load-qa-events path)]
      (should= [{:type :aim :x 1 :y 2} {:type :quit}] events))))

(describe "apply-scenario"
  (it "honors angled enemy origin in scenario enemies"
    (let [base (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
          state (input/apply-scenario
                 base
                 {:enemies [{:origin [50 0] :target [:city 0]}]})
          m (first (core/enemy-missiles state))]
      (should= 50.0 (double (:x0 m)))
      (should= 0.0 (double (:y0 m)))
      (should-not= (double (:x0 m)) (double (:x1 m)))
      (should= :city (:target-kind m))
      (should= 0 (:target-id m))
      (should (:wave-had-enemies? state))
      (should-not (:wave-complete? state))))

  (it "leaves wave flags alone when enemies list is empty"
    (let [base (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
          state (input/apply-scenario base {:enemies []})]
      (should-not (:wave-had-enemies? state))
      (should= 0 (count (core/enemy-missiles state)))))

  (it "seeds high-score table and capacity from scenario"
    (let [state (input/apply-scenario
                 (core/new-game {:width 800 :height 600})
                 {:high-score-capacity 3
                  :high-scores [{:initials "AAA" :score 1000}
                                {:initials "BBB" :score 900}]})]
      (should= 3 (core/high-score-capacity state))
      (should= 2 (count (core/high-score-table state)))
      (should= "AAA" (:initials (first (core/high-score-table state))))))

  (it "applies wave, size, batteries, cities, targets, and default enemy origins"
    (let [state (input/apply-scenario
                 (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                 {:wave 3
                  :width 640
                  :height 480
                  :batteries {:left {:ammo 2}
                              :right {:destroyed true :ammo 0}}
                  :cities {:destroyed [5]}
                  :targets [{:x 10 :y 20}]
                  :enemies [{:target [:city 0]}
                            {:origin [100 0] :target [:battery :center]}]})
          enemies (core/enemy-missiles state)
          city-enemy (first (filter #(= :city (:target-kind %)) enemies))
          bat-enemy (first (filter #(= :battery (:target-kind %)) enemies))]
      (should= 3 (core/wave state))
      (should= 640 (core/playfield-width state))
      (should= 480 (core/playfield-height state))
      (should= 2 (:missiles (core/battery state :left)))
      (should (:destroyed? (core/battery state :right)))
      (should-not (core/living-city? state 5))
      (should= 1 (count (core/destroyable-targets state)))
      (should= 2 (count enemies))
      (should= (double (:x (core/city state 0))) (double (:x0 city-enemy)))
      (should= 100.0 (double (:x0 bat-enemy)))
      (should= :center (:target-id bat-enemy))))

  (it "spawns MIRV scenario enemies with child count and split progress"
    (let [state (input/apply-scenario
                 (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                 {:enemies [{:kind :mirv
                             :target [:city 1]
                             :child-count 4
                             :split-progress 0.4}]})
          m (first (core/enemy-missiles state))
          line (input/format-sim-telemetry-line state)]
      (should= 1 (count (core/mirv-parents state)))
      (should= core/enemy-kind-mirv (:enemy-kind m))
      (should= 4 (:child-count m))
      (should= 0.4 (double (:split-progress m)))
      (should= 1 (:target-id m))
      (should (str/includes? line "enemy_kind=mirv"))))


  (it "marks wave flags when scenario enemies are applied"
    (let [state (input/apply-scenario
                 (core/new-game {:width 800 :height 600})
                 {:enemies [{:target [:city 0]}]})]
      (should (:wave-had-enemies? state))
      (should-not (:wave-complete? state))))

  (it "assigns drop ids from zero when scenario flyers include drops"
    (let [state (input/apply-scenario
                 (core/new-game {:width 800 :height 600})
                 {:flyers [{:kind :bomber
                            :from [0 80]
                            :to [800 80]
                            :speed 100
                            :drops [{:at-progress 0.2 :target [:city 1]}
                                    {:at-progress 0.5 :target [:city 2]}]}]})
          f (first (core/flyers state))
          drops (:drops f)]
      (should= 2 (count drops))
      (should= 0 (:id (first drops)))
      (should= 1 (:id (second drops)))
      (should= 0.2 (double (:at-progress (first drops))))))



  (it "defaults flyer drop target to city zero when omitted"
    (let [state (input/apply-scenario
                 (core/new-game {:width 800 :height 600})
                 {:flyers [{:drops [{:at-progress 0.3}]}]})
          drop (first (:drops (first (core/flyers state))))]
      (should= [:city 0] (:target drop))
      (should= 0.3 (double (:at-progress drop)))))

  (it "applies mute and difficulty scenario options"
    (let [state (input/apply-scenario
                 (core/new-game {:width 800 :height 600})
                 {:mute true :difficulty :easy})]
      (should (core/mute? state))
      (should= :easy (core/difficulty state))))

  (it "merges nested options map from scenario"
    (let [state (input/apply-scenario
                 (core/new-game {:width 800 :height 600})
                 {:options {:mute true}})]
      (should (core/mute? state)))))

(describe "wave banner telemetry"
  (it "reports none fields when not on wave-banner"
    (let [state (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
          line (input/format-sim-telemetry-line state)]
      (should (str/includes? line "banner_text=none"))
      (should (str/includes? line "banner_phase=none"))))

  (it "reports banner text phase and position during wave-banner"
    (let [start (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                    (core/set-wave-enemies-active 1))
          state (loop [s start n 0]
                  (cond
                    (core/wave-banner? s) s
                    (> n 10000) s
                    :else (recur (:state (core/tick s 0.05)) (inc n))))
          line (input/format-sim-telemetry-line state)]
      (should (core/wave-banner? state))
      (should (str/includes? line "banner_text=WAVE_2"))
      (should (str/includes? line "banner_phase=enter"))
      (should (str/includes? line "banner_announced_wave=2")))))
