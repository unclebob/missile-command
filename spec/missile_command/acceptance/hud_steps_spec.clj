(ns missile-command.acceptance.hud-steps-spec
  (:require [speclj.core :refer :all]
            [missile-command.acceptance.steps :as steps]
            [missile-command.core :as core]))

(defn- fresh-playing
  ([] (fresh-playing 800 600))
  ([w h]
   {:state (core/start-game (core/new-game {:width w :height h}))}))

(defn- dispatch
  [world text example]
  (steps/dispatch-step world {:text text} example))

(describe "HUD acceptance steps"
  (it "asserts score wave multiplier ammo cities and reserve"
    (let [world (fresh-playing)]
      (should= world
               (dispatch world "the hud shows score <score>" {"score" "0"}))
      (should= world
               (dispatch world "the hud shows wave <wave>" {"wave" "1"}))
      (should= world
               (dispatch world "the hud shows multiplier <multiplier>"
                         {"multiplier" "1"}))
      (should= world
               (dispatch world "the hud shows left ammo <left_ammo>"
                         {"left_ammo" "10"}))
      (should= world
               (dispatch world "the hud shows center ammo <center_ammo>"
                         {"center_ammo" "10"}))
      (should= world
               (dispatch world "the hud shows right ammo <right_ammo>"
                         {"right_ammo" "10"}))
      (should= world
               (dispatch world "the hud shows living cities <living_cities>"
                         {"living_cities" "6"}))
      (should= world
               (dispatch world "the hud shows bonus cities <bonus_cities>"
                         {"bonus_cities" "0"}))
      (should= world
               (dispatch world "each non-destroyed battery has matching hud ammo" {}))
      (should-throw Exception #"hud score"
        (dispatch world "the hud shows score <score>" {"score" "99"}))
      (should-throw Exception #"hud left ammo"
        (dispatch world "the hud shows left ammo <left_ammo>"
                  {"left_ammo" "0"}))
      (should-throw Exception #"hud living cities"
        (dispatch world "the hud shows living cities <living_cities>"
                  {"living_cities" "1"}))))

  (it "asserts battery ammo by name and example param"
    (let [world (fresh-playing)]
      (should= world
               (dispatch world
                         "the hud shows left ammo <ammo>"
                         {"ammo" "10"}))
      (should= world
               (dispatch world
                         "the hud shows <battery> ammo <ammo>"
                         {"battery" "center" "ammo" "10"}))
      (should-throw Exception #"hud :center ammo"
        (dispatch world
                  "the hud shows <battery> ammo <ammo>"
                  {"battery" "center" "ammo" "1"}))))

  (it "asserts full playing HUD is not required on title"
    (let [title {:state (core/new-game {:width 800 :height 600})}]
      (should= title
               (dispatch title "the full playing hud is not required" {}))
      (should-throw Exception #"full playing HUD"
        (dispatch (fresh-playing)
                  "the full playing hud is not required" {})))))

(describe "screens via HUD steps"
  (it "recognizes playing paused title and the-end screens"
    (let [playing (fresh-playing)
          paused (dispatch playing "the player pauses the game" {})
          title {:state (core/new-game {:width 800 :height 600})}
          ended (-> playing
                    (update :state
                            #(reduce core/destroy-city %
                                     (map :id (core/cities %))))
                    (update :state core/set-bonus-city-reserve 0)
                    (update :state core/evaluate-game-over))]
      (should= playing (dispatch playing "the screen is playing" {}))
      (should= paused (dispatch paused "the screen is paused" {}))
      (should= title (dispatch title "the screen is title" {}))
      (should (core/the-end? (:state ended))))))
