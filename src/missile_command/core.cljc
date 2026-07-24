(ns missile-command.core
  (:require [missile-command.batteries :as batteries]
            [missile-command.input :as input]
            [missile-command.missiles :as missiles]
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
          :crosshair (center-crosshair width height)
          :defensive-missiles []
          :fireballs []
          :destroyable-targets []
          :sim-time 0.0
          :last-applied-dt 0.0
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
  (filterv :alive? (cities state)))

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

(defn defensive-missiles
  [state]
  (or (:defensive-missiles state) []))

(defn fireballs
  [state]
  (or (:fireballs state) []))

(defn destroyable-targets
  [state]
  (or (:destroyable-targets state) []))

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
              (if (= missiles/arrived result)
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

(defn tick
  "Advance simulation by dt seconds (clamped). Returns {:state s :events [...]}."
  [state dt]
  (let [applied (missiles/clamp-dt dt)
        state (-> state
                  (assoc :last-applied-dt applied)
                  (update :sim-time (fnil + 0.0) applied)
                  (tick-defensive-missiles applied)
                  (tick-fireballs applied)
                  (destroy-targets-in-fireballs))]
    {:state state :events []}))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T11:56:39.45099-05:00", :module-hash "-477247784", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "283709622"} {:id "def/initial-score", :kind "def", :line 7, :end-line 7, :hash "485183318"} {:id "def/initial-entity-id", :kind "def", :line 8, :end-line 8, :hash "-2006662704"} {:id "defn-/clamp", :kind "defn-", :line 10, :end-line 12, :hash "1680610514"} {:id "defn-/clamp-point", :kind "defn-", :line 14, :end-line 17, :hash "-1082243884"} {:id "defn-/center-crosshair", :kind "defn-", :line 19, :end-line 21, :hash "502710703"} {:id "defn-/reclamp-crosshair", :kind "defn-", :line 23, :end-line 26, :hash "-777862263"} {:id "defn-/update-battery", :kind "defn-", :line 28, :end-line 30, :hash "-630735181"} {:id "defn-/next-entity-id", :kind "defn-", :line 32, :end-line 35, :hash "-611923035"} {:id "defn-/no-events", :kind "defn-", :line 37, :end-line 39, :hash "652168329"} {:id "defn/new-game", :kind "defn", :line 41, :end-line 50, :hash "-2058566716"} {:id "defn/resize", :kind "defn", :line 52, :end-line 59, :hash "-1344415357"} {:id "defn/playfield-width", :kind "defn", :line 61, :end-line 63, :hash "-1043537513"} {:id "defn/playfield-height", :kind "defn", :line 65, :end-line 67, :hash "344252362"} {:id "defn/cities", :kind "defn", :line 69, :end-line 71, :hash "1240083502"} {:id "defn/living-cities", :kind "defn", :line 73, :end-line 75, :hash "-1556555524"} {:id "defn/batteries", :kind "defn", :line 77, :end-line 79, :hash "206298614"} {:id "defn/battery", :kind "defn", :line 81, :end-line 83, :hash "-1555624967"} {:id "defn/on-ground?", :kind "defn", :line 85, :end-line 88, :hash "1609612027"} {:id "defn/city-on-ground?", :kind "defn", :line 90, :end-line 92, :hash "-1878088970"} {:id "defn/crosshair", :kind "defn", :line 94, :end-line 96, :hash "-2027795649"} {:id "defn/score", :kind "defn", :line 98, :end-line 100, :hash "-1700557235"} {:id "defn/defensive-missiles", :kind "defn", :line 102, :end-line 104, :hash "-1457861839"} {:id "defn/set-battery-ammo", :kind "defn", :line 106, :end-line 109, :hash "975933252"} {:id "defn/destroy-battery", :kind "defn", :line 111, :end-line 114, :hash "674766162"} {:id "defn-/fire-battery", :kind "defn-", :line 116, :end-line 127, :hash "-618779090"} {:id "defn-/aim", :kind "defn-", :line 129, :end-line 135, :hash "242968114"} {:id "defn/click-zone", :kind "defn", :line 137, :end-line 140, :hash "943238228"} {:id "defn/click-fallback-order", :kind "defn", :line 142, :end-line 145, :hash "-1791151582"} {:id "defn-/click-fire", :kind "defn-", :line 147, :end-line 157, :hash "1116051741"} {:id "defn/handle", :kind "defn", :line 159, :end-line 167, :hash "-1723942109"}]}
;; clj-mutate-manifest-end
