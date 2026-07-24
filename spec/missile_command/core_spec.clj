(ns missile-command.core-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]))

(describe "new-game"
  (it "records the playfield width and height"
    (let [state (core/new-game {:width 800 :height 600})]
      (should= 800 (core/playfield-width state))
      (should= 600 (core/playfield-height state))))

  (it "records other playfield sizes"
    (let [state (core/new-game {:width 1920 :height 1080})]
      (should= 1920 (core/playfield-width state))
      (should= 1080 (core/playfield-height state)))))
