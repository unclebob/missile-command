(ns missile-command.hud
  "In-game HUD projection for hosts and acceptance checks."
  (:require [missile-command.cities :as cities]
            [missile-command.screens :as screens]
            [missile-command.waves :as waves]))

(defn- battery
  [state id]
  (first (filter #(= id (:id %)) (or (:batteries state) []))))

(defn- battery-ammo
  "Remaining missiles; destroyed batteries report 0."
  [state battery-id]
  (let [b (battery state battery-id)]
    (if (or (nil? b) (:destroyed? b))
      0
      (long (or (:missiles b) 0)))))

(defn- battery-ammo-map
  [state]
  {:left (battery-ammo state :left)
   :center (battery-ammo state :center)
   :right (battery-ammo state :right)})

(defn projection
  "HUD fields: score, wave, multiplier, ammo, cities, reserve.
  full-playing-hud? is true during playing and paused."
  [state]
  (let [ammo (battery-ammo-map state)
        wave (or (:wave state) waves/initial-wave)]
    {:wave wave
     :score (or (:score state) 0)
     :multiplier (waves/multiplier wave)
     :bonus-cities (long (or (:bonus-cities state) 0))
     :living-cities (count (cities/living (or (:cities state) [])))
     :left-ammo (:left ammo)
     :center-ammo (:center ammo)
     :right-ammo (:right ammo)
     :ammo ammo
     :full-playing-hud? (or (screens/playing? state) (screens/paused? state))
     :screen (screens/of state)
     :the-end? (screens/the-end? state)
     :end-message (:end-message state)
     :title-game-name (screens/title-game-name-of state)}))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T21:14:30.725398-05:00", :module-hash "-484243791", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "-1606708647"} {:id "defn-/battery", :kind "defn-", :line 7, :end-line 9, :hash "-913679822"} {:id "defn-/battery-ammo", :kind "defn-", :line 11, :end-line 17, :hash "880246023"} {:id "defn-/battery-ammo-map", :kind "defn-", :line 19, :end-line 23, :hash "335275764"} {:id "defn/projection", :kind "defn", :line 25, :end-line 44, :hash "597622685"}]}
;; clj-mutate-manifest-end
