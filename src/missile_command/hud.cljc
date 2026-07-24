(ns missile-command.hud
  "In-game HUD projection for hosts and acceptance checks."
  (:require [missile-command.cities :as cities]
            [missile-command.screens :as screens]
            [missile-command.waves :as waves]))

(defn- battery
  [state id]
  (first (filter #(= id (:id %)) (or (:batteries state) []))))

(defn- battery-ammo
  [state battery-id]
  (long (or (:missiles (battery state battery-id)) 0)))

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
