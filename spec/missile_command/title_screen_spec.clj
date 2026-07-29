(ns missile-command.title-screen-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]
            [missile-command.title-screen :as title-screen]))

(describe "title screen controls"
  (it "offers high scores and options off phone"
    (let [state (core/new-game {:width 800 :height 600})]
      (should= [:high-scores :options]
               (mapv :id (title-screen/buttons state)))))

  (it "offers high scores only on phone"
    (let [state (assoc (core/new-game {:width 390 :height 844}) :phone? true)]
      (should= [:high-scores]
               (mapv :id (title-screen/buttons state)))))

  (it "maps title button clicks to commands and the rest to start"
    (let [state (core/new-game {:width 800 :height 600})
          high-scores (first (title-screen/buttons state))
          center-x (+ (:x high-scores) (/ (:w high-scores) 2.0))
          center-y (+ (:y high-scores) (/ (:h high-scores) 2.0))]
      (should= {:type :open-high-scores}
               (title-screen/command-at state center-x center-y))
      (should= {:type :start}
               (title-screen/command-at state 20 20))))

  (it "maps high score and options footer buttons to return commands"
    (let [state (core/new-game {:width 800 :height 600})
          hs-button (first (title-screen/high-scores-buttons state))
          hs-x (+ (:x hs-button) (/ (:w hs-button) 2.0))
          hs-y (+ (:y hs-button) (/ (:h hs-button) 2.0))
          options-button (first (title-screen/options-buttons state))
          options-x (+ (:x options-button) (/ (:w options-button) 2.0))
          options-y (+ (:y options-button) (/ (:h options-button) 2.0))]
      (should= {:type :close-high-scores}
               (title-screen/high-scores-command-at state hs-x hs-y))
      (should= {:type :leave-options}
               (title-screen/options-command-at state options-x options-y))
      (should-not (title-screen/high-scores-command-at state 10 10))
      (should-not (title-screen/options-command-at state 10 10))))

  (it "maps the options mute checkbox to a mute toggle command"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    core/open-options)
          checkbox (title-screen/mute-checkbox state)
          center-x (+ (:x checkbox) (/ (:w checkbox) 2.0))
          center-y (+ (:y checkbox) (/ (:h checkbox) 2.0))]
      (should= {:type :set-mute :mute true}
               (title-screen/options-command-at state center-x center-y))
      (should= {:type :set-mute :mute false}
               (title-screen/options-command-at (core/set-mute state true)
                                                center-x
                                                center-y)))))
