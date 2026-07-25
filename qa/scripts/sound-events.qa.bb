#!/usr/bin/env bb
;; Executable QA for sound-events (core emit + mute host play flag).

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[clojure.java.io :as io])

(defn die! [msg]
  (binding [*out* *err*] (println (str "FAIL: " msg)))
  (System/exit 1))

(defn assert! [ok? msg] (when-not ok? (die! msg)))

(defn field [line key]
  (when-let [[_ v] (re-find (re-pattern (str key "=([^\\s]+)")) line)] v))

(defn sims [out]
  (->> (str/split-lines out) (map str/trim) (filter #(str/starts-with? % "qa-sim ")) vec))

(defn sfx-lines [out]
  (->> (str/split-lines out) (map str/trim) (filter #(str/starts-with? % "qa-sfx ")) vec))

(defn write-edn! [path data]
  (io/make-parents path)
  (spit path (pr-str data)))

(defn write-events! [path lines]
  (io/make-parents path)
  (spit path (str (str/join "\n" lines) "\n")))

(defn run! [label cmd]
  (println (str "==> " label ": " cmd)) (flush)
  (let [r (p/shell {:out :string :err :string :continue true} "bash" "-lc" cmd)
        out (str (:out r) (:err r))]
    (print out) (flush)
    {:exit (:exit r) :out out}))

(defn launch!
  [{:keys [scenario-path events-path scores-path timeout-ms]
    :or {timeout-ms 45000}}]
  (let [cmd (str "bb play 800 600 --qa --qa-speed 8"
                 (when scores-path (str " --scores-file " scores-path))
                 (when scenario-path (str " --qa-scenario " scenario-path))
                 (when events-path (str " --qa-events " events-path)))]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out) :sfx (sfx-lines out)})))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/sound-events.qa.md")) "missing procedure")
  (assert! (.exists (io/file "features/sound-events.feature")) "missing feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)qa-sfx" readme) "README missing qa-sfx")
    (assert! (re-find #"(?m)sfx_count=" readme) "README missing sfx_count="))

  (let [u (run! "unit" "bb test")
        a (run! "accept" "bb accept")
        c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit u)) "unit failed")
    (assert! (zero? (:exit a)) "accept failed")
    (assert! (re-find #"(?i)sound[-_]?events" (:out a)) "accept missing sound-events")
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: fire → sfx/launch played when unmuted
  (write-events! "tmp/sfx-launch.txt"
                 ["wait 0.1" "start" "aim 400 200" "key z" "wait 0.2" "quit"])
  (let [r (launch! {:events-path "tmp/sfx-launch.txt"
                    :scores-path "tmp/sfx-empty.edn"})
        launch (first (filter #(re-find #"type=sfx/launch" %) (:sfx r)))]
    (assert! launch (str "B no launch sfx: " (:sfx r)))
    (assert! (re-find #"played=true" launch) (str "B should play: " launch)))

  ;; I: mute → core still logs, played=false
  (write-events! "tmp/sfx-mute.txt"
                 ["wait 0.1" "open-options" "mute true" "leave-options"
                  "start" "aim 400 200" "key z" "wait 0.2" "quit"])
  (let [r (launch! {:events-path "tmp/sfx-mute.txt"
                    :scores-path "tmp/sfx-mute.edn"})
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
                    :scores-path "tmp/sfx-end.edn"})
        end-sfx (some #(re-find #"type=sfx/the-end" %) (:sfx r))]
    (assert! end-sfx (str "H no the-end sfx: " (:sfx r))))

  (println "\nPASS: sound-events automated QA (A, B, H, I)")
  (println "PASS: look-and-feel optional; SFX telemetry approved")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
