(ns missile-command.high-scores
  "High-score table rules and state projection."
  (:require [clojure.string :as str]
            [missile-command.screens :as screens]))

(def default-capacity 10)
(def initials-length 3)

(defn normalize-initials
  "Uppercase A–Z/0–9 only; keep first three characters."
  [raw]
  (->> (str/upper-case (str raw))
       (filter #(re-matches #"[A-Z0-9]" (str %)))
       (take initials-length)
       (apply str)))

(defn- lowest-score
  [entries]
  (when (seq entries)
    (apply min (map :score entries))))

(defn qualifies?
  "True when score earns a table slot: positive score and (table not full
  or score >= current lowest). Zero never qualifies (avoids entry on a
  default empty table after THE END)."
  [entries capacity score]
  (let [cap (long capacity)
        s (long score)
        n (count entries)]
    (and (pos? s)
         (or (< n cap)
             (let [low (lowest-score entries)]
               (and low (>= s (long low))))))))

(defn sort-entries
  "Descending by score."
  [entries]
  (vec (sort-by (comp - :score) entries)))

(defn insert
  "Insert initials+score, re-sort descending, keep at most capacity."
  [entries capacity initials score]
  (let [entry {:initials (normalize-initials initials)
               :score (long score)}
        cap (max 1 (long capacity))]
    (->> (conj (vec entries) entry)
         sort-entries
         (take cap)
         vec)))

(defn ordered?
  [entries]
  (let [scores (mapv :score entries)]
    (= scores (vec (sort (comp - compare) scores)))))

(defn entry-at-rank
  "1-based rank."
  [entries rank]
  (nth entries (dec (long rank)) nil))

(defn table
  [state]
  (vec (or (:high-scores state) [])))

(defn capacity
  [state]
  (long (or (:high-score-capacity state) default-capacity)))

(defn pending
  [state]
  (:pending-high-score state))

(defn submitted-initials
  [state]
  (:submitted-high-score-initials state))

(defn entry-screen?
  [state]
  (screens/high-score-entry? state))

(defn view-screen?
  [state]
  (screens/high-scores-view? state))

(defn carry
  "Copy high-score table from source onto target (threadable: target first)."
  [target source]
  (assoc target
         :high-scores (table source)
         :high-score-capacity (capacity source)
         :pending-high-score nil
         :submitted-high-score-initials (submitted-initials source)))

(defn set-capacity
  [state capacity]
  (assoc state :high-score-capacity (long capacity)))

(defn add-entry
  "Seed or append a table entry without changing screen."
  [state initials score]
  (update state :high-scores
          (fn [entries]
            (insert (or entries []) (capacity state) initials score))))

(defn begin-entry
  "Enter initials entry with the given pending score."
  [state score]
  (assoc state
         :screen screens/high-score-entry
         :pending-high-score (long score)))

(defn open-view
  "View high-score table from title."
  [state]
  (if (screens/title? state)
    (assoc state :screen screens/high-scores)
    state))

(defn close-view
  "Return from high-scores view to title."
  [state]
  (if (view-screen? state)
    (assoc state :screen screens/title)
    state))

(defn with-submitted-entry
  "Insert pending/final score under initials into the table."
  [state initials score]
  (let [norm (normalize-initials initials)]
    (-> state
        (assoc :high-scores (insert (table state) (capacity state) norm score))
        (assoc :submitted-high-score-initials norm))))

(defn qualifies-state?
  [state score]
  (qualifies? (table state) (capacity state) score))

(defn to-shell
  "Thread high-score table onto a blank shell and set the screen."
  [blank source screen]
  (-> blank
      (carry source)
      (assoc :screen screen
             :pending-high-score nil
             :submitted-high-score-initials nil)))

(defn start-playing
  "Begin a playing run from blank shell, carrying high scores."
  [blank source]
  (to-shell blank source screens/playing))

(defn confirm-end
  "After THE END: open entry if score qualifies, else return to title shell."
  [state at-end? score blank-title]
  (if-not at-end?
    state
    (if (qualifies-state? state score)
      (begin-entry state score)
      (to-shell blank-title state screens/title))))

(defn submit-entry
  "Insert initials for pending score and return to title shell."
  [state on-entry-screen? score initials blank-title]
  (if-not on-entry-screen?
    state
    (let [with-entry (with-submitted-entry state initials score)]
      (-> (to-shell blank-title with-entry screens/title)
          (assoc :submitted-high-score-initials
                 (submitted-initials with-entry))))))
