(ns missile-command.jvm.scenario
  "QA scenario EDN apply and host automation event files. Pure; no Quil."
  (:require [clojure.string :as str]
            [missile-command.core :as core]
            [missile-command.jvm.cli :as cli]))

(defn load-scenario-edn
  "Read a QA scenario EDN map from path."
  [path]
  (when path
    (read-string (slurp path))))

(defn- apply-scenario-wave
  [state scenario]
  (if-let [w (:wave scenario)]
    (core/set-wave state w)
    state))

(defn- apply-scenario-screen
  "Optional :screen keyword (e.g. :playing) for staged host setups."
  [state scenario]
  (if-let [s (:screen scenario)]
    (assoc state :screen (keyword s))
    state))

(defn- apply-scenario-wave-attack
  "Optional :wave-attack k begins that sequential salvo (1-based)."
  [state scenario]
  (if-let [k (:wave-attack scenario)]
    (core/begin-wave-attack state (long k))
    state))

(defn- apply-scenario-rng-seed
  "Optional :rng-seed for deterministic sky origins."
  [state scenario]
  (if-let [seed (:rng-seed scenario)]
    (core/with-rng-seed state (long seed))
    state))

(defn- apply-scenario-size
  [state scenario]
  (if-let [w (:width scenario)]
    (core/resize state w (or (:height scenario) (core/playfield-height state)))
    state))

(defn- apply-scenario-battery
  [state [id opts]]
  (cond-> state
    (:destroyed opts) (core/destroy-battery id)
    (contains? opts :ammo) (core/set-battery-ammo id (:ammo opts))))

(defn- apply-scenario-batteries
  [state scenario]
  (reduce apply-scenario-battery state (or (:batteries scenario) {})))

(defn- apply-scenario-cities
  [state scenario]
  (reduce core/destroy-city state
          (or (get-in scenario [:cities :destroyed]) [])))

(defn- apply-scenario-targets
  [state scenario]
  (reduce (fn [s t] (core/add-destroyable-target s (:x t) (:y t)))
          state
          (or (:targets scenario) [])))

(defn- apply-scenario-bonus-threshold
  [state scenario]
  (if-let [t (:bonus-city-threshold scenario)]
    (core/set-bonus-city-threshold state t)
    state))

(defn- apply-scenario-score-and-bonus
  "Apply explicit score/reserve after enemies so score sync sees final score."
  [state scenario]
  (cond-> state
    (contains? scenario :bonus-cities)
    (core/set-bonus-city-reserve (:bonus-cities scenario))
    (contains? scenario :score)
    (core/set-score (:score scenario))))

(defn- apply-scenario-high-scores
  "Seed table capacity and entries from scenario keys."
  [state scenario]
  (let [state (if (contains? scenario :high-score-capacity)
                (core/set-high-score-capacity state (:high-score-capacity scenario))
                state)
        entries (or (:high-scores scenario) [])]
    (reduce (fn [s e]
              (core/add-high-score-entry s
                                         (str (:initials e))
                                         (long (:score e))))
            (if (contains? scenario :high-scores)
              (assoc state :high-scores [])
              state)
            entries)))

(defn- apply-scenario-mute
  [state scenario]
  (if (contains? scenario :mute)
    (core/set-mute state (:mute scenario))
    state))

(defn- apply-scenario-difficulty
  [state scenario]
  (if (contains? scenario :difficulty)
    (core/set-difficulty state (:difficulty scenario))
    state))

(defn- apply-scenario-options-map
  [state scenario]
  (if (map? (:options scenario))
    (core/import-settings
     state
     (merge (core/export-settings state)
            {:options (merge (core/game-options state)
                             (:options scenario))}))
    state))

(defn- apply-scenario-options
  [state scenario]
  (-> state
      (apply-scenario-mute scenario)
      (apply-scenario-difficulty scenario)
      (apply-scenario-options-map scenario)))

(defn- spawn-scenario-mirv
  [state e]
  (let [[_ id] (:target e)]
    (core/spawn-mirv-targeting-city
     state id
     (or (:child-count e) 3)
     (or (:split-progress e) 0.5))))

(defn- spawn-with-optional-origin
  [state origin id spawn-from spawn-default]
  (if origin
    (spawn-from state (first origin) (second origin) id)
    (spawn-default state id)))

