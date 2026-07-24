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
  (case zone
    :left [:left :center :right]
    :center [:center :left :right]
    :right [:right :center :left]))

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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T11:43:51.164451-05:00", :module-hash "1401770526", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "-291954255"} {:id "def/initial-score", :kind "def", :line 6, :end-line 6, :hash "485183318"} {:id "def/initial-entity-id", :kind "def", :line 7, :end-line 7, :hash "-2006662704"} {:id "defn-/clamp", :kind "defn-", :line 9, :end-line 11, :hash "1680610514"} {:id "defn-/clamp-point", :kind "defn-", :line 13, :end-line 16, :hash "-1082243884"} {:id "defn-/center-crosshair", :kind "defn-", :line 18, :end-line 20, :hash "502710703"} {:id "defn-/reclamp-crosshair", :kind "defn-", :line 22, :end-line 25, :hash "-777862263"} {:id "defn-/update-battery", :kind "defn-", :line 27, :end-line 29, :hash "-630735181"} {:id "defn-/next-entity-id", :kind "defn-", :line 31, :end-line 34, :hash "-611923035"} {:id "defn-/no-events", :kind "defn-", :line 36, :end-line 38, :hash "652168329"} {:id "defn/new-game", :kind "defn", :line 40, :end-line 49, :hash "-2058566716"} {:id "defn/resize", :kind "defn", :line 51, :end-line 58, :hash "-1344415357"} {:id "defn/playfield-width", :kind "defn", :line 60, :end-line 62, :hash "-1043537513"} {:id "defn/playfield-height", :kind "defn", :line 64, :end-line 66, :hash "344252362"} {:id "defn/cities", :kind "defn", :line 68, :end-line 70, :hash "1240083502"} {:id "defn/living-cities", :kind "defn", :line 72, :end-line 74, :hash "-1556555524"} {:id "defn/batteries", :kind "defn", :line 76, :end-line 78, :hash "206298614"} {:id "defn/battery", :kind "defn", :line 80, :end-line 82, :hash "-1555624967"} {:id "defn/on-ground?", :kind "defn", :line 84, :end-line 87, :hash "1609612027"} {:id "defn/city-on-ground?", :kind "defn", :line 89, :end-line 91, :hash "-1878088970"} {:id "defn/crosshair", :kind "defn", :line 93, :end-line 95, :hash "-2027795649"} {:id "defn/score", :kind "defn", :line 97, :end-line 99, :hash "-1700557235"} {:id "defn/defensive-missiles", :kind "defn", :line 101, :end-line 103, :hash "-1457861839"} {:id "defn/set-battery-ammo", :kind "defn", :line 105, :end-line 108, :hash "975933252"} {:id "defn/destroy-battery", :kind "defn", :line 110, :end-line 113, :hash "674766162"} {:id "defn-/fire-battery", :kind "defn-", :line 115, :end-line 126, :hash "-618779090"} {:id "defn-/aim", :kind "defn-", :line 128, :end-line 134, :hash "242968114"} {:id "defn/handle", :kind "defn", :line 136, :end-line 143, :hash "374931850"}]}
;; clj-mutate-manifest-end
