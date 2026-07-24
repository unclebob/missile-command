(ns missile-command.jvm.sketch
  "Quil sketch: route mouse/keyboard UI events into pure core commands."
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [quil.applet :as applet]
            [missile-command.core :as core]
            [missile-command.jvm.input :as input]
            [missile-command.jvm.render :as render]
            [missile-command.jvm.window :as window]))

(def default-width 800)
(def default-height 600)

(defonce launch-options
  (atom {:qa-telemetry? false
         :destroy-batteries []
         :qa-events nil
         :qa-targets []
         :qa-enemies []
         :qa-fireballs []
         :launch-anchor nil
         :restore-focus-app nil}))

(defonce pending-qa-events (atom []))
(defonce fireball-phases (atom {}))
(defonce last-frame-ms (atom nil))

(defn configure!
  [opts]
  (reset! launch-options (select-keys opts [:qa-telemetry?
                                            :destroy-batteries
                                            :qa-events
                                            :qa-targets
                                            :qa-enemies
                                            :qa-fireballs
                                            :launch-anchor
                                            :restore-focus-app]))
  (reset! pending-qa-events
          (if-let [path (:qa-events opts)]
            (input/load-qa-events path)
            []))
  (reset! fireball-phases {})
  (reset! last-frame-ms nil))

(defn- emit!
  [line]
  (when (:qa-telemetry? @launch-options)
    (println line)
    (flush)))

(defn- emit-telemetry-fire!
  [result]
  (emit! (input/format-telemetry-line result)))

(defn- emit-fireball-phases!
  [state]
  (let [[events next-map] (input/detect-fireball-phase-events
                           @fireball-phases
                           (core/fireballs state))]
    (reset! fireball-phases next-map)
    (doseq [e events]
      (emit! (input/format-fireball-phase-line state (:fireball e) (:phase e))))))

