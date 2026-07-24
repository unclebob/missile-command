(ns missile-command.acceptance.title-steps
  "Gherkin steps for title screen and session start."
  (:require [clojure.string :as str]
            [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]))

(def handlers
  [
{:pattern #"^the player starts the game$"
    :fn (fn [world _ _]
          (assoc world :state (core/start-game (:state world))))}

   {:pattern #"^the player confirms the end screen$"
    :fn (fn [world _ _]
          (assoc world :state (core/confirm-end-screen (:state world))))}

   {:pattern #"^the screen is title$"
    :fn (fn [world _ _]
          (support/assert-condition (core/title? (:state world))
                                    (str "screen is " (core/screen (:state world))
                                         " expected title"))
          world)}

   {:pattern #"^the screen is playing$"
    :fn (fn [world _ _]
          (support/assert-condition (core/playing? (:state world))
                                    (str "screen is " (core/screen (:state world))
                                         " expected playing"))
          world)}

   {:pattern #"^the title game name is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param] example]
          (let [expected (str/replace (str (support/require-value example name-param))
                                      #"_" " ")
                actual (core/title-game-name-of (:state world))]
            (support/assert-condition (= expected actual)
                                      (str "title name " actual
                                           " expected " expected)))
          world)}

   {:pattern #"^the title shows a start affordance$"
    :fn (fn [world _ _]
          (support/assert-condition
           (core/title-shows-start-affordance? (:state world))
           "title missing start affordance")
          world)}
])

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T15:48:34.272894-05:00", :module-hash "-1068892810", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "-1075084431"} {:id "def/handlers", :kind "def", :line 7, :end-line 47, :hash "-38358753"}]}
;; clj-mutate-manifest-end
