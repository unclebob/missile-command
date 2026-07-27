(ns missile-command.jvm.global-scores-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]
            [missile-command.global-scores :as global]
            [missile-command.jvm.global-scores :as client])
  (:import [java.net.http HttpClient HttpResponse]))

(defn- response
  [status body]
  (reify HttpResponse
    (statusCode [_] status)
    (body [_] body)
    (headers [_] nil)
    (request [_] nil)
    (previousResponse [_] (java.util.Optional/empty))
    (sslSession [_] (java.util.Optional/empty))
    (uri [_] nil)
    (version [_] nil)))

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

  (it "uses defaults when JVM options and environment are absent"
    (let [state (client/initial-state {})]
      (should= true (:enabled? state))
      (should= global/default-url (:url state))
      (should= global/default-name (:configured-name state))
      (should= nil (:player-name state))))

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

  (it "saves a player when the settings file does not exist yet"
    (let [file (java.io.File/createTempFile "missile-command-new-player" ".edn")
          path (.getPath file)
          player {:player-id "p2" :player-token "tok2"}]
      (.delete file)
      (should= player (client/save-player! path player))
      (should= {:global-player player} (read-string (slurp path)))))

  (it "builds JSON HTTP requests"
    (let [get-request (#'client/json-request :get "https://scores.test/leaderboard" nil)
          post-request (#'client/json-request :post "https://scores.test/scores" {:score 10})]
      (should= "GET" (.method get-request))
      (should= "POST" (.method post-request))
      (should= ["application/json"] (.allValues (.headers get-request) "accept"))
      (should= ["application/json"] (.allValues (.headers post-request) "content-type"))))

  (it "parses successful JSON responses and ignores unsuccessful responses"
    (let [requests (atom [])
          fake-client (delay
                        (proxy [HttpClient] []
                          (send [request _handler]
                            (swap! requests conj {:method (.method request)
                                                  :uri (str (.uri request))})
                            (if (= "POST" (.method request))
                              (response 201 "{\"accepted\":true}")
                              (response 200 "{\"entries\":[]}")))))
          failing-client (delay
                           (proxy [HttpClient] []
                             (send [_request _handler]
                               (response 503 "{\"error\":\"offline\"}"))))]
      (with-redefs [client/client fake-client]
        (should= {:entries []} (#'client/get-json "https://scores.test/leaderboard"))
        (should= {:accepted true} (#'client/post-json "https://scores.test/scores" {:score 10}))
        (should= [{:method "GET" :uri "https://scores.test/leaderboard"}
                  {:method "POST" :uri "https://scores.test/scores"}]
                 @requests))
      (with-redefs [client/client failing-client]
        (should= nil (#'client/get-json "https://scores.test/leaderboard"))
        (should= nil (#'client/post-json "https://scores.test/scores" {:score 10})))))

  (it "uses the concrete HTTP transport shape for leaderboard, player, and score calls"
    (let [file (java.io.File/createTempFile "missile-command-http-player" ".edn")
          state (atom (assoc global/empty-state
                             :url "https://scores.test"
                             :configured-name "Board"))
          calls (atom [])
          fake-client (delay
                        (proxy [HttpClient] []
                          (send [request _handler]
                            (let [uri (str (.uri request))
                                  method (.method request)]
                              (swap! calls conj [method uri])
                              (cond
                                (= uri "https://scores.test/leaderboard?limit=10")
                                (response 200 "{\"entries\":[]}")

                                (= uri "https://scores.test/players")
                                (response 200 "{\"player_id\":\"p1\",\"player_token\":\"tok\",\"public_code\":\"ABC123\",\"display_name\":\"Ada\"}")

                                (= uri "https://scores.test/scores")
                                (response 200 "{\"accepted\":true}")

                                :else
                                (response 404 "{}"))))))]
      (.delete file)
      (with-redefs [client/client fake-client]
        (let [transport (#'client/http-client (.getPath file) state)]
          (should= {:entries []} ((:fetch-leaderboard transport) @state))
          (should= {:player-id "p1"
                    :player-token "tok"
                    :public-code "ABC123"
                    :display-name "Ada"}
                   ((:ensure-player transport) "Ada"))
          (should= {:accepted true} ((:submit-score transport) nil {:score 99}))))
      (should= [["GET" "https://scores.test/leaderboard?limit=10"]
                ["POST" "https://scores.test/players"]
                ["POST" "https://scores.test/scores"]]
               @calls)))

  (it "delegates ensure-player through the default transport arity"
    (let [called (atom nil)]
      (with-redefs [client/load-player (fn [path]
                                         (reset! called path)
                                         {:player-id "saved"})]
        (should= {:player-id "saved"}
                 (client/ensure-player! "tmp/saved-player.edn"
                                        (atom global/empty-state)
                                        "Ada"))
        (should= "tmp/saved-player.edn" @called))))

  (it "does not start a leaderboard fetch when global scores are disabled"
    (let [state (atom (assoc global/empty-state :enabled? false))]
      (should= nil (client/fetch-leaderboard! state
                                              {:fetch-leaderboard
                                               (fn [_] (throw (ex-info "unused" {})))}
                                              (constantly 111)))
      (should= :idle (:status @state))))

  (it "marks leaderboard data ready through injected transport"
    (let [state (atom (assoc global/empty-state
                             :enabled? true
                             :url "https://scores.test"
                             :configured-name "Board"))
          f (client/fetch-leaderboard! state
                                       {:fetch-leaderboard
                                        (fn [_]
                                          {:entries [{:initials "AAA"
                                                      :score 100}]})}
                                       (constantly 111))]
      @f
      (should= :ready (:status @state))
      (should= true (:read-succeeded? @state))
      (should= 111 (:last-updated-ms @state))))

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
