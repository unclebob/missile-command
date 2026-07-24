(ns missile-command.core
  (:require [missile-command.world :as world]))

(defn- clamp
  [n lo hi]
  (max lo (min hi n)))

(defn- clamp-point
  [width height x y]
  {:x (clamp x 0 (dec width))
   :y (clamp y 0 (dec height))})

(defn- update-battery
  [state battery-id f]
  (update state :batteries
          (fn [batteries]
            (mapv (fn [battery]
                    (if (= battery-id (:id battery))
                      (f battery)
                      battery))
                  batteries))))

(defn- next-entity-id
  [state]
  (let [id (or (:next-entity-id state) 0)]
    [id (assoc state :next-entity-id (inc id))]))

(defn new-game
  "Create a new game state for the given playfield size."
  [{:keys [width height]}]
  (merge {:width width
          :height height
          :score 0
          :crosshair (clamp-point width height (quot width 2) (quot height 2))
          :defensive-missiles []
          :next-entity-id 0}
         (world/apply-layout width height)))

(defn resize
  "Reflow layout for a new playfield size, preserving entity progress fields."
  [state width height]
  (let [crosshair (or (:crosshair state) {:x 0 :y 0})]
    (merge state
           {:width width
            :height height
            :crosshair (clamp-point width height (:x crosshair) (:y crosshair))}
           (world/apply-layout width height state))))

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
  (update-battery state battery-id #(assoc % :missiles ammo)))

(defn destroy-battery
  "Test/setup helper: mark a battery destroyed."
  [state battery-id]
  (update-battery state battery-id #(assoc % :destroyed? true)))

(defn- can-fire?
  [battery]
  (and battery
       (not (:destroyed? battery))
       (pos? (:missiles battery))))

(defn- fire-battery
  [state battery-id]
  (let [bat (battery state battery-id)]
    (if-not (can-fire? bat)
      {:state state :events []}
      (let [[missile-id state] (next-entity-id state)
            aim (crosshair state)
            missile {:id missile-id
                     :battery battery-id
                     :x0 (:x bat)
                     :y0 (:y bat)
                     :x1 (:x aim)
                     :y1 (:y aim)
                     :speed (:missile-speed bat)}]
        {:state (-> state
                    (update :defensive-missiles (fnil conj []) missile)
                    (update-battery battery-id #(update % :missiles dec)))
         :events [{:type :sfx/launch :battery battery-id}]}))))

(defn- aim
  [state x y]
  {:state (assoc state :crosshair
                 (clamp-point (playfield-width state)
                              (playfield-height state)
                              x y))
   :events []})

(defn handle
  "Apply a player command. Returns {:state s :events [...]}."
  [state command]
  (case (:type command)
    :aim (aim state (:x command) (:y command))
    :fire (fire-battery state (:battery command))
    (throw (ex-info (str "unsupported command: " (:type command))
                    {:command command}))))
