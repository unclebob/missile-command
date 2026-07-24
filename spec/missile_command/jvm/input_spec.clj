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

  (it "defaults qa-speed to 1.0"
    (let [opts (input/parse-cli-args [] 800 600)]
      (should= 1.0 (:qa-speed opts))))

  (it "accepts --qa as telemetry alias and --qa-scenario"
    (let [opts (input/parse-cli-args
                ["--qa" "--qa-scenario" "tmp/s.edn" "--qa-speed" "10.5"]
                800 600)]
      (should (:qa-telemetry? opts))
      (should= "tmp/s.edn" (:qa-scenario opts))
      (should= 10.5 (:qa-speed opts))))

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
    (let [state (core/new-game {:width 800 :height 600})
          next (input/resize-if-needed state 1024 768
                                       core/resize
                                       core/playfield-width
                                       core/playfield-height)]
      (should= 1024 (core/playfield-width next))
      (should= 768 (core/playfield-height next))))

  (it "keeps the same state object when size is unchanged"
    (let [state (core/new-game {:width 800 :height 600})
          next (input/resize-if-needed state 800 600
                                       core/resize
                                       core/playfield-width
                                       core/playfield-height)]
      (should (identical? state next)))))

(describe "format-telemetry-line"
  (it "reports battery none with zero missiles"
    (let [state (core/new-game {:width 900 :height 600})
          line (input/format-telemetry-line {:state state :events []})]
      (should (str/includes? line "battery=none"))
      (should (str/includes? line "missiles_in_flight=0"))))

  (it "sim telemetry includes score and multiplier"
    (let [state (core/new-game {:width 800 :height 600})
          line (input/format-sim-telemetry-line state)]
      (should (str/includes? line "score=0"))
      (should (str/includes? line "multiplier=1"))))

  (it "includes flight vectors after a click fire"
    (let [state (core/new-game {:width 900 :height 600})
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
                 (core/new-game {:width 800 :height 600})
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
      (should (str/includes? line "cities_alive=")))))

(describe "apply-scenario"
  (it "honors angled enemy origin in scenario enemies"
    (let [base (core/new-game {:width 800 :height 600})
          state (input/apply-scenario
                 base
                 {:enemies [{:origin [50 0] :target [:city 0]}]})
          m (first (core/enemy-missiles state))]
      (should= 50.0 (double (:x0 m)))
      (should= 0.0 (double (:y0 m)))
      (should-not= (double (:x0 m)) (double (:x1 m)))
      (should= :city (:target-kind m))
      (should= 0 (:target-id m))))

  (it "applies wave, size, batteries, cities, targets, and default enemy origins"
    (let [state (input/apply-scenario
                 (core/new-game {:width 800 :height 600})
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
                 (core/new-game {:width 800 :height 600})
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
      (should (str/includes? line "enemy_kind=mirv")))))