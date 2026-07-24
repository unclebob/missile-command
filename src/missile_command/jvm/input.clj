(ns missile-command.jvm.input
  "Pure host input mapping: UI events → core commands, CLI, telemetry. No Quil."
  (:require [clojure.string :as str]))

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

(defn click-command
  [x y]
  {:type :click :x x :y y})

(defn key-char->command
  "Map a raw key character to a core command, or nil if not a game key."
  [ch]
  (when-let [battery (key-char->battery ch)]
    (fire-command battery)))

(defn escape-key?
  [ch]
  (= ch (char 27)))

(defn parse-destroy-list
  "Parse comma-separated battery names into keyword ids."
  [s]
  (->> (str/split (str s) #",")
       (map str/trim)
       (remove str/blank?)
       (map keyword)
       vec))

(defn- int-token?
  [s]
  (boolean (re-matches #"-?\d+" (str s))))

(defn- parse-int-token
  [s]
  (Integer/parseInt (str s)))

(def ^:private switch-handlers
  {"--" (fn [opts xs] [opts (rest xs)])
   "--qa-telemetry" (fn [opts xs] [(assoc opts :qa-telemetry? true) (rest xs)])
   "--destroy-batteries"
   (fn [opts xs]
     [(assoc opts :destroy-batteries (parse-destroy-list (second xs)))
      (drop 2 xs)])
   "--qa-events"
   (fn [opts xs]
     [(assoc opts :qa-events (second xs)) (drop 2 xs)])})

(defn- apply-switch
  "Consume one CLI switch from xs. Returns [opts remaining-xs] or nil."
  [opts xs]
  (when-let [handler (get switch-handlers (first xs))]
    (handler opts xs)))

(defn- apply-size-token
  "Consume a numeric width/height token. Returns [opts remaining-xs] or nil."
  [opts xs]
  (when (int-token? (first xs))
    (let [n (parse-int-token (first xs))]
      (cond
        (nil? (:width-set? opts))
        [(assoc opts :width n :width-set? true) (rest xs)]

        (nil? (:height-set? opts))
        [(assoc opts :height n :height-set? true) (rest xs)]

        :else nil))))

(defn parse-cli-args
  "Parse launch args: optional width height, then switches.
  Switches: --qa-telemetry, --destroy-batteries LIST, --qa-events PATH"
  ([args]
   (parse-cli-args args 800 600))
  ([args default-width default-height]
   (loop [xs (seq args)
          opts {:width default-width
                :height default-height
                :qa-telemetry? false
                :destroy-batteries []
                :qa-events nil}]
     (if-not xs
       (dissoc opts :width-set? :height-set?)
       (if-let [[opts' xs'] (or (apply-switch opts xs)
                                (apply-size-token opts xs))]
         (recur (seq xs') opts')
         (throw (ex-info (str "unknown launch argument: " (first xs))
                         {:arg (first xs)})))))))

(defn parse-window-size
  "Legacy helper: numeric width/height only (no switches)."
  [args default-width default-height]
  (let [opts (parse-cli-args args default-width default-height)]
    [(:width opts) (:height opts)]))

(defn resize-if-needed
  "Return reflowed state when playfield size changed; else unchanged state."
  [state width height resize-fn playfield-width-fn playfield-height-fn]
  (if (or (not= width (playfield-width-fn state))
          (not= height (playfield-height-fn state)))
    (resize-fn state width height)
    state))

(defn battery-from-events
  "Extract fired battery from core events, or :none."
  [events]
  (or (some (fn [e]
              (when (= :sfx/launch (:type e))
                (:battery e)))
            events)
      :none))

(defn format-telemetry-line
  "Build a parseable qa-fire telemetry line from a handle result."
  [result]
  (let [state (:state result)
        battery (name (battery-from-events (:events result)))
        missiles (or (:defensive-missiles state) [])
        vectors (mapcat (fn [m]
                          [(str "origin_x=" (:x0 m))
                           (str "origin_y=" (:y0 m))
                           (str "target_x=" (:x1 m))
                           (str "target_y=" (:y1 m))])
                        missiles)]
    (str/join
     " "
     (into [(str "qa-fire battery=" battery)
            (str "missiles_in_flight=" (count missiles))]
           vectors))))

(def ^:private qa-event-parsers
  {"click" (fn [a b] {:type :click :x (parse-int-token a) :y (parse-int-token b)})
   "aim" (fn [a b] {:type :aim :x (parse-int-token a) :y (parse-int-token b)})
   "key" (fn [a _] {:type :key :ch (first a)})
   "quit" (fn [_ _] {:type :quit})})

(defn parse-qa-event-line
  "Parse one host automation event line: click X Y | key CHAR | quit"
  [line]
  (let [line (str/trim (str line))]
    (when (seq line)
      (let [[op a b] (str/split line #"\s+")
            parser (get qa-event-parsers op)]
        (if parser
          (parser a b)
          (throw (ex-info (str "unknown qa event: " line) {:line line})))))))

(defn load-qa-events
  [path]
  (->> (slurp path)
       str/split-lines
       (map parse-qa-event-line)
       (remove nil?)
       vec))
