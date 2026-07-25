(ns missile-command.bonus-cities
  "Bonus city reserve: award mid-wave from score thresholds; place only at wave end."
  (:require [missile-command.cities :as cities]
            [missile-command.scoring :as scoring]
            [missile-command.sfx :as sfx]
            [missile-command.world :as world]))

(def initial-reserve 0)
(def initial-awarded 0)
(def initial-earned-events 0)

(defn- long-key
  [state k default]
  (long (or (get state k) default)))

(defn reserve
  "Bonus cities held in reserve (not yet placed on the playfield)."
  [state]
  (long-key state :bonus-cities initial-reserve))

(defn threshold
  [state]
  (long-key state :bonus-city-threshold scoring/default-bonus-city-threshold))

(defn earned-events
  [state]
  (long-key state :bonus-city-earned-events initial-earned-events))

(defn- assoc-long
  [state k n]
  (assoc state k (long n)))

(defn set-threshold
  "Test/setup helper: configure the score interval for bonus city awards."
  [state n]
  (assoc-long state :bonus-city-threshold n))

(defn set-reserve
  "Test/setup helper: set bonus cities currently held in reserve."
  [state n]
  (assoc-long state :bonus-cities n))

(defn- lowest-destroyed-city-id
  [cities]
  (->> cities
       (remove :alive?)
       (map :id)
       sort
       first))

(defn- living-count
  [cities]
  (count (filter :alive? cities)))

(defn- update-city
  [state city-id f]
  (update state :cities
          (fn [cs]
            (mapv (fn [c]
                    (if (= city-id (:id c)) (f c) c))
                  (or cs [])))))

(defn apply-from-reserve
  "Place reserve cities onto destroyed slots while living cities stay under max.
  Sets :bonus-city-for-banner? when any city is restored (for wave banner)."
  [state]
  (loop [s state
         placed? false]
    (let [cs (or (:cities s) [])
          id (when (and (pos? (reserve s))
                        (< (living-count cs) world/city-count))
               (lowest-destroyed-city-id cs))]
      (if id
        (recur (-> s
                   (update-city id cities/restore)
                   (update :bonus-cities dec))
               true)
        (if placed?
          (assoc s :bonus-city-for-banner? true)
          s)))))

(defn sync-from-score
  "Award reserve cities for newly crossed score thresholds (no mid-wave place)."
  [state]
  (let [score (long (or (:score state) 0))
        thr (threshold state)
        already (long-key state :bonus-cities-awarded initial-awarded)
        new-awards (scoring/new-bonus-city-awards score thr already)
        earned (scoring/thresholds-crossed score thr)]
    (if (pos? new-awards)
      (-> state
          (assoc :bonus-cities-awarded earned)
          (update :bonus-cities (fnil + initial-reserve) new-awards)
          (update :bonus-city-earned-events
                  (fnil + initial-earned-events) new-awards)
          (sfx/emit :sfx/bonus-city))
      state)))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-25T11:10:14.065444-05:00", :module-hash "-550446732", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 6, :hash "-521014101"} {:id "def/initial-reserve", :kind "def", :line 8, :end-line 8, :hash "-609448841"} {:id "def/initial-awarded", :kind "def", :line 9, :end-line 9, :hash "1709139305"} {:id "def/initial-earned-events", :kind "def", :line 10, :end-line 10, :hash "273985318"} {:id "defn-/long-key", :kind "defn-", :line 12, :end-line 14, :hash "-1645808299"} {:id "defn/reserve", :kind "defn", :line 16, :end-line 19, :hash "-1932761285"} {:id "defn/threshold", :kind "defn", :line 21, :end-line 23, :hash "-1051720"} {:id "defn/earned-events", :kind "defn", :line 25, :end-line 27, :hash "1399409350"} {:id "defn-/assoc-long", :kind "defn-", :line 29, :end-line 31, :hash "1236782351"} {:id "defn/set-threshold", :kind "defn", :line 33, :end-line 36, :hash "-1052881477"} {:id "defn/set-reserve", :kind "defn", :line 38, :end-line 41, :hash "-466599069"} {:id "defn-/lowest-destroyed-city-id", :kind "defn-", :line 43, :end-line 49, :hash "-1591641332"} {:id "defn-/living-count", :kind "defn-", :line 51, :end-line 53, :hash "-1846203363"} {:id "defn-/update-city", :kind "defn-", :line 55, :end-line 61, :hash "253645926"} {:id "defn/apply-from-reserve", :kind "defn", :line 63, :end-line 80, :hash "112891178"} {:id "defn/sync-from-score", :kind "defn", :line 82, :end-line 97, :hash "-493561009"}]}
;; clj-mutate-manifest-end
