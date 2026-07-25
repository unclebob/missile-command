(ns missile-command.combat
  "Combat simulation: defensive missiles, fireballs, enemies, MIRVs, flyers.
  Pure state transforms — no host I/O."
  (:require [missile-command.batteries :as batteries]
            [missile-command.bonus-cities :as bc]
            [missile-command.cities :as cities]
            [missile-command.flyers :as flyers]
            [missile-command.missiles :as missiles]
            [missile-command.options :as options]
            [missile-command.scoring :as scoring]
            [missile-command.sfx :as sfx]
            [missile-command.waves :as waves]))

(def enemy-kind-ballistic :ballistic)
(def enemy-kind-mirv :mirv)
(def enemy-kind-mirv-child :mirv-child)
(def enemy-kind-smart :smart)

;; Edge band for smart-bomb evasion: outer ring of the blast (ratio of radius).
(def smart-bomb-edge-inner-factor 0.45)
(def ^:private smart-not-yet-evaded false)
(def smart-bomb-evade-clearance 12.0)

(defn- defensive-missiles [state]
  (or (:defensive-missiles state) []))

(defn- fireballs [state]
  (or (:fireballs state) []))

(defn- enemy-missiles [state]
  (or (:enemy-missiles state) []))

(defn- flyers-of [state]
  (or (:flyers state) []))

(defn allocate-entity-id
  "Return [id state'] with a fresh entity id reserved on state."
  [state]
  (let [id (long (or (:next-entity-id state) 0))]
    [id (assoc state :next-entity-id (inc id))]))

(defn- wave [state]
  (long (or (:wave state) waves/initial-wave)))

(defn- multiplier [state]
  (long (or (:multiplier state)
            (waves/multiplier (wave state)))))

(defn- difficulty [state]
  (options/difficulty (options/of state)))

(defn- enemy-speed-for-state
  [state]
  (:enemy-speed (waves/schedule-metrics (wave state) (difficulty state))))

(defn- city [state city-id]
  (cities/by-id (:cities state) city-id))

