(ns missile-command.acceptance.steps-spec
  (:require [speclj.core :refer :all]
            [missile-command.acceptance.steps :as steps]
            [missile-command.core :as core]))

(defn- fresh-world
  ([] (fresh-world 800 600))
  ([w h] {:state (core/new-game {:width w :height h})}))

(defn- dispatch
  [world text example]
  (steps/dispatch-step world {:text text} example))

(describe "dispatch-step"
  (it "creates a new game from example parameters"
    (let [world (dispatch {} "a new game with width <width> and height <height>"
                          {"width" "800" "height" "600"})]
      (should= 800 (core/playfield-width (:state world)))
      (should= 600 (core/playfield-height (:state world)))))

  (it "asserts playfield width"
    (let [world (fresh-world)]
      (should= world (dispatch world "the playfield width is <width>" {"width" "800"}))))

  (it "asserts playfield height"
    (let [world (fresh-world)]
      (should= world (dispatch world "the playfield height is <height>" {"height" "600"}))))

  (it "fails when playfield width does not match"
    (let [world (fresh-world)]
      (should-throw Exception #"playfield width 800 expected 1"
        (dispatch world "the playfield width is <width>" {"width" "1"}))))

  (it "fails for unsupported steps"
    (should-throw Exception #"unsupported step: dance"
      (dispatch {} "dance" {})))

  (it "asserts living city count"
    (let [world (fresh-world)]
      (should= world
               (dispatch world "there are <city_count> living cities"
                         {"city_count" "6"}))))

  (it "fails when living city count mismatches"
    (let [world (fresh-world)]
      (should-throw Exception #"living cities count 6 expected 1"
        (dispatch world "there are <city_count> living cities"
                  {"city_count" "1"}))))

  (it "resizes the playfield"
    (let [world (fresh-world)
          resized (dispatch world
                            "the playfield is resized to width <new_width> and height <new_height>"
                            {"new_width" "1920" "new_height" "1080"})]
      (should= 1920 (core/playfield-width (:state resized)))
      (should= 1080 (core/playfield-height (:state resized)))
      (should= 6 (count (core/living-cities (:state resized))))))

  (it "asserts non-destroyed batteries and ammo"
    (let [world (fresh-world)]
      (should= world
               (dispatch world
                         "there are <battery_count> non-destroyed batteries named left center and right"
                         {"battery_count" "3"}))
      (should= world
               (dispatch world "each battery has <ammo> missiles" {"ammo" "10"}))))

  (it "asserts city ordering, bounds, and ground band"
    (let [world (fresh-world)
          ex {"width" "800" "height" "600"}]
      (should= world (dispatch world "city x positions increase with city index" {}))
      (should= world
               (dispatch world
                         "every city x is between 0 inclusive and <width> exclusive"
                         ex))
      (should= world
               (dispatch world
                         "every city y is in the ground band for height <height>"
                         ex))
      (should= world
               (dispatch world
                         "the leftmost city x is less than one third of width <width>"
                         ex))
      (should= world
               (dispatch world
                         "the rightmost city x is greater than two thirds of width <width>"
                         ex))))

  (it "asserts battery placement and speeds"
    (let [world (fresh-world)
          ex {"width" "800" "height" "600"}]
      (should= world
               (dispatch world "the left battery x is less than the center battery x" {}))
      (should= world
               (dispatch world "the center battery x is less than the right battery x" {}))
      (should= world
               (dispatch world
                         "the left battery x is less than one third of width <width>"
                         ex))
      (should= world
               (dispatch world
                         "the center battery x is between one third and two thirds of width <width>"
                         ex))
      (should= world
               (dispatch world
                         "the right battery x is greater than two thirds of width <width>"
                         ex))
      (should= world
               (dispatch world
                         "every battery y is in the ground band for height <height>"
                         ex))
      (should= world
               (dispatch world
                         "the center battery missile speed is greater than the left battery missile speed"
                         {}))
      (should= world
               (dispatch world
                         "the center battery missile speed is greater than the right battery missile speed"
                         {}))))

  (it "asserts city horizontal span bounds"
    (let [world (fresh-world)
          ex {"width" "800"}]
      (should= world
               (dispatch world
                         "the horizontal span of the cities is greater than half of width <width>"
                         ex))
      (should= world
               (dispatch world
                         "the horizontal span of the cities is less than width <width>"
                         ex))))

  (it "aims the crosshair"
    (let [world (fresh-world)
          aimed (dispatch world "the player aims at <x> <y>" {"x" "100" "y" "200"})]
      (should= {:x 100 :y 200} (core/crosshair (:state aimed)))))

  (it "asserts the crosshair position"
    (let [world {:state (:state (core/handle (core/new-game {:width 800 :height 600})
                                             {:type :aim :x 100 :y 200}))}]
      (should= world
               (dispatch world "the crosshair is at <expected_x> <expected_y>"
                         {"expected_x" "100" "expected_y" "200"}))))

  (it "asserts the score"
    (let [world (fresh-world)]
      (should= world
               (dispatch world "the score is <score>" {"score" "0"})))))
