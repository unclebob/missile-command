(ns missile-command.core
  (:require [missile-command.batteries :as batteries]
            [missile-command.cities :as cities]
            [missile-command.input :as input]
            [missile-command.missiles :as missiles]
            [missile-command.waves :as waves]
            [missile-command.world :as world]))

(def initial-score 0)
(def initial-entity-id 0)

(defn- clamp
  [n lo hi]
  (max lo (min hi n)))

(defn- clamp-point
  [width height x y]
  {:x (clamp x 0 (dec width))
   :y (clamp y 0 (dec height))})

(defn- center-crosshair
  [width height]
  (clamp-point width height (quot width 2) (quot height 2)))

(defn- reclamp-crosshair
  [state width height]
  (let [crosshair (or (:crosshair state) {:x 0 :y 0})]
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
          :wave-complete? false
          :wave-had-enemies? false
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
        target {:id id :x x :y y :destroyed? false}]
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
        (assoc :wave-had-enemies? true
               :wave-complete? false))))

(defn spawn-enemy-targeting-city
  "Spawn an enemy missile from the top of the sky toward a living city."
  [state city-id]
  (let [c (city state city-id)]
    (when-not c
      (throw (ex-info (str "unknown city " city-id) {:city-id city-id})))
    (spawn-enemy-at state
                    {:x (:x c) :y 0}
                    {:x (:x c) :y (:y c)}
                    :city city-id)))

(defn spawn-enemy-targeting-battery
  "Spawn an enemy missile from the top of the sky toward a battery."
  [state battery-id]
  (let [b (battery state battery-id)]
    (when-not b
      (throw (ex-info (str "unknown battery " battery-id) {:battery-id battery-id})))
    (spawn-enemy-at state
                    {:x (:x b) :y 0}
                    {:x (:x b) :y (:y b)}
                    :battery battery-id)))

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

(defn- spawn-fireball-from-missile
  [state missile]
  (let [[fid state] (next-entity-id state)
        fireball (missiles/make-fireball fid (:x1 missile) (:y1 missile))]
    (update state :fireballs (fnil conj []) fireball)))

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
  (let [[fid state] (next-entity-id state)
        fb (missiles/make-fireball fid (:x1 enemy) (:y1 enemy))]
    (update state :fireballs (fnil conj []) fb)))

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

(defn- maybe-complete-wave
  "When all active wave enemies are gone, mark the wave complete and advance."
  [state]
  (if (and (:wave-had-enemies? state)
           (not (:wave-complete? state))
           (empty? (enemy-missiles state)))
    (-> state
        (assoc :wave-complete? true
               :wave-had-enemies? false)
        (update :wave (fnil inc waves/initial-wave)))
    state))

(defn- map-living-batteries
  [state f]
  (update state :batteries
          (fn [bs]
            (mapv (fn [b]
                    (if (:destroyed? b) b (f b)))
                  bs))))

(defn rearm-surviving-batteries
  "Refill non-destroyed batteries to full ammo."
  [state]
  (map-living-batteries state #(batteries/set-ammo % waves/full-ammo)))

(defn set-wave-enemies-active
  "Test helper: replace in-flight enemies with n scheduled wave enemies."
  [state n]
  (let [state (assoc state
                     :enemy-missiles []
                     :wave-complete? false
                     :wave-had-enemies? (pos? n))
        living (mapv :id (living-cities state))
        targets (if (seq living)
                  (take n (cycle living))
                  [])]
    (reduce (fn [s city-id]
              (spawn-enemy-targeting-city s city-id))
            state
            targets)))

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
         :wave-complete? false
         :wave-had-enemies? false
         :enemy-missiles []))

