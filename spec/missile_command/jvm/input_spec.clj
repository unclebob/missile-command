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
                 "--qa-events" "tmp/events.txt"]
                800 600)]
      (should= 1280 (:width opts))
      (should= 720 (:height opts))
      (should (:qa-telemetry? opts))
      (should= [:left :center] (:destroy-batteries opts))
      (should= "tmp/events.txt" (:qa-events opts))))

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
  (it "parses click aim key and quit"
    (should= {:type :click :x 10 :y 20} (input/parse-qa-event-line "click 10 20"))
    (should= {:type :aim :x 30 :y 40} (input/parse-qa-event-line "aim 30 40"))
    (should= {:type :key :ch \z} (input/parse-qa-event-line "key z"))
    (should= {:type :quit} (input/parse-qa-event-line "quit")))

  (it "ignores blank lines"
    (should-be-nil (input/parse-qa-event-line "   ")))

  (it "rejects unknown event ops"
    (should-throw Exception
      (input/parse-qa-event-line "warp 1 2"))))
