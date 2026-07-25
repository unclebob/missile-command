(ns missile-command.sfx
  "SFX event contract for hosts and acceptance checks.

  Source of truth: cumulative vector `:sfx-events` on game state.
  Each entry is a map with at least `{:type keyword}` (e.g. `:sfx/launch`).

  Hosts (JVM + browser):
  1. Remember the previous log length (or a cursor).
  2. Play `(take-new state prev-count)` — the new events since last frame.
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
