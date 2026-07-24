(ns missile-command.acceptance.steps
  (:require [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]))

(def step-handlers
  [{:pattern #"^a new game with width <([A-Za-z0-9_]+)> and height <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param height-param] example]
          (let [width (support/parse-int (support/require-value example width-param) "width")
                height (support/parse-int (support/require-value example height-param) "height")]
            (assoc world :state (core/new-game {:width width :height height}))))}

   {:pattern #"^the playfield width is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [expected (support/parse-int (support/require-value example width-param) "width")
                actual (core/playfield-width (:state world))]
            (when-not (= expected actual)
              (support/fail! (str "playfield width " actual " expected " expected)))
            world))}

   {:pattern #"^the playfield height is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ height-param] example]
          (let [expected (support/parse-int (support/require-value example height-param) "height")
                actual (core/playfield-height (:state world))]
            (when-not (= expected actual)
              (support/fail! (str "playfield height " actual " expected " expected)))
            world))}])

(defn dispatch-step
  [world step example]
  (let [{:keys [text]} step]
    (if-let [handler (first (filter #(re-matches (:pattern %) text) step-handlers))]
      ((:fn handler) world (re-matches (:pattern handler) text) example)
      (support/fail! (str "unsupported step: " text)))))
