(ns missile-command.jvm.global-scores
  "Asynchronous Cloudflare leaderboard client for the JVM host."
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
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

(defn- player-from-response
  [payload]
  {:player-id (:player_id payload)
   :player-token (:player_token payload)
   :public-code (:public_code payload)
   :display-name (:display_name payload)})

(defn- http-client
  [settings-path global-state]
  {:fetch-leaderboard (fn [global-state]
                        (get-json (str (:url global-state) "/leaderboard?limit=10")))
   :ensure-player (fn [display-name]
                    (or (load-player settings-path)
                        (let [{:keys [url configured-name]} @global-state
                              payload (post-json (str url "/players")
                                                 (global/player-create-payload
                                                  display-name
                                                  configured-name))
                              player (player-from-response payload)]
                          (save-player! settings-path player))))
   :submit-score (fn [_player payload]
                   (post-json (str (:url @global-state) "/scores") payload))})

(defn fetch-leaderboard!
  ([global-state]
   (fetch-leaderboard! global-state (http-client nil global-state) #(System/currentTimeMillis)))
  ([global-state transport now-ms]
   (when (:enabled? @global-state)
     (swap! global-state assoc :status :loading :error nil)
     (future
       (try
         (let [{:keys [url configured-name] :as current} @global-state
               payload ((:fetch-leaderboard transport) current)]
           (swap! global-state merge
                  (global/leaderboard-ready-state payload url configured-name (now-ms))))
         (catch Exception e
           (swap! global-state assoc
                  :status :failed
                  :error (or (.getMessage e) "request failed"))))))))

(defn ensure-player!
  ([settings-path global-state display-name]
   (ensure-player! settings-path global-state display-name (http-client settings-path global-state)))
  ([settings-path global-state display-name transport]
   ((:ensure-player transport) display-name)))

(defn- submit-score-body
  [settings-path global-state state initials display-name transport run-id game-version]
  (future
    (try
      (let [current @global-state
            selected-name (global/select-player-display-name current display-name initials)
            player (ensure-player! settings-path global-state selected-name transport)
            payload (global/score-submit-payload state player initials "desktop"
                                                 (run-id) game-version)
            response ((:submit-score transport) player payload)]
        (swap! global-state assoc
               :submit-status (global/submit-status-from-response response)))
      (catch Exception e
        (swap! global-state assoc
               :submit-status :failed
               :error (or (.getMessage e) "submit failed"))))))

(defn submit-score!
  ([settings-path global-state state initials display-name]
   (submit-score! settings-path global-state state initials display-name
                  (http-client settings-path global-state)
                  #(str (UUID/randomUUID))
                  "dev"))
  ([settings-path global-state state initials display-name transport run-id game-version]
   (if-let [status (global/submit-skip-status @global-state)]
     (swap! global-state assoc :submit-status status)
     (do
       (swap! global-state assoc :submit-status :pending)
       (submit-score-body settings-path global-state state initials display-name
                          transport run-id game-version)))))
