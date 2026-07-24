(ns missile-command.core
  (:require [missile-command.batteries :as batteries]
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

(defn set-battery-ammo
  "Test/setup helper: set remaining missiles for a battery."
  [state battery-id ammo]
  (update-battery state battery-id #(batteries/set-ammo % ammo)))

(defn destroy-battery
  "Test/setup helper: mark a battery destroyed."
  [state battery-id]
  (update-battery state battery-id batteries/destroy))

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

(def click-fallback-orders
  "Battery preference order for each click zone, preferred first."
  {:left [:left :center :right]
   :center [:center :left :right]
   :right [:right :center :left]})

(defn click-zone
  "Horizontal third for a playfield x coordinate: :left, :center, or :right."
  [width x]
  (let [third (/ (double width) 3.0)]
    (cond
      (< x third) :left
      (< x (* 2.0 third)) :center
      :else :right)))

(defn click-fallback-order
  "Battery preference order for a click zone, preferred first."
  [zone]
  (get click-fallback-orders zone))

(defn- first-fireable
  [state battery-ids]
  (first (filter #(batteries/can-fire? (battery state %)) battery-ids)))

(defn- click-fire
  "Aim at the click point, then fire preferred zone battery with adjacent fallback."
  [state x y]
  (let [aimed (:state (aim state x y))
        zone (click-zone (playfield-width aimed) (:x (crosshair aimed)))
        battery-id (first-fireable aimed (click-fallback-order zone))]
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
