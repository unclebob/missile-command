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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:19:03.141364-05:00", :module-hash "1557826775", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 7, :hash "-2000060040"} {:id "def/default-fire-keys", :kind "def", :line 9, :end-line 12, :hash "534045824"} {:id "defn/key-char->battery", :kind "defn", :line 14, :end-line 17, :hash "-1717611502"} {:id "defn/fire-command", :kind "defn", :line 19, :end-line 21, :hash "1270260925"} {:id "defn/aim-command", :kind "defn", :line 23, :end-line 25, :hash "93232397"} {:id "defn/click-command", :kind "defn", :line 27, :end-line 29, :hash "427388192"} {:id "defn/key-char->command", :kind "defn", :line 31, :end-line 35, :hash "-766332071"} {:id "defn/escape-key?", :kind "defn", :line 37, :end-line 39, :hash "1460313293"} {:id "defn/resize-if-needed", :kind "defn", :line 41, :end-line 46, :hash "-1111450443"} {:id "def/parse-destroy-list", :kind "def", :line 50, :end-line 50, :hash "1371638041"} {:id "def/parse-xy-pair", :kind "def", :line 51, :end-line 51, :hash "-1274995538"} {:id "def/parse-enemy-spec", :kind "def", :line 52, :end-line 52, :hash "-44373697"} {:id "def/parse-fireball-spec", :kind "def", :line 53, :end-line 53, :hash "-941175530"} {:id "def/parse-cli-args", :kind "def", :line 54, :end-line 54, :hash "180273914"} {:id "def/parse-window-size", :kind "def", :line 55, :end-line 55, :hash "-1259031187"} {:id "def/battery-from-events", :kind "def", :line 58, :end-line 58, :hash "-2024061361"} {:id "def/format-telemetry-line", :kind "def", :line 59, :end-line 59, :hash "-1997133924"} {:id "def/format-sim-telemetry-line", :kind "def", :line 60, :end-line 60, :hash "-1775409689"} {:id "def/format-fireball-phase-line", :kind "def", :line 61, :end-line 61, :hash "1810926766"} {:id "def/fireball-report-phase", :kind "def", :line 62, :end-line 62, :hash "2055373732"} {:id "def/detect-fireball-phase-events", :kind "def", :line 63, :end-line 63, :hash "353629549"} {:id "def/load-scenario-edn", :kind "def", :line 66, :end-line 66, :hash "-560660408"} {:id "def/apply-scenario", :kind "def", :line 67, :end-line 67, :hash "1401229870"} {:id "def/apply-destroy-batteries", :kind "def", :line 68, :end-line 68, :hash "-916639401"} {:id "def/apply-qa-targets", :kind "def", :line 69, :end-line 69, :hash "932918052"} {:id "def/apply-enemy-spec", :kind "def", :line 70, :end-line 70, :hash "-313703005"} {:id "def/apply-qa-enemies", :kind "def", :line 71, :end-line 71, :hash "-598662462"} {:id "def/apply-qa-fireballs", :kind "def", :line 72, :end-line 72, :hash "400907860"} {:id "def/parse-qa-event-line", :kind "def", :line 73, :end-line 73, :hash "-81287771"} {:id "def/load-qa-events", :kind "def", :line 74, :end-line 74, :hash "387188188"}]}
;; clj-mutate-manifest-end
