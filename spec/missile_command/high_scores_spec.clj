(ns missile-command.high-scores-spec
  (:require [speclj.core :refer :all]
            [missile-command.high-scores :as hs]
            [missile-command.core :as core]))

(describe "high-scores pure table"
  (it "normalizes initials to three uppercase alphanumerics"
    (should= "XYZ" (hs/normalize-initials "xyz"))
    (should= "A1B" (hs/normalize-initials "a1b"))
    (should= "ABC" (hs/normalize-initials "ABC")))

  (it "qualifies positive scores when the table has room or score meets lowest"
    (should-not (hs/qualifies? [] 10 0))
    (should (hs/qualifies? [] 10 1))
    (should (hs/qualifies? [{:initials "AAA" :score 1000}] 10 1))
    (should (hs/qualifies? [{:initials "A" :score 1000}
                            {:initials "B" :score 900}
                            {:initials "C" :score 800}]
                           3 800))
    (should-not (hs/qualifies? [{:initials "A" :score 1000}
                                {:initials "B" :score 900}
                                {:initials "C" :score 800}]
                               3 799)))

  (it "inserts and caps the table in descending order"
    (let [table (-> []
                    (hs/insert 3 "AAA" 1000)
                    (hs/insert 3 "CCC" 500)
                    (hs/insert 3 "BOB" 750))]
      (should= 3 (count table))
      (should (hs/ordered? table))
      (should= "AAA" (:initials (hs/entry-at-rank table 1)))
      (should= "BOB" (:initials (hs/entry-at-rank table 2)))
      (should= 750 (:score (hs/entry-at-rank table 2))))))

(describe "high-score screens"
  (it "opens entry for a qualifying score after THE END confirm"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-high-score-capacity 10)
                    (core/set-score 500)
                    (#(reduce core/destroy-city % (map :id (core/cities %))))
                    (core/set-bonus-city-reserve 0)
                    core/evaluate-game-over
                    core/confirm-end-screen)]
      (should (core/high-score-entry? state))
      (should= 500 (core/pending-high-score state))))

  (it "returns to title without entry when score does not qualify"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-high-score-capacity 3)
                    (core/add-high-score-entry "AAA" 1000)
                    (core/add-high-score-entry "BBB" 900)
                    (core/add-high-score-entry "CCC" 800)
                    (core/set-score 700)
                    (#(reduce core/destroy-city % (map :id (core/cities %))))
                    (core/set-bonus-city-reserve 0)
                    core/evaluate-game-over
                    core/confirm-end-screen)]
      (should (core/title? state))
      (should-not (core/high-score-entry? state))))

  (it "submits initials and returns to title with ordered table"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-high-score-capacity 10)
                    (core/add-high-score-entry "AAA" 1000)
                    (core/add-high-score-entry "CCC" 500)
                    (core/set-score 750)
                    (#(reduce core/destroy-city % (map :id (core/cities %))))
                    (core/set-bonus-city-reserve 0)
                    core/evaluate-game-over
                    core/confirm-end-screen
                    (core/submit-high-score-initials "bob"))
          table (core/high-score-table state)]
      (should (core/title? state))
      (should= "BOB" (core/submitted-high-score-initials state))
      (should= 3 (count table))
      (should= "BOB" (:initials (nth table 1)))
      (should= 750 (:score (nth table 1)))))

  (it "opens high scores view from title"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/add-high-score-entry "AAA" 1000)
                    core/open-high-scores)]
      (should (core/high-scores-view? state))
      (should= "AAA" (:initials (first (core/high-score-table state)))))))
