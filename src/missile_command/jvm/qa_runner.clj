(ns missile-command.jvm.qa-runner
  "Scripted QA event queue handling for the JVM host."
  (:require [clojure.string :as str]
            [missile-command.core :as core]
            [missile-command.jvm.input :as input]))

(def nanos-per-milli 1000000)
(def nanos-per-second 1000000000.0)

(defn seconds->nanos
  [seconds]
  (long (* nanos-per-second (double seconds))))

(defn- emit-sim-when-qa!
  [ctx state]
  (when (:qa-telemetry? ctx)
    ((:emit-sim! ctx) state)))

(defn- apply-and-emit!
  [ctx state command]
  (let [s ((:apply-handle ctx) state command)]
    (emit-sim-when-qa! ctx s)
    s))

(defn- apply-key-event
  [ctx state ev]
  (let [ch (:ch ev)
        key-name (str/lower-case (str ch))
        apply-handle (:apply-handle ctx)
        s (apply-handle state {:type :key :key key-name})
        fired? (not= (count (core/defensive-missiles s))
                     (count (core/defensive-missiles state)))]
    (cond
      fired? s
      (or (= \p ch) (= \P ch)
          (core/pause-key-includes? state key-name))
      ((:toggle-pause ctx) state)
      (or (= \o ch) (= \O ch))
      (cond
        (core/title? state) (apply-and-emit! ctx state {:type :open-options})
        (core/options? state) (apply-and-emit! ctx state {:type :leave-options})
        :else state)
      (or (= \h ch) (= \H ch))
      (cond
        (core/title? state) (apply-and-emit! ctx state {:type :open-high-scores})
        (core/high-scores-view? state) (apply-and-emit! ctx state {:type :close-high-scores})
        :else state)
      (or (= \newline ch) (= \return ch))
      (cond
        (core/title? state) (apply-handle state {:type :start})
        (core/the-end? state) (apply-handle state {:type :confirm})
        (core/high-score-entry? state)
        (let [draft @(:initials-draft ctx)
              s2 (if (seq draft)
                   (apply-handle state {:type :submit-high-score
                                        :initials draft})
                   state)]
          (emit-sim-when-qa! ctx s2)
          s2)
        :else state)
      :else s)))

(defn- apply-event
  [ctx state ev]
  (case (:type ev)
    :click (let [s ((:apply-handle ctx) state (input/click-command (:x ev) (:y ev)))
                 s (if (and (core/playing? s) (not (core/playing? state)))
                     ((:apply-destroy-options ctx) s)
                     s)]
             (when (and (:qa-telemetry? ctx)
                        (not= (core/screen state) (core/screen s)))
               ((:emit-sim! ctx) s))
             s)
    :aim ((:apply-handle ctx) state (input/aim-command (:x ev) (:y ev)))
    :start (let [s ((:apply-handle ctx) state {:type :start})
                 s (cond-> s
                     (core/playing? s) ((:apply-destroy-options ctx)))]
             (emit-sim-when-qa! ctx s)
             s)
    :confirm (apply-and-emit! ctx state {:type :confirm})
    :pause (apply-and-emit! ctx state {:type :pause})
    :resume (apply-and-emit! ctx state {:type :resume})
    :open-high-scores (apply-and-emit! ctx state {:type :open-high-scores})
    :close-high-scores (apply-and-emit! ctx state {:type :close-high-scores})
    :submit-high-score (apply-and-emit! ctx state {:type :submit-high-score
                                                   :initials (:initials ev)})
    :open-options (apply-and-emit! ctx state {:type :open-options})
    :leave-options (apply-and-emit! ctx state {:type :leave-options})
    :set-mute (apply-and-emit! ctx state {:type :set-mute :mute (:mute ev)})
    :set-difficulty (apply-and-emit! ctx state {:type :set-difficulty
                                                :difficulty (:difficulty ev)})
    :bind-fire-key (apply-and-emit! ctx state {:type :bind-fire-key
                                               :battery (:battery ev)
                                               :key (:key ev)})
    :reset-scenario (let [s (-> ((:new-game ctx))
                                ((:load-persisted ctx))
                                ((:apply-destroy-options ctx))
                                ((:apply-scenario ctx)
                                 ((:load-scenario-edn ctx) (:path ev)))
                                (core/evaluate-game-over))]
                      (reset! (:initials-draft ctx) "")
                      (emit-sim-when-qa! ctx s)
                      s)
    :key (apply-key-event ctx state ev)
    :enemy ((:apply-enemy-spec ctx) state (:spec ev))
    :fireball (let [{:keys [x y radius]} (:spec ev)]
                ((:apply-qa-fireballs ctx) state [{:x x :y y :radius radius}]))
    state))

(defn- now-ns
  [ctx]
  (if-let [f (:now-ns ctx)]
    (f)
    (* nanos-per-milli ((:now-ms ctx)))))

(defn- wait-deadline-ns
  [ctx ev]
  (+ (now-ns ctx)
     (seconds->nanos (:seconds ev))))

(defn drain-one-event
  [ctx state]
  (let [pending-events (:pending-events ctx)
        events @pending-events]
    (if (empty? events)
      state
      (let [ev (first events)]
        (case (:type ev)
          :wait
          (let [until (or (:until-ns ev)
                          (wait-deadline-ns ctx ev))]
            (if (nil? (:until-ns ev))
              (do
                (reset! pending-events
                        (vec (cons (assoc ev :until-ns until) (rest events))))
                state)
              (if (>= (now-ns ctx) until)
                (do (reset! pending-events (vec (rest events))) state)
                state)))

          :quit
          (do (reset! pending-events [])
              ((:persist-settings! ctx) state)
              ((:exit! ctx))
              state)

          (do
            (reset! pending-events (vec (rest events)))
            (apply-event ctx state ev)))))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:35:42.006596-05:00", :module-hash "1263506324", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "1893744529"} {:id "def/nanos-per-milli", :kind "def", :line 7, :end-line 7, :hash "1184227686"} {:id "def/nanos-per-second", :kind "def", :line 8, :end-line 8, :hash "1276479865"} {:id "defn/seconds->nanos", :kind "defn", :line 10, :end-line 12, :hash "-1980400153"} {:id "defn-/emit-sim-when-qa!", :kind "defn-", :line 14, :end-line 17, :hash "384781164"} {:id "defn-/apply-and-emit!", :kind "defn-", :line 19, :end-line 23, :hash "-1319046755"} {:id "defn-/apply-key-event", :kind "defn-", :line 25, :end-line 61, :hash "-1242483882"} {:id "defn-/apply-event", :kind "defn-", :line 63, :end-line 99, :hash "-872152135"} {:id "defn-/now-ns", :kind "defn-", :line 101, :end-line 105, :hash "-1650924997"} {:id "defn-/wait-deadline-ns", :kind "defn-", :line 107, :end-line 110, :hash "-1911989476"} {:id "defn/drain-one-event", :kind "defn", :line 112, :end-line 140, :hash "-1620646213"}]}
;; clj-mutate-manifest-end
