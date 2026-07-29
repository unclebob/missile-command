(ns missile-command.host-input-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]
            [missile-command.host-input :as host-input]))

(describe "key-intent"
  (it "starts from title and confirms THE END with enter"
    (should= {:command {:type :start}}
             (host-input/key-intent (core/new-game {:width 800 :height 600})
                                    ""
                                    {:enter? true}))
    (should= {:command {:type :confirm} :draft ""}
             (host-input/key-intent (assoc (core/new-game {:width 800 :height 600})
                                           :screen :the-end)
                                    ""
                                    {:enter? true})))

  (it "opens and leaves options with O, and applies options shortcuts"
    (let [title (core/new-game {:width 800 :height 600})
          options (assoc title :screen :options)]
      (should= {:command {:type :open-options}}
               (host-input/key-intent title "" {:ch \o :key-name "o"}))
      (should= {:command {:type :leave-options}}
               (host-input/key-intent options "" {:ch \O :key-name "o"}))
      (should= {:command {:type :set-mute :mute true}}
               (host-input/key-intent options "" {:ch \m :key-name "m"}))
      (should= {:command {:type :set-difficulty :difficulty "normal"}}
               (host-input/key-intent options "" {:ch \2 :key-name "2"}))))

  (it "does not open options from a phone title screen"
    (let [phone-title (assoc (core/new-game {:width 390 :height 844}) :phone? true)]
      (should-not
       (host-input/key-intent phone-title "" {:ch \o :key-name "o"}))))

  (it "toggles pause from playing and paused"
    (let [playing (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
          paused (assoc playing :screen :paused)]
      (should= {:command {:type :pause}}
               (host-input/key-intent playing "" {:ch \p :key-name "p"}))
      (should= {:command {:type :resume}}
               (host-input/key-intent paused "" {:ch \p :key-name "p"}))))

  (it "leaves high scores with H but leaves Escape unhandled for host quit"
    (let [high-scores (assoc (core/new-game {:width 800 :height 600})
                             :screen :high-scores)]
      (should= {:command {:type :close-high-scores}}
               (host-input/key-intent high-scores "" {:ch \h :key-name "h"}))
      (should-not (host-input/key-intent high-scores "" {:escape? true}))))

  (it "edits and submits a player name"
    (let [entry (assoc (core/new-game {:width 800 :height 600})
                       :screen :high-score-entry)]
      (should= {:draft "Ab"}
               (host-input/key-intent entry "A" {:ch \b :key-name "b"}))
      (should= {:draft "Uncle Bob"}
               (host-input/key-intent entry "Uncle Bo" {:ch \b :key-name "b"}))
      (should= {:draft "A"}
               (host-input/key-intent entry "AB" {:backspace? true}))
      (should= {:command {:type :submit-high-score
                          :initials "Uncle Bob"
                          :display-name "Uncle Bob"}}
               (host-input/key-intent entry " Uncle Bob " {:enter? true}))))

  (it "forwards playing keys to core remappable fire handling"
    (let [playing (assoc (core/new-game {:width 800 :height 600}) :screen :playing)]
      (should= {:command {:type :key :key "z"}}
               (host-input/key-intent playing "" {:ch \z :key-name "z"})))))
