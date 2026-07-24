(ns missile-command.core
  (:require [missile-command.batteries :as batteries]
            [missile-command.cities :as cities]
            [missile-command.input :as input]
            [missile-command.missiles :as missiles]
            [missile-command.waves :as waves]
            [missile-command.world :as world]))

(def initial-score 0)
(def initial-entity-id 0)
(def wave-flag-off false)
(def wave-flag-on true)
(def wave-starts-complete? wave-flag-off)
(def wave-starts-with-enemies? wave-flag-off)
(def clamp-lo 0)
(def default-crosshair {:x clamp-lo :y clamp-lo})
(def target-starts-destroyed? wave-flag-off)

(defn- clamp
  [n lo hi]
  (max lo (min hi n)))

(defn- clamp-point
  [width height x y]
  {:x (clamp x clamp-lo (dec width))
   :y (clamp y clamp-lo (dec height))})

(defn- center-crosshair
  [width height]
  (clamp-point width height (quot width 2) (quot height 2)))

(defn- reclamp-crosshair
  [state width height]
  (let [crosshair (or (:crosshair state) default-crosshair)]
    (clamp-point width height (:x crosshair) (:y crosshair))))

(defn- update-battery
  [state battery-id f]
  (update state :batteries #(batteries/update-battery % battery-id f)))

(defn- next-entity-id
  [state]
  (let [id (or (:next-entity-id state) initial-entity-id)]
    [id (assoc state :next-entity-id (inc id))]))

(defn- no-events
  [state]
  {:state state :events []})

(defn new-game
  "Create a new game state for the given playfield size."
  [{:keys [width height]}]
  (merge {:width width
          :height height
          :score initial-score
          :wave waves/initial-wave
          :wave-complete? wave-starts-complete?
          :wave-had-enemies? wave-starts-with-enemies?
          :crosshair (center-crosshair width height)
          :defensive-missiles []
          :fireballs []
          :enemy-missiles []
          :destroyable-targets []
          :sim-time 0.0
          :last-applied-dt 0.0
          :last-enemy-fate nil
          :next-entity-id initial-entity-id}
         (world/apply-layout width height)))

(defn resize
  "Reflow layout for a new playfield size, preserving entity progress fields."
  [state width height]
  (merge state
         {:width width
          :height height
          :crosshair (reclamp-crosshair state width height)}
         (world/apply-layout width height state)))

(defn playfield-width
  [state]
  (:width state))

(defn playfield-height
  [state]
  (:height state))

(defn cities
  [state]
  (:cities state))

(defn living-cities
  [state]
  (cities/living (cities state)))

(defn batteries
  [state]
  (:batteries state))

(defn battery
  [state id]
  (first (filter #(= id (:id %)) (batteries state))))

(defn on-ground?
  "True when the entity's y sits in the ground band for this playfield."
  [state entity]
  (world/in-ground-band? (:y entity) (playfield-height state)))

(defn city-on-ground?
  [state city]
  (on-ground? state city))

(defn crosshair
  [state]
  (:crosshair state))

(defn score
  [state]
  (:score state))

(defn wave
  [state]
  (or (:wave state) waves/initial-wave))

(defn wave-complete?
  [state]
  (boolean (:wave-complete? state)))

(defn hud
  "Minimal HUD projection for hosts and tests."
  [state]
  {:wave (wave state)
   :score (score state)})

(defn defensive-missiles
  [state]
  (or (:defensive-missiles state) []))

(defn fireballs
  [state]
  (or (:fireballs state) []))

(defn enemy-missiles
  [state]
  (or (:enemy-missiles state) []))

(defn destroyable-targets
  [state]
  (or (:destroyable-targets state) []))

(defn last-enemy-fate
  [state]
  (:last-enemy-fate state))

(defn city
  [state city-id]
  (cities/by-id (cities state) city-id))

(defn living-city?
  [state city-id]
  (boolean (:alive? (city state city-id))))

(defn sim-time
  [state]
  (double (or (:sim-time state) 0.0)))

(defn last-applied-dt
  [state]
  (double (or (:last-applied-dt state) 0.0)))

(defn max-fireball-radius
  [_]
  missiles/fireball-max-radius)

(defn set-battery-ammo
  "Test/setup helper: set remaining missiles for a battery."
  [state battery-id ammo]
  (update-battery state battery-id #(batteries/set-ammo % ammo)))

(defn destroy-battery
  "Test/setup helper: mark a battery destroyed."
  [state battery-id]
  (update-battery state battery-id batteries/destroy))

(defn add-destroyable-target
  "Test/setup helper: place a fireball-vulnerable stub at x,y."
  [state x y]
  (let [[id state] (next-entity-id state)
        target {:id id :x x :y y :destroyed? target-starts-destroyed?}]
    (update state :destroyable-targets (fnil conj []) target)))

(defn- update-city
  [state city-id f]
  (update state :cities #(cities/update-city % city-id f)))

(defn destroy-city
  [state city-id]
  (update-city state city-id cities/destroy))

(defn- enemy-speed-for-state
  [state]
  (waves/enemy-speed (wave state)))

(defn spawn-enemy-at
  "Spawn an enemy missile from origin toward a target point."
  [state origin target target-kind target-id]
  (let [[mid state] (next-entity-id state)
        missile (missiles/make-enemy mid origin target
                                     (enemy-speed-for-state state)
                                     target-kind target-id)]
    (-> state
        (update :enemy-missiles (fnil conj []) missile)
        (assoc :wave-had-enemies? wave-flag-on
               :wave-complete? wave-starts-complete?))))

(defn spawn-enemy-targeting-city-from
  "Spawn an enemy missile from an explicit sky origin toward a city."
  [state origin-x origin-y city-id]
  (let [c (city state city-id)]
    (when-not c
      (throw (ex-info (str "unknown city " city-id) {:city-id city-id})))
    (spawn-enemy-at state
                    {:x origin-x :y origin-y}
                    {:x (:x c) :y (:y c)}
                    :city city-id)))

(defn spawn-enemy-targeting-city
  "Spawn an enemy missile from the top of the sky toward a living city."
  [state city-id]
  (if-let [c (city state city-id)]
    (spawn-enemy-targeting-city-from state (:x c) 0 city-id)
    (throw (ex-info (str "unknown city " city-id) {:city-id city-id}))))

(defn spawn-enemy-targeting-battery-from
  "Spawn an enemy missile from an explicit sky origin toward a battery."
  [state origin-x origin-y battery-id]
  (let [b (battery state battery-id)]
    (when-not b
      (throw (ex-info (str "unknown battery " battery-id) {:battery-id battery-id})))
    (spawn-enemy-at state
                    {:x origin-x :y origin-y}
                    {:x (:x b) :y (:y b)}
                    :battery battery-id)))

(defn spawn-enemy-targeting-battery
  "Spawn an enemy missile from the top of the sky toward a battery."
  [state battery-id]
  (if-let [b (battery state battery-id)]
    (spawn-enemy-targeting-battery-from state (:x b) 0 battery-id)
    (throw (ex-info (str "unknown battery " battery-id) {:battery-id battery-id}))))
(defn spawn-enemies-targeting-distinct-cities
  "Spawn n enemy missiles each aimed at a different living city."
  [state n]
  (let [ids (mapv :id (take n (living-cities state)))]
    (reduce spawn-enemy-targeting-city state ids)))

(defn add-static-fireball
  "Test/setup helper: place a fixed-radius fireball."
  [state x y radius]
  (let [[fid state] (next-entity-id state)
        fb (missiles/make-static-fireball fid x y radius)]
    (update state :fireballs (fnil conj []) fb)))

(defn route-enemy-through-point
  "Retarget the first enemy so its path starts at the given point (e.g. fireball)."
  [state x y]
  (update state :enemy-missiles
          (fn [ms]
            (if (seq ms)
              (let [m (first ms)
                    retargeted (missiles/make-enemy (:id m)
                                                    {:x x :y y}
                                                    {:x (:x1 m) :y (:y1 m)}
                                                    (:speed m)
                                                    (:target-kind m)
                                                    (:target-id m))]
                (into [retargeted] (rest ms)))
              ms))))

(defn- impact-target
  [state enemy]
  (case (:target-kind enemy)
    :city (destroy-city state (:target-id enemy))
    :battery (destroy-battery state (:target-id enemy))
    state))

(defn- enemy-hit-by-fireball?
  [enemy fireballs]
  (some #(missiles/point-in-fireball? % (:x enemy) (:y enemy)) fireballs))

(defn- fire-battery
  [state battery-id]
  (let [bat (battery state battery-id)]
    (if-not (batteries/can-fire? bat)
      (no-events state)
      (let [[missile-id state] (next-entity-id state)
            missile (missiles/make-defensive missile-id battery-id bat
                                             (crosshair state))]
        {:state (-> state
                    (update :defensive-missiles (fnil conj []) missile)
                    (update-battery battery-id batteries/spend-ammo))
         :events [{:type :sfx/launch :battery battery-id}]}))))

(defn- aim
  [state x y]
  (no-events
   (assoc state :crosshair
          (clamp-point (playfield-width state)
                       (playfield-height state)
                       x y))))

(defn click-zone
  "Horizontal third for a playfield x coordinate: :left, :center, or :right."
  [width x]
  (input/click-zone width x))

(defn click-fallback-order
  "Battery preference order for a click zone, preferred first."
  [zone]
  (input/click-fallback-order zone))

(defn- click-fire
  "Aim at the click point, then fire preferred zone battery with adjacent fallback."
  [state x y]
  (let [aimed (:state (aim state x y))
        zone (input/click-zone (playfield-width aimed) (:x (crosshair aimed)))
        battery-id (input/first-preferred
                    zone
                    #(batteries/can-fire? (battery aimed %)))]
    (if battery-id
      (fire-battery aimed battery-id)
      (no-events aimed))))

(defn handle
  "Apply a player command. Returns {:state s :events [...]}."
  [state command]
  (case (:type command)
    :aim (aim state (:x command) (:y command))
    :fire (fire-battery state (:battery command))
    :click (click-fire state (:x command) (:y command))
    (throw (ex-info (str "unsupported command: " (:type command))
                    {:command command}))))

(defn- spawn-fireball-at
  "Allocate and attach a expanding fireball centered at x,y."
  [state x y]
  (let [[fid state] (next-entity-id state)
        fireball (missiles/make-fireball fid x y)]
    (update state :fireballs (fnil conj []) fireball)))

(defn- spawn-fireball-from-missile
  [state missile]
  (spawn-fireball-at state (:x1 missile) (:y1 missile)))
(defn- tick-defensive-missiles
  [state dt]
  (reduce (fn [s missile]
            (let [result (missiles/advance-defensive missile dt)]
              (if (missiles/arrived? result)
                (spawn-fireball-from-missile s missile)
                (update s :defensive-missiles (fnil conj []) result))))
          (assoc state :defensive-missiles [])
          (defensive-missiles state)))

(defn- tick-fireballs
  [state dt]
  (reduce (fn [s fireball]
            (let [result (missiles/advance-fireball fireball dt)]
              (if (= missiles/expired result)
                s
                (update s :fireballs (fnil conj []) result))))
          (assoc state :fireballs [])
          (fireballs state)))

(defn- target-hit-by-fireball?
  [target fireballs]
  (some #(missiles/point-in-fireball? % (:x target) (:y target)) fireballs))

(defn- destroy-targets-in-fireballs
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

(defn- destroy-enemy-by-fireball
  [state]
  (assoc state :last-enemy-fate :fireball))

(defn- spawn-impact-fireball
  "Visual/game blast at the impact point (ground strike)."
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

(defn- tick-one-enemy
  [state enemy dt fireballs]
  (cond
    (enemy-hit-by-fireball? enemy fireballs)
    (destroy-enemy-by-fireball state)

    :else
    (let [result (missiles/advance-enemy enemy dt)]
      (cond
        (missiles/arrived? result)
        (resolve-enemy-impact state enemy)

        (enemy-hit-by-fireball? result fireballs)
        (destroy-enemy-by-fireball state)

        :else
        (keep-flying-enemy state result)))))

(defn- tick-enemy-missiles
  [state dt]
  (let [fbs (fireballs state)]
    (reduce (fn [s enemy]
              (tick-one-enemy s enemy dt fbs))
            (assoc state :enemy-missiles [])
            (enemy-missiles state))))

(defn- wave-ready-to-complete?
  [state]
  (boolean
   (and (:wave-had-enemies? state)
        (not (:wave-complete? state))
        (empty? (enemy-missiles state)))))

(defn- mark-wave-complete
  [state]
  (-> state
      (assoc :wave-complete? wave-flag-on
             :wave-had-enemies? wave-starts-with-enemies?)
      (update :wave (fnil inc waves/initial-wave))))

(defn- maybe-complete-wave
  "When all active wave enemies are gone, mark the wave complete and advance."
  [state]
  (if (wave-ready-to-complete? state) (mark-wave-complete state) state))

(defn- transform-living-battery
  [battery f]
  (if (:destroyed? battery) battery (f battery)))

(defn- map-living-batteries
  [state f]
  (update state :batteries
          (fn [bs]
            (mapv #(transform-living-battery % f) bs))))

(defn rearm-surviving-batteries
  "Refill non-destroyed batteries to full ammo."
  [state]
  (map-living-batteries state #(batteries/set-ammo % waves/full-ammo)))

(defn- wave-targets-for
  [living n]
  (if (seq living) (take n (cycle living)) []))

(defn- wave-sky-origin-x
  "Deterministic sky entry x for the i-th of n wave enemies across the playfield."
  [state i n]
  (let [w (double (playfield-width state))]
    (if (pos? n)
      (* w (/ (+ (double i) 0.5) (double n)))
      0.0)))

(defn set-wave-enemies-active
  "Test helper: replace in-flight enemies with n scheduled wave enemies."
  [state n]
  (let [active? (pos? n)
        state (assoc state
                     :enemy-missiles []
                     :wave-complete? wave-starts-complete?
                     :wave-had-enemies? active?)
        living (mapv :id (living-cities state))
        targets (vec (wave-targets-for living n))]
    (reduce (fn [s [i city-id]]
              (spawn-enemy-targeting-city-from
               s (wave-sky-origin-x s i n) 0 city-id))
            state
            (map-indexed vector targets))))

(defn set-non-destroyed-battery-ammo
  "Test helper: set ammo on every non-destroyed battery."
  [state ammo]
  (map-living-batteries state #(batteries/set-ammo % ammo)))

(defn wave-schedule-metrics
  [wave-number]
  (waves/schedule-metrics wave-number))

(defn harder-wave?
  [low-metrics high-metrics]
  (waves/harder? low-metrics high-metrics))

(defn set-wave
  "Test helper: jump to a wave number without auto-completing."
  [state wave-number]
  (assoc state
         :wave wave-number
         :wave-complete? wave-starts-complete?
         :wave-had-enemies? wave-starts-with-enemies?
         :enemy-missiles []))

(defn start-next-wave
  "Begin the next wave: rearm survivors; wave number already advanced on complete."
  [state]
  (-> state
      (assoc :wave-complete? wave-starts-complete?
             :wave-had-enemies? wave-starts-with-enemies?)
      (rearm-surviving-batteries)))

(defn tick
  "Advance simulation by dt seconds (clamped). Returns {:state s :events [...]}."
  [state dt]
  (let [applied (missiles/clamp-dt dt)
        state (-> state
                  (assoc :last-applied-dt applied)
                  (update :sim-time (fnil + 0.0) applied)
                  (tick-defensive-missiles applied)
                  (tick-fireballs applied)
                  (destroy-targets-in-fireballs)
                  (tick-enemy-missiles applied)
                  (maybe-complete-wave))]
    {:state state :events []}))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T13:09:08.286057-05:00", :module-hash "-1598427011", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 7, :hash "-1338775330"} {:id "def/initial-score", :kind "def", :line 9, :end-line 9, :hash "485183318"} {:id "def/initial-entity-id", :kind "def", :line 10, :end-line 10, :hash "-2006662704"} {:id "def/wave-flag-off", :kind "def", :line 11, :end-line 11, :hash "1734145513"} {:id "def/wave-flag-on", :kind "def", :line 12, :end-line 12, :hash "1660732832"} {:id "def/wave-starts-complete?", :kind "def", :line 13, :end-line 13, :hash "1259204240"} {:id "def/wave-starts-with-enemies?", :kind "def", :line 14, :end-line 14, :hash "929188796"} {:id "def/clamp-lo", :kind "def", :line 15, :end-line 15, :hash "-224595111"} {:id "def/default-crosshair", :kind "def", :line 16, :end-line 16, :hash "-249046571"} {:id "def/target-starts-destroyed?", :kind "def", :line 17, :end-line 17, :hash "224311611"} {:id "defn-/clamp", :kind "defn-", :line 19, :end-line 21, :hash "1680610514"} {:id "defn-/clamp-point", :kind "defn-", :line 23, :end-line 26, :hash "-1550073030"} {:id "defn-/center-crosshair", :kind "defn-", :line 28, :end-line 30, :hash "502710703"} {:id "defn-/reclamp-crosshair", :kind "defn-", :line 32, :end-line 35, :hash "-495207193"} {:id "defn-/update-battery", :kind "defn-", :line 37, :end-line 39, :hash "-630735181"} {:id "defn-/next-entity-id", :kind "defn-", :line 41, :end-line 44, :hash "-611923035"} {:id "defn-/no-events", :kind "defn-", :line 46, :end-line 48, :hash "652168329"} {:id "defn/new-game", :kind "defn", :line 50, :end-line 68, :hash "1405822368"} {:id "defn/resize", :kind "defn", :line 70, :end-line 77, :hash "-1344415357"} {:id "defn/playfield-width", :kind "defn", :line 79, :end-line 81, :hash "-1043537513"} {:id "defn/playfield-height", :kind "defn", :line 83, :end-line 85, :hash "344252362"} {:id "defn/cities", :kind "defn", :line 87, :end-line 89, :hash "1240083502"} {:id "defn/living-cities", :kind "defn", :line 91, :end-line 93, :hash "416648848"} {:id "defn/batteries", :kind "defn", :line 95, :end-line 97, :hash "206298614"} {:id "defn/battery", :kind "defn", :line 99, :end-line 101, :hash "-1555624967"} {:id "defn/on-ground?", :kind "defn", :line 103, :end-line 106, :hash "1609612027"} {:id "defn/city-on-ground?", :kind "defn", :line 108, :end-line 110, :hash "-1878088970"} {:id "defn/crosshair", :kind "defn", :line 112, :end-line 114, :hash "-2027795649"} {:id "defn/score", :kind "defn", :line 116, :end-line 118, :hash "-1700557235"} {:id "defn/wave", :kind "defn", :line 120, :end-line 122, :hash "1109090166"} {:id "defn/wave-complete?", :kind "defn", :line 124, :end-line 126, :hash "-334236383"} {:id "defn/hud", :kind "defn", :line 128, :end-line 132, :hash "1267084367"} {:id "defn/defensive-missiles", :kind "defn", :line 134, :end-line 136, :hash "-1457861839"} {:id "defn/fireballs", :kind "defn", :line 138, :end-line 140, :hash "-47675919"} {:id "defn/enemy-missiles", :kind "defn", :line 142, :end-line 144, :hash "-1649887754"} {:id "defn/destroyable-targets", :kind "defn", :line 146, :end-line 148, :hash "-2146081921"} {:id "defn/last-enemy-fate", :kind "defn", :line 150, :end-line 152, :hash "-1164295963"} {:id "defn/city", :kind "defn", :line 154, :end-line 156, :hash "417115868"} {:id "defn/living-city?", :kind "defn", :line 158, :end-line 160, :hash "808122796"} {:id "defn/sim-time", :kind "defn", :line 162, :end-line 164, :hash "1526499425"} {:id "defn/last-applied-dt", :kind "defn", :line 166, :end-line 168, :hash "-1011651673"} {:id "defn/max-fireball-radius", :kind "defn", :line 170, :end-line 172, :hash "421742428"} {:id "defn/set-battery-ammo", :kind "defn", :line 174, :end-line 177, :hash "975933252"} {:id "defn/destroy-battery", :kind "defn", :line 179, :end-line 182, :hash "674766162"} {:id "defn/add-destroyable-target", :kind "defn", :line 184, :end-line 189, :hash "-1701043486"} {:id "defn-/update-city", :kind "defn-", :line 191, :end-line 193, :hash "-2016100813"} {:id "defn/destroy-city", :kind "defn", :line 195, :end-line 197, :hash "1888198826"} {:id "defn-/enemy-speed-for-state", :kind "defn-", :line 199, :end-line 201, :hash "-677140217"} {:id "defn/spawn-enemy-at", :kind "defn", :line 203, :end-line 213, :hash "-191047273"} {:id "defn/spawn-enemy-targeting-city", :kind "defn", :line 215, :end-line 224, :hash "49686352"} {:id "defn/spawn-enemy-targeting-battery", :kind "defn", :line 226, :end-line 235, :hash "1983609581"} {:id "defn/spawn-enemies-targeting-distinct-cities", :kind "defn", :line 237, :end-line 241, :hash "314430177"} {:id "defn/add-static-fireball", :kind "defn", :line 243, :end-line 248, :hash "2053229248"} {:id "defn/route-enemy-through-point", :kind "defn", :line 250, :end-line 264, :hash "-439948960"} {:id "defn-/impact-target", :kind "defn-", :line 266, :end-line 271, :hash "-984684299"} {:id "defn-/enemy-hit-by-fireball?", :kind "defn-", :line 273, :end-line 275, :hash "-583076826"} {:id "defn-/fire-battery", :kind "defn-", :line 277, :end-line 288, :hash "-618779090"} {:id "defn-/aim", :kind "defn-", :line 290, :end-line 296, :hash "242968114"} {:id "defn/click-zone", :kind "defn", :line 298, :end-line 301, :hash "943238228"} {:id "defn/click-fallback-order", :kind "defn", :line 303, :end-line 306, :hash "-1791151582"} {:id "defn-/click-fire", :kind "defn-", :line 308, :end-line 318, :hash "-533006603"} {:id "defn/handle", :kind "defn", :line 320, :end-line 328, :hash "-1723942109"} {:id "defn-/spawn-fireball-from-missile", :kind "defn-", :line 330, :end-line 334, :hash "1462937916"} {:id "defn-/tick-defensive-missiles", :kind "defn-", :line 336, :end-line 344, :hash "465906604"} {:id "defn-/tick-fireballs", :kind "defn-", :line 346, :end-line 354, :hash "-1794535937"} {:id "defn-/target-hit-by-fireball?", :kind "defn-", :line 356, :end-line 358, :hash "-18018455"} {:id "defn-/destroy-targets-in-fireballs", :kind "defn-", :line 360, :end-line 370, :hash "-1920073096"} {:id "defn-/destroy-enemy-by-fireball", :kind "defn-", :line 372, :end-line 374, :hash "650263139"} {:id "defn-/resolve-enemy-impact", :kind "defn-", :line 376, :end-line 380, :hash "-69074775"} {:id "defn-/keep-flying-enemy", :kind "defn-", :line 382, :end-line 384, :hash "-1439807545"} {:id "defn-/tick-one-enemy", :kind "defn-", :line 386, :end-line 402, :hash "56096190"} {:id "defn-/tick-enemy-missiles", :kind "defn-", :line 404, :end-line 410, :hash "-1658169989"} {:id "defn-/wave-ready-to-complete?", :kind "defn-", :line 412, :end-line 417, :hash "-1319953137"} {:id "defn-/mark-wave-complete", :kind "defn-", :line 419, :end-line 424, :hash "746013251"} {:id "defn-/maybe-complete-wave", :kind "defn-", :line 426, :end-line 429, :hash "1290373418"} {:id "defn-/transform-living-battery", :kind "defn-", :line 431, :end-line 433, :hash "-703267492"} {:id "defn-/map-living-batteries", :kind "defn-", :line 435, :end-line 439, :hash "-1924229694"} {:id "defn/rearm-surviving-batteries", :kind "defn", :line 441, :end-line 444, :hash "1499444922"} {:id "defn-/wave-targets-for", :kind "defn-", :line 446, :end-line 448, :hash "-1532070143"} {:id "defn/set-wave-enemies-active", :kind "defn", :line 450, :end-line 462, :hash "322414878"} {:id "defn/set-non-destroyed-battery-ammo", :kind "defn", :line 464, :end-line 467, :hash "510144529"} {:id "defn/wave-schedule-metrics", :kind "defn", :line 469, :end-line 471, :hash "-550911174"} {:id "defn/harder-wave?", :kind "defn", :line 473, :end-line 475, :hash "-1219120849"} {:id "defn/set-wave", :kind "defn", :line 477, :end-line 484, :hash "1005128065"} {:id "defn/start-next-wave", :kind "defn", :line 486, :end-line 492, :hash "98352319"} {:id "defn/tick", :kind "defn", :line 494, :end-line 506, :hash "-1833206829"}]}
;; clj-mutate-manifest-end
