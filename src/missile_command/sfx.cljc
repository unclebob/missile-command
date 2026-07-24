(ns missile-command.sfx
  "Cumulative SFX event log for hosts and acceptance checks.")

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
