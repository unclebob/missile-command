(ns missile-command.wave-schedule
  "Sequential wave attacks: ballistics each salvo; specials on the last."
  (:require [missile-command.waves :as waves]))

;; Defaults for scheduled advanced enemies (arcade-style mid-descent split).
(def default-mirv-child-count 3)
(def default-mirv-split-progress 0.5)
(def default-flyer-speed 100.0)
(def default-flyer-altitude-fraction 0.18)
(def default-flyer-drop-count 3)
;; First drop progress; later drops are staggered evenly toward this upper bound.
(def default-flyer-drop-progress-start 0.25)
(def default-flyer-drop-progress-end 0.75)

(defn- living-city-ids
  [living-cities]
  (mapv :id living-cities))

(defn- cycle-living-city-ids
  "Take n living city ids, cycling when fewer cities than n."
  [living-cities n]
  (let [ids (living-city-ids living-cities)]
    (if (seq ids)
      (vec (take (long n) (cycle ids)))
      [])))

(defn- flyer-drop-progresses
  "Stagger n drop times between start and end so bombs fall one after another."
  [n]
  (let [n (long n)
        lo default-flyer-drop-progress-start
        hi default-flyer-drop-progress-end]
    (if (<= n 1)
      [lo]
      (mapv (fn [i]
              (+ lo (* (- hi lo) (/ (double i) (double (dec n))))))
            (range n)))))

