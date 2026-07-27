(ns missile-command.global-scores-spec
  (:require [speclj.core :refer :all]
            [missile-command.global-scores :as global]))

(describe "global score screen state"
  (it "keeps the high-score view on local scores until a network read succeeds"
    (let [state (global/attach {} global/empty-state 1000 7000)]
      (should= :local (:high-score-page state))))

  (it "rotates between local and global after a successful network read"
    (let [ready (assoc global/empty-state :read-succeeded? true)]
      (should= :local (:high-score-page (global/attach {} ready 1000 4000)))
      (should= :global (:high-score-page (global/attach {} ready 1000 7000)))))

  (it "normalizes wire entries and hides the public player id in display labels"
    (let [entry (global/normalize-entry {:rank 1
                                         :public_code "AB12CD"
                                         :display_name "Uncle Bob"
                                         :initials "BOB"
                                         :score 12345
                                         :wave 4
                                         :created_at "2026-07-27T00:00:00Z"})]
      (should= "AB12CD" (:public-code entry))
      (should= "Uncle Bob" (global/entry-label entry)))))
