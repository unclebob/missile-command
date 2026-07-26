(ns missile-command.jvm.scenario
  "QA scenario EDN apply and host automation event files. Pure; no Quil."
  (:require [clojure.string :as str]
            [missile-command.core :as core]
            [missile-command.testing :as testing]
            [missile-command.jvm.cli :as cli]))

(defn load-scenario-edn
  "Read a QA scenario EDN map from path."
  [path]
  (when path
    (read-string (slurp path))))

(defn- apply-scenario-wave
  [state scenario]
  (if-let [w (:wave scenario)]
    (testing/set-wave state w)
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
    (testing/begin-wave-attack state (long k))
    state))

(defn- apply-scenario-rng-seed
  "Optional :rng-seed for deterministic sky origins."
  [state scenario]
  (if-let [seed (:rng-seed scenario)]
    (testing/with-rng-seed state (long seed))
    state))

(defn- apply-scenario-size
  [state scenario]
  (if-let [w (:width scenario)]
    (core/resize state w (or (:height scenario) (core/playfield-height state)))
    state))

(defn- apply-scenario-battery
  [state [id opts]]
  (cond-> state
    (:destroyed opts) (testing/destroy-battery id)
    (contains? opts :ammo) (testing/set-battery-ammo id (:ammo opts))))

(defn apply-destroy-batteries
  "Apply QA launch option battery destruction."
  [state battery-ids]
  (reduce testing/destroy-battery state battery-ids))

(defn- apply-scenario-batteries
  [state scenario]
  (reduce apply-scenario-battery state (or (:batteries scenario) {})))

(defn- apply-scenario-cities
  [state scenario]
  (reduce testing/destroy-city state
          (or (get-in scenario [:cities :destroyed]) [])))

(defn- apply-scenario-targets
  [state scenario]
  (reduce (fn [s t] (testing/add-destroyable-target s (:x t) (:y t)))
          state
          (or (:targets scenario) [])))

(defn apply-qa-targets
  "Apply QA launch option destroyable targets."
  [state targets]
  (reduce (fn [s {:keys [x y]}]
            (testing/add-destroyable-target s x y))
          state
          targets))

(defn- apply-scenario-bonus-threshold
  [state scenario]
  (if-let [t (:bonus-city-threshold scenario)]
    (testing/set-bonus-city-threshold state t)
    state))

(defn- apply-scenario-score-and-bonus
  "Apply explicit score/reserve after enemies so score sync sees final score."
  [state scenario]
  (cond-> state
    (contains? scenario :bonus-cities)
    (testing/set-bonus-city-reserve (:bonus-cities scenario))
    (contains? scenario :score)
    (testing/set-score (:score scenario))))

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
    (testing/spawn-mirv-targeting-city
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
             testing/spawn-enemy-targeting-city-from
             testing/spawn-enemy-targeting-city)
      :battery (spawn-with-optional-origin
                state origin id
                testing/spawn-enemy-targeting-battery-from
                testing/spawn-enemy-targeting-battery)
      state)))
(defn- spawn-scenario-enemy
  "Spawn one scenario enemy, honoring MIRV/smart kinds and optional angled origin."
  [state e]
  (case (:kind e)
    :mirv (spawn-scenario-mirv state e)
    :smart (testing/spawn-smart-bomb-targeting-city state (second (:target e)))
    (spawn-scenario-ballistic state e)))

(defn apply-enemy-spec
  "Apply a QA launch/event enemy spec."
  [state {:keys [kind id]}]
  (case kind
    :city (testing/spawn-enemy-targeting-city state id)
    :battery (testing/spawn-enemy-targeting-battery state id)
    state))

(defn apply-qa-enemies
  "Apply QA launch option enemy specs."
  [state enemies]
  (reduce apply-enemy-spec state enemies))

(defn apply-qa-fireballs
  "Apply QA launch option static fireballs."
  [state fireballs]
  (reduce (fn [s {:keys [x y radius]}]
            (testing/add-static-fireball s x y radius))
          state
          fireballs))

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
        state (testing/spawn-flyer state
                                   (get flyer :kind :bomber)
                                   x0 y0 x1 y1
                                   (get flyer :speed default-flyer-speed))]
    (if-let [drops (seq (:drops flyer))]
      (testing/set-flyer-drops state (scenario-flyer-drops drops))
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
   "reset-scenario" (fn [a _] {:type :reset-scenario :path (str a)})
   "quit" (fn [_ _] {:type :quit})})

