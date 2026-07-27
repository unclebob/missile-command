(ns missile-command.jvm.global-scores-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]
            [missile-command.global-scores :as global]
            [missile-command.jvm.global-scores :as client]))

(describe "JVM global score client seam"
  (it "skips submit until a leaderboard read has succeeded"
    (let [state (atom global/empty-state)]
      (should= :skipped_no_read
               (:submit-status
                (client/submit-score! "tmp/no-file.edn"
                                      state
                                      (core/new-game {:width 800 :height 600})
                                      "AAA"
                                      nil
                                      {:ensure-player (fn [_] (throw (ex-info "unused" {})))
                                       :submit-score (fn [_ _] (throw (ex-info "unused" {})))}
                                      (constantly "run")
                                      "test")))
      (should= :skipped_no_read (:submit-status @state))))

  (it "submits through injected functions without an HTTP server"
    (let [submitted (atom nil)
          state (atom (assoc global/empty-state
                             :read-succeeded? true
                             :player-name "Configured Player"))
          game (assoc (core/new-game {:width 800 :height 600})
                      :pending-high-score 4567
                      :wave 4
                      :sim-time 3.25)
          f (client/submit-score! "tmp/no-file.edn"
                                  state
                                  game
                                  "BOB"
                                  "Submitted Player"
                                  {:ensure-player (fn [display-name]
                                                    {:player-id "p1"
                                                     :player-token "tok"
                                                     :display-name display-name})
                                   :submit-score (fn [player payload]
                                                   (reset! submitted {:player player
                                                                      :payload payload})
                                                   {:accepted true})}
                                  (constantly "run-123")
                                  "test-version")]
      @f
      (should= :accepted (:submit-status @state))
      (should= "Configured Player" (get-in @submitted [:player :display-name]))
      (should= {:player_id "p1"
                :player_token "tok"
                :run_id "run-123"
                :initials "BOB"
                :score 4567
                :wave 4
                :duration_ms 3250
                :game_version "test-version"
                :host "desktop"}
               (:payload @submitted))))

  (it "marks submit failures without throwing to callers"
    (let [state (atom (assoc global/empty-state :read-succeeded? true))
          f (client/submit-score! "tmp/no-file.edn"
                                  state
                                  (core/new-game {:width 800 :height 600})
                                  "ERR"
                                  nil
                                  {:ensure-player (fn [_] {:player-id "p1" :player-token "tok"})
                                   :submit-score (fn [_ _] (throw (ex-info "offline" {})))}
                                  (constantly "run")
                                  "test")]
      @f
      (should= :failed (:submit-status @state))
      (should= "offline" (:error @state))))

  (it "marks leaderboard fetch failures without throwing to callers"
    (let [state (atom global/empty-state)
          f (client/fetch-leaderboard! state
                                       {:fetch-leaderboard
                                        (fn [_] (throw (ex-info "read failed" {})))}
                                       (constantly 111))]
      @f
      (should= :failed (:status @state))
      (should= "read failed" (:error @state)))))
