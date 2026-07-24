(ns missile-command.jvm.sketch
  "Quil sketch: route mouse/keyboard UI events into pure core commands."
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [missile-command.core :as core]
            [missile-command.jvm.input :as input]
            [missile-command.jvm.render :as render]))

(def default-width 800)
(def default-height 600)

(defonce launch-options
  (atom {:qa-telemetry? false
         :destroy-batteries []
         :qa-events nil}))

(defonce pending-qa-events
  (atom []))

(defn configure!
  [opts]
  (reset! launch-options (select-keys opts [:qa-telemetry?
                                            :destroy-batteries
                                            :qa-events]))
  (reset! pending-qa-events
          (if-let [path (:qa-events opts)]
            (input/load-qa-events path)
            [])))

(defn- emit-telemetry!
  [result]
  (when (:qa-telemetry? @launch-options)
    (println (input/format-telemetry-line result))
    (flush)))

(defn- apply-handle
  [state command]
  (let [result (core/handle state command)]
    (when (#{:fire :click} (:type command))
      (emit-telemetry! result))
    (:state result)))

(defn- apply-destroy-options
  [state]
  (reduce (fn [s id]
            (core/destroy-battery s id))
          state
          (:destroy-batteries @launch-options)))

(defn setup
  []
  (q/frame-rate 120)
  (q/no-cursor)
  (-> (core/new-game {:width (q/width) :height (q/height)})
      apply-destroy-options))

(defn- drain-one-qa-event
  [state]
  (let [events @pending-qa-events]
    (if (empty? events)
      state
      (let [ev (first events)]
        (reset! pending-qa-events (vec (rest events)))
        (case (:type ev)
          :click (apply-handle state (input/click-command (:x ev) (:y ev)))
          :aim (apply-handle state (input/aim-command (:x ev) (:y ev)))
          :key (if-let [cmd (input/key-char->command (:ch ev))]
                 (apply-handle state cmd)
                 state)
          :quit (do (q/exit) state)
          state)))))

(defn update-state
  [state]
  ;; While scripted QA events remain, do not overwrite aim from the OS mouse
  ;; (often 0,0 before the pointer enters the window).
  (let [scripted? (seq @pending-qa-events)
        state (-> state
                  (input/resize-if-needed (q/width) (q/height)
                                          core/resize core/playfield-width core/playfield-height))]
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

(defn mouse-clicked
  [state _event]
  (apply-handle state (input/click-command (q/mouse-x) (q/mouse-y))))

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
  "Open the playfield window and process UI events until quit."
  ([]
   (run-sketch! default-width default-height))
  ([width height]
   (q/sketch
    :title "Missile Command"
    :size [width height]
    :setup setup
    :update update-state
    :draw draw
    :mouse-moved mouse-moved
    :mouse-dragged mouse-dragged
    :mouse-clicked mouse-clicked
    :key-pressed key-pressed
    :middleware [m/fun-mode]
    :features [:resizable]
    :settings #(q/smooth 2))))
