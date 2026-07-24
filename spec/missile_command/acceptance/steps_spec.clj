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

  (it "creates a new game from literal dimensions"
    (let [world (dispatch {} "a new game with width 1024 and height 768" {})]
      (should= 1024 (core/playfield-width (:state world)))
      (should= 768 (core/playfield-height (:state world)))))

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

  (it "accepts city x of zero and rejects exclusive upper edge"
    (let [world (fresh-world)
          cities (core/cities (:state world))
          zero (assoc (first cities) :x 0)
          edge (assoc (first cities) :x 800)
          world-zero (assoc-in world [:state :cities]
                               (mapv (fn [c] (if (= (:id c) (:id zero)) zero c)) cities))
          world-edge (assoc-in world [:state :cities]
                               (mapv (fn [c] (if (= (:id c) (:id edge)) edge c)) cities))]
      (should= world-zero
               (dispatch world-zero
                         "every city x is between 0 inclusive and <width> exclusive"
                         {"width" "800"}))
      (should-throw Exception #"not in \[0,800\)"
        (dispatch world-edge
                  "every city x is between 0 inclusive and <width> exclusive"
                  {"width" "800"}))))

  (it "fails open inequalities when values equal the bound"
    (let [world (fresh-world)
          left (core/battery (:state world) :left)
          center (core/battery (:state world) :center)
          right (core/battery (:state world) :right)
          one-third (/ 800 3.0)
          two-thirds (* 800 (/ 2.0 3))
          eq-left-center (assoc-in world [:state :batteries]
                                   [(assoc left :x 400.0)
                                    (assoc center :x 400.0)
                                    right])
          eq-left-third (assoc-in world [:state :batteries]
                                  [(assoc left :x one-third) center right])
          eq-center-lo (assoc-in world [:state :batteries]
                                 [left (assoc center :x one-third) right])
          eq-center-hi (assoc-in world [:state :batteries]
                                 [left (assoc center :x two-thirds) right])
          eq-right-two-thirds (assoc-in world [:state :batteries]
                                        [left center (assoc right :x two-thirds)])]
      (should-throw Exception #"left battery x not less than center"
        (dispatch eq-left-center "the left battery x is less than the center battery x" {}))
      (should-throw Exception #"left battery x"
        (dispatch eq-left-third
                  "the left battery x is less than one third of width <width>"
                  {"width" "800"}))
      (should-throw Exception #"center battery x"
        (dispatch eq-center-lo
                  "the center battery x is between one third and two thirds of width <width>"
                  {"width" "800"}))
      (should-throw Exception #"center battery x"
        (dispatch eq-center-hi
                  "the center battery x is between one third and two thirds of width <width>"
                  {"width" "800"}))
      (should-throw Exception #"right battery x"
        (dispatch eq-right-two-thirds
                  "the right battery x is greater than two thirds of width <width>"
                  {"width" "800"}))))

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
               (dispatch world "the score is <score>" {"score" "0"}))))

  (it "fires a battery toward the crosshair"
    (let [world (-> (fresh-world)
                    (dispatch "the player aims at <x> <y>" {"x" "400" "y" "200"}))
          fired (dispatch world "the player fires the <battery> battery"
                          {"battery" "left"})]
      (should= 9 (:missiles (core/battery (:state fired) :left)))
      (should= 1 (count (core/defensive-missiles (:state fired))))
      (should= fired
               (dispatch fired "the <battery> battery has <ammo> missiles"
                         {"battery" "left" "ammo" "9"}))
      (should= fired
               (dispatch fired "there are <missile_count> defensive missiles in flight"
                         {"missile_count" "1"}))
      (should= fired
               (dispatch fired
                         "a defensive missile from the <battery> battery targets <aim_x> <aim_y>"
                         {"battery" "left" "aim_x" "400" "aim_y" "200"}))))

  (it "sets battery ammo and destruction for fire scenarios"
    (let [empty (-> (fresh-world)
                    (dispatch "the <battery> battery ammo is set to <ammo>"
                              {"battery" "left" "ammo" "0"}))
          destroyed (-> (fresh-world)
                        (dispatch "the <battery> battery is destroyed"
                                  {"battery" "center"}))]
      (should= 0 (:missiles (core/battery (:state empty) :left)))
      (should (:destroyed? (core/battery (:state destroyed) :center)))))

  (it "fires every battery and checks center missile speed"
    (let [world (-> (fresh-world)
                    (dispatch "the player aims at <x> <y>" {"x" "400" "y" "100"})
                    (dispatch "the player fires every battery once" {}))]
      (should= 3 (count (core/defensive-missiles (:state world))))
      (should= world
               (dispatch world
                         "the center defensive missile is faster than each side defensive missile"
                         {}))))

  (it "fires by click zone"
    (let [world (fresh-world 900 600)
          clicked (dispatch world "the player clicks at <x> <y>" {"x" "100" "y" "100"})]
      (should= :left (:battery (first (core/defensive-missiles (:state clicked)))))
      (should= {:x 100 :y 100} (core/crosshair (:state clicked)))))

  (it "prepares click fallback by emptying earlier batteries"
    (let [world (fresh-world 900 600)
          prepared (dispatch world
                             "the click must fall back to the <battery> battery because earlier batteries are empty"
                             {"battery" "center" "x" "100"})
          clicked (dispatch prepared "the player clicks at <x> <y>" {"x" "100" "y" "100"})]
      (should= 0 (:missiles (core/battery (:state prepared) :left)))
      (should= :center (:battery (first (core/defensive-missiles (:state clicked))))))))
