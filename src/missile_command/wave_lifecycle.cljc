(ns missile-command.wave-lifecycle
  "Between-wave rearm, combat clear, and end-of-wave attack completion."
  (:require [missile-command.batteries :as batteries]
            [missile-command.scoring :as scoring]
            [missile-command.sfx :as sfx]
            [missile-command.wave-banner :as wave-banner]
            [missile-command.wave-schedule :as wave-schedule]
            [missile-command.waves :as waves]))

(defn unused-defensive-missiles
  "Sum of remaining ammo on non-destroyed batteries (before rearm)."
  [batteries]
  (->> batteries
       (remove :destroyed?)
       (map #(long (or (:missiles %) 0)))
       (reduce + 0)))

(defn rearm-all-batteries
  "Restore every battery: clear destroyed and refill to full ammo."
  [state]
  (update state :batteries
          (fn [bs]
            (mapv #(batteries/restore % waves/full-ammo) (or bs [])))))

(defn clear-combat-entities
  "Remove in-flight combat visuals between waves."
  [state]
  (assoc state
         :defensive-missiles []
         :fireballs []
         :destroyable-targets []))

(defn start-next-wave
  "Leave banner, clear combat leftovers, reset attack index, rearm batteries."
  [state wave-starts-complete? wave-starts-with-enemies?]
  (-> state
      wave-banner/clear
      clear-combat-entities
      (assoc :wave-complete? wave-starts-complete?
             :wave-had-enemies? wave-starts-with-enemies?
             :wave-attack nil)
      rearm-all-batteries))

(defn set-wave
  "Jump to a wave number without auto-completing; clear sky and attack index."
  [state wave-number wave-starts-complete? wave-starts-with-enemies?]
  (assoc state
         :wave wave-number
         :wave-complete? wave-starts-complete?
         :wave-had-enemies? wave-starts-with-enemies?
         :wave-attack nil
         :enemy-missiles []
         :flyers []))

(defn complete-wave
  "Award wave-end bonuses, place reserve cities, advance wave, enter banner.
  apply-bonus-fn and add-score-fn are injected from core (state transformers)."
  [state {:keys [apply-bonus-fn add-score-fn living-city-count multiplier
                 wave-flag-on wave-starts-with-enemies?]}]
  (if-not (wave-schedule/wave-ready-to-complete? state)
    state
    (let [points (scoring/wave-end-points
                  (unused-defensive-missiles (:batteries state))
                  living-city-count
                  multiplier)
          state (-> state
                    (add-score-fn points)
                    apply-bonus-fn)
          bonus-city-added? (boolean (:bonus-city-for-banner? state))]
      (-> state
          (dissoc :bonus-city-for-banner?)
          (assoc :wave-complete? wave-flag-on
                 :wave-had-enemies? wave-starts-with-enemies?
                 :wave-attack nil)
          (update :wave (fnil inc waves/initial-wave))
          (#(wave-banner/enter % (:wave %) bonus-city-added?))
          (sfx/emit :sfx/wave)))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-25T10:11:49.104199-05:00", :module-hash "-2078711559", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 8, :hash "600755896"} {:id "defn/unused-defensive-missiles", :kind "defn", :line 10, :end-line 16, :hash "-1781286393"} {:id "defn/rearm-all-batteries", :kind "defn", :line 18, :end-line 23, :hash "-301696151"} {:id "defn/clear-combat-entities", :kind "defn", :line 25, :end-line 31, :hash "1667906381"} {:id "defn/start-next-wave", :kind "defn", :line 33, :end-line 42, :hash "767823261"} {:id "defn/set-wave", :kind "defn", :line 44, :end-line 53, :hash "-1363531434"} {:id "defn/complete-wave", :kind "defn", :line 55, :end-line 77, :hash "844750503"}]}
;; clj-mutate-manifest-end
