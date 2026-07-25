(ns missile-command.jvm.telemetry
  "QA telemetry formatters for the JVM host. Pure; no Quil."
  (:require [clojure.string :as str]
            [missile-command.core :as core]))

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

(defn- high-score-rank-fields
  "Emit first few ranks as hs_rankN_initials / hs_rankN_score."
  [state]
  (mapcat (fn [i e]
            (let [n (inc i)]
              [(str "hs_rank" n "_initials=" (:initials e))
               (str "hs_rank" n "_score=" (long (:score e)))]))
          (range)
          (take 10 (core/high-score-table state))))

(defn- high-score-sim-fields
  [state]
  (let [pending (core/pending-high-score state)
        submitted (core/submitted-high-score-initials state)
        draft (or (:initials-draft state) "")]
    (concat [(str "high_score_count=" (count (core/high-score-table state)))
             (str "high_score_capacity=" (core/high-score-capacity state))
             (str "pending_high_score=" (if pending (long pending) "none"))
             (str "submitted_initials=" (or submitted "none"))
             (str "initials_draft=" (if (seq draft) draft "none"))]
            (high-score-rank-fields state))))

(defn- fire-keys-field
  [state battery]
  (str/join "," (sort (map str (get-in (core/game-options state)
                                       [:keys :fire battery] #{})))))

(defn- options-sim-fields
  [state]
  (let [opts (core/game-options state)]
    [(str "mute=" (boolean (:mute opts)))
     (str "difficulty=" (name (or (:difficulty opts) :arcade)))
     (str "fire_key_left=" (fire-keys-field state :left))
     (str "fire_key_center=" (fire-keys-field state :center))
     (str "fire_key_right=" (fire-keys-field state :right))
     (str "pause_keys="
          (str/join "," (sort (map str (get-in opts [:keys :pause] #{})))))]))

(defn- sfx-type-token
  [type]
  (if (namespace type)
    (str (namespace type) "/" (name type))
    (name type)))

(defn- sfx-sim-fields
  [state]
  (let [events (or (:sfx-events state) [])
        types (map (comp sfx-type-token :type) (take-last 8 events))]
    [(str "sfx_count=" (count events))
     (str "sfx_last=" (if (seq types)
                        (str/join "," types)
                        "none"))]))

(defn- inactive-wave-banner-fields
  []
  [(str "banner_text=none")
   (str "banner_phase=none")
   (str "banner_x=0")
   (str "banner_y=0")
   (str "banner_announced_wave=0")])

(defn- active-wave-banner-fields
  [state]
  (let [pos (core/wave-banner-text-position state)
        ph (core/wave-banner-phase state)]
    [(str "banner_text="
          (str/replace (or (core/wave-banner-text state) "") #"\s+" "_"))
     (str "banner_phase=" (if ph (name ph) "none"))
     (str "banner_x=" (double (:x pos)))
     (str "banner_y=" (double (:y pos)))
     (str "banner_announced_wave="
          (core/wave-banner-announced-wave state))]))

(defn- wave-banner-sim-fields
  [state]
  (if (core/wave-banner? state)
    (active-wave-banner-fields state)
    (inactive-wave-banner-fields)))

(defn format-sim-telemetry-line
  "Periodic simulation snapshot line."
  [state]
  (let [missiles (core/defensive-missiles state)
        fireballs (core/fireballs state)
        enemies (core/enemy-missiles state)
        targets (core/destroyable-targets state)
        metrics (core/wave-schedule-metrics-for state (core/wave state))
        hud (core/hud state)]
    (str/join
     " "
     (concat [(str "qa-sim t=" (core/sim-time state))
              (str "wave=" (core/wave state))
              (str "wave_complete=" (boolean (core/wave-complete? state)))
              (str "wave_attack="
                   (if-let [a (:wave-attack state)] (long a) "none"))
              (str "wave_attacks_per_wave=3")
              (str "wave_enemy_count=" (:enemy-count metrics))
              (str "wave_enemy_speed=" (:enemy-speed metrics))
              (str "wave_mirv_count=" (long (:mirv-count metrics 0)))
              (str "wave_smart_bomb_count=" (long (:smart-bomb-count metrics 0)))
              (str "wave_bomber_count=" (long (:bomber-count metrics 0)))
              (str "wave_satellite_count=" (long (:satellite-count metrics 0)))
              (str "mirv_parents=" (count (core/mirv-parents state)))
              (str "smart_bombs=" (count (core/smart-bombs state)))
              (str "flyers_bomber=" (count (core/flyers-of-kind state :bomber)))
              (str "flyers_satellite=" (count (core/flyers-of-kind state :satellite)))
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
              (str "ballistic_missiles="
                   (count (filter #(= :ballistic
                                      (or (:enemy-kind %) :ballistic))
                                  enemies)))
              (str "cities_alive=" (count (core/living-cities state)))
              (str "hud_score=" (:score hud))
              (str "hud_wave=" (:wave hud))
              (str "hud_multiplier=" (:multiplier hud))
              (str "hud_living_cities=" (:living-cities hud))
              (str "hud_bonus_cities=" (:bonus-cities hud))
              (str "hud_full=" (boolean (:full-playing-hud? hud)))]
             (high-score-sim-fields state)
             (options-sim-fields state)
             (sfx-sim-fields state)
             (wave-banner-sim-fields state)
             (battery-sim-fields state)
             (fireball-sim-fields fireballs)
             (enemy-sim-fields enemies)
             (target-sim-fields targets)
             (last-enemy-fate-fields state)))))

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

