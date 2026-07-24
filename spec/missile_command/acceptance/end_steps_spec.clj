(ns missile-command.acceptance.end-steps-spec
  (:require [speclj.core :refer :all]
            [missile-command.acceptance.steps :as steps]
            [missile-command.core :as core]))

(defn- fresh-world
  ([] (fresh-world 800 600))
  ([w h] {:state (core/new-game {:width w :height h})}))

(defn- dispatch
  [world text example]
  (steps/dispatch-step world {:text text} example))

(describe "THE END acceptance steps"
  (it "evaluates entry messaging and fireball fill"
    (let [ended (-> (fresh-world)
                    (dispatch "all cities have been destroyed" {})
                    (dispatch "the bonus city reserve is set to 0" {})
                    (dispatch "game over conditions are evaluated" {}))]
      (should= ended (dispatch ended "the game is at THE END" {}))
      (should= ended (dispatch ended "the end message is THE END" {}))
      (should= ended
               (dispatch ended "the end message is not <wrong>"
                         {"wrong" "Game_Over"}))
      (should-throw Exception #"game is not at THE END"
        (dispatch (fresh-world) "the game is at THE END" {}))
      (should= (fresh-world)
               (dispatch (fresh-world) "the game is not at THE END" {}))
      (should-throw Exception #"final score"
        (dispatch ended "the final score is <score>" {"score" "-1"}))
      (should= ended
               (dispatch ended "the end fireball is centered at the playfield center" {}))
      (let [filled (dispatch ended
                             "time advances until the end fireball reaches max radius"
                             {})]
        (should= filled
                 (dispatch filled "the end fireball radius fills the playfield" {}))
        (let [shrunk (dispatch filled
                               "time advances into the end fireball shrink phase"
                               {})]
          (should= shrunk
                   (dispatch shrunk
                             "the end fireball radius is less than its max radius"
                             {})))))))
