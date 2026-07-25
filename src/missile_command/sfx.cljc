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
;; {:version 1, :tested-at "2026-07-24T16:17:43.490776-05:00", :module-hash "443994014", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "-384622950"} {:id "defn/emit", :kind "defn", :line 4, :end-line 7, :hash "687167721"} {:id "defn/emit-many", :kind "defn", :line 9, :end-line 11, :hash "-1337034557"} {:id "defn/events", :kind "defn", :line 13, :end-line 15, :hash "-501259531"} {:id "defn/emitted?", :kind "defn", :line 17, :end-line 21, :hash "-894919108"} {:id "defn/launch-events", :kind "defn", :line 23, :end-line 28, :hash "2033453565"} {:id "defn/maybe-emit", :kind "defn", :line 30, :end-line 33, :hash "-1495564980"}]}
;; clj-mutate-manifest-end
