(ns missile-command.jvm.global-scores-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]
            [missile-command.global-scores :as global]
            [missile-command.jvm.global-scores :as client]))

(describe "JVM global score client seam"
  (it "builds initial state from JVM options"
    (let [state (client/initial-state {:no-global-scores? true
                                       :leaderboard-url "https://scores.test"
                                       :leaderboard-name "Test Board"
                                       :player-name "Ada"})]
      (should= false (:enabled? state))
      (should= "https://scores.test" (:url state))
      (should= "Test Board" (:configured-name state))
      (should= "Ada" (:player-name state))))

  (it "persists and reloads the player without disturbing other settings"
    (let [path (str (java.io.File/createTempFile "missile-command-player" ".edn"))
          player {:player-id "p1" :player-token "tok"}]
      (spit path (pr-str {:volume 2}))
      (should= player (client/save-player! path player))
      (should= player (client/load-player path))
      (should= 2 (:volume (read-string (slurp path))))))

  (it "returns nil for missing or unreadable player settings"
    (let [missing (str (java.io.File/createTempFile "missile-command-missing-player" ".edn"))]
      (.delete (java.io.File. missing))
      (should= nil (client/load-player missing))
      (spit missing "{")
      (should= nil (client/load-player missing))))

  (it "builds JSON HTTP requests"
    (let [get-request (#'client/json-request :get "https://scores.test/leaderboard" nil)
          post-request (#'client/json-request :post "https://scores.test/scores" {:score 10})]
      (should= "GET" (.method get-request))
      (should= "POST" (.method post-request))
      (should= ["application/json"] (.allValues (.headers get-request) "accept"))
      (should= ["application/json"] (.allValues (.headers post-request) "content-type"))))

  (it "skips submit when global scores are disabled"
    (let [state (atom (assoc global/empty-state
                             :enabled? false
                             :read-succeeded? true))]
      (should= :skipped_disabled
               (:submit-status
                (client/submit-score! "tmp/no-file.edn"
                                      state
                                      (core/new-game {:width 800 :height 600})
                                      "AAA"
                                      nil
                                      {:ensure-player (fn [_] (throw (ex-info "unused" {})))
                                       :submit-score (fn [_ _] (throw (ex-info "unused" {})))}
                                      (constantly "run")
                                      "test")))))

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
