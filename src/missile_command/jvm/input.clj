(ns missile-command.jvm.input
  "Pure host input mapping: UI events → core commands. No Quil.")

(def default-fire-keys
  {\z :left \Z :left \1 :left
   \x :center \X :center \2 :center
   \c :right \C :right \3 :right})

(defn key-char->battery
  "Default fire keys: left Z/1, center X/2, right C/3."
  [ch]
  (get default-fire-keys ch))

(defn fire-command
  [battery-id]
  {:type :fire :battery battery-id})

(defn aim-command
  [x y]
  {:type :aim :x x :y y})

(defn key-char->command
  "Map a raw key character to a core command, or nil if not a game key."
  [ch]
  (when-let [battery (key-char->battery ch)]
    (fire-command battery)))

(defn escape-key?
  [ch]
  (= ch (char 27)))

(defn parse-window-size
  "Parse optional width/height string args with defaults."
  [args default-width default-height]
  (let [[w h] (vec args)]
    [(if w (Integer/parseInt (str w)) default-width)
     (if h (Integer/parseInt (str h)) default-height)]))

(defn resize-if-needed
  "Return reflowed state when playfield size changed; else unchanged state."
  [state width height resize-fn playfield-width-fn playfield-height-fn]
  (if (or (not= width (playfield-width-fn state))
          (not= height (playfield-height-fn state)))
    (resize-fn state width height)
    state))