(defn parse-qa-event-line
  "Parse host automation: click X Y | aim X Y | key CHAR | wait SECONDS | quit
  Also: open-high-scores | close-high-scores | initials ABC | reset-scenario PATH"
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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:19:06.701656-05:00", :module-hash "1278718815", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 6, :hash "1620897903"} {:id "defn/load-scenario-edn", :kind "defn", :line 8, :end-line 12, :hash "702073124"} {:id "defn-/apply-scenario-wave", :kind "defn-", :line 14, :end-line 18, :hash "665021942"} {:id "defn-/apply-scenario-screen", :kind "defn-", :line 20, :end-line 25, :hash "448966260"} {:id "defn-/apply-scenario-wave-attack", :kind "defn-", :line 27, :end-line 32, :hash "-141594200"} {:id "defn-/apply-scenario-rng-seed", :kind "defn-", :line 34, :end-line 39, :hash "-1194143982"} {:id "defn-/apply-scenario-size", :kind "defn-", :line 41, :end-line 45, :hash "-409420687"} {:id "defn-/apply-scenario-battery", :kind "defn-", :line 47, :end-line 51, :hash "-1270830876"} {:id "defn/apply-destroy-batteries", :kind "defn", :line 53, :end-line 56, :hash "-1238041924"} {:id "defn-/apply-scenario-batteries", :kind "defn-", :line 58, :end-line 60, :hash "-1182610143"} {:id "defn-/apply-scenario-cities", :kind "defn-", :line 62, :end-line 65, :hash "-845527766"} {:id "defn-/apply-scenario-targets", :kind "defn-", :line 67, :end-line 71, :hash "-1250784535"} {:id "defn/apply-qa-targets", :kind "defn", :line 73, :end-line 79, :hash "1114579005"} {:id "defn-/apply-scenario-bonus-threshold", :kind "defn-", :line 81, :end-line 85, :hash "880924155"} {:id "defn-/apply-scenario-score-and-bonus", :kind "defn-", :line 87, :end-line 94, :hash "1875400561"} {:id "defn-/apply-scenario-high-scores", :kind "defn-", :line 96, :end-line 110, :hash "-1208080297"} {:id "defn-/apply-scenario-mute", :kind "defn-", :line 112, :end-line 116, :hash "-382843912"} {:id "defn-/apply-scenario-difficulty", :kind "defn-", :line 118, :end-line 122, :hash "306163409"} {:id "defn-/apply-scenario-options-map", :kind "defn-", :line 124, :end-line 132, :hash "18991043"} {:id "defn-/apply-scenario-options", :kind "defn-", :line 134, :end-line 139, :hash "27805209"} {:id "defn-/spawn-scenario-mirv", :kind "defn-", :line 141, :end-line 147, :hash "-1005955178"} {:id "defn-/spawn-with-optional-origin", :kind "defn-", :line 149, :end-line 153, :hash "-1706604714"} {:id "defn-/spawn-scenario-ballistic", :kind "defn-", :line 155, :end-line 168, :hash "-1951507746"} {:id "defn-/spawn-scenario-enemy", :kind "defn-", :line 169, :end-line 175, :hash "1961476205"} {:id "defn/apply-enemy-spec", :kind "defn", :line 177, :end-line 183, :hash "347627411"} {:id "defn/apply-qa-enemies", :kind "defn", :line 185, :end-line 188, :hash "758172028"} {:id "defn/apply-qa-fireballs", :kind "defn", :line 190, :end-line 196, :hash "-55288772"} {:id "defn-/apply-scenario-enemies", :kind "defn-", :line 198, :end-line 203, :hash "287745192"} {:id "def/default-flyer-from", :kind "def", :line 205, :end-line 205, :hash "531809922"} {:id "def/default-flyer-to", :kind "def", :line 206, :end-line 206, :hash "1285077568"} {:id "def/default-flyer-speed", :kind "def", :line 207, :end-line 207, :hash "712485135"} {:id "def/first-drop-id", :kind "def", :line 209, :end-line 209, :hash "772225316"} {:id "defn-/scenario-flyer-drops", :kind "defn-", :line 211, :end-line 218, :hash "867803261"} {:id "defn-/apply-scenario-flyer", :kind "defn-", :line 220, :end-line 230, :hash "-535868616"} {:id "defn-/apply-scenario-flyers", :kind "defn-", :line 232, :end-line 234, :hash "-136070644"} {:id "defn/apply-scenario", :kind "defn", :line 236, :end-line 253, :hash "540794563"} {:id "def/qa-event-parsers", :kind "def", :line 255, :end-line 278, :hash "-886446121"} {:id "defn/parse-qa-event-line", :kind "defn", :line 280, :end-line 290, :hash "-1292649055"} {:id "defn/load-qa-events", :kind "defn", :line 292, :end-line 298, :hash "361312324"}]}
;; clj-mutate-manifest-end
