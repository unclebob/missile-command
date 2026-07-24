(ns missile-command.acceptance.end-steps
  "Gherkin steps for THE END game-over presentation."
  (:require [clojure.string :as str]
            [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]))
(def handlers
  [
   {:pattern #"^game over conditions are evaluated$"
    :fn (fn [world _ _]
          (assoc world :state (core/evaluate-game-over (:state world))))}

   {:pattern #"^the game is at THE END$"
    :fn (fn [world _ _]
          (support/assert-condition (core/the-end? (:state world))
                                    "game is not at THE END")
          world)}

   {:pattern #"^the game is not at THE END$"
    :fn (fn [world _ _]
          (support/assert-condition (not (core/the-end? (:state world)))
                                    "game is at THE END but should not be")
          world)}

   {:pattern #"^the end message is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ msg-param] example]
          (let [expected (str/replace (str (support/require-value example msg-param))
                                      #"_" " ")
                actual (core/end-message (:state world))]
            (support/assert-condition (= expected actual)
                                      (str "end message " actual
                                           " expected " expected)))
          world)}

   {:pattern #"^the end message is THE END$"
    :fn (fn [world _ _]
          (support/assert-condition (= "THE END" (core/end-message (:state world)))
                                    (str "end message "
                                         (core/end-message (:state world))))
          world)}

   {:pattern #"^the end message is not <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ msg-param] example]
          (let [wrong (str/replace (str (support/require-value example msg-param))
                                   #"_" " ")
                actual (core/end-message (:state world))]
            (support/assert-condition (not= wrong actual)
                                      (str "end message should not be " wrong)))
          world)}

   {:pattern #"^the final score is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ score-param] example]
          (let [expected (support/example-int example score-param "final score")
                actual (core/final-score (:state world))]
            (support/assert-condition (= expected actual)
                                      (str "final score " actual
                                           " expected " expected)))
          world)}

   {:pattern #"^the end fireball is centered at the playfield center$"
    :fn (fn [world _ _]
          (support/assert-condition (core/end-fireball-centered? (:state world))
                                    "end fireball not centered")
          world)}

   {:pattern #"^time advances until the end fireball reaches max radius$"
    :fn (fn [world _ _]
          (when-not (core/end-fireball (:state world))
            (support/fail! "missing end fireball"))
          (support/advance-until world
                                 core/end-fireball-fills-playfield?
                                 core/tick
                                 0.05
                                 20000
                                 "end fireball never reached max"))}

   {:pattern #"^the end fireball radius fills the playfield$"
    :fn (fn [world _ _]
          (support/assert-condition
           (core/end-fireball-fills-playfield? (:state world))
           "end fireball does not fill playfield")
          world)}

   {:pattern #"^time advances into the end fireball shrink phase$"
    :fn (fn [world _ _]
          (when-not (core/end-fireball (:state world))
            (support/fail! "missing end fireball"))
          (support/advance-until
           world
           (fn [s]
             (let [fb (core/end-fireball s)
                   age (double (:age fb 0.0))
                   expand (double (:expand-seconds fb))]
               (and (> age expand)
                    (< (double (:radius fb)) (double (:max-radius fb))))))
           core/tick
           0.05
           20000
           "end fireball never shrank"))}

   {:pattern #"^the end fireball radius is less than its max radius$"
    :fn (fn [world _ _]
          (let [fb (core/end-fireball (:state world))]
            (support/assert-condition fb "missing end fireball")
            (support/assert-lt (double (:radius fb))
                               (double (:max-radius fb))
                               "end fireball still at max radius"))
          world)}

   {:pattern #"^the end message glyph bounds fill the end fireball max expanse$"
    :fn (fn [world _ _]
          (support/assert-condition
           (core/end-message-fills-max-expanse? (:state world))
           "end message does not fill fireball max expanse")
          world)}

   {:pattern #"^the end message is centered at the playfield center$"
    :fn (fn [world _ _]
          (support/assert-condition (core/end-message-centered? (:state world))
                                    "end message not centered")
          world)}

   {:pattern #"^time advances until the end fireball radius is (\d+)$"
    :fn (fn [world [_ r-text] _]
          (let [target (support/parse-int r-text "partial radius")]
            (when-not (core/end-fireball (:state world))
              (support/fail! "missing end fireball"))
            (let [advanced (support/advance-until
                            world
                            (fn [s]
                              (>= (double (:radius (core/end-fireball s) 0.0))
                                  target))
                            core/tick
                            0.01
                            20000
                            "end fireball radius never reached")]
              (assoc advanced :end-reveal-at-partial
                     (core/end-message-reveal (:state advanced))))))}

   {:pattern #"^the end message is not Game_Over$"
    :fn (fn [world _ _]
          (support/assert-condition (not= "Game Over" (core/end-message (:state world)))
                                    "end message should not be Game Over")
          world)}

   {:pattern #"^time advances until the end fireball radius is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ r-param] example]
          (let [target (support/example-int example r-param "partial radius")]
            (when-not (core/end-fireball (:state world))
              (support/fail! "missing end fireball"))
            (let [advanced (support/advance-until
                            world
                            (fn [s]
                              (>= (double (:radius (core/end-fireball s) 0.0))
                                  target))
                            core/tick
                            0.01
                            20000
                            "end fireball radius never reached")]
              (assoc advanced :end-reveal-at-partial
                     (core/end-message-reveal (:state advanced))))))}

   {:pattern #"^the end message visibility is clipped to the end fireball disk$"
    :fn (fn [world _ _]
          (support/assert-condition
           (core/end-message-visibility-clipped? (:state world))
           "end message not clipped to fireball")
          world)}

   {:pattern #"^the end message is not visible outside the end fireball$"
    :fn (fn [world _ _]
          (let [s (:state world)
                fb (core/end-fireball s)
                ;; Sample a point far outside the disk
                ox (+ (double (:x fb)) (double (:radius fb)) 50.0)
                oy (double (:y fb))]
            (support/assert-condition
             (not (core/end-message-point-visible? s ox oy))
             "end message visible outside fireball"))
          world)}

   {:pattern #"^more of the end message is revealed as the end fireball radius grows$"
    :fn (fn [world _ _]
          (let [s (:state world)
                before (double (or (:end-reveal-at-partial world) 0.0))
                after (:state (core/tick s 0.5))
                reveal (core/end-message-reveal after)]
            (support/assert-condition (> reveal before)
                                      (str "reveal did not increase: "
                                           before " -> " reveal)))
          world)}
])

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T15:39:32.660582-05:00", :module-hash "2143778125", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "725814157"} {:id "def/handlers", :kind "def", :line 6, :end-line 168, :hash "461917026"}]}
;; clj-mutate-manifest-end
