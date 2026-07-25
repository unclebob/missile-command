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

(defn set-threshold
  "Test/setup helper: configure the score interval for bonus city awards."
  [state n]
  (assoc state :bonus-city-threshold (long n)))

(defn set-reserve
  "Test/setup helper: set bonus cities currently held in reserve."
  [state n]
  (assoc state :bonus-cities (long n)))

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