(defn- spawn-scenario-ballistic
  [state e]
  (let [[kind id] (:target e)
        origin (:origin e)]
    (case kind
      :city (spawn-with-optional-origin
             state origin id
             core/spawn-enemy-targeting-city-from
             core/spawn-enemy-targeting-city)
      :battery (spawn-with-optional-origin
                state origin id
                core/spawn-enemy-targeting-battery-from
                core/spawn-enemy-targeting-battery)
      state)))
(defn- spawn-scenario-enemy
  "Spawn one scenario enemy, honoring MIRV/smart kinds and optional angled origin."
  [state e]
  (case (:kind e)
    :mirv (spawn-scenario-mirv state e)
    :smart (core/spawn-smart-bomb-targeting-city state (second (:target e)))
    (spawn-scenario-ballistic state e)))
(defn- apply-scenario-enemies
  [state scenario]
  (let [enemies (or (:enemies scenario) [])]
    (if (seq enemies)
      (reduce spawn-scenario-enemy state enemies)
      state)))

(def ^:private default-flyer-from [0 80])
(def ^:private default-flyer-to [800 80])
(def ^:private default-flyer-speed 100)

(def ^:private first-drop-id 0)

(defn- scenario-flyer-drops
  [drops]
  (mapv (fn [i d]
          {:id (+ first-drop-id i)
           :at-progress (double (get d :at-progress 0.5))
           :target (get d :target [:city 0])})
        (range (count drops))
        drops))

(defn- apply-scenario-flyer
  [state flyer]
  (let [[x0 y0] (get flyer :from default-flyer-from)
        [x1 y1] (get flyer :to default-flyer-to)
        state (core/spawn-flyer state
                                (get flyer :kind :bomber)
                                x0 y0 x1 y1
                                (get flyer :speed default-flyer-speed))]
    (if-let [drops (seq (:drops flyer))]
      (core/set-flyer-drops state (scenario-flyer-drops drops))
      state)))

(defn- apply-scenario-flyers
  [state scenario]
  (reduce apply-scenario-flyer state (or (:flyers scenario) [])))

(defn apply-scenario
  "Apply documented scenario keys onto a new-game state."
  [state scenario]
  (-> state
      (apply-scenario-wave scenario)
      (apply-scenario-bonus-threshold scenario)
      (apply-scenario-size scenario)
      (apply-scenario-batteries scenario)
      (apply-scenario-cities scenario)
      (apply-scenario-targets scenario)
      (apply-scenario-enemies scenario)
      (apply-scenario-flyers scenario)
      (apply-scenario-score-and-bonus scenario)
      (apply-scenario-high-scores scenario)
      (apply-scenario-options scenario)
      (apply-scenario-screen scenario)
      (apply-scenario-rng-seed scenario)
      (apply-scenario-wave-attack scenario)))

(def ^:private qa-event-parsers
  {"click" (fn [a b] {:type :click :x (cli/parse-int-token a) :y (cli/parse-int-token b)})
   "aim" (fn [a b] {:type :aim :x (cli/parse-int-token a) :y (cli/parse-int-token b)})
   "key" (fn [a _] {:type :key :ch (first a)})
   "wait" (fn [a _] {:type :wait :seconds (cli/parse-float-token a)})
   "enemy" (fn [a _] {:type :enemy :spec (cli/parse-enemy-spec a)})
   "fireball" (fn [a _] {:type :fireball :spec (cli/parse-fireball-spec a)})
   "start" (fn [_ _] {:type :start})
   "confirm" (fn [_ _] {:type :confirm})
   "pause" (fn [_ _] {:type :pause})
   "resume" (fn [_ _] {:type :resume})
   "open-high-scores" (fn [_ _] {:type :open-high-scores})
   "close-high-scores" (fn [_ _] {:type :close-high-scores})
   "initials" (fn [a _] {:type :submit-high-score :initials (str a)})
   "open-options" (fn [_ _] {:type :open-options})
   "leave-options" (fn [_ _] {:type :leave-options})
   "mute" (fn [a _] {:type :set-mute
                     :mute (contains? #{"true" "1" "yes" "on"}
                                      (str/lower-case (str a)))})
   "difficulty" (fn [a _] {:type :set-difficulty :difficulty (str a)})
   "bind-fire" (fn [a b] {:type :bind-fire-key
                          :battery (keyword a)
                          :key (str b)})
   "quit" (fn [_ _] {:type :quit})})

(defn parse-qa-event-line
  "Parse host automation: click X Y | aim X Y | key CHAR | wait SECONDS | quit
  Also: open-high-scores | close-high-scores | initials ABC"
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