(defn- configure-last-flyer-drops
  "Attach staggered drop schedule to the most recently spawned flyer."
  [state living-cities drop-count]
  (update state :flyers
          (fn [fs]
            (if (seq fs)
              (let [fs (vec fs)
                    idx (dec (count fs))
                    ids (living-city-ids living-cities)
                    n (long drop-count)
                    targets (if (seq ids)
                              (vec (take n (cycle ids)))
                              [])
                    progresses (flyer-drop-progresses (count targets))
                    drops (mapv (fn [j city-id at]
                                  {:id j
                                   :at-progress (double at)
                                   :target [:city city-id]})
                                (range (count targets))
                                targets
                                progresses)]
                (assoc fs idx (assoc (nth fs idx)
                                     :drops drops
                                     :drops-fired #{})))
              (vec fs)))))

(defn- spawn-wave-mirvs
  "Spawn n MIRV parents toward living cities from random sky origins."
  [state n {:keys [living-cities city playfield-width spawn-enemy-at
                   enemy-kind-mirv]}]
  (let [city-ids (cycle-living-city-ids (living-cities state) n)
        width (playfield-width state)]
    (reduce (fn [s city-id]
              (let [c (city s city-id)]
                (if c
                  (spawn-enemy-at s
                                  {:x (waves/random-sky-origin-x width) :y 0}
                                  {:x (:x c) :y (:y c)}
                                  :city city-id
                                  {:enemy-kind enemy-kind-mirv
                                   :child-count default-mirv-child-count
                                   :split-progress default-mirv-split-progress})
                  s)))
            state
            city-ids)))

(defn- spawn-wave-smart-bombs
  "Spawn n smart bombs toward living cities."
  [state n {:keys [living-cities spawn-smart-bomb-targeting-city]}]
  (reduce (fn [s city-id]
            (spawn-smart-bomb-targeting-city s city-id))
          state
          (cycle-living-city-ids (living-cities state) n)))

(defn- spawn-wave-flyer
  "Spawn one bomber or satellite crossing the upper sky with city-bound drops."
  [state flyer-kind {:keys [living-cities playfield-width playfield-height
                            spawn-flyer]}]
  (let [w (double (playfield-width state))
        h (double (playfield-height state))
        y (* h default-flyer-altitude-fraction)]
    (-> state
        (spawn-flyer flyer-kind 0.0 y w y default-flyer-speed)
        (configure-last-flyer-drops (living-cities state)
                                    default-flyer-drop-count))))

(defn spawn-specials
  "MIRVs, smart bombs, and flyers for the current wave (final attack only)."
  [state hooks]
  (let [{:keys [wave]} hooks
        m (waves/schedule-metrics-for-state state (wave state))
        mirvs (long (:mirv-count m 0))
        smarts (long (:smart-bomb-count m 0))
        bombers (long (:bomber-count m 0))
        sats (long (:satellite-count m 0))]
    (cond-> state
      (pos? mirvs) (spawn-wave-mirvs mirvs hooks)
      (pos? smarts) (spawn-wave-smart-bombs smarts hooks)
      (pos? bombers) (spawn-wave-flyer :bomber hooks)
      (pos? sats) (spawn-wave-flyer :satellite hooks))))

(defn begin-attack
  "Begin attack k (1-based): ballistic salvo; specials only on the last attack."
  [state k hooks]
  (let [{:keys [wave set-wave-enemies-active]} hooks
        n (long waves/attacks-per-wave)
        k (long k)
        m (waves/schedule-metrics-for-state state (wave state))
        ballistic (long (:enemy-count m 0))
        state (-> state
                  (assoc :flyers []
                         :wave-attack k
                         :wave-complete? false)
                  (set-wave-enemies-active ballistic))
        state (if (= k n)
                (spawn-specials state hooks)
                state)]
    ;; Always mark the attack as started so a cleared (or empty) sky can
    ;; advance/complete even if no living targets remain.
    (assoc state
           :wave-attack k
           :wave-had-enemies? true)))

(defn activate
  "Start attack 1 of the current wave (ballistic salvo). Later attacks and
  final specials are started by the host/core advance loop."
  [state hooks]
  (begin-attack state 1 hooks))

(defn set-enemies-active
  "Replace in-flight enemies with n scheduled wave enemies.
  hooks: :living-cities :batteries-living :playfield-width
  :spawn-city-from :spawn-battery-from :wave-starts-complete?"
  [state n hooks]
  (let [{:keys [living-cities batteries-living playfield-width
                spawn-city-from spawn-battery-from wave-starts-complete?]} hooks
        active? (pos? n)
        state (assoc state
                     :enemy-missiles []
                     :wave-complete? (boolean wave-starts-complete?)
                     :wave-had-enemies? active?)
        pool (waves/target-pool (mapv :id (living-cities state))
                                (mapv :id (batteries-living state)))
        targets (waves/cycle-targets pool n)
        width (playfield-width state)
        spawn (fn [s origin-x target-spec]
                (let [[kind id] target-spec]
                  (case kind
                    :city (spawn-city-from s origin-x 0 id)
                    :battery (spawn-battery-from s origin-x 0 id)
                    s)))]
    (reduce (fn [s target-spec]
              (spawn s (waves/random-sky-origin-x width) target-spec))
            state
            targets)))

(defn sky-clear?
  [state]
  (and (empty? (or (:enemy-missiles state) []))
       (empty? (or (:flyers state) []))))

(defn current-attack
  "1-based salvo index within the wave, or nil if salvos have not started."
  [state]
  (when-let [a (:wave-attack state)]
    (long a)))

(defn attack-cleared?
  "Sky empty after this attack had enemies."
  [state]
  (boolean
   (and (:wave-had-enemies? state)
        (not (:wave-complete? state))
        (sky-clear? state))))

(defn wave-ready-to-complete?
  "Wave ends after the last sequential attack is cleared.
  Unset :wave-attack (tests) completes on first clear."
  [state]
  (boolean
   (and (attack-cleared? state)
        (let [a (current-attack state)
              n (long waves/attacks-per-wave)]
          (or (nil? a) (>= a n))))))

(defn maybe-advance-attack
  "When the current attack is cleared and more remain, start the next attack
  via begin-attack-fn (state k)."
  [state begin-attack-fn]
  (if-not (attack-cleared? state)
    state
    (let [a (current-attack state)
          n (long waves/attacks-per-wave)]
      (if (and a (< a n))
        (begin-attack-fn state (inc a))
        state))))
