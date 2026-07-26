(ns missile-command.jvm.input
  "Pure host input mapping: UI events → core commands.
  CLI, telemetry, and QA scenario engines live in sibling namespaces;
  this ns re-exports them for sketch/main/spec stability. No Quil."
  (:require [missile-command.jvm.cli :as cli]
            [missile-command.jvm.scenario :as scenario]
            [missile-command.jvm.telemetry :as telemetry]))

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

(defn resize-if-needed
  [state width height resize-fn playfield-width-fn playfield-height-fn]
  (if (or (not= width (playfield-width-fn state))
          (not= height (playfield-height-fn state)))
    (resize-fn state width height)
    state))


;; --- re-exports: CLI ---
(def parse-destroy-list cli/parse-destroy-list)
(def parse-xy-pair cli/parse-xy-pair)
(def parse-enemy-spec cli/parse-enemy-spec)
(def parse-fireball-spec cli/parse-fireball-spec)
(def parse-cli-args cli/parse-cli-args)
(def parse-window-size cli/parse-window-size)

;; --- re-exports: telemetry ---
(def battery-from-events telemetry/battery-from-events)
(def format-telemetry-line telemetry/format-telemetry-line)
(def format-sim-telemetry-line telemetry/format-sim-telemetry-line)
(def format-fireball-phase-line telemetry/format-fireball-phase-line)
(def fireball-report-phase telemetry/fireball-report-phase)
(def detect-fireball-phase-events telemetry/detect-fireball-phase-events)

;; --- re-exports: scenario / QA automation ---
(def load-scenario-edn scenario/load-scenario-edn)
(def apply-scenario scenario/apply-scenario)
(def apply-destroy-batteries scenario/apply-destroy-batteries)
(def apply-qa-targets scenario/apply-qa-targets)
(def apply-enemy-spec scenario/apply-enemy-spec)
(def apply-qa-enemies scenario/apply-qa-enemies)
(def apply-qa-fireballs scenario/apply-qa-fireballs)
(def parse-qa-event-line scenario/parse-qa-event-line)
(def load-qa-events scenario/load-qa-events)
