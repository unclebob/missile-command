(ns missile-command.options
  "Player options: mute, difficulty, remappable keys."
  (:require [clojure.string :as str]
            [missile-command.screens :as screens]))

(def difficulty-arcade :arcade)
(def difficulty-normal :normal)
(def difficulty-easy :easy)

(def difficulty-factors
  {difficulty-arcade 1.0
   difficulty-normal 0.85
   difficulty-easy 0.7})

(def default-fire-keys
  {:left #{"z" "1"}
   :center #{"x" "2"}
   :right #{"c" "3"}})

(def default-pause-keys
  #{"p" "escape"})

(def default-options
  {:mute false
   :difficulty difficulty-arcade
   :keys {:fire default-fire-keys
          :pause default-pause-keys}})

(defn normalize-key
  "Lowercase string form of a key name."
  [raw]
  (str/lower-case (str raw)))

(defn parse-difficulty
  [raw]
  (let [s (if (keyword? raw) (name raw) (str raw))
        d (keyword (str/lower-case s))]
    (if (contains? difficulty-factors d)
      d
      difficulty-arcade)))

(defn parse-mute
  [raw]
  (let [s (str/lower-case (str raw))]
    (case s
      ("true" "1" "yes") true
      ("false" "0" "no") false
      (boolean raw))))

(defn difficulty-factor
  [difficulty]
  (double (get difficulty-factors (parse-difficulty difficulty) 1.0)))

(defn scale-enemy-count
  "Floor arcade count by factor; keep at least 1 when arcade count is positive."
  [arcade-count factor]
  (let [c (long arcade-count)
        f (double factor)]
    (if (pos? c)
      (max 1 (long (Math/floor (* c f))))
      0)))

(defn scale-enemy-speed
  [arcade-speed factor]
  (* (double arcade-speed) (double factor)))

(defn mute?
  [options]
  (boolean (:mute options)))

(defn difficulty
  [options]
  (parse-difficulty (or (:difficulty options) difficulty-arcade)))

(defn fire-keys
  [options battery-id]
  (set (map normalize-key
            (get-in options [:keys :fire battery-id] #{}))))

(defn pause-keys
  [options]
  (set (map normalize-key (get-in options [:keys :pause] #{}))))

(defn fire-key-includes?
  [options battery-id key]
  (contains? (fire-keys options battery-id) (normalize-key key)))

(defn pause-key-includes?
  [options key]
  (contains? (pause-keys options) (normalize-key key)))

(defn set-mute
  [options mute?]
  (assoc options :mute (boolean mute?)))

(defn set-difficulty
  [options difficulty]
  (assoc options :difficulty (parse-difficulty difficulty)))

(defn bind-fire-key
  "Replace fire bindings for battery with a single key."
  [options battery-id key]
  (assoc-in options [:keys :fire battery-id] #{(normalize-key key)}))

(defn key->battery
  "Resolve a raw key to a fire battery id, or nil."
  [options key]
  (let [k (normalize-key key)]
    (some (fn [battery]
            (when (fire-key-includes? options battery k)
              battery))
          [:left :center :right])))

(defn pause-key?
  [options key]
  (pause-key-includes? options key))

(defn of
  [state]
  (or (:options state) default-options))

(defn mute-state?
  [state]
  (mute? (of state)))

(defn difficulty-of
  [state]
  (difficulty (of state)))

(defn screen?
  [state]
  (screens/options? state))

(defn carry
  "Copy options from source onto target (threadable: target first)."
  [target source]
  (assoc target :options (of source)))

(defn open
  "Open options from the title screen."
  [state]
  (if (screens/title? state)
    (assoc state :screen screens/options)
    state))

(defn leave
  "Return from options to title."
  [state]
  (if (screen? state)
    (assoc state :screen screens/title)
    state))

(defn set-mute-state
  [state mute-value]
  (assoc state :options (set-mute (of state) mute-value)))

(defn set-difficulty-state
  [state difficulty]
  (assoc state :options (set-difficulty (of state) difficulty)))

(defn bind-fire-key-state
  [state battery-id key]
  (assoc state :options (bind-fire-key (of state) battery-id key)))

(defn fire-key-includes-state?
  [state battery-id key]
  (fire-key-includes? (of state) battery-id key))

(defn pause-key-includes-state?
  [state key]
  (pause-key-includes? (of state) key))
