(ns missile-command.acceptance.wave-banner-steps
  "Gherkin steps for between-waves WAVE N banner."
  (:require [clojure.string :as str]
            [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]))

(def handlers
  [{:pattern #"^the screen is wave-banner$"
    :fn (fn [world _ _]
          (support/assert-condition (core/wave-banner? (:state world))
                                    (str "screen is " (core/screen (:state world))
                                         " expected wave-banner"))
          (assoc world :banner-center-distance
                 (core/wave-banner-distance-to-center (:state world))))}

   {:pattern #"^the wave banner announces wave <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param] example]
          (let [expected (support/example-int example wave-param "announced wave")
                actual (core/wave-banner-announced-wave (:state world))]
            (support/assert-condition (= expected actual)
                                      (str "announced wave " actual
                                           " expected " expected)))
          world)}

   {:pattern #"^the wave banner text is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ text-param] example]
          (let [expected (str/replace
                          (str (support/require-value example text-param))
                          #"_" " ")
                actual (core/wave-banner-text (:state world))]
            (support/assert-condition (= expected actual)
                                      (str "banner text " actual
                                           " expected " expected)))
          world)}

   {:pattern #"^the wave banner phase is (enter|exit)$"
    :fn (fn [world [_ phase] _]
          (let [actual (name (or (core/wave-banner-phase (:state world)) :none))]
            (support/assert-condition (= phase actual)
                                      (str "banner phase " actual
                                           " expected " phase)))
          world)}

   {:pattern #"^the wave banner text has moved closer to the playfield center$"
    :fn (fn [world _ _]
          (let [before (double (or (:banner-center-distance world) 1.0e9))
                after (core/wave-banner-distance-to-center (:state world))]
            (support/assert-condition (< after before)
                                      (str "banner distance " after
                                           " not closer than " before))
            (assoc world :banner-center-distance after)))}

   {:pattern #"^the wave banner text has moved farther from the playfield center$"
    :fn (fn [world _ _]
          (let [before (double (or (:banner-center-distance world) 0.0))
                after (core/wave-banner-distance-to-center (:state world))]
            (support/assert-condition (> after before)
                                      (str "banner distance " after
                                           " not farther than " before))
            (assoc world :banner-center-distance after)))}

   {:pattern #"^the wave banner text is not fully off screen$"
    :fn (fn [world _ _]
          (let [p (core/wave-banner-text-position (:state world))
                w (core/playfield-width (:state world))
                x (:x p)]
            (support/assert-condition (and (> x -200.0) (< x (+ w 200.0)))
                                      (str "banner x " x " looks off-screen"))
            world))}

   {:pattern #"^time advances until the wave banner text reaches the playfield center$"
    :fn (fn [world _ _]
          (loop [s (:state world) n 0]
            (cond
              (not (core/wave-banner? s))
              (support/fail! "left wave banner before reaching center")
              (= :exit (core/wave-banner-phase s))
              (assoc world :state s
                     :banner-center-distance
                     (core/wave-banner-distance-to-center s))
              (> n 5000)
              (support/fail! "banner never reached center")
              :else
              (recur (:state (core/tick s 0.05)) (inc n)))))}

   {:pattern #"^time advances until the wave banner finishes$"
    :fn (fn [world _ _]
          (loop [s (:state world) n 0]
            (cond
              (core/playing? s) (assoc world :state s)
              (> n 10000) (support/fail! "wave banner never finished")
              :else (recur (:state (core/tick s 0.05)) (inc n)))))}])
