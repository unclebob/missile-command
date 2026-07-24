(ns missile-command.acceptance.steps-spec
  (:require [speclj.core :refer :all]
            [missile-command.acceptance.steps :as steps]
            [missile-command.core :as core]))

(describe "dispatch-step"
  (it "creates a new game from example parameters"
    (let [world (steps/dispatch-step
                 {}
                 {:text "a new game with width <width> and height <height>"}
                 {"width" "800" "height" "600"})]
      (should= 800 (core/playfield-width (:state world)))
      (should= 600 (core/playfield-height (:state world)))))

  (it "asserts playfield width"
    (let [world {:state (core/new-game {:width 800 :height 600})}]
      (should= world
               (steps/dispatch-step
                world
                {:text "the playfield width is <width>"}
                {"width" "800"}))))

  (it "asserts playfield height"
    (let [world {:state (core/new-game {:width 800 :height 600})}]
      (should= world
               (steps/dispatch-step
                world
                {:text "the playfield height is <height>"}
                {"height" "600"}))))

  (it "fails when playfield width does not match"
    (let [world {:state (core/new-game {:width 800 :height 600})}]
      (should-throw Exception #"playfield width 800 expected 1"
        (steps/dispatch-step
         world
         {:text "the playfield width is <width>"}
         {"width" "1"}))))

  (it "fails for unsupported steps"
    (should-throw Exception #"unsupported step: dance"
      (steps/dispatch-step {} {:text "dance"} {})))

  (it "asserts living city count"
    (let [world {:state (core/new-game {:width 800 :height 600})}]
      (should= world
               (steps/dispatch-step
                world
                {:text "there are <city_count> living cities"}
                {"city_count" "6"}))))

  (it "fails when living city count mismatches"
    (let [world {:state (core/new-game {:width 800 :height 600})}]
      (should-throw Exception #"living cities count 6 expected 1"
        (steps/dispatch-step
         world
         {:text "there are <city_count> living cities"}
         {"city_count" "1"}))))

  (it "resizes the playfield"
    (let [world {:state (core/new-game {:width 800 :height 600})}
          resized (steps/dispatch-step
                   world
                   {:text "the playfield is resized to width <new_width> and height <new_height>"}
                   {"new_width" "1920" "new_height" "1080"})]
      (should= 1920 (core/playfield-width (:state resized)))
      (should= 1080 (core/playfield-height (:state resized)))
      (should= 6 (count (core/living-cities (:state resized)))))))
