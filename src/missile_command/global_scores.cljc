(ns missile-command.global-scores
  "Host-owned global leaderboard metadata. No HTTP here.")

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
