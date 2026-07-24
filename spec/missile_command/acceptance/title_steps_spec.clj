(ns missile-command.acceptance.title-steps-spec
  (:require [speclj.core :refer :all]
            [missile-command.acceptance.steps :as steps]
            [missile-command.core :as core]))

(defn- fresh-world
  ([] (fresh-world 800 600))
  ([w h] {:state (core/new-game {:width w :height h})}))

(defn- playing-world
  ([] (playing-world 800 600))
  ([w h]
   (let [world (fresh-world w h)]
     (assoc world :state (core/start-game (:state world))))))

(defn- dispatch
  [world text example]
  (steps/dispatch-step world {:text text} example))

(describe "title screen acceptance steps"
  (it "starts a new game on the title screen with name and affordance"
    (let [world (fresh-world)]
      (should= world (dispatch world "the screen is title" {}))
      (should= world
               (dispatch world "the title game name is <game_name>"
                         {"game_name" "Missile_Command"}))
      (should= world (dispatch world "the title shows a start affordance" {}))
      (should-throw Exception #"title name"
        (dispatch world "the title game name is <game_name>"
                  {"game_name" "Wrong_Name"}))
      (should-throw Exception #"expected title"
        (dispatch (playing-world) "the screen is title" {}))))

  (it "starts the game into playing and rejects title-only asserts"
    (let [started (dispatch (fresh-world) "the player starts the game" {})]
      (should (core/playing? (:state started)))
      (should-not (core/title? (:state started)))
      (should= started (dispatch started "the screen is playing" {}))
      (should-throw Exception #"expected playing"
        (dispatch (fresh-world) "the screen is playing" {}))
      (should-throw Exception #"title missing start affordance"
        (dispatch started "the title shows a start affordance" {}))))

  (it "returns to title when confirming THE END without high score"
    (let [ended (-> (playing-world)
                    (update :state
                            #(reduce core/destroy-city % (map :id (core/cities %))))
                    (update :state core/set-bonus-city-reserve 0)
                    (update :state core/evaluate-game-over))
          back (dispatch ended "the player confirms the end screen" {})]
      (should (core/the-end? (:state ended)))
      (should (core/title? (:state back)))
      (should-not (core/the-end? (:state back))))))
