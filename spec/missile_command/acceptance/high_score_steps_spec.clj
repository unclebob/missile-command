(ns missile-command.acceptance.high-score-steps-spec
  (:require [speclj.core :refer :all]
            [missile-command.acceptance.steps :as steps]
            [missile-command.core :as core]))

(defn- fresh-world
  ([] (fresh-world 800 600))
  ([w h] {:state (core/new-game {:width w :height h})}))

(defn- dispatch
  [world text example]
  (steps/dispatch-step world {:text text} example))

(defn- end-and-confirm
  [world score]
  (-> world
      (dispatch "the score becomes <score>" {"score" (str score)})
      (dispatch "all cities have been destroyed" {})
      (dispatch "the bonus city reserve is set to 0" {})
      (dispatch "game over conditions are evaluated" {})
      (dispatch "the player confirms the end screen" {})))

(describe "high score acceptance steps"
  (it "seeds capacity and entries then opens entry for a qualifying score"
    (let [world (-> (fresh-world)
                    (dispatch "the high score table capacity is <capacity>"
                              {"capacity" "3"})
                    (dispatch "a high score entry initials AAA with score 1000" {})
                    (dispatch "a high score entry initials BBB with score 900" {})
                    (end-and-confirm 950))]
      (should= world
               (dispatch world "the screen is high-score-entry" {}))
      (should= world
               (dispatch world "the pending high score is <score>"
                         {"score" "950"}))
      (should-throw Exception #"pending high score"
        (dispatch world "the pending high score is <score>"
                  {"score" "1"}))
      (should-throw Exception #"expected high-score-entry"
        (dispatch (fresh-world) "the screen is high-score-entry" {}))))

  (it "rejects entry for a non-qualifying score"
    (let [world (-> (fresh-world)
                    (dispatch "the high score table capacity is <capacity>"
                              {"capacity" "3"})
                    (dispatch "a high score entry initials AAA with score 1000" {})
                    (dispatch "a high score entry initials BBB with score 900" {})
                    (dispatch "a high score entry initials CCC with score 800" {})
                    (end-and-confirm 700))]
      (should= world
               (dispatch world "the screen is not high-score-entry" {}))
      (should (core/title? (:state world)))
      (should-throw Exception #"high-score-entry but should not be"
        (dispatch (end-and-confirm (fresh-world) 100)
                  "the screen is not high-score-entry" {}))))

  (it "submits initials ranked and capped with length normalization"
    (let [entered (-> (fresh-world)
                      (dispatch "the high score table capacity is <capacity>"
                                {"capacity" "3"})
                      (dispatch "a high score entry initials AAA with score 1000" {})
                      (dispatch "a high score entry initials CCC with score 500" {})
                      (end-and-confirm 750)
                      (dispatch "the player enters high score initials <initials>"
                                {"initials" "bob"}))]
      (should= entered
               (dispatch entered
                         "the high score table is ordered by score descending" {}))
      (should= entered
               (dispatch entered
                         "the high score at rank <rank> has initials <initials> and score <score>"
                         {"rank" "2" "initials" "BOB" "score" "750"}))
      (should= entered
               (dispatch entered
                         "the high score table has <entry_count> entries"
                         {"entry_count" "3"}))
      (should= entered
               (dispatch entered
                         "the submitted high score initials are <normalized>"
                         {"normalized" "BOB"}))
      (should= entered
               (dispatch entered
                         "the submitted high score initials length is <length>"
                         {"length" "3"}))
      (should-throw Exception #"rank 2 initials"
        (dispatch entered
                  "the high score at rank <rank> has initials <initials> and score <score>"
                  {"rank" "2" "initials" "AAA" "score" "750"}))
      (should-throw Exception #"rank 2 score"
        (dispatch entered
                  "the high score at rank <rank> has initials <initials> and score <score>"
                  {"rank" "2" "initials" "BOB" "score" "1"}))
      (should-throw Exception #"high score entries"
        (dispatch entered
                  "the high score table has <entry_count> entries"
                  {"entry_count" "9"}))
      (should-throw Exception #"submitted initials"
        (dispatch entered
                  "the submitted high score initials are <normalized>"
                  {"normalized" "XXX"}))
      (should-throw Exception #"initials length"
        (dispatch entered
                  "the submitted high score initials length is <length>"
                  {"length" "1"}))))

  (it "drops the lowest score when the table is full"
    (let [capped (-> (fresh-world)
                     (dispatch "the high score table capacity is <capacity>"
                               {"capacity" "3"})
                     (dispatch "a high score entry initials AAA with score 1000" {})
                     (dispatch "a high score entry initials BBB with score 900" {})
                     (dispatch "a high score entry initials CCC with score 800" {})
                     (end-and-confirm 850)
                     (dispatch "the player enters high score initials <initials>"
                               {"initials" "NEW"}))]
      (should= capped
               (dispatch capped
                         "the high score table has <capacity> entries"
                         {"capacity" "3"}))
      (should= capped
               (dispatch capped
                         "the high score table lowest score is <lowest>"
                         {"lowest" "850"}))
      (should= capped
               (dispatch capped
                         "the high score table does not include score <dropped>"
                         {"dropped" "800"}))
      (should-throw Exception #"lowest high score"
        (dispatch capped
                  "the high score table lowest score is <lowest>"
                  {"lowest" "1"}))
      (should-throw Exception #"table still includes score"
        (dispatch capped
                  "the high score table does not include score <dropped>"
                  {"dropped" "850"}))))

  (it "opens the high scores view with literal rank asserts"
    (let [world (-> (fresh-world)
                    (dispatch "a high score entry initials AAA with score 1000" {})
                    (dispatch "a high score entry initials BBB with score 500" {})
                    (dispatch "the player opens high scores from the title" {}))]
      (should= world (dispatch world "the screen is high-scores" {}))
      (should= world
               (dispatch world
                         "the high score at rank 1 has initials AAA and score 1000"
                         {}))
      (should= world
               (dispatch world
                         "the high score at rank 2 has initials BBB and score 500"
                         {}))
      (should= world
               (dispatch world "the high score table has 2 entries" {}))
      (should-throw Exception #"rank 1 initials"
        (dispatch world
                  "the high score at rank 1 has initials ZZZ and score 1000"
                  {}))
      (should-throw Exception #"rank 1 score"
        (dispatch world
                  "the high score at rank 1 has initials AAA and score 1"
                  {}))
      (should-throw Exception #"high score entries"
        (dispatch world "the high score table has 9 entries" {}))
      (should-throw Exception #"expected high-scores"
        (dispatch (fresh-world) "the screen is high-scores" {}))))

  (it "covers mixed literal initials and submitted length asserts"
    (let [entered (-> (fresh-world)
                      (dispatch "the high score table capacity is 10" {})
                      (end-and-confirm 100)
                      (dispatch "the player enters high score initials a1b!!" {}))]
      (should= entered
               (dispatch entered
                         "the high score at rank <rank> has initials A1B and score <score>"
                         {"rank" "1" "score" "100"}))
      (should-throw Exception #"rank 1 initials"
        (dispatch entered
                  "the high score at rank <rank> has initials NEW and score <score>"
                  {"rank" "1" "score" "100"}))
      (should-throw Exception #"rank 1 score"
        (dispatch entered
                  "the high score at rank <rank> has initials A1B and score <score>"
                  {"rank" "1" "score" "1"}))
      (should= entered
               (dispatch entered "the submitted high score initials are A1B" {}))
      (should= entered
               (dispatch entered "the submitted high score initials length is 3" {}))
      (should-throw Exception #"submitted initials"
        (dispatch entered "the submitted high score initials are ZZZ" {}))
      (should-throw Exception #"initials length"
        (dispatch entered "the submitted high score initials length is 1" {})))))
