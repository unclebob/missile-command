(ns missile-command.screens)

(def title :title)
(def playing :playing)
(def paused :paused)
(def the-end :the-end)

(def title-game-name "Missile Command")
(def title-start-affordance "Press Enter or click to start")

(defn of
  [state]
  (or (:screen state) title))

(defn title?
  [state]
  (= title (of state)))

(defn playing?
  [state]
  (= playing (of state)))

(defn paused?
  [state]
  (= paused (of state)))

(defn the-end?
  [state]
  (= the-end (of state)))

(defn title-game-name-of
  [state]
  (or (:title-game-name state) title-game-name))

(defn title-shows-start-affordance?
  [state]
  (boolean (and (title? state) (:title-start-affordance state))))
