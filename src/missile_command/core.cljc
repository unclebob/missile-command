(ns missile-command.core
  (:require [missile-command.world :as world]))

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

(defn- no-events
  [state]
  {:state state :events []})

(defn new-game
  "Create a new game state for the given playfield size."
  [{:keys [width height]}]
  (merge {:width width
          :height height
          :score 0
          :crosshair (center-crosshair width height)
          :defensive-missiles []
          :next-entity-id 0}
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

(defn city-on-ground?
  [state city]
  (world/in-ground-band? (:y city) (playfield-height state)))

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

(defn- make-defensive-missile
  [missile-id battery-id bat aim]
  {:id missile-id
   :battery battery-id
   :x0 (:x bat)
   :y0 (:y bat)
   :x1 (:x aim)
   :y1 (:y aim)
   :speed (:missile-speed bat)})

(defn- fire-battery
  [state battery-id]
  (let [bat (battery state battery-id)]
    (if-not (can-fire? bat)
      (no-events state)
      (let [[missile-id state] (next-entity-id state)
            missile (make-defensive-missile missile-id battery-id bat
                                            (crosshair state))]
        {:state (-> state
                    (update :defensive-missiles (fnil conj []) missile)
                    (update-battery battery-id #(update % :missiles dec)))
         :events [{:type :sfx/launch :battery battery-id}]}))))

(defn- aim
  [state x y]
  (no-events
   (assoc state :crosshair
          (clamp-point (playfield-width state)
                       (playfield-height state)
                       x y))))

(defn handle
  "Apply a player command. Returns {:state s :events [...]}."
  [state command]
  (case (:type command)
    :aim (aim state (:x command) (:y command))
    :fire (fire-battery state (:battery command))
    (throw (ex-info (str "unsupported command: " (:type command))
                    {:command command}))))
