(ns missile-command.global-scores
  "Host-owned global leaderboard metadata and pure client payload logic. No HTTP here."
  (:require [clojure.string :as str]))

(def default-url
  "https://missile-command-leaderboard.unclebob-missile-command.workers.dev")

(def default-name "Official Missile Command")

(def page-millis 5000)

(def empty-state
  {:enabled? true
   :url default-url
   :configured-name default-name
   :status :idle
   :leaderboard nil
   :scores []
   :last-updated-ms nil
   :error nil
   :read-succeeded? false
   :submit-status :idle})

(defn host-label
  [url]
  (try
    #?(:clj (.getHost (java.net.URI/create (str url)))
       :cljs (.-host (js/URL. (str url))))
    (catch #?(:clj Exception :cljs :default) _
      nil)))

(defn display-name
  [global]
  (or (get-in global [:leaderboard :display_name])
      (get-in global [:leaderboard :display-name])
      (:configured-name global)
      (host-label (:url global))
      "Custom leaderboard"))

(defn normalize-entry
  [entry]
  {:rank (:rank entry)
   :public-code (or (:public_code entry) (:public-code entry))
   :display-name (or (:display_name entry) (:display-name entry))
   :initials (:initials entry)
   :score (:score entry)
   :wave (:wave entry)
   :created-at (or (:created_at entry) (:created-at entry))})

(defn entry-label
  [entry]
  (str (or (:display-name entry) (:initials entry) "PLAYER")))

(defn normalize-response
  [payload url configured-name]
  (let [leaderboard (:leaderboard payload)]
    (-> empty-state
        (assoc :url url
               :configured-name configured-name
               :status :ready
               :leaderboard leaderboard
               :scores (mapv normalize-entry (or (:scores payload) []))
               :read-succeeded? true
               :error nil))))

(defn leaderboard-ready-state
  "Normalize a leaderboard wire payload into state ready to merge into global-state."
  [payload url configured-name now-ms]
  (assoc (normalize-response payload url configured-name)
         :last-updated-ms now-ms))

(defn player-create-payload
  [display-name configured-name]
  {:display_name (or display-name configured-name)})

(defn select-player-display-name
  [global-state submitted-display-name initials]
  (or (:player-name global-state)
      submitted-display-name
      initials
      (:configured-name global-state)))

(defn normalized-initials
  [initials]
  (-> (str (or initials ""))
      str/trim
      str/upper-case))

(defn score-submit-payload
  [state player initials host run-id game-version]
  {:player_id (:player-id player)
   :player_token (:player-token player)
   :run_id run-id
   :initials (normalized-initials initials)
   :score (long (or (:final-score state)
                    (:pending-high-score state)
                    (:score state)
                    0))
   :wave (long (or (:wave state) 1))
   :duration_ms (long (* 1000.0 (double (or (:sim-time state) 0.0))))
   :game_version game-version
   :host host})

(defn submit-status-from-response
  [response]
  (if (:accepted response) :accepted :failed))

(defn submit-skip-status
  "Return the host-visible submit status when a score must not be sent."
  [global-state]
  (cond
    (not (:enabled? global-state)) :skipped_disabled
    (not (:read-succeeded? global-state)) :skipped_no_read
    :else nil))

(defn page
  [opened-ms now-ms]
  (if (even? (quot (max 0 (- (long now-ms) (long (or opened-ms now-ms))))
                   page-millis))
    :local
    :global))

(defn attach
  [state global opened-ms now-ms]
  (assoc state
         :global-high-scores global
         :high-score-page (if (:read-succeeded? global)
                            (page opened-ms now-ms)
                            :local)))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-27T13:34:05.058481-05:00", :module-hash "-916695345", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "1145262978"} {:id "def/default-url", :kind "def", :line 5, :end-line 6, :hash "-138762986"} {:id "def/default-name", :kind "def", :line 8, :end-line 8, :hash "-833691469"} {:id "def/page-millis", :kind "def", :line 10, :end-line 10, :hash "860768059"} {:id "def/empty-state", :kind "def", :line 12, :end-line 22, :hash "-2111862127"} {:id "defn/host-label", :kind "defn", :line 24, :end-line 30, :hash "-693128264"} {:id "defn/display-name", :kind "defn", :line 32, :end-line 38, :hash "-1752388465"} {:id "defn/normalize-entry", :kind "defn", :line 40, :end-line 48, :hash "-72411472"} {:id "defn/entry-label", :kind "defn", :line 50, :end-line 52, :hash "1193033982"} {:id "defn/normalize-response", :kind "defn", :line 54, :end-line 64, :hash "116010773"} {:id "defn/leaderboard-ready-state", :kind "defn", :line 66, :end-line 70, :hash "1923102578"} {:id "defn/player-create-payload", :kind "defn", :line 72, :end-line 74, :hash "-973637138"} {:id "defn/select-player-display-name", :kind "defn", :line 76, :end-line 81, :hash "-237006946"} {:id "defn/normalized-initials", :kind "defn", :line 83, :end-line 87, :hash "-1243285185"} {:id "defn/score-submit-payload", :kind "defn", :line 89, :end-line 102, :hash "-367596739"} {:id "defn/submit-status-from-response", :kind "defn", :line 104, :end-line 106, :hash "829547178"} {:id "defn/submit-skip-status", :kind "defn", :line 108, :end-line 114, :hash "-1118996571"} {:id "defn/page", :kind "defn", :line 116, :end-line 121, :hash "478368730"} {:id "defn/attach", :kind "defn", :line 123, :end-line 129, :hash "537966215"}]}
;; clj-mutate-manifest-end
