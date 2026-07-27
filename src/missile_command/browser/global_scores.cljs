(ns missile-command.browser.global-scores
  "Asynchronous Cloudflare leaderboard client for the browser host."
  (:require [missile-command.global-scores :as global]))

(def player-storage-key "missile-command-global-player")
(def timeout-ms 1200)

(defn initial-state
  []
  (let [cfg (or (.-MISSILE_COMMAND_LEADERBOARD js/window) #js {})]
    (assoc global/empty-state
           :enabled? (not (true? (.-disabled cfg)))
           :url (or (.-url cfg) global/default-url)
           :configured-name (or (.-name cfg) global/default-name)
           :player-name (.-playerName cfg))))

(defn- read-player
  []
  (try
    (when (exists? js/localStorage)
      (some-> (.getItem js/localStorage player-storage-key)
              js/JSON.parse
              (js->clj :keywordize-keys true)))
    (catch :default _ nil)))

(defn- save-player!
  [player]
  (when (exists? js/localStorage)
    (.setItem js/localStorage player-storage-key
              (js/JSON.stringify (clj->js player))))
  player)

(defn- timeout-signal
  []
  (let [controller (js/AbortController.)
        id (js/setTimeout #(.abort controller) timeout-ms)]
    {:signal (.-signal controller)
     :clear #(js/clearTimeout id)}))

(defn- fetch-json!
  ([url]
   (fetch-json! url nil))
  ([url opts]
   (let [{:keys [signal clear]} (timeout-signal)]
     (-> (js/fetch url (clj->js (assoc (or opts {}) :signal signal)))
         (.then (fn [response]
                  (clear)
                  (if (.-ok response)
                    (.json response)
                    (throw (js/Error. (str "http " (.-status response)))))))
         (.then #(js->clj % :keywordize-keys true))
         (.catch (fn [error]
                   (clear)
                   (throw error)))))))

(defn- player-from-response
  [payload]
  {:player-id (:player_id payload)
   :player-token (:player_token payload)
   :public-code (:public_code payload)
   :display-name (:display_name payload)})

(defn- http-client
  [global-state]
  {:fetch-leaderboard (fn [global-state]
                        (fetch-json! (str (:url global-state) "/leaderboard?limit=10")))
   :ensure-player (fn [display-name]
                    (if-let [player (read-player)]
                      (js/Promise.resolve player)
                      (let [{:keys [url configured-name]} @global-state]
                        (-> (fetch-json! (str url "/players")
                                         {:method "POST"
                                          :headers {"content-type" "application/json"}
                                          :body (js/JSON.stringify
                                                 (clj->js
                                                  (global/player-create-payload
                                                   display-name
                                                   configured-name)))})
                            (.then (fn [payload]
                                     (save-player! (player-from-response payload))))))))
   :submit-score (fn [_player payload]
                   (fetch-json! (str (:url @global-state) "/scores")
                                {:method "POST"
                                 :headers {"content-type" "application/json"}
                                 :body (js/JSON.stringify (clj->js payload))}))})

(defn fetch-leaderboard!
  ([global-state]
   (fetch-leaderboard! global-state (http-client global-state) #(.now js/Date)))
  ([global-state transport now-ms]
   (when (:enabled? @global-state)
     (swap! global-state assoc :status :loading :error nil)
     (let [{:keys [url configured-name] :as current} @global-state]
       (-> (js/Promise.resolve ((:fetch-leaderboard transport) current))
           (.then (fn [payload]
                    (swap! global-state merge
                           (global/leaderboard-ready-state payload url configured-name (now-ms)))))
           (.catch (fn [error]
                     (swap! global-state assoc
                            :status :failed
                            :error (.-message error)))))))))

(defn ensure-player!
  ([global-state display-name]
   (ensure-player! global-state display-name (http-client global-state)))
  ([_global-state display-name transport]
   ((:ensure-player transport) display-name)))

(defn submit-score!
  ([global-state state initials display-name]
   (submit-score! global-state state initials display-name
                  (http-client global-state)
                  #(str (random-uuid))
                  "dev"))
  ([global-state state initials display-name transport run-id game-version]
   (cond
     (not (:enabled? @global-state))
     (swap! global-state assoc :submit-status :skipped_disabled)

     (not (:read-succeeded? @global-state))
     (swap! global-state assoc :submit-status :skipped_no_read)

     :else
     (do
       (swap! global-state assoc :submit-status :pending)
       (let [current @global-state
             selected-name (global/select-player-display-name current display-name initials)]
         (-> (js/Promise.resolve (ensure-player! global-state selected-name transport))
             (.then
              (fn [player]
                (js/Promise.resolve
                 ((:submit-score transport)
                  player
                  (global/score-submit-payload state player initials "browser"
                                               (run-id) game-version)))))
             (.then (fn [payload]
                      (swap! global-state assoc
                             :submit-status (global/submit-status-from-response payload))))
             (.catch (fn [error]
                       (swap! global-state assoc
                              :submit-status :failed
                              :error (.-message error))))))))))
