(ns missile-command.jvm.input-spec
  (:require [speclj.core :refer :all]
            [missile-command.jvm.input :as input]
            [missile-command.core :as core]))

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

(describe "aim-command"
  (it "builds an aim command"
    (should= {:type :aim :x 10 :y 20} (input/aim-command 10 20))))

(describe "escape-key?"
  (it "detects Esc"
    (should (input/escape-key? (char 27)))
    (should-not (input/escape-key? \q))))

(describe "parse-window-size"
  (it "uses defaults when args are empty"
    (should= [800 600] (input/parse-window-size [] 800 600)))

  (it "parses width and height strings"
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
