(ns missile-command.acceptance.pause-steps-spec
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

(describe "pause acceptance steps"
  (it "pauses resumes and freezes recorded enemy progress"
    (let [world (-> (fresh-playing)
                    (dispatch "an enemy missile targeting city <city_index>"
                              {"city_index" "0"})
                    (dispatch "time advances by <seconds> seconds"
                              {"seconds" "0.1"}))
          recorded (dispatch world
                             "the first enemy missile progress is recorded" {})
          progress (double (:recorded-enemy-progress recorded))
          paused (dispatch recorded "the player pauses the game" {})
          held (dispatch paused
                         "time advances by <seconds> seconds"
                         {"seconds" "0.5"})]
      (should (< 0.0 progress))
      (should= paused (dispatch paused "the screen is paused" {}))
      (should= held
               (dispatch held
                         "the first enemy missile progress equals the recorded progress"
                         {}))
      (should-throw Exception #"progress"
        (dispatch (assoc-in held [:state :enemy-missiles 0 :progress]
                            (+ progress 0.2))
                  "the first enemy missile progress equals the recorded progress"
                  {}))
      (let [resumed (dispatch held "the player resumes the game" {})
            advanced (dispatch resumed
                               "time advances by <seconds> seconds"
                               {"seconds" "0.1"})]
        (should (core/playing? (:state resumed)))
        (should= advanced
                 (dispatch advanced
                           "the first enemy missile progress is greater than the recorded progress"
                           {})))))

  (it "rejects pause assertion while playing"
    (should-throw Exception #"expected paused"
      (dispatch (fresh-playing) "the screen is paused" {}))))
