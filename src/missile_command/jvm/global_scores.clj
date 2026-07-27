(ns missile-command.jvm.global-scores
  "Asynchronous Cloudflare leaderboard client for the JVM host."
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [missile-command.core :as core]
            [missile-command.global-scores :as global])
  (:import [java.net URI]
           [java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers]
           [java.time Duration]
           [java.util UUID]))

(def timeout-ms 1200)

(defonce client
  (delay (-> (HttpClient/newBuilder)
             (.connectTimeout (Duration/ofMillis timeout-ms))
             (.build))))

(defn initial-state
  [opts]
  (assoc global/empty-state
         :enabled? (not (:no-global-scores? opts))
         :url (or (:leaderboard-url opts)
                  (System/getenv "MC_LEADERBOARD_URL")
                  global/default-url)
         :configured-name (or (:leaderboard-name opts)
                              (System/getenv "MC_LEADERBOARD_NAME")
                              global/default-name)
         :player-name (or (:player-name opts)
                          (System/getenv "MC_PLAYER_NAME"))))

(defn- settings-file
  [settings-path]
  (io/file settings-path))

(defn load-player
  [settings-path]
  (let [file (settings-file settings-path)]
    (when (.isFile file)
      (try
        (:global-player (edn/read-string (slurp file)))
        (catch Exception _
          nil)))))

(defn save-player!
  [settings-path player]
  (let [file (settings-file settings-path)
        settings (if (.isFile file)
                   (try (edn/read-string (slurp file))
                        (catch Exception _ {}))
                   {})]
    (io/make-parents file)
    (spit file (pr-str (assoc settings :global-player player)))
    player))

(defn- json-request
  [method url body]
  (let [builder (-> (HttpRequest/newBuilder (URI/create url))
                    (.timeout (Duration/ofMillis timeout-ms))
                    (.header "accept" "application/json"))]
    (case method
      :get (-> builder .GET .build)
      :post (-> builder
                (.header "content-type" "application/json")
                (.POST (HttpRequest$BodyPublishers/ofString (json/write-str body)))
                .build))))

(defn- parse-json
  [body]
  (json/read-str body :key-fn keyword))

(defn- get-json
  [url]
  (let [response (.send @client (json-request :get url nil)
                        (HttpResponse$BodyHandlers/ofString))
        status (.statusCode response)]
    (when (<= 200 status 299)
      (parse-json (.body response)))))

(defn- post-json
  [url body]
  (let [response (.send @client (json-request :post url body)
                        (HttpResponse$BodyHandlers/ofString))
        status (.statusCode response)]
    (when (<= 200 status 299)
      (parse-json (.body response)))))

(defn fetch-leaderboard!
  [global-state]
  (when (:enabled? @global-state)
    (swap! global-state assoc :status :loading :error nil)
    (future
      (try
        (let [{:keys [url configured-name]} @global-state
              payload (get-json (str url "/leaderboard?limit=10"))
              ready (global/normalize-response payload url configured-name)]
          (swap! global-state merge ready
                 {:last-updated-ms (System/currentTimeMillis)}))
        (catch Exception e
          (swap! global-state assoc
                 :status :failed
                 :error (or (.getMessage e) "request failed")))))))

(defn ensure-player!
  [settings-path global-state display-name]
  (or (load-player settings-path)
      (let [{:keys [url configured-name]} @global-state
            payload (post-json (str url "/players")
                               {:display_name (or display-name configured-name)})
            player {:player-id (:player_id payload)
                    :player-token (:player_token payload)
                    :public-code (:public_code payload)
                    :display-name (:display_name payload)}]
        (save-player! settings-path player))))

(defn submit-score!
  [settings-path global-state state initials display-name]
  (cond
    (not (:enabled? @global-state))
    (swap! global-state assoc :submit-status :skipped_disabled)

    (not (:read-succeeded? @global-state))
    (swap! global-state assoc :submit-status :skipped_no_read)

    :else
    (do
      (swap! global-state assoc :submit-status :pending)
      (future
        (try
          (let [player (ensure-player! settings-path global-state
                                       (or (:player-name @global-state)
                                           display-name
                                           initials))
                payload {:player_id (:player-id player)
                         :player_token (:player-token player)
                         :run_id (str (UUID/randomUUID))
                         :initials initials
                         :score (long (or (core/final-score state)
                                          (core/pending-high-score state)
                                          0))
                         :wave (long (or (core/wave state) 1))
                         :duration_ms (long (* 1000.0 (double (or (core/sim-time state) 0.0))))
                         :game_version "dev"
                         :host "desktop"}
                response (post-json (str (:url @global-state) "/scores") payload)]
            (swap! global-state assoc
                   :submit-status (if (:accepted response) :accepted :failed)))
          (catch Exception e
            (swap! global-state assoc
                   :submit-status :failed
                   :error (or (.getMessage e) "submit failed"))))))))