(defn- battery [state battery-id]
  (first (filter #(= battery-id (:id %)) (or (:batteries state) []))))

(defn- update-city
  [state city-id f]
  (update state :cities #(cities/update-city % city-id f)))

(defn- update-battery
  [state battery-id f]
  (update state :batteries #(batteries/update-battery % battery-id f)))

(defn- destroy-city
  [state city-id]
  (let [c (city state city-id)]
    (sfx/maybe-emit (update-city state city-id cities/destroy)
                    (and c (:alive? c))
                    :sfx/city-destroyed)))

(defn- destroy-battery
  [state battery-id]
  (let [bat (battery state battery-id)]
    (sfx/maybe-emit (update-battery state battery-id batteries/destroy)
                    (and bat (not (:destroyed? bat)))
                    :sfx/battery-destroyed)))

(defn- add-score
  [state points]
  (-> state
      (update :score (fnil + 0) (long points))
      bc/sync-from-score))

(defn spawn-fireball-at
  "Allocate and attach an expanding fireball centered at x,y.
  Emits :sfx/boom at onset (defensive intercept or ground impact)."
  [state x y]
  (let [[fid state] (allocate-entity-id state)
        fireball (missiles/make-fireball fid x y)]
    (-> state
        (update :fireballs (fnil conj []) fireball)
        (sfx/emit :sfx/boom))))

(defn- spawn-fireball-from-missile
  [state missile]
  (spawn-fireball-at state (:x1 missile) (:y1 missile)))

(defn tick-defensive
  "Advance defensive missiles; arrived missiles become fireballs."
  [state dt]
  (reduce (fn [s missile]
            (let [result (missiles/advance-defensive missile dt)]
              (if (missiles/arrived? result)
                (spawn-fireball-from-missile s missile)
                (update s :defensive-missiles (fnil conj []) result))))
          (assoc state :defensive-missiles [])
          (defensive-missiles state)))

(defn tick-fireballs
  "Advance fireballs; drop expired ones."
  [state dt]
  (reduce (fn [s fireball]
            (let [result (missiles/advance-fireball fireball dt)]
              (if (= missiles/expired result)
                s
                (update s :fireballs (fnil conj []) result))))
          (assoc state :fireballs [])
          (fireballs state)))

(defn- target-hit-by-fireball?
  [target fbs]
  (some #(missiles/point-in-fireball? % (:x target) (:y target)) fbs))

(defn destroy-targets-in-fireballs
  "Mark destroyable targets hit by any live fireball."
  [state]
  (let [fbs (fireballs state)]
    (update state :destroyable-targets
            (fn [targets]
              (mapv (fn [target]
                      (if (or (:destroyed? target)
                              (target-hit-by-fireball? target fbs))
                        (assoc target :destroyed? true)
                        target))
                    (or targets []))))))

(defn tick-defensive-phase
  "Defensive missiles + fireballs + destroyable targets for one dt step."
  [state dt]
  (-> state
      (tick-defensive dt)
      (tick-fireballs dt)
      destroy-targets-in-fireballs))

(defn mirv-parent?
  [enemy]
  (= enemy-kind-mirv (:enemy-kind enemy)))

(defn mirv-child?
  [enemy]
  (= enemy-kind-mirv-child (:enemy-kind enemy)))

(defn smart-bomb?
  [enemy]
  (= enemy-kind-smart (:enemy-kind enemy)))

(defn mirv-parents
  [state]
  (filterv mirv-parent? (enemy-missiles state)))

(defn mirv-children
  [state]
  (filterv mirv-child? (enemy-missiles state)))

(defn smart-bombs
  [state]
  (filterv smart-bomb? (enemy-missiles state)))

(defn spawn-enemy-at
  "Spawn an enemy missile from origin toward a target point.
  Optional attrs merge onto the missile (e.g. MIRV fields)."
  ([state origin target target-kind target-id]
   (spawn-enemy-at state origin target target-kind target-id nil))
  ([state origin target target-kind target-id attrs]
   (let [[mid state] (allocate-entity-id state)
         missile (merge (missiles/make-enemy mid origin target
                                             (enemy-speed-for-state state)
                                             target-kind target-id)
                        {:enemy-kind enemy-kind-ballistic}
                        attrs)]
     (-> state
         (update :enemy-missiles (fnil conj []) missile)
         (assoc :wave-had-enemies? true
                :wave-complete? false)))))

(defn- impact-target
  [state enemy]
  (case (:target-kind enemy)
    :city (destroy-city state (:target-id enemy))
    :battery (destroy-battery state (:target-id enemy))
    state))

(defn- enemy-hit-by-fireball?
  [enemy fireballs]
  (some #(missiles/point-in-fireball? % (:x enemy) (:y enemy)) fireballs))

(defn- distance-to-fireball
  [enemy fireball]
  (let [dx (- (double (:x enemy)) (double (:x fireball)))
        dy (- (double (:y enemy)) (double (:y fireball)))]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

(defn- first-touching-fireball
  [enemy fireballs]
  (first (filter #(missiles/point-in-fireball? % (:x enemy) (:y enemy)) fireballs)))

(defn smart-bomb-edge-band?
  "True when distance is outside the lethal core but still inside the blast."
  [d radius]
  (let [edge-inner (* (double radius) smart-bomb-edge-inner-factor)]
    (and (> d edge-inner) (<= d (double radius)))))

(defn- enemy-attrs-to-preserve
  [enemy]
  (select-keys enemy [:enemy-kind :child-count :split-progress
                      :smart-evaded? :last-enemy-fate-local]))

(defn- evade-smart-bomb
  [enemy fireball]
  (let [fx (double (:x fireball))
        fy (double (:y fireball))
        r (double (:radius fireball))
        ex (double (:x enemy))
        ey (double (:y enemy))
        dx (- ex fx)
        dy (- ey fy)
        dist (max 1.0e-6 (Math/sqrt (+ (* dx dx) (* dy dy))))
        clear (+ r smart-bomb-evade-clearance)
        nx (+ fx (* dx (/ clear dist)))
        ny (+ fy (* dy (/ clear dist)))
        retargeted (missiles/make-enemy (:id enemy)
                                        {:x nx :y ny}
                                        {:x (:x1 enemy) :y (:y1 enemy)}
                                        (:speed enemy)
                                        (:target-kind enemy)
                                        (:target-id enemy))]
    (merge retargeted
           (enemy-attrs-to-preserve enemy)
           {:enemy-kind enemy-kind-smart
            :smart-evaded? true
            :smart-evaded-fireball-id (:id fireball)})))

(defn- destroy-enemy-by-fireball
  [state enemy]
  (-> state
      (add-score (scoring/enemy-kill-points
                  (if (smart-bomb? enemy) :smart :ballistic)
                  (multiplier state)))
      (assoc :last-enemy-fate :fireball)
      (sfx/emit :sfx/intercepted)))

(defn- spawn-impact-fireball
  [state enemy]
  (spawn-fireball-at state (:x1 enemy) (:y1 enemy)))

(defn- resolve-enemy-impact
  [state enemy]
  (-> state
      (impact-target enemy)
      (spawn-impact-fireball enemy)
      (assoc :last-enemy-fate :impact)))

(defn- keep-flying-enemy
  [state enemy]
  (update state :enemy-missiles (fnil conj []) enemy))

(defn- resolve-fireball-contact
  [state enemy fireballs]
  (if-let [fb (first-touching-fireball enemy fireballs)]
    (if (and (smart-bomb? enemy)
             (not (:smart-evaded? enemy))
             (smart-bomb-edge-band? (distance-to-fireball enemy fb)
                                    (:radius fb)))
      (keep-flying-enemy state (evade-smart-bomb enemy fb))
      (destroy-enemy-by-fireball state enemy))
    (keep-flying-enemy state enemy)))

(defn- progress-of
  [enemy-or-result]
  (if (missiles/arrived? enemy-or-result)
    1.0
    (double (:progress enemy-or-result 0.0))))

(defn- index-of-id
  [xs x]
  (first (keep-indexed (fn [i v] (when (= v x) i)) xs)))

(defn- mirv-child-target-ids
  [state preferred-city-id n]
  (let [living (mapv :id (cities/living (:cities state)))]
    (if (seq living)
      (let [idx (or (index-of-id living preferred-city-id) 0)
            ordered (vec (concat (subvec living idx) (subvec living 0 idx)))]
        (vec (take n (cycle ordered))))
      [])))

(defn- split-mirv-parent
  [state parent]
  (let [split-p (double (:split-progress parent 0.5))
        at (missiles/position-at-progress parent split-p)
        n (long (:child-count parent 0))
        targets (mirv-child-target-ids state (:target-id parent) n)
        origin {:x (:x at) :y (:y at)}]
    (reduce (fn [s city-id]
              (let [c (city s city-id)]
                (if c
                  (spawn-enemy-at s origin {:x (:x c) :y (:y c)} :city city-id
                                  {:enemy-kind enemy-kind-mirv-child})
                  s)))
            state
            targets)))

(defn- should-split-mirv?
  [enemy result]
  (and (mirv-parent? enemy)
       (>= (progress-of result)
           (double (:split-progress enemy 1.0)))))

(defn- resolve-advanced-enemy
  [state enemy result fireballs]
  (cond
    (should-split-mirv? enemy result)
    (split-mirv-parent state enemy)

    (missiles/arrived? result)
    (resolve-enemy-impact state enemy)

    (enemy-hit-by-fireball? result fireballs)
    (resolve-fireball-contact state result fireballs)

    :else
    (keep-flying-enemy state result)))

(defn- tick-one-enemy
  [state enemy dt fireballs]
  (if (enemy-hit-by-fireball? enemy fireballs)
    (resolve-fireball-contact state enemy fireballs)
    (resolve-advanced-enemy state enemy
                            (missiles/advance-enemy enemy dt)
                            fireballs)))

(defn tick-enemies
  "Advance all enemy missiles for one dt step."
  [state dt]
  (let [fbs (fireballs state)]
    (reduce (fn [s enemy]
              (tick-one-enemy s enemy dt fbs))
            (assoc state :enemy-missiles [])
            (enemy-missiles state))))

(defn- destroy-flyer-by-fireball
  [state]
  (-> state
      (add-score (scoring/flyer-kill-points (multiplier state)))
      (assoc :last-enemy-fate :fireball
             :last-flyer-fate :fireball)
      (sfx/emit :sfx/intercepted)))

(defn- apply-flyer-drops
  [state flyer]
  (let [drops (flyers/pending-drops flyer (:progress flyer))]
    (if (seq drops)
      (let [state (reduce
                   (fn [s drop]
                     (let [at (flyers/position-at flyer (:at-progress drop))
                           [kind id] (:target drop)
                           c (when (= kind :city) (city s id))]
                       (if (and (= kind :city) c)
                         (spawn-enemy-at s at {:x (:x c) :y (:y c)} :city id
                                         {:dropped-from-flyer? true})
                         s)))
                   state
                   drops)
            flyer' (update flyer :drops-fired
                           (fnil into #{}) (map :id drops))]
        [state flyer'])
      [state flyer])))

(defn- keep-flying-flyer
  [state flyer]
  (update state :flyers (fnil conj []) flyer))

(defn- tick-one-flyer
  [state flyer dt fireballs]
  (if (flyers/hit-by-fireball? flyer fireballs)
    (destroy-flyer-by-fireball state)
    (let [result (flyers/advance flyer dt)]
      (cond
        (= :left result)
        state

        (flyers/hit-by-fireball? result fireballs)
        (destroy-flyer-by-fireball state)

        :else
        (let [[s flyer'] (apply-flyer-drops state result)]
          (keep-flying-flyer s flyer'))))))

(defn tick-flyers
  "Advance all flyers for one dt step."
  [state dt]
  (let [fbs (fireballs state)]
    (reduce (fn [s flyer]
              (tick-one-flyer s flyer dt fbs))
            (assoc state :flyers [])
            (flyers-of state))))

(defn tick-playing-combat
  "Full combat phase for a playing tick (defensive + enemies + flyers)."
  [state dt]
  (-> state
      (tick-defensive-phase dt)
      (tick-enemies dt)
      (tick-flyers dt)))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-25T12:07:26.554141-05:00", :module-hash "676706302", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 12, :hash "-529205955"} {:id "def/enemy-kind-ballistic", :kind "def", :line 14, :end-line 14, :hash "844796837"} {:id "def/enemy-kind-mirv", :kind "def", :line 15, :end-line 15, :hash "-903061239"} {:id "def/enemy-kind-mirv-child", :kind "def", :line 16, :end-line 16, :hash "53130372"} {:id "def/enemy-kind-smart", :kind "def", :line 17, :end-line 17, :hash "246438905"} {:id "def/smart-bomb-edge-inner-factor", :kind "def", :line 20, :end-line 20, :hash "1336091741"} {:id "def/smart-not-yet-evaded", :kind "def", :line 21, :end-line 21, :hash "101087391"} {:id "def/smart-bomb-evade-clearance", :kind "def", :line 22, :end-line 22, :hash "-1155691061"} {:id "defn-/defensive-missiles", :kind "defn-", :line 24, :end-line 25, :hash "757290843"} {:id "defn-/fireballs", :kind "defn-", :line 27, :end-line 28, :hash "-1238649430"} {:id "defn-/enemy-missiles", :kind "defn-", :line 30, :end-line 31, :hash "2059725599"} {:id "defn-/flyers-of", :kind "defn-", :line 33, :end-line 34, :hash "1442821226"} {:id "defn/allocate-entity-id", :kind "defn", :line 36, :end-line 40, :hash "-686877707"} {:id "defn-/wave", :kind "defn-", :line 42, :end-line 43, :hash "1821923209"} {:id "defn-/multiplier", :kind "defn-", :line 45, :end-line 47, :hash "-703566906"} {:id "defn-/difficulty", :kind "defn-", :line 49, :end-line 50, :hash "1928195985"} {:id "defn-/enemy-speed-for-state", :kind "defn-", :line 52, :end-line 54, :hash "-1425169229"} {:id "defn-/city", :kind "defn-", :line 56, :end-line 57, :hash "-2029908419"} {:id "defn-/battery", :kind "defn-", :line 59, :end-line 60, :hash "-2097468789"} {:id "defn-/update-city", :kind "defn-", :line 62, :end-line 64, :hash "-568830589"} {:id "defn-/update-battery", :kind "defn-", :line 66, :end-line 68, :hash "-1221674523"} {:id "defn-/destroy-city", :kind "defn-", :line 70, :end-line 75, :hash "1022396857"} {:id "defn-/destroy-battery", :kind "defn-", :line 77, :end-line 82, :hash "1317023240"} {:id "defn-/add-score", :kind "defn-", :line 84, :end-line 88, :hash "-785943690"} {:id "defn/spawn-fireball-at", :kind "defn", :line 90, :end-line 98, :hash "-969725387"} {:id "defn-/spawn-fireball-from-missile", :kind "defn-", :line 100, :end-line 102, :hash "1839324960"} {:id "defn/tick-defensive", :kind "defn", :line 104, :end-line 113, :hash "128741151"} {:id "defn/tick-fireballs", :kind "defn", :line 115, :end-line 124, :hash "-1331599335"} {:id "defn-/target-hit-by-fireball?", :kind "defn-", :line 126, :end-line 128, :hash "2042783862"} {:id "defn/destroy-targets-in-fireballs", :kind "defn", :line 130, :end-line 141, :hash "-1400979185"} {:id "defn/tick-defensive-phase", :kind "defn", :line 143, :end-line 149, :hash "1446767887"} {:id "defn/mirv-parent?", :kind "defn", :line 151, :end-line 153, :hash "-1708498289"} {:id "defn/mirv-child?", :kind "defn", :line 155, :end-line 157, :hash "756554581"} {:id "defn/smart-bomb?", :kind "defn", :line 159, :end-line 161, :hash "-1164976151"} {:id "defn/mirv-parents", :kind "defn", :line 163, :end-line 165, :hash "-1734114206"} {:id "defn/mirv-children", :kind "defn", :line 167, :end-line 169, :hash "1045363527"} {:id "defn/smart-bombs", :kind "defn", :line 171, :end-line 173, :hash "1657617417"} {:id "defn/spawn-enemy-at", :kind "defn", :line 175, :end-line 190, :hash "467325811"} {:id "defn-/impact-target", :kind "defn-", :line 192, :end-line 197, :hash "-984684299"} {:id "defn-/enemy-hit-by-fireball?", :kind "defn-", :line 199, :end-line 201, :hash "-583076826"} {:id "defn-/distance-to-fireball", :kind "defn-", :line 203, :end-line 207, :hash "-1214701948"} {:id "defn-/first-touching-fireball", :kind "defn-", :line 209, :end-line 211, :hash "949967404"} {:id "defn/smart-bomb-edge-band?", :kind "defn", :line 213, :end-line 217, :hash "506603366"} {:id "defn-/enemy-attrs-to-preserve", :kind "defn-", :line 219, :end-line 222, :hash "-111533279"} {:id "defn-/evade-smart-bomb", :kind "defn-", :line 224, :end-line 247, :hash "-436214465"} {:id "defn-/destroy-enemy-by-fireball", :kind "defn-", :line 249, :end-line 256, :hash "-172702724"} {:id "defn-/spawn-impact-fireball", :kind "defn-", :line 258, :end-line 260, :hash "-676222870"} {:id "defn-/resolve-enemy-impact", :kind "defn-", :line 262, :end-line 267, :hash "1944987463"} {:id "defn-/keep-flying-enemy", :kind "defn-", :line 269, :end-line 271, :hash "-1439807545"} {:id "defn-/resolve-fireball-contact", :kind "defn-", :line 273, :end-line 282, :hash "182961607"} {:id "defn-/progress-of", :kind "defn-", :line 284, :end-line 288, :hash "1779378488"} {:id "defn-/index-of-id", :kind "defn-", :line 290, :end-line 292, :hash "-1471330163"} {:id "defn-/mirv-child-target-ids", :kind "defn-", :line 294, :end-line 301, :hash "-2000573154"} {:id "defn-/split-mirv-parent", :kind "defn-", :line 303, :end-line 317, :hash "1829539482"} {:id "defn-/should-split-mirv?", :kind "defn-", :line 319, :end-line 323, :hash "-1686921459"} {:id "defn-/resolve-advanced-enemy", :kind "defn-", :line 325, :end-line 338, :hash "-352236925"} {:id "defn-/tick-one-enemy", :kind "defn-", :line 340, :end-line 346, :hash "2100624266"} {:id "defn/tick-enemies", :kind "defn", :line 348, :end-line 355, :hash "-1600639743"} {:id "defn-/destroy-flyer-by-fireball", :kind "defn-", :line 357, :end-line 363, :hash "-1739016762"} {:id "defn-/apply-flyer-drops", :kind "defn-", :line 365, :end-line 383, :hash "1084438316"} {:id "defn-/keep-flying-flyer", :kind "defn-", :line 385, :end-line 387, :hash "1225384403"} {:id "defn-/tick-one-flyer", :kind "defn-", :line 389, :end-line 403, :hash "1709281062"} {:id "defn/tick-flyers", :kind "defn", :line 405, :end-line 412, :hash "-1420027545"} {:id "defn/tick-playing-combat", :kind "defn", :line 414, :end-line 420, :hash "1425781395"}]}
;; clj-mutate-manifest-end
