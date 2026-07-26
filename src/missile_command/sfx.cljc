(ns missile-command.sfx
  "SFX event contract for hosts and acceptance checks.

  Source of truth: cumulative vector `:sfx-events` on game state.
  Each entry is a map with at least `{:type keyword}` (e.g. `:sfx/launch`).

  Hosts (JVM + browser):
  1. Remember the previous log length (or a cursor).
  2. Play the events returned by `(take-new-with-cursor state cursor)`.
  3. Update the cursor to `(count (events state))`.

  `:events` on `handle`/`tick` results is not the SFX log. Fire may still
  return step-local launch events for telemetry; prefer `:sfx-events` +
  take-new for audio.

  Acceptance / unit tests may use the full cumulative log via `events` and
  `emitted?`. Production hosts should not retain unbounded growth beyond
  their cursor needs — use `truncate-to` or `drain` if the log must shrink.")

(defn emit
  "Append an SFX event to the state's cumulative log."
  [state type]
  (update state :sfx-events (fnil conj []) {:type type}))

(defn emit-many
  [state events]
  (update state :sfx-events (fnil into []) events))

(defn events
  [state]
  (vec (or (:sfx-events state) [])))

(defn take-new
  "SFX events from index `from` (inclusive) to the end of the log."
  [state from]
  (let [ev (events state)
        n (count ev)
        from (long (max 0 (min (long from) n)))]
    (subvec ev from n)))

(defn take-new-with-cursor
  "Return [fresh-events next-cursor] for a host-owned SFX cursor."
  [state cursor]
  (let [ev (events state)
        next-cursor (count ev)]
    [(take-new state (or cursor 0)) next-cursor]))

(defn truncate-to
  "Keep only the first `n` log entries (clear when n is 0)."
  [state n]
  (assoc state :sfx-events (vec (take (max 0 (long n)) (events state)))))

(defn drain
  "Return [events state'] with the log cleared after reading all events."
  [state]
  (let [ev (events state)]
    [ev (assoc state :sfx-events [])]))

(defn emitted?
  "True when an event of the given type (keyword or sfx/... string) was logged."
  [state type]
  (let [t (if (keyword? type) type (keyword type))]
    (boolean (some #(= t (:type %)) (events state)))))

(defn event-type-name
  "Stable host-facing text form for an SFX event type."
  [event]
  (let [kw (:type event)]
    (if (namespace kw)
      (str (namespace kw) "/" (name kw))
      (name kw))))

(defn launch-events
  "Launch event, plus low-ammo when remaining after fire is 1."
  [battery-id remaining-after]
  (cond-> [{:type :sfx/launch :battery battery-id}]
    (= 1 (long remaining-after))
    (conj {:type :sfx/low-ammo :battery battery-id})))

(defn maybe-emit
  "Emit type when pred is truthy; otherwise return state unchanged."
  [state pred type]
  (if pred (emit state type) state))

(defn maybe-title-warning
  "Play warning once each time the title screen is entered.
  `on-title?` is supplied by the host of game state (avoids sfx→core deps)."
  [state on-title?]
  (if on-title?
    (if (:title-warning-played? state)
      state
      (-> state
          (assoc :title-warning-played? true)
          (emit :sfx/warning)))
    (dissoc state :title-warning-played?)))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:35:28.425994-05:00", :module-hash "2122741749", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 18, :hash "-204239440"} {:id "defn/emit", :kind "defn", :line 20, :end-line 23, :hash "687167721"} {:id "defn/emit-many", :kind "defn", :line 25, :end-line 27, :hash "-1337034557"} {:id "defn/events", :kind "defn", :line 29, :end-line 31, :hash "-501259531"} {:id "defn/take-new", :kind "defn", :line 33, :end-line 39, :hash "339574723"} {:id "defn/take-new-with-cursor", :kind "defn", :line 41, :end-line 46, :hash "-2077759948"} {:id "defn/truncate-to", :kind "defn", :line 48, :end-line 51, :hash "1612498976"} {:id "defn/drain", :kind "defn", :line 53, :end-line 57, :hash "48629765"} {:id "defn/emitted?", :kind "defn", :line 59, :end-line 63, :hash "-894919108"} {:id "defn/event-type-name", :kind "defn", :line 65, :end-line 71, :hash "-1768252475"} {:id "defn/launch-events", :kind "defn", :line 73, :end-line 78, :hash "2033453565"} {:id "defn/maybe-emit", :kind "defn", :line 80, :end-line 83, :hash "-1495564980"} {:id "defn/maybe-title-warning", :kind "defn", :line 85, :end-line 95, :hash "-1112519407"}]}
;; clj-mutate-manifest-end
