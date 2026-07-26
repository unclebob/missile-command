(ns missile-command.jvm.qa-runner
  "Scripted QA event queue handling for the JVM host."
  (:require [clojure.string :as str]
            [missile-command.core :as core]
            [missile-command.jvm.input :as input]))

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
    :key (apply-key-event ctx state ev)
    :enemy ((:apply-enemy-spec ctx) state (:spec ev))
    :fireball (let [{:keys [x y radius]} (:spec ev)]
                ((:apply-qa-fireballs ctx) state [{:x x :y y :radius radius}]))
    state))

(defn drain-one-event
  [ctx state]
  (let [pending-events (:pending-events ctx)
        events @pending-events]
    (if (empty? events)
      state
      (let [ev (first events)]
        (case (:type ev)
          :wait
          (let [until (or (:until-ms ev)
                          (+ ((:now-ms ctx))
                             (long (* 1000.0 (double (:seconds ev))))))]
            (if (nil? (:until-ms ev))
              (do
                (reset! pending-events
                        (vec (cons (assoc ev :until-ms until) (rest events))))
                state)
              (if (>= ((:now-ms ctx)) until)
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
;; {:version 1, :tested-at "2026-07-26T10:25:50.872899-05:00", :module-hash "1837863906", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "1893744529"} {:id "defn-/emit-sim-when-qa!", :kind "defn-", :line 7, :end-line 10, :hash "384781164"} {:id "defn-/apply-and-emit!", :kind "defn-", :line 12, :end-line 16, :hash "-1319046755"} {:id "defn-/apply-key-event", :kind "defn-", :line 18, :end-line 54, :hash "-1242483882"} {:id "defn-/apply-event", :kind "defn-", :line 56, :end-line 92, :hash "-872152135"} {:id "defn/drain-one-event", :kind "defn", :line 94, :end-line 123, :hash "-1361301969"}]}
;; clj-mutate-manifest-end
