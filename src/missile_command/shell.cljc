(ns missile-command.shell
  "Screen shell transitions: title, pause, options handoff, high scores, end confirm.
  Combat and wave logic stay elsewhere; this module only changes screen shells."
  (:require [missile-command.high-scores :as high-scores]
            [missile-command.options :as options]
            [missile-command.screens :as screens]))

(defn- switch-screen-when
  "If current screen equals `from`, switch to `to`; otherwise leave state."
  [state from to]
  (if (= from (:screen state))
    (assoc state :screen to)
    state))

(defn pause-game
  "Enter paused from playing only; ignore on title/end/already paused."
  [state]
  (switch-screen-when state screens/playing screens/paused))

(defn resume-game
  "Return from paused to playing; no-op otherwise."
  [state]
  (switch-screen-when state screens/paused screens/playing))

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
  [state entry? pending-score initials display-name public-code blank-shell]
  (apply-shell
   (high-scores/submit-entry state
                             entry?
                             pending-score
                             initials
                             display-name
                             public-code
                             (blank-shell state))
   state))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-25T11:13:40.023372-05:00", :module-hash "1933794666", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 6, :hash "231099063"} {:id "defn-/switch-screen-when", :kind "defn-", :line 8, :end-line 13, :hash "1192285012"} {:id "defn/pause-game", :kind "defn", :line 15, :end-line 18, :hash "-61201790"} {:id "defn/resume-game", :kind "defn", :line 20, :end-line 23, :hash "-1808709720"} {:id "defn/apply-shell", :kind "defn", :line 25, :end-line 28, :hash "1840097145"} {:id "defn/export-settings", :kind "defn", :line 30, :end-line 35, :hash "-998698733"} {:id "defn/import-settings", :kind "defn", :line 37, :end-line 46, :hash "-71389308"} {:id "defn/start-game", :kind "defn", :line 48, :end-line 53, :hash "-978828198"} {:id "defn/confirm-end-screen", :kind "defn", :line 55, :end-line 63, :hash "1167624982"} {:id "defn/submit-high-score-initials", :kind "defn", :line 65, :end-line 74, :hash "-614208574"}]}
;; clj-mutate-manifest-end
