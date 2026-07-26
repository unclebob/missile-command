#!/usr/bin/env bb
;; Executable QA for host-input-split (deferred: pure input + host path).

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[clojure.java.io :as io])

(defn die! [msg]
  (binding [*out* *err*] (println (str "FAIL: " msg)))
  (System/exit 1))

(defn assert! [ok? msg] (when-not ok? (die! msg)))

(defn field [line key]
  (when-let [[_ v] (re-find (re-pattern (str key "=([^\\s]+)")) line)] v))

(defn long-field [line key]
  (when-let [v (field line key)]
    (try (Long/parseLong v) (catch Exception _ nil))))

(defn sims [out]
  (->> (str/split-lines out) (map str/trim) (filter #(str/starts-with? % "qa-sim ")) vec))

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

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/host-input-split.qa.md")) "missing procedure")
  (assert! (.exists (io/file "docs/architecture/plans/pr-07-host-input-split.md")) "missing plan")

  (let [c (run! "arch" "bb arch-check")
        p (run! "property" "bb property")]
    (assert! (zero? (:exit c)) "arch failed")
    (assert! (zero? (:exit p)) "property failed"))

  (let [readme (slurp "docs/architecture/plans/README.md")
        input (slurp "src/missile_command/jvm/input.clj")
        cli (slurp "src/missile_command/jvm/cli.clj")
        telemetry (slurp "src/missile_command/jvm/telemetry.clj")
        scenario (slurp "src/missile_command/jvm/scenario.clj")
        sketch (slurp "src/missile_command/jvm/sketch.clj")]
    (assert! (re-find #"(?i)host-input-split.*Done" readme)
             "B plan index should mark host-input-split Done")
    (assert! (re-find #"No Quil" input) "B input should document pure/no Quil")
    (assert! (not (re-find #"quil\.|quil/" input)) "B input must not require Quil")
    (assert! (re-find #"missile-command\.jvm\.input" sketch)
             "B sketch must require jvm.input")
    (assert! (re-find #"missile-command\.jvm\.cli" cli)
             "B cli ns must exist")
    (assert! (re-find #"format-sim-telemetry-line" telemetry)
             "B telemetry must own sim telemetry")
    (assert! (re-find #"apply-scenario" scenario)
             "B scenario must own apply-scenario")
    (assert! (re-find #"apply-scenario|load-scenario|format-sim-telemetry" input)
             "B input must re-export scenario/telemetry for sketch"))

  (write-edn! "tmp/his.edn" {:screen :playing :wave 1})
  (write-events! "tmp/his.txt" ["wait 0.2" "quit"])
  (let [cmd "bb play 800 600 --qa --no-keyfocus --qa-speed 8 --scores-file tmp/his-empty.edn --qa-scenario tmp/his.edn --qa-events tmp/his.txt"
        _ (println "==> host:" cmd)
        r (p/shell {:out :string :err :string :continue true :timeout 45000}
                   "bash" "-lc" cmd)
        out (str (:out r) (:err r))
        lines (sims out)
        a1 (first (filter #(and (= "playing" (field % "screen"))
                                (= 1 (long-field % "wave_attack")))
                          lines))]
    (print out) (flush)
    (assert! a1 (str "C scenario path failed: " (mapv #(field % "screen") lines))))

  (println "\nPASS: host-input-split automated QA (A–C; full cli/telemetry/scenario split)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
