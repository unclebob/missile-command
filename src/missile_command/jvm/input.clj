(ns missile-command.jvm.input
  "Pure host input mapping: UI events → core commands, CLI, telemetry. No Quil."
  (:require [clojure.string :as str]
            [missile-command.core :as core]))

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
      (case (:size-phase opts)
        :need-width [(assoc opts :width n :size-phase :need-height) (rest xs)]
        :need-height [(assoc opts :height n :size-phase :done) (rest xs)]
        nil))))

(defn parse-cli-args
  "Parse launch args: optional width height, then switches."
  ([args]
   (parse-cli-args args 800 600))
  ([args default-width default-height]
   (loop [xs (seq args)
          opts {:width default-width
                :height default-height
                :size-phase :need-width
                :qa-speed 1.0
                :destroy-batteries []
                :qa-events nil
                :qa-scenario nil
                :qa-targets []
                :qa-enemies []
                :qa-fireballs []}]
     (if-not xs
       (-> opts
           (dissoc :size-phase)
           (update :qa-telemetry? boolean))
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
  (let [id (:target-id e)]
    (str (name (:target-kind e)) ":"
         (if (keyword? id) (name id) (str id)))))

(defn- fireball-sim-fields
  [fireballs]
  (mapcat (fn [fb]
            [(str "center_x=" (:x fb))
             (str "center_y=" (:y fb))
             (str "radius=" (:radius fb))])
          fireballs))

(defn- enemy-sim-fields
  [enemies]
  (mapcat (fn [e]
            [(str "enemy_id=" (:id e))
             (str "enemy_kind=" (name (or (:enemy-kind e) :ballistic)))
             (str "enemy_x=" (:x e))
             (str "enemy_y=" (:y e))
             (str "enemy_origin_x=" (:x0 e))
             (str "enemy_origin_y=" (:y0 e))
             (str "enemy_target_x=" (:x1 e))
             (str "enemy_target_y=" (:y1 e))
             (str "enemy_target=" (enemy-target-label e))])
          enemies))

(defn- battery-ammo
  "Ammo for telemetry; missing missiles counts as 0."
  [battery]
  (long (if (nil? (:missiles battery)) 0 (:missiles battery))))

(defn- battery-sim-fields
  [state]
  (mapcat (fn [id]
            (let [b (core/battery state id)]
              [(str "battery_" (name id) "_destroyed=" (boolean (:destroyed? b)))
               (str "battery_" (name id) "_ammo=" (battery-ammo b))]))
          [:left :center :right]))

(defn- target-sim-fields
  [targets]
  (mapcat (fn [t]
            [(str "target_id=" (:id t))
             (str "target_x=" (:x t))
             (str "target_y=" (:y t))
             (str "destroyed=" (boolean (:destroyed? t)))])
          targets))

(defn- last-enemy-fate-fields
  [state]
  (when-let [f (core/last-enemy-fate state)]
    [(str "last_enemy_fate=" (name f))]))

(defn format-sim-telemetry-line
  "Periodic simulation snapshot line."
  [state]
  (let [missiles (core/defensive-missiles state)
        fireballs (core/fireballs state)
        enemies (core/enemy-missiles state)
        targets (core/destroyable-targets state)
        metrics (core/wave-schedule-metrics (core/wave state))]
    (str/join
     " "
     (concat [(str "qa-sim t=" (core/sim-time state))
              (str "wave=" (core/wave state))
              (str "wave_complete=" (boolean (core/wave-complete? state)))
              (str "wave_enemy_count=" (:enemy-count metrics))
              (str "wave_enemy_speed=" (:enemy-speed metrics))
              (str "score=" (core/score state))
              (str "final_score=" (core/final-score state))
              (str "multiplier=" (core/multiplier state))
              (str "bonus_cities=" (core/bonus-cities state))
              (str "bonus_city_earned_events=" (core/bonus-city-earned-events state))
              (str "screen=" (name (core/screen state)))
              (str "the_end=" (boolean (core/the-end? state)))
              ;; Single token for key=value telemetry (space would split fields).
              (str "end_message="
                   (str/replace (or (core/end-message state) "none") #"\s+" "_"))
              (str "title_game_name="
                   (str/replace (core/title-game-name-of state) #"\s+" "_"))
              (str "end_fireball_radius="
                   (double (or (:radius (core/end-fireball state)) 0.0)))
              (str "end_message_reveal=" (core/end-message-reveal state))
              (str "missiles_in_flight=" (count missiles))
              (str "fireballs=" (count fireballs))
              (str "enemy_missiles=" (count enemies))
              (str "cities_alive=" (count (core/living-cities state)))
              (str "hud_score=" (:score (core/hud state)))
              (str "hud_wave=" (:wave (core/hud state)))
              (str "hud_multiplier=" (:multiplier (core/hud state)))
              (str "hud_living_cities=" (:living-cities (core/hud state)))
              (str "hud_bonus_cities=" (:bonus-cities (core/hud state)))
              (str "hud_full=" (boolean (:full-playing-hud? (core/hud state))))]
             (battery-sim-fields state)
             (fireball-sim-fields fireballs)
             (enemy-sim-fields enemies)
             (target-sim-fields targets)
             (last-enemy-fate-fields state)))))

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
      (apply-scenario-score-and-bonus scenario)))

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
   "start" (fn [_ _] {:type :start})
   "confirm" (fn [_ _] {:type :confirm})
   "pause" (fn [_ _] {:type :pause})
   "resume" (fn [_ _] {:type :resume})
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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T15:45:57.236437-05:00", :module-hash "1978543927", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "-216504222"} {:id "def/default-fire-keys", :kind "def", :line 6, :end-line 9, :hash "534045824"} {:id "defn/key-char->battery", :kind "defn", :line 11, :end-line 14, :hash "-1717611502"} {:id "defn/fire-command", :kind "defn", :line 16, :end-line 18, :hash "1270260925"} {:id "defn/aim-command", :kind "defn", :line 20, :end-line 22, :hash "93232397"} {:id "defn/click-command", :kind "defn", :line 24, :end-line 26, :hash "427388192"} {:id "defn/key-char->command", :kind "defn", :line 28, :end-line 32, :hash "-766332071"} {:id "defn/escape-key?", :kind "defn", :line 34, :end-line 36, :hash "1460313293"} {:id "defn/parse-destroy-list", :kind "defn", :line 38, :end-line 45, :hash "700474553"} {:id "defn/parse-xy-pair", :kind "defn", :line 47, :end-line 52, :hash "-1833854870"} {:id "defn/parse-enemy-spec", :kind "defn", :line 54, :end-line 63, :hash "1564602249"} {:id "defn/parse-fireball-spec", :kind "defn", :line 65, :end-line 71, :hash "188453021"} {:id "defn-/int-token?", :kind "defn-", :line 73, :end-line 75, :hash "-1353543315"} {:id "defn-/parse-int-token", :kind "defn-", :line 77, :end-line 79, :hash "-512914460"} {:id "defn-/parse-float-token", :kind "defn-", :line 81, :end-line 83, :hash "322473366"} {:id "defn-/parse-qa-speed", :kind "defn-", :line 85, :end-line 92, :hash "-672414233"} {:id "def/switch-handlers", :kind "def", :line 94, :end-line 122, :hash "-1506412388"} {:id "defn-/apply-switch", :kind "defn-", :line 124, :end-line 128, :hash "821595473"} {:id "defn-/apply-size-token", :kind "defn-", :line 130, :end-line 138, :hash "-1102995660"} {:id "defn/parse-cli-args", :kind "defn", :line 140, :end-line 164, :hash "1057416244"} {:id "defn/parse-window-size", :kind "defn", :line 166, :end-line 169, :hash "1406683016"} {:id "defn/resize-if-needed", :kind "defn", :line 171, :end-line 176, :hash "-1111450443"} {:id "defn/battery-from-events", :kind "defn", :line 178, :end-line 184, :hash "-1027790243"} {:id "defn-/missile-vector-fields", :kind "defn-", :line 186, :end-line 191, :hash "-360289837"} {:id "defn/format-telemetry-line", :kind "defn", :line 193, :end-line 204, :hash "-324640397"} {:id "defn-/enemy-target-label", :kind "defn-", :line 206, :end-line 208, :hash "-1853462768"} {:id "defn-/fireball-sim-fields", :kind "defn-", :line 210, :end-line 216, :hash "-665089407"} {:id "defn-/enemy-sim-fields", :kind "defn-", :line 218, :end-line 230, :hash "119666309"} {:id "defn-/battery-ammo", :kind "defn-", :line 232, :end-line 235, :hash "-465332238"} {:id "defn-/battery-sim-fields", :kind "defn-", :line 237, :end-line 243, :hash "2005192070"} {:id "defn-/target-sim-fields", :kind "defn-", :line 245, :end-line 252, :hash "-1515145261"} {:id "defn-/last-enemy-fate-fields", :kind "defn-", :line 254, :end-line 257, :hash "74226316"} {:id "defn/format-sim-telemetry-line", :kind "defn", :line 259, :end-line 295, :hash "3433864"} {:id "defn/load-scenario-edn", :kind "defn", :line 297, :end-line 301, :hash "702073124"} {:id "defn-/apply-scenario-wave", :kind "defn-", :line 303, :end-line 307, :hash "-443119528"} {:id "defn-/apply-scenario-size", :kind "defn-", :line 309, :end-line 313, :hash "-409420687"} {:id "defn-/apply-scenario-battery", :kind "defn-", :line 315, :end-line 319, :hash "-987212579"} {:id "defn-/apply-scenario-batteries", :kind "defn-", :line 321, :end-line 323, :hash "-1182610143"} {:id "defn-/apply-scenario-cities", :kind "defn-", :line 325, :end-line 328, :hash "-1688876883"} {:id "defn-/apply-scenario-targets", :kind "defn-", :line 330, :end-line 334, :hash "1503255751"} {:id "defn-/apply-scenario-bonus-threshold", :kind "defn-", :line 336, :end-line 340, :hash "-471658285"} {:id "defn-/apply-scenario-score-and-bonus", :kind "defn-", :line 342, :end-line 349, :hash "585091910"} {:id "defn-/spawn-scenario-mirv", :kind "defn-", :line 351, :end-line 357, :hash "520740822"} {:id "defn-/spawn-with-optional-origin", :kind "defn-", :line 359, :end-line 363, :hash "-1706604714"} {:id "defn-/spawn-scenario-ballistic", :kind "defn-", :line 365, :end-line 378, :hash "73557027"} {:id "defn-/spawn-scenario-enemy", :kind "defn-", :line 379, :end-line 385, :hash "-646465745"} {:id "defn-/apply-scenario-enemies", :kind "defn-", :line 386, :end-line 391, :hash "287745192"} {:id "def/default-flyer-from", :kind "def", :line 393, :end-line 393, :hash "531809922"} {:id "def/default-flyer-to", :kind "def", :line 394, :end-line 394, :hash "1285077568"} {:id "def/default-flyer-speed", :kind "def", :line 395, :end-line 395, :hash "712485135"} {:id "def/first-drop-id", :kind "def", :line 397, :end-line 397, :hash "772225316"} {:id "defn-/scenario-flyer-drops", :kind "defn-", :line 399, :end-line 406, :hash "867803261"} {:id "defn-/apply-scenario-flyer", :kind "defn-", :line 408, :end-line 418, :hash "-2071231105"} {:id "defn-/apply-scenario-flyers", :kind "defn-", :line 420, :end-line 422, :hash "-136070644"} {:id "defn/apply-scenario", :kind "defn", :line 424, :end-line 436, :hash "-612531263"} {:id "defn/format-fireball-phase-line", :kind "defn", :line 438, :end-line 448, :hash "-1337292110"} {:id "defn/fireball-report-phase", :kind "defn", :line 450, :end-line 459, :hash "-303396465"} {:id "defn-/live-phase-events", :kind "defn-", :line 461, :end-line 472, :hash "437115347"} {:id "defn/detect-fireball-phase-events", :kind "defn", :line 474, :end-line 490, :hash "-1706900064"} {:id "def/qa-event-parsers", :kind "def", :line 492, :end-line 501, :hash "-558880826"} {:id "defn/parse-qa-event-line", :kind "defn", :line 503, :end-line 512, :hash "273909649"} {:id "defn/load-qa-events", :kind "defn", :line 514, :end-line 520, :hash "361312324"}]}
;; clj-mutate-manifest-end
