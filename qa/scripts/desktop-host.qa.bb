#!/usr/bin/env bb
;; Executable QA for desktop-host (launch, shell, fire, persist smoke).

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
  [{:keys [events-path scores-path extra timeout-ms]
    :or {timeout-ms 45000 extra ""}}]
  (let [cmd (str "bb play 800 600 --qa --qa-speed 8"
                 (when (seq extra) (str " " extra))
                 (when scores-path (str " --scores-file " scores-path))
                 (when events-path (str " --qa-events " events-path)))]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out)})))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/desktop-host.qa.md")) "missing procedure")
  (assert! (.exists (io/file "features/desktop-host.feature")) "missing feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)^bb play" readme) "README missing bb play")
    (assert! (re-find #"(?m)missile-command-settings" readme) "README missing settings")
    (assert! (re-find #"(?m)--qa" readme) "README missing --qa"))

  (let [u (run! "unit" "bb test")
        a (run! "accept" "bb accept")
        c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit u)) "unit failed")
    (assert! (zero? (:exit a)) "accept failed")
    (assert! (re-find #"(?i)desktop[-_]?host" (:out a)) "accept missing desktop-host")
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: title → start → playing + HUD
  (write-events! "tmp/desk-start.txt" ["wait 0.1" "start" "wait 0.2" "quit"])
  (let [r (launch! {:events-path "tmp/desk-start.txt"
                    :scores-path "tmp/desk-empty.edn"})
        playing (first (filter #(= "playing" (field % "screen")) (:sims r)))]
    (assert! playing (str "B never playing: " (map #(field % "screen") (:sims r))))
    (assert! (= "true" (field playing "hud_full")) (str "B HUD: " playing)))

  ;; C: fire + pause
  (write-events! "tmp/desk-fire.txt"
                 ["wait 0.1" "start" "aim 400 200" "key z" "pause" "wait 0.15"
                  "resume" "wait 0.1" "quit"])
  (let [r (launch! {:events-path "tmp/desk-fire.txt"
                    :scores-path "tmp/desk-empty.edn"})
        fire (some #(re-find #"qa-fire battery=left" %) (str/split-lines (:out r)))
        paused (some #(= "paused" (field % "screen")) (:sims r))]
    (assert! fire (str "C fire: " (:out r)))
    (assert! paused "C never paused"))

  ;; Meta: high scores + options reachable
  (write-events! "tmp/desk-meta.txt"
                 ["wait 0.1" "open-high-scores" "close-high-scores"
                  "open-options" "leave-options" "quit"])
  (let [r (launch! {:events-path "tmp/desk-meta.txt"
                    :scores-path "tmp/desk-empty.edn"})
        screens (set (map #(field % "screen") (:sims r)))]
    (assert! (contains? screens "high-scores") (str "meta high-scores: " screens))
    (assert! (contains? screens "options") (str "meta options: " screens)))

  (println "\nPASS: desktop-host automated QA (A–C shell + meta)")
  (println "PASS: look-and-feel approved (full desktop package)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
