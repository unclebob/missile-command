(ns missile-command.browser.global-scores
  "Asynchronous Cloudflare leaderboard client for the browser host."
  (:require [missile-command.core :as core]
            [missile-command.global-scores :as global]))

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

(defn fetch-leaderboard!
  [global-state]
  (when (:enabled? @global-state)
    (swap! global-state assoc :status :loading :error nil)
    (let [{:keys [url configured-name]} @global-state]
      (-> (fetch-json! (str url "/leaderboard?limit=10"))
          (.then (fn [payload]
                   (swap! global-state merge
                          (global/normalize-response payload url configured-name)
                          {:last-updated-ms (.now js/Date)})))
          (.catch (fn [error]
                    (swap! global-state assoc
                           :status :failed
                           :error (.-message error))))))))

(defn ensure-player!
  [global-state display-name]
  (if-let [player (read-player)]
    (js/Promise.resolve player)
    (let [{:keys [url configured-name]} @global-state]
      (-> (fetch-json! (str url "/players")
                       {:method "POST"
                        :headers {"content-type" "application/json"}
                        :body (js/JSON.stringify
                               (clj->js {:display_name (or display-name configured-name)}))})
          (.then (fn [payload]
                   (save-player!
                    {:player-id (:player_id payload)
                     :player-token (:player_token payload)
                     :public-code (:public_code payload)
                     :display-name (:display_name payload)})))))))

(defn submit-score!
  [global-state state initials display-name]
  (cond
    (not (:enabled? @global-state))
    (swap! global-state assoc :submit-status :skipped_disabled)

    (not (:read-succeeded? @global-state))
    (swap! global-state assoc :submit-status :skipped_no_read)

    :else
      (do
        (swap! global-state assoc :submit-status :pending)
        (-> (ensure-player! global-state (or (:player-name @global-state)
                                             display-name
                                             initials))
            (.then
             (fn [player]
               (let [payload {:player_id (:player-id player)
                              :player_token (:player-token player)
                              :run_id (str (random-uuid))
                              :initials initials
                              :score (long (or (core/final-score state)
                                               (core/pending-high-score state)
                                               0))
                              :wave (long (or (core/wave state) 1))
                              :duration_ms (long (* 1000.0 (double (or (core/sim-time state) 0.0))))
                              :game_version "dev"
                              :host "browser"}]
                 (fetch-json! (str (:url @global-state) "/scores")
                              {:method "POST"
                               :headers {"content-type" "application/json"}
                               :body (js/JSON.stringify (clj->js payload))}))))
            (.then (fn [payload]
                     (swap! global-state assoc
                            :submit-status (if (:accepted payload) :accepted :failed))))
            (.catch (fn [error]
                      (swap! global-state assoc
                             :submit-status :failed
                             :error (.-message error))))))))
