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

(defn- refresh-leaderboard-body!
  [global-state transport now-ms]
  (let [{:keys [url configured-name] :as current} @global-state
        payload ((:fetch-leaderboard transport) current)]
    (swap! global-state merge
           (assoc (global/leaderboard-ready-state payload url configured-name (now-ms))
                  :submit-status (:submit-status current)))))

(defn fetch-leaderboard!
  ([global-state]
   (fetch-leaderboard! global-state (http-client nil global-state) #(System/currentTimeMillis)))
  ([global-state transport now-ms]
   (when (:enabled? @global-state)
     (swap! global-state assoc :status :loading :error nil)
     (future
       (try
         (refresh-leaderboard-body! global-state transport now-ms)
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
            response ((:submit-score transport) player payload)
            submit-status (global/submit-status-from-response response)]
        (swap! global-state assoc
               :submit-status submit-status)
        (when (= :accepted submit-status)
          (swap! global-state assoc :status :loading :error nil)
          (try
            (refresh-leaderboard-body! global-state transport #(System/currentTimeMillis))
            (catch Exception e
              (swap! global-state assoc
                     :status :failed
                     :error (or (.getMessage e) "refresh failed"))))))
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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-27T13:34:19.134506-05:00", :module-hash "441163661", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 10, :hash "307913632"} {:id "def/timeout-ms", :kind "def", :line 12, :end-line 12, :hash "-1499539124"} {:id "form/2/defonce", :kind "defonce", :line 14, :end-line 17, :hash "-491269976"} {:id "defn/initial-state", :kind "defn", :line 19, :end-line 30, :hash "-1811902500"} {:id "defn-/settings-file", :kind "defn-", :line 32, :end-line 34, :hash "-2064380865"} {:id "defn/load-player", :kind "defn", :line 36, :end-line 43, :hash "615842040"} {:id "defn/save-player!", :kind "defn", :line 45, :end-line 54, :hash "-381688520"} {:id "defn-/json-request", :kind "defn-", :line 56, :end-line 66, :hash "1081292317"} {:id "defn-/parse-json", :kind "defn-", :line 68, :end-line 70, :hash "-1202903860"} {:id "defn-/get-json", :kind "defn-", :line 72, :end-line 78, :hash "-849957245"} {:id "defn-/post-json", :kind "defn-", :line 80, :end-line 86, :hash "536275538"} {:id "defn-/player-from-response", :kind "defn-", :line 88, :end-line 93, :hash "-1325201342"} {:id "defn-/http-client", :kind "defn-", :line 95, :end-line 109, :hash "178833973"} {:id "defn/fetch-leaderboard!", :kind "defn", :line 111, :end-line 126, :hash "-807634602"} {:id "defn/ensure-player!", :kind "defn", :line 128, :end-line 132, :hash "1707057468"} {:id "defn-/submit-score-body", :kind "defn-", :line 134, :end-line 149, :hash "-443880132"} {:id "defn/submit-score!", :kind "defn", :line 151, :end-line 163, :hash "-1035113856"}]}
;; clj-mutate-manifest-end