(defn- apply-handle
  [state command]
  (let [result (core/handle state command)]
    (when (#{:fire :click} (:type command))
      (emit-telemetry-fire! result))
    (:state result)))

(defn- apply-destroy-options
  [state]
  (reduce (fn [s id]
            (core/destroy-battery s id))
          state
          (:destroy-batteries @launch-options)))

(defn- apply-qa-targets
  [state]
  (reduce (fn [s {:keys [x y]}]
            (core/add-destroyable-target s x y))
          state
          (:qa-targets @launch-options)))

(defn- apply-enemy-spec
  [state {:keys [kind id]}]
  (case kind
    :city (core/spawn-enemy-targeting-city state id)
    :battery (core/spawn-enemy-targeting-battery state id)
    state))

(defn- apply-qa-enemies
  [state]
  (reduce apply-enemy-spec state (:qa-enemies @launch-options)))

(defn- apply-qa-fireballs
  [state]
  (reduce (fn [s {:keys [x y radius]}]
            (core/add-static-fireball s x y radius))
          state
          (:qa-fireballs @launch-options)))

(defn- configure-display!
  []
  (try
    (let [surface (.getSurface (applet/current-applet))
          anchor (:launch-anchor @launch-options)
          prev (:restore-focus-app @launch-options)]
      (window/place-on-launch-screen! surface (q/width) (q/height) anchor prev))
    (catch Exception e
      (binding [*out* *err*]
        (println "window placement skipped:" (.getMessage e))))))

(defn setup
  []
  (q/frame-rate 60)
  (q/no-cursor)
  (configure-display!)
  (reset! last-frame-ms (System/currentTimeMillis))
  (-> (core/new-game {:width (q/width) :height (q/height)})
      apply-destroy-options
      apply-qa-targets
      apply-qa-enemies
      apply-qa-fireballs))

(defn- frame-dt-seconds
  []
  (let [now (System/currentTimeMillis)
        prev (or @last-frame-ms now)
        raw (/ (double (- now prev)) 1000.0)]
    (reset! last-frame-ms now)
    (max 0.0 (min raw 0.25))))

(defn- tick-state
  [state]
  (let [dt (frame-dt-seconds)
        result (core/tick state dt)
        state' (:state result)]
    (emit-fireball-phases! state')
    (when (and (:qa-telemetry? @launch-options)
               (or (seq (core/fireballs state'))
                   (seq (core/enemy-missiles state'))
                   (seq (core/destroyable-targets state'))))
      (emit! (input/format-sim-telemetry-line state')))
    ;; One final snapshot when an enemy just resolved and nothing is flying.
    (when (and (:qa-telemetry? @launch-options)
               (core/last-enemy-fate state')
               (empty? (core/enemy-missiles state'))
               (empty? (core/fireballs state'))
               (not= (:last-emitted-fate @launch-options)
                     (core/last-enemy-fate state')))
      (emit! (input/format-sim-telemetry-line state'))
      (swap! launch-options assoc :last-emitted-fate (core/last-enemy-fate state')))
    state'))

(defn- drain-one-qa-event
  [state]
  (let [events @pending-qa-events]
    (if (empty? events)
      state
      (let [ev (first events)]
        (case (:type ev)
          :wait
          (let [until (or (:until-ms ev)
                          (+ (System/currentTimeMillis)
                             (long (* 1000.0 (double (:seconds ev))))))]
            (if (nil? (:until-ms ev))
              (do
                (reset! pending-qa-events
                        (vec (cons (assoc ev :until-ms until) (rest events))))
                state)
              (if (>= (System/currentTimeMillis) until)
                (do (reset! pending-qa-events (vec (rest events))) state)
                state)))

          :quit
          (do (reset! pending-qa-events [])
              (q/exit)
              state)

          (do
            (reset! pending-qa-events (vec (rest events)))
            (case (:type ev)
              :click (apply-handle state (input/click-command (:x ev) (:y ev)))
              :aim (apply-handle state (input/aim-command (:x ev) (:y ev)))
              :key (if-let [cmd (input/key-char->command (:ch ev))]
                     (apply-handle state cmd)
                     state)
              :enemy (apply-enemy-spec state (:spec ev))
              :fireball (let [{:keys [x y radius]} (:spec ev)]
                          (core/add-static-fireball state x y radius))
              state)))))))

(defn update-state
  [state]
  (let [scripted? (seq @pending-qa-events)
        state (-> state
                  (input/resize-if-needed (q/width) (q/height)
                                          core/resize core/playfield-width core/playfield-height)
                  tick-state)]
    (if scripted?
      (drain-one-qa-event state)
      (-> state
          (as-> s (apply-handle s (input/aim-command (q/mouse-x) (q/mouse-y))))
          drain-one-qa-event))))

(defn draw
  [state]
  (render/draw-world! state)
  (render/crosshair-at! (q/mouse-x) (q/mouse-y)))

(defn mouse-moved
  [state _event]
  (apply-handle state (input/aim-command (q/mouse-x) (q/mouse-y))))

(defn mouse-dragged
  [state event]
  (mouse-moved state event))

(defn- left-button?
  [event]
  (let [b (or (:button event) (q/mouse-button))]
    (or (nil? b) (= b :left) (= b 37) (= (str b) "left"))))

(defn mouse-pressed
  [state event]
  (if (left-button? event)
    (apply-handle state (input/click-command (q/mouse-x) (q/mouse-y)))
    state))

(defn key-pressed
  [state _event]
  (let [ch (q/raw-key)]
    (cond
      (input/escape-key? ch)
      (do (q/exit) state)

      (input/key-char->command ch)
      (apply-handle state (input/key-char->command ch))

      :else state)))

(defn run-sketch!
  ([]
   (run-sketch! default-width default-height))
  ([width height]
   (q/sketch
    :title "Missile Command"
    :size [width height]
    ;; java2d → AWT Frame so setAutoRequestFocus(false) can prevent focus steal
    :renderer :java2d
    :setup setup
    :update update-state
    :draw draw
    :mouse-moved mouse-moved
    :mouse-dragged mouse-dragged
    :mouse-pressed mouse-pressed
    :key-pressed key-pressed
    :middleware [m/fun-mode]
    :features [:resizable]
    :settings #(q/smooth 2))))
