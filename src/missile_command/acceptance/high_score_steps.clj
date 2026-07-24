(ns missile-command.acceptance.high-score-steps
  "Gherkin steps for high-score table and initials entry."
  (:require [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]
            [missile-command.high-scores :as high-scores]))

(def handlers
  [{:pattern #"^the high score table capacity is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ cap-param] example]
          (assoc world :state
                 (core/set-high-score-capacity
                  (:state world)
                  (support/example-int example cap-param "capacity"))))}

   {:pattern #"^a high score entry initials ([A-Za-z0-9]+) with score (\d+)$"
    :fn (fn [world [_ initials score-text] _]
          (assoc world :state
                 (core/add-high-score-entry
                  (:state world)
                  initials
                  (support/parse-int score-text "score"))))}

   {:pattern #"^the player enters high score initials <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ initials-param] example]
          (assoc world :state
                 (core/submit-high-score-initials
                  (:state world)
                  (support/require-value example initials-param))))}

   {:pattern #"^the player opens high scores from the title$"
    :fn (fn [world _ _]
          (assoc world :state (core/open-high-scores (:state world))))}

   {:pattern #"^the screen is high-score-entry$"
    :fn (fn [world _ _]
          (support/assert-condition (core/high-score-entry? (:state world))
                                    (str "screen is " (core/screen (:state world))
                                         " expected high-score-entry"))
          world)}

   {:pattern #"^the screen is not high-score-entry$"
    :fn (fn [world _ _]
          (support/assert-condition (not (core/high-score-entry? (:state world)))
                                    "screen is high-score-entry but should not be")
          world)}

   {:pattern #"^the screen is high-scores$"
    :fn (fn [world _ _]
          (support/assert-condition (core/high-scores-view? (:state world))
                                    (str "screen is " (core/screen (:state world))
                                         " expected high-scores"))
          world)}

   {:pattern #"^the pending high score is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ score-param] example]
          (let [expected (support/example-int example score-param "pending score")
                actual (core/pending-high-score (:state world))]
            (support/assert-condition (= expected actual)
                                      (str "pending high score " actual
                                           " expected " expected)))
          world)}

   {:pattern #"^the high score table is ordered by score descending$"
    :fn (fn [world _ _]
          (let [table (core/high-score-table (:state world))]
            (support/assert-condition (high-scores/ordered? table)
                                      (str "high scores not ordered: " table)))
          world)}

   {:pattern #"^the high score at rank <([A-Za-z0-9_]+)> has initials <([A-Za-z0-9_]+)> and score <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ rank-param init-param score-param] example]
          (let [rank (support/example-int example rank-param "rank")
                initials (str (support/require-value example init-param))
                score (support/example-int example score-param "score")
                entry (high-scores/entry-at-rank
                       (core/high-score-table (:state world)) rank)]
            (support/assert-condition entry (str "missing rank " rank))
            (support/assert-condition (= initials (:initials entry))
                                      (str "rank " rank " initials "
                                           (:initials entry) " expected " initials))
            (support/assert-condition (= score (:score entry))
                                      (str "rank " rank " score "
                                           (:score entry) " expected " score)))
          world)}

   {:pattern #"^the high score at rank (\d+) has initials ([A-Za-z0-9]+) and score (\d+)$"
    :fn (fn [world [_ rank-text initials score-text] _]
          (let [rank (support/parse-int rank-text "rank")
                score (support/parse-int score-text "score")
                entry (high-scores/entry-at-rank
                       (core/high-score-table (:state world)) rank)]
            (support/assert-condition entry (str "missing rank " rank))
            (support/assert-condition (= initials (:initials entry))
                                      (str "rank " rank " initials "
                                           (:initials entry) " expected " initials))
            (support/assert-condition (= score (:score entry))
                                      (str "rank " rank " score "
                                           (:score entry) " expected " score)))
          world)}

   {:pattern #"^the high score table has <([A-Za-z0-9_]+)> entries$"
    :fn (fn [world [_ count-param] example]
          (let [expected (support/example-int example count-param "entry count")
                actual (count (core/high-score-table (:state world)))]
            (support/assert-condition (= expected actual)
                                      (str "high score entries " actual
                                           " expected " expected)))
          world)}

   {:pattern #"^the high score table has (\d+) entries$"
    :fn (fn [world [_ count-text] _]
          (let [expected (support/parse-int count-text "entry count")
                actual (count (core/high-score-table (:state world)))]
            (support/assert-condition (= expected actual)
                                      (str "high score entries " actual
                                           " expected " expected)))
          world)}

   {:pattern #"^the high score table lowest score is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ score-param] example]
          (let [expected (support/example-int example score-param "lowest")
                scores (map :score (core/high-score-table (:state world)))
                actual (when (seq scores) (apply min scores))]
            (support/assert-condition (= expected actual)
                                      (str "lowest high score " actual
                                           " expected " expected)))
          world)}

   {:pattern #"^the high score table does not include score <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ score-param] example]
          (let [dropped (support/example-int example score-param "dropped")
                scores (set (map :score (core/high-score-table (:state world))))]
            (support/assert-condition (not (contains? scores dropped))
                                      (str "table still includes score " dropped)))
          world)}

   {:pattern #"^the submitted high score initials are <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ init-param] example]
          (let [expected (str (support/require-value example init-param))
                actual (core/submitted-high-score-initials (:state world))]
            (support/assert-condition (= expected actual)
                                      (str "submitted initials " actual
                                           " expected " expected)))
          world)}

   {:pattern #"^the submitted high score initials length is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ len-param] example]
          (let [expected (support/example-int example len-param "length")
                actual (count (str (core/submitted-high-score-initials
                                    (:state world))))]
            (support/assert-condition (= expected actual)
                                      (str "initials length " actual
                                           " expected " expected)))
          world)}])
