(ns missile-command.wave-schedule
  "Activate full wave enemy schedules (ballistics, MIRVs, smart bombs, flyers)."
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
  "Spawn n MIRV parents toward living cities from distributed sky origins."
  [state n {:keys [living-cities city playfield-width spawn-enemy-at
                   enemy-kind-mirv]}]
  (let [city-ids (cycle-living-city-ids (living-cities state) n)
        width (playfield-width state)
        total (count city-ids)]
    (reduce (fn [s [i city-id]]
              (let [c (city s city-id)]
                (if c
                  (spawn-enemy-at s
                                  {:x (waves/sky-origin-x width i total) :y 0}
                                  {:x (:x c) :y (:y c)}
                                  :city city-id
                                  {:enemy-kind enemy-kind-mirv
                                   :child-count default-mirv-child-count
                                   :split-progress default-mirv-split-progress})
                  s)))
            state
            (map-indexed vector city-ids))))

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

(defn activate
  "Spawn the full current-wave schedule via host-supplied spawn hooks.
  `hooks` keys: :wave :living-cities :city :playfield-width :playfield-height
  :set-wave-enemies-active :spawn-enemy-at :spawn-smart-bomb-targeting-city
  :spawn-flyer :enemy-kind-mirv."
  [state hooks]
  (let [{:keys [wave set-wave-enemies-active]} hooks
        m (waves/schedule-metrics-for-state state (wave state))
        ballistic (long (:enemy-count m 0))
        mirvs (long (:mirv-count m 0))
        smarts (long (:smart-bomb-count m 0))
        bombers (long (:bomber-count m 0))
        sats (long (:satellite-count m 0))]
    (cond-> (-> state
                (assoc :flyers [])
                (set-wave-enemies-active ballistic))
      (pos? mirvs) (spawn-wave-mirvs mirvs hooks)
      (pos? smarts) (spawn-wave-smart-bombs smarts hooks)
      (pos? bombers) (spawn-wave-flyer :bomber hooks)
      (pos? sats) (spawn-wave-flyer :satellite hooks))))
