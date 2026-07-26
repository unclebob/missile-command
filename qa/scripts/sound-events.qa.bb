#!/usr/bin/env bb
;; Executable QA for sound-events (core emit + mute host play flag).

(load-file "qa/scripts/lib/common.bb")

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/sound-events.qa.md")) "missing procedure")
  (assert! (.exists (io/file "features/sound-events.feature")) "missing feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)qa-sfx" readme) "README missing qa-sfx")
    (assert! (re-find #"(?m)sfx_count=" readme) "README missing sfx_count="))

  (let [c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: fire → sfx/launch played when unmuted
  (write-events! "tmp/sfx-launch.txt"
                 ["wait 0.1" "start" "aim 400 200" "key z" "wait 0.2" "quit"])
  (let [r (launch! {:events-path "tmp/sfx-launch.txt"
                    :scores-path "tmp/sfx-empty.edn"
                    :include-sfx? true})
        launch (first (filter #(re-find #"type=sfx/launch" %) (:sfx r)))]
    (assert! launch (str "B no launch sfx: " (:sfx r)))
    (assert! (re-find #"played=true" launch) (str "B should play: " launch)))

  ;; I: mute → core still logs, played=false
  (write-events! "tmp/sfx-mute.txt"
                 ["wait 0.1" "open-options" "mute true" "leave-options"
                  "start" "aim 400 200" "key z" "wait 0.2" "quit"])
  (let [r (launch! {:events-path "tmp/sfx-mute.txt"
                    :scores-path "tmp/sfx-mute.edn"
                    :include-sfx? true})
        launch (first (filter #(re-find #"type=sfx/launch" %) (:sfx r)))
        last-sim (last (:sims r))]
    (assert! launch (str "I muted fire no sfx line: " (:sfx r)))
    (assert! (re-find #"played=false" launch) (str "I should suppress: " launch))
    (assert! (re-find #"sfx/launch" (or (field last-sim "sfx_last") ""))
             (str "I core should still log: " last-sim)))

  ;; H: THE END emits sfx/the-end
  (write-edn! "tmp/sfx-end.edn"
              {:cities {:destroyed [0 1 2 3 4 5]} :bonus-cities 0 :score 10})
  (write-events! "tmp/sfx-end.txt" ["wait 0.5" "quit"])
  (let [r (launch! {:scenario-path "tmp/sfx-end.edn"
                    :events-path "tmp/sfx-end.txt"
                    :scores-path "tmp/sfx-end.edn"
                    :include-sfx? true})
        end-sfx (some #(re-find #"type=sfx/the-end" %) (:sfx r))]
    (assert! end-sfx (str "H no the-end sfx: " (:sfx r))))

  (println "\nPASS: sound-events automated QA (A, B, H, I)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
