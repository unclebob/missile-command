(ns missile-command.high-scores
  (:require [clojure.string :as str]))

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
