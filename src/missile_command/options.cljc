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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T16:17:30.235241-05:00", :module-hash "1117284808", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "1083973568"} {:id "def/difficulty-arcade", :kind "def", :line 6, :end-line 6, :hash "-1386604751"} {:id "def/difficulty-normal", :kind "def", :line 7, :end-line 7, :hash "-303865677"} {:id "def/difficulty-easy", :kind "def", :line 8, :end-line 8, :hash "-487868870"} {:id "def/difficulty-factors", :kind "def", :line 10, :end-line 13, :hash "-1040124489"} {:id "def/default-fire-keys", :kind "def", :line 15, :end-line 18, :hash "-1590896162"} {:id "def/default-pause-keys", :kind "def", :line 20, :end-line 21, :hash "1988438166"} {:id "def/default-options", :kind "def", :line 23, :end-line 27, :hash "1053110458"} {:id "defn/normalize-key", :kind "defn", :line 29, :end-line 32, :hash "1523118349"} {:id "defn/parse-difficulty", :kind "defn", :line 34, :end-line 40, :hash "-659829357"} {:id "defn/parse-mute", :kind "defn", :line 42, :end-line 48, :hash "-1842128516"} {:id "defn/difficulty-factor", :kind "defn", :line 50, :end-line 52, :hash "-599339465"} {:id "defn/scale-enemy-count", :kind "defn", :line 54, :end-line 61, :hash "-780541258"} {:id "defn/scale-enemy-speed", :kind "defn", :line 63, :end-line 65, :hash "715002788"} {:id "defn/mute?", :kind "defn", :line 67, :end-line 69, :hash "-1035573297"} {:id "defn/difficulty", :kind "defn", :line 71, :end-line 73, :hash "2114647608"} {:id "defn/fire-keys", :kind "defn", :line 75, :end-line 78, :hash "-496243755"} {:id "defn/pause-keys", :kind "defn", :line 80, :end-line 82, :hash "1250712945"} {:id "defn/fire-key-includes?", :kind "defn", :line 84, :end-line 86, :hash "1799389198"} {:id "defn/pause-key-includes?", :kind "defn", :line 88, :end-line 90, :hash "1432171354"} {:id "defn/set-mute", :kind "defn", :line 92, :end-line 94, :hash "-1508985195"} {:id "defn/set-difficulty", :kind "defn", :line 96, :end-line 98, :hash "-1683413112"} {:id "defn/bind-fire-key", :kind "defn", :line 100, :end-line 103, :hash "99571229"} {:id "defn/key->battery", :kind "defn", :line 105, :end-line 112, :hash "225170494"} {:id "defn/pause-key?", :kind "defn", :line 114, :end-line 116, :hash "1852682469"} {:id "defn/of", :kind "defn", :line 118, :end-line 120, :hash "433731555"} {:id "defn/mute-state?", :kind "defn", :line 122, :end-line 124, :hash "848723469"} {:id "defn/difficulty-of", :kind "defn", :line 126, :end-line 128, :hash "1490486714"} {:id "defn/screen?", :kind "defn", :line 130, :end-line 132, :hash "1958717376"} {:id "defn/carry", :kind "defn", :line 134, :end-line 137, :hash "2039692674"} {:id "defn/open", :kind "defn", :line 139, :end-line 144, :hash "80620788"} {:id "defn/leave", :kind "defn", :line 146, :end-line 151, :hash "-1846746854"} {:id "defn/set-mute-state", :kind "defn", :line 153, :end-line 155, :hash "-595084965"} {:id "defn/set-difficulty-state", :kind "defn", :line 157, :end-line 159, :hash "959621576"} {:id "defn/bind-fire-key-state", :kind "defn", :line 161, :end-line 163, :hash "852603139"} {:id "defn/fire-key-includes-state?", :kind "defn", :line 165, :end-line 167, :hash "-2058306288"} {:id "defn/pause-key-includes-state?", :kind "defn", :line 169, :end-line 171, :hash "-1163875488"}]}
;; clj-mutate-manifest-end
