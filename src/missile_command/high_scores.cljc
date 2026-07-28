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

(defn normalize-display-name
  [raw]
  (let [s (-> (str raw)
              (str/replace #"\s+" " ")
              str/trim)]
    (subs s 0 (min 16 (count s)))))

(defn normalize-public-code
  [raw]
  (->> (str/upper-case (str raw))
       (filter #(re-matches #"[A-Z0-9]" (str %)))
       (take 6)
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
  ([entries capacity initials score]
   (insert entries capacity initials score nil nil))
  ([entries capacity initials score display-name public-code]
   (insert entries capacity initials score display-name public-code nil))
  ([entries capacity initials score display-name public-code created-at]
   (let [norm (normalize-initials initials)
         name (normalize-display-name display-name)
         code (normalize-public-code public-code)
         created-at-text (some-> created-at str)
         entry (cond-> {:initials norm
                        :score (long score)}
                 (seq name) (assoc :display-name name)
                 (seq code) (assoc :public-code code)
                 (seq created-at-text) (assoc :created-at created-at-text))
         cap (max 1 (long capacity))]
     (->> (conj (vec entries) entry)
          sort-entries
          (take cap)
          vec))))

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
  (screens/open-from-title state screens/high-scores))

(defn close-view
  "Return from high-scores view to title."
  [state]
  (screens/return-to-title-when state view-screen?))

(defn with-submitted-entry
  "Insert pending/final score under initials into the table."
  ([state initials score]
   (with-submitted-entry state initials score nil nil))
  ([state initials score display-name public-code]
   (with-submitted-entry state initials score display-name public-code nil))
  ([state initials score display-name public-code created-at]
   (let [norm (normalize-initials initials)]
     (-> state
         (assoc :high-scores (insert (table state) (capacity state) norm score
                                     display-name public-code created-at))
         (assoc :submitted-high-score-initials norm)))))

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
  ([state on-entry-screen? score initials blank-title]
   (submit-entry state on-entry-screen? score initials nil nil blank-title))
  ([state on-entry-screen? score initials display-name public-code blank-title]
   (submit-entry state on-entry-screen? score initials display-name public-code nil blank-title))
  ([state on-entry-screen? score initials display-name public-code created-at blank-title]
   (if-not on-entry-screen?
     state
     (let [with-entry (with-submitted-entry state initials score display-name public-code created-at)]
       (-> (to-shell blank-title with-entry screens/title)
           (assoc :submitted-high-score-initials
                  (submitted-initials with-entry)))))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-27T13:34:08.545792-05:00", :module-hash "-328923653", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "231863397"} {:id "def/default-capacity", :kind "def", :line 6, :end-line 6, :hash "-1289277682"} {:id "def/initials-length", :kind "def", :line 7, :end-line 7, :hash "527954842"} {:id "defn/normalize-initials", :kind "defn", :line 9, :end-line 15, :hash "-2143639226"} {:id "defn/normalize-display-name", :kind "defn", :line 17, :end-line 22, :hash "-1001456822"} {:id "defn/normalize-public-code", :kind "defn", :line 24, :end-line 29, :hash "-2028185641"} {:id "defn-/lowest-score", :kind "defn-", :line 31, :end-line 34, :hash "1465582881"} {:id "defn/qualifies?", :kind "defn", :line 36, :end-line 47, :hash "-1287410378"} {:id "defn/sort-entries", :kind "defn", :line 49, :end-line 52, :hash "-1748063370"} {:id "defn/insert", :kind "defn", :line 54, :end-line 70, :hash "586836552"} {:id "defn/ordered?", :kind "defn", :line 72, :end-line 75, :hash "-972250860"} {:id "defn/entry-at-rank", :kind "defn", :line 77, :end-line 80, :hash "-208101917"} {:id "defn/table", :kind "defn", :line 82, :end-line 84, :hash "-1230873889"} {:id "defn/capacity", :kind "defn", :line 86, :end-line 88, :hash "-1236949115"} {:id "defn/pending", :kind "defn", :line 90, :end-line 92, :hash "-907900431"} {:id "defn/submitted-initials", :kind "defn", :line 94, :end-line 96, :hash "-774179172"} {:id "defn/entry-screen?", :kind "defn", :line 98, :end-line 100, :hash "1214021367"} {:id "defn/view-screen?", :kind "defn", :line 102, :end-line 104, :hash "-1251321752"} {:id "defn/carry", :kind "defn", :line 106, :end-line 113, :hash "913920084"} {:id "defn/set-capacity", :kind "defn", :line 115, :end-line 117, :hash "1095962770"} {:id "defn/add-entry", :kind "defn", :line 119, :end-line 124, :hash "986895154"} {:id "defn/begin-entry", :kind "defn", :line 126, :end-line 131, :hash "-1446708916"} {:id "defn/open-view", :kind "defn", :line 133, :end-line 136, :hash "1348424807"} {:id "defn/close-view", :kind "defn", :line 138, :end-line 141, :hash "-216560420"} {:id "defn/with-submitted-entry", :kind "defn", :line 143, :end-line 152, :hash "1501394344"} {:id "defn/qualifies-state?", :kind "defn", :line 154, :end-line 156, :hash "-1669423122"} {:id "defn/to-shell", :kind "defn", :line 158, :end-line 165, :hash "1499774989"} {:id "defn/start-playing", :kind "defn", :line 167, :end-line 170, :hash "-1309424530"} {:id "defn/confirm-end", :kind "defn", :line 172, :end-line 179, :hash "966988508"} {:id "defn/submit-entry", :kind "defn", :line 181, :end-line 191, :hash "1651239293"}]}
;; clj-mutate-manifest-end
