(ns missile-command.global-scores-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]
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
      (should= "Uncle Bob" (global/entry-label entry))))

  (it "builds a ready state from a leaderboard wire payload"
    (let [state (global/leaderboard-ready-state
                 {:leaderboard {:display_name "Club Board"}
                  :scores [{:rank 2
                            :public_code "XYZZY1"
                            :display_name "Ada"
                            :initials "ADA"
                            :score 900
                            :wave 3
                            :created_at "2026-07-27T00:00:00Z"}]}
                 "https://example.test"
                 "Configured Board"
                 12345)]
      (should= :ready (:status state))
      (should= true (:read-succeeded? state))
      (should= 12345 (:last-updated-ms state))
      (should= "https://example.test" (:url state))
      (should= "Configured Board" (:configured-name state))
      (should= "XYZZY1" (:public-code (first (:scores state))))
      (should= "Ada" (global/entry-label (first (:scores state))))))

  (it "selects the player display name by configured submitted initials then leaderboard"
    (should= "Configured Player"
             (global/select-player-display-name {:player-name "Configured Player"
                                                 :configured-name "Board"}
                                                "Submitted" "ABC"))
    (should= "Submitted"
             (global/select-player-display-name {:configured-name "Board"}
                                                "Submitted" "ABC"))
    (should= "ABC"
             (global/select-player-display-name {:configured-name "Board"}
                                                nil "ABC"))
    (should= "Board"
             (global/select-player-display-name {:configured-name "Board"}
                                                nil nil)))

  (it "builds player create payloads with configured-name fallback"
    (should= {:display_name "Ada"}
             (global/player-create-payload "Ada" "Board"))
    (should= {:display_name "Board"}
             (global/player-create-payload nil "Board")))

  (it "builds score submit payloads for host clients"
    (let [state (assoc (core/new-game {:width 800 :height 600})
                       :final-score 1234
                       :wave 5
                       :sim-time 12.75)
          player {:player-id "p1" :player-token "tok"}]
      (should= {:player_id "p1"
                :player_token "tok"
                :run_id "run-1"
                :initials "ACE"
                :score 1234
                :wave 5
                :duration_ms 12750
                :game_version "v-test"
                :host "desktop"}
               (global/score-submit-payload state player "ACE" "desktop" "run-1" "v-test"))
      (should= "browser"
               (:host (global/score-submit-payload state player "ACE" "browser" "run-2" "v-test")))))

  (it "maps submit responses to accepted or failed"
    (should= :accepted (global/submit-status-from-response {:accepted true}))
    (should= :failed (global/submit-status-from-response {:accepted false}))
    (should= :failed (global/submit-status-from-response {})))

  (it "decides when host clients must skip submit"
    (should= :skipped_disabled
             (global/submit-skip-status (assoc global/empty-state
                                               :enabled? false
                                               :read-succeeded? true)))
    (should= :skipped_no_read
             (global/submit-skip-status (assoc global/empty-state
                                               :enabled? true
                                               :read-succeeded? false)))
    (should= nil
             (global/submit-skip-status (assoc global/empty-state
                                               :enabled? true
                                               :read-succeeded? true)))))
