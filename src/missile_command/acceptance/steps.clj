(ns missile-command.acceptance.steps
  (:require [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]))

(defn- assert-playfield-dimension
  [world example param-name label reader]
  (let [expected (support/example-int example param-name label)
        actual (reader (:state world))]
    (when-not (= expected actual)
      (support/fail! (str "playfield " label " " actual " expected " expected)))
    world))

(def step-handlers
  [{:pattern #"^a new game with width <([A-Za-z0-9_]+)> and height <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param height-param] example]
          (assoc world :state
                 (core/new-game
                  {:width (support/example-int example width-param "width")
                   :height (support/example-int example height-param "height")})))}

   {:pattern #"^the playfield width is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (assert-playfield-dimension world example width-param "width"
                                      core/playfield-width))}

   {:pattern #"^the playfield height is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ height-param] example]
          (assert-playfield-dimension world example height-param "height"
                                      core/playfield-height))}])

(defn- match-handler
  [text]
  (some (fn [handler]
          (when-let [matches (re-matches (:pattern handler) text)]
            [handler matches]))
        step-handlers))

(defn dispatch-step
  [world step example]
  (let [text (:text step)]
    (if-let [[handler matches] (match-handler text)]
      ((:fn handler) world matches example)
      (support/fail! (str "unsupported step: " text)))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T09:44:32.740429-05:00", :module-hash "-292796897", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "817333596"} {:id "defn-/assert-playfield-dimension", :kind "defn-", :line 5, :end-line 11, :hash "-1180112229"} {:id "def/step-handlers", :kind "def", :line 13, :end-line 29, :hash "2054280564"} {:id "defn-/match-handler", :kind "defn-", :line 31, :end-line 36, :hash "-760290467"} {:id "defn/dispatch-step", :kind "defn", :line 38, :end-line 43, :hash "-1121977204"}]}
;; clj-mutate-manifest-end