(defn start-next-wave
  "Begin the next wave: rearm survivors; wave number already advanced on complete."
  [state]
  (-> state
      (assoc :wave-complete? false
             :wave-had-enemies? false)
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
;; {:version 1, :tested-at "2026-07-24T12:47:17.848203-05:00", :module-hash "-703830912", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 6, :hash "452595998"} {:id "def/initial-score", :kind "def", :line 8, :end-line 8, :hash "485183318"} {:id "def/initial-entity-id", :kind "def", :line 9, :end-line 9, :hash "-2006662704"} {:id "defn-/clamp", :kind "defn-", :line 11, :end-line 13, :hash "1680610514"} {:id "defn-/clamp-point", :kind "defn-", :line 15, :end-line 18, :hash "-1082243884"} {:id "defn-/center-crosshair", :kind "defn-", :line 20, :end-line 22, :hash "502710703"} {:id "defn-/reclamp-crosshair", :kind "defn-", :line 24, :end-line 27, :hash "-777862263"} {:id "defn-/update-battery", :kind "defn-", :line 29, :end-line 31, :hash "-630735181"} {:id "defn-/next-entity-id", :kind "defn-", :line 33, :end-line 36, :hash "-611923035"} {:id "defn-/no-events", :kind "defn-", :line 38, :end-line 40, :hash "652168329"} {:id "defn/new-game", :kind "defn", :line 42, :end-line 57, :hash "1683099286"} {:id "defn/resize", :kind "defn", :line 59, :end-line 66, :hash "-1344415357"} {:id "defn/playfield-width", :kind "defn", :line 68, :end-line 70, :hash "-1043537513"} {:id "defn/playfield-height", :kind "defn", :line 72, :end-line 74, :hash "344252362"} {:id "defn/cities", :kind "defn", :line 76, :end-line 78, :hash "1240083502"} {:id "defn/living-cities", :kind "defn", :line 80, :end-line 82, :hash "416648848"} {:id "defn/batteries", :kind "defn", :line 84, :end-line 86, :hash "206298614"} {:id "defn/battery", :kind "defn", :line 88, :end-line 90, :hash "-1555624967"} {:id "defn/on-ground?", :kind "defn", :line 92, :end-line 95, :hash "1609612027"} {:id "defn/city-on-ground?", :kind "defn", :line 97, :end-line 99, :hash "-1878088970"} {:id "defn/crosshair", :kind "defn", :line 101, :end-line 103, :hash "-2027795649"} {:id "defn/score", :kind "defn", :line 105, :end-line 107, :hash "-1700557235"} {:id "defn/defensive-missiles", :kind "defn", :line 109, :end-line 111, :hash "-1457861839"} {:id "defn/fireballs", :kind "defn", :line 113, :end-line 115, :hash "-47675919"} {:id "defn/enemy-missiles", :kind "defn", :line 117, :end-line 119, :hash "-1649887754"} {:id "defn/destroyable-targets", :kind "defn", :line 121, :end-line 123, :hash "-2146081921"} {:id "defn/last-enemy-fate", :kind "defn", :line 125, :end-line 127, :hash "-1164295963"} {:id "defn/city", :kind "defn", :line 129, :end-line 131, :hash "417115868"} {:id "defn/living-city?", :kind "defn", :line 133, :end-line 135, :hash "808122796"} {:id "defn/sim-time", :kind "defn", :line 137, :end-line 139, :hash "1526499425"} {:id "defn/last-applied-dt", :kind "defn", :line 141, :end-line 143, :hash "-1011651673"} {:id "defn/max-fireball-radius", :kind "defn", :line 145, :end-line 147, :hash "421742428"} {:id "defn/set-battery-ammo", :kind "defn", :line 149, :end-line 152, :hash "975933252"} {:id "defn/destroy-battery", :kind "defn", :line 154, :end-line 157, :hash "674766162"} {:id "defn/add-destroyable-target", :kind "defn", :line 159, :end-line 164, :hash "1896236082"} {:id "defn-/update-city", :kind "defn-", :line 166, :end-line 168, :hash "-2016100813"} {:id "defn/destroy-city", :kind "defn", :line 170, :end-line 172, :hash "1888198826"} {:id "defn/spawn-enemy-at", :kind "defn", :line 174, :end-line 181, :hash "-487725036"} {:id "defn/spawn-enemy-targeting-city", :kind "defn", :line 183, :end-line 192, :hash "49686352"} {:id "defn/spawn-enemy-targeting-battery", :kind "defn", :line 194, :end-line 203, :hash "1983609581"} {:id "defn/spawn-enemies-targeting-distinct-cities", :kind "defn", :line 205, :end-line 209, :hash "314430177"} {:id "defn/add-static-fireball", :kind "defn", :line 211, :end-line 216, :hash "2053229248"} {:id "defn/route-enemy-through-point", :kind "defn", :line 218, :end-line 232, :hash "-439948960"} {:id "defn-/impact-target", :kind "defn-", :line 234, :end-line 239, :hash "-984684299"} {:id "defn-/enemy-hit-by-fireball?", :kind "defn-", :line 241, :end-line 243, :hash "-583076826"} {:id "defn-/fire-battery", :kind "defn-", :line 245, :end-line 256, :hash "-618779090"} {:id "defn-/aim", :kind "defn-", :line 258, :end-line 264, :hash "242968114"} {:id "defn/click-zone", :kind "defn", :line 266, :end-line 269, :hash "943238228"} {:id "defn/click-fallback-order", :kind "defn", :line 271, :end-line 274, :hash "-1791151582"} {:id "defn-/click-fire", :kind "defn-", :line 276, :end-line 286, :hash "-533006603"} {:id "defn/handle", :kind "defn", :line 288, :end-line 296, :hash "-1723942109"} {:id "defn-/spawn-fireball-from-missile", :kind "defn-", :line 298, :end-line 302, :hash "1462937916"} {:id "defn-/tick-defensive-missiles", :kind "defn-", :line 304, :end-line 312, :hash "465906604"} {:id "defn-/tick-fireballs", :kind "defn-", :line 314, :end-line 322, :hash "-1794535937"} {:id "defn-/target-hit-by-fireball?", :kind "defn-", :line 324, :end-line 326, :hash "-18018455"} {:id "defn-/destroy-targets-in-fireballs", :kind "defn-", :line 328, :end-line 338, :hash "-1920073096"} {:id "defn-/destroy-enemy-by-fireball", :kind "defn-", :line 340, :end-line 342, :hash "650263139"} {:id "defn-/resolve-enemy-impact", :kind "defn-", :line 344, :end-line 348, :hash "-69074775"} {:id "defn-/keep-flying-enemy", :kind "defn-", :line 350, :end-line 352, :hash "-1439807545"} {:id "defn-/tick-one-enemy", :kind "defn-", :line 354, :end-line 370, :hash "56096190"} {:id "defn-/tick-enemy-missiles", :kind "defn-", :line 372, :end-line 378, :hash "-1658169989"} {:id "defn/tick", :kind "defn", :line 380, :end-line 391, :hash "-608187191"}]}
;; clj-mutate-manifest-end
