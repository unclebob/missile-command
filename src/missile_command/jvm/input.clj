(ns missile-command.jvm.input
  "Pure host input mapping: UI events → core commands, CLI, telemetry. No Quil."
  (:require [clojure.string :as str]
            [missile-command.core :as core]
            [missile-command.missiles :as missiles]))

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

(defn parse-xy-pair
  "Parse \"x,y\" into {:x :y}."
  [s]
  (let [[xs ys] (str/split (str s) #"," 2)]
    {:x (Integer/parseInt (str/trim xs))
     :y (Integer/parseInt (str/trim ys))}))

(defn parse-enemy-spec
  "Parse city:N or battery:left|center|right."
  [s]
  (let [[kind id] (str/split (str s) #":" 2)
        kind (str/trim kind)
        id (str/trim (str id))]
    (case kind
      "city" {:kind :city :id (Integer/parseInt id)}
      "battery" {:kind :battery :id (keyword id)}
      (throw (ex-info (str "unknown enemy spec: " s) {:spec s})))))

(defn parse-fireball-spec
  "Parse x,y,radius for a static QA fireball."
  [s]
  (let [[xs ys rs] (str/split (str s) #"," 3)]
    {:x (Integer/parseInt (str/trim xs))
     :y (Integer/parseInt (str/trim ys))
     :radius (Double/parseDouble (str/trim rs))}))

(defn- int-token?
  [s]
  (boolean (re-matches #"-?\d+" (str s))))

(defn- parse-int-token
  [s]
  (Integer/parseInt (str s)))

(defn- parse-float-token
  [s]
  (Double/parseDouble (str s)))

(defn- parse-qa-speed
  "Positive sim-time multiplier (default 1.0). Speeds wall-clock QA waits."
  [s]
  (let [n (parse-float-token s)]
    (when-not (and (double? n) (pos? n) (Double/isFinite n))
      (throw (ex-info (str "invalid --qa-speed (need positive number): " s)
                      {:arg s})))
    n))

(def ^:private switch-handlers
  {"--" (fn [opts xs] [opts (rest xs)])
   "--qa" (fn [opts xs] [(assoc opts :qa-telemetry? true) (rest xs)])
   "--qa-telemetry" (fn [opts xs] [(assoc opts :qa-telemetry? true) (rest xs)])
   "--destroy-batteries"
   (fn [opts xs]
     [(assoc opts :destroy-batteries (parse-destroy-list (second xs)))
      (drop 2 xs)])
   "--qa-events"
   (fn [opts xs]
     [(assoc opts :qa-events (second xs)) (drop 2 xs)])
   "--qa-scenario"
   (fn [opts xs]
     [(assoc opts :qa-scenario (second xs)) (drop 2 xs)])
   "--qa-speed"
   (fn [opts xs]
     [(assoc opts :qa-speed (parse-qa-speed (second xs))) (drop 2 xs)])
   "--qa-target"
   (fn [opts xs]
     [(update opts :qa-targets (fnil conj []) (parse-xy-pair (second xs)))
      (drop 2 xs)])
   "--qa-enemy"
   (fn [opts xs]
     [(update opts :qa-enemies (fnil conj []) (parse-enemy-spec (second xs)))
      (drop 2 xs)])
   "--qa-fireball"
   (fn [opts xs]
     [(update opts :qa-fireballs (fnil conj []) (parse-fireball-spec (second xs)))
      (drop 2 xs)])})

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
  "Parse launch args: optional width height, then switches."
  ([args]
   (parse-cli-args args 800 600))
  ([args default-width default-height]
   (loop [xs (seq args)
          opts {:width default-width
                :height default-height
                :qa-telemetry? false
                :qa-speed 1.0
                :destroy-batteries []
                :qa-events nil
                :qa-scenario nil
                :qa-targets []
                :qa-enemies []
                :qa-fireballs []}]
     (if-not xs
       (dissoc opts :width-set? :height-set?)
       (if-let [[opts' xs'] (or (apply-switch opts xs)
                                (apply-size-token opts xs))]
         (recur (seq xs') opts')
         (throw (ex-info (str "unknown launch argument: " (first xs))
                         {:arg (first xs)})))))))

(defn parse-window-size
  [args default-width default-height]
  (let [opts (parse-cli-args args default-width default-height)]
    [(:width opts) (:height opts)]))

(defn resize-if-needed
  [state width height resize-fn playfield-width-fn playfield-height-fn]
  (if (or (not= width (playfield-width-fn state))
          (not= height (playfield-height-fn state)))
    (resize-fn state width height)
    state))

(defn battery-from-events
  [events]
  (or (some (fn [e]
              (when (= :sfx/launch (:type e))
                (:battery e)))
            events)
      :none))

(defn- missile-vector-fields
  [m]
  [(str "origin_x=" (:x0 m))
   (str "origin_y=" (:y0 m))
   (str "target_x=" (:x1 m))
   (str "target_y=" (:y1 m))])

(defn format-telemetry-line
  "Build a parseable qa-fire telemetry line from a handle result."
  [result]
  (let [state (:state result)
        battery (name (battery-from-events (:events result)))
        missiles (core/defensive-missiles state)
        vectors (mapcat missile-vector-fields missiles)]
    (str/join
     " "
     (into [(str "qa-fire battery=" battery)
            (str "missiles_in_flight=" (count missiles))]
           vectors))))

(defn- enemy-target-label
  [e]
  (str (name (:target-kind e)) ":" (:target-id e)))

(defn format-sim-telemetry-line
  "Periodic simulation snapshot line."
  [state]
  (let [missiles (core/defensive-missiles state)
        fireballs (core/fireballs state)
        enemies (core/enemy-missiles state)
        targets (core/destroyable-targets state)
        cities-alive (count (core/living-cities state))
        fb-fields (mapcat (fn [fb]
                            [(str "center_x=" (:x fb))
                             (str "center_y=" (:y fb))
                             (str "radius=" (:radius fb))])
                          fireballs)
        enemy-fields (mapcat (fn [e]
                               [(str "enemy_id=" (:id e))
                                (str "enemy_kind=" (name (or (:enemy-kind e) :ballistic)))
                                (str "enemy_x=" (:x e))
                                (str "enemy_y=" (:y e))
                                (str "enemy_origin_x=" (:x0 e))
                                (str "enemy_origin_y=" (:y0 e))
                                (str "enemy_target_x=" (:x1 e))
                                (str "enemy_target_y=" (:y1 e))
                                (str "enemy_target=" (enemy-target-label e))])
                             enemies)
        bat-fields (mapcat (fn [id]
                             (let [b (core/battery state id)]
                               [(str "battery_" (name id) "_destroyed="
                                     (boolean (:destroyed? b)))
                                (str "battery_" (name id) "_ammo="
                                     (long (or (:missiles b) 0)))]))
                           [:left :center :right])
        metrics (core/wave-schedule-metrics (core/wave state))
        tgt-fields (mapcat (fn [t]
                             [(str "target_id=" (:id t))
                              (str "target_x=" (:x t))
                              (str "target_y=" (:y t))
                              (str "destroyed=" (boolean (:destroyed? t)))])
                           targets)
        fate (when-let [f (core/last-enemy-fate state)]
               [(str "last_enemy_fate=" (name f))])]
    (str/join
     " "
     (concat [(str "qa-sim t=" (core/sim-time state))
              (str "wave=" (core/wave state))
              (str "wave_complete=" (boolean (core/wave-complete? state)))
              (str "wave_enemy_count=" (:enemy-count metrics))
              (str "wave_enemy_speed=" (:enemy-speed metrics))
              (str "score=" (core/score state))
              (str "multiplier=" (core/multiplier state))
              (str "bonus_cities=" (core/bonus-cities state))
              (str "bonus_city_earned_events=" (core/bonus-city-earned-events state))
              (str "missiles_in_flight=" (count missiles))
              (str "fireballs=" (count fireballs))
              (str "enemy_missiles=" (count enemies))
              (str "cities_alive=" cities-alive)]
             bat-fields
             fb-fields
             enemy-fields
             tgt-fields
             fate))))

(defn load-scenario-edn
  "Read a QA scenario EDN map from path."
  [path]
  (when path
    (read-string (slurp path))))

(defn apply-scenario
  "Apply documented scenario keys onto a new-game state."
  [state scenario]
  (let [state (if-let [w (:wave scenario)]
                (core/set-wave state w)
                state)
        state (if-let [t (:bonus-city-threshold scenario)]
                (core/set-bonus-city-threshold state t)
                state)
        state (if-let [w (:width scenario)]
                (core/resize state w (or (:height scenario) (core/playfield-height state)))
                state)
        state (reduce (fn [s [id opts]]
                        (let [s (if (:destroyed opts)
                                  (core/destroy-battery s id)
                                  s)]
                          (if (contains? opts :ammo)
                            (core/set-battery-ammo s id (:ammo opts))
                            s)))
                      state
                      (or (:batteries scenario) {}))
        state (reduce core/destroy-city state
                      (or (get-in scenario [:cities :destroyed]) []))
        state (reduce (fn [s t]
                        (core/add-destroyable-target s (:x t) (:y t)))
                      state
                      (or (:targets scenario) []))
        enemies (or (:enemies scenario) [])
        state (if (seq enemies)
                (assoc state :wave-had-enemies? true :wave-complete? false)
                state)
        state (reduce (fn [s e]
                        (let [[kind id] (:target e)
                              origin (:origin e)
                              [ox oy] (when origin [(first origin) (second origin)])
                              enemy-kind (:kind e)]
                          (cond
                            (= :mirv enemy-kind)
                            (core/spawn-mirv-targeting-city
                             s id
                             (or (:child-count e) 3)
                             (or (:split-progress e) 0.5))

                            (= :smart enemy-kind)
                            (core/spawn-smart-bomb-targeting-city s id)

                            (= kind :city)
                            (if origin
                              (core/spawn-enemy-targeting-city-from s ox oy id)
                              (core/spawn-enemy-targeting-city s id))

                            (= kind :battery)
                            (if origin
                              (core/spawn-enemy-targeting-battery-from s ox oy id)
                              (core/spawn-enemy-targeting-battery s id))

                            :else s)))
                      state
                      enemies)
        state (if (contains? scenario :bonus-cities)
                (core/set-bonus-city-reserve state (:bonus-cities scenario))
                state)
        state (if (contains? scenario :score)
                (core/set-score state (:score scenario))
                state)]
    state))

(defn format-fireball-phase-line
  "Phase timing line for one fireball."
  [state fireball phase]
  (str/join
   " "
   [(str "qa-fireball id=" (:id fireball))
    (str "phase=" (name phase))
    (str "t=" (core/sim-time state))
    (str "center_x=" (:x fireball))
    (str "center_y=" (:y fireball))
    (str "radius=" (double (:radius fireball 0.0)))]))

(defn fireball-report-phase
  "Map fireball age to a reportable QA phase: start (incl. expand), shrink, or end."
  [fireball]
  (let [age (double (:age fireball 0.0))
        expand (double (:expand-seconds fireball))
        contract (double (:contract-seconds fireball 0.0))]
    (cond
      (< age expand) :start
      (< age (+ expand contract)) :shrink
      :else :end)))

(defn- live-phase-events
  [prev-phases fireball]
  (let [id (:id fireball)
        prev (get prev-phases id)
        phase (fireball-report-phase fireball)]
    (cond
      (= prev phase) []
      (and (= phase :shrink) (not (#{:max :shrink} prev)))
      [{:id id :phase :max :fireball fireball}
       {:id id :phase :shrink :fireball fireball}]
      :else
      [{:id id :phase phase :fireball fireball}])))

(defn detect-fireball-phase-events
  "Given previous phase map id->phase and current fireballs, return
  [events next-phase-map] where events are {:id :phase :fireball}."
  [prev-phases fireballs]
  (let [current-ids (set (map :id fireballs))
        ended (for [[id phase] prev-phases
                    :when (and (not (current-ids id))
                               (not= phase :end))]
                {:id id :phase :end :fireball {:id id :x 0 :y 0 :radius 0.0}})
        live (mapcat #(live-phase-events prev-phases %) fireballs)
        events (vec (concat ended live))
        next-map (reduce (fn [m e] (assoc m (:id e) (:phase e)))
                         (into {} (map (fn [fb]
                                         [(:id fb) (fireball-report-phase fb)])
                                       fireballs))
                         events)]
    [events next-map]))

(def ^:private qa-event-parsers
  {"click" (fn [a b] {:type :click :x (parse-int-token a) :y (parse-int-token b)})
   "aim" (fn [a b] {:type :aim :x (parse-int-token a) :y (parse-int-token b)})
   "key" (fn [a _] {:type :key :ch (first a)})
   "wait" (fn [a _] {:type :wait :seconds (parse-float-token a)})
   "enemy" (fn [a _] {:type :enemy :spec (parse-enemy-spec a)})
   "fireball" (fn [a _] {:type :fireball :spec (parse-fireball-spec a)})
   "quit" (fn [_ _] {:type :quit})})

(defn parse-qa-event-line
  "Parse host automation: click X Y | aim X Y | key CHAR | wait SECONDS | quit"
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
