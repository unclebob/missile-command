(ns missile-command.shell
  "Screen shell transitions: title, pause, options handoff, high scores, end confirm.
  Combat and wave logic stay elsewhere; this module only changes screen shells."
  (:require [missile-command.high-scores :as high-scores]
            [missile-command.options :as options]
            [missile-command.screens :as screens]))

(defn pause-game
  "Enter paused from playing only; ignore on title/end/already paused."
  [state]
  (if (= screens/playing (:screen state))
    (assoc state :screen screens/paused)
    state))

(defn resume-game
  "Return from paused to playing; no-op otherwise."
  [state]
  (if (= screens/paused (:screen state))
    (assoc state :screen screens/playing)
    state))

(defn apply-shell
  "High-score/title shell transition that also carries options from source."
  [shell-state source]
  (options/carry shell-state source))

(defn export-settings
  "Serializable high scores and options for host persistence."
  [state]
  {:options (options/of state)
   :high-scores (high-scores/table state)
   :high-score-capacity (high-scores/capacity state)})

(defn import-settings
  "Restore high scores and options onto a shell state."
  [state settings]
  (let [settings (or settings {})]
    (assoc state
           :options (or (:options settings) options/default-options)
           :high-scores (vec (or (:high-scores settings) []))
           :high-score-capacity
           (long (or (:high-score-capacity settings)
                     high-scores/default-capacity)))))

(defn start-game
  "Leave title (or any shell) and begin a fresh playing run.
  blank-shell is (fn [source] → new empty game state at same size)."
  [state blank-shell]
  (-> (high-scores/start-playing (blank-shell state) state)
      (apply-shell state)))

(defn confirm-end-screen
  "After THE END: open initials entry if score qualifies, else return to title."
  [state the-end? final-score blank-shell]
  (apply-shell
   (high-scores/confirm-end state
                            the-end?
                            final-score
                            (blank-shell state))
   state))

(defn submit-high-score-initials
  "Insert pending score with initials, then return to title."
  [state entry? pending-score initials blank-shell]
  (apply-shell
   (high-scores/submit-entry state
                             entry?
                             pending-score
                             initials
                             (blank-shell state))
   state))
