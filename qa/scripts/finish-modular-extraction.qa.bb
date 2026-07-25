#!/usr/bin/env bb
;; Executable QA for finish-modular-extraction.

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
      {:exit (:exit r) :out out :sims (sims out)})))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/finish-modular-extraction.qa.md"))
           "missing procedure")
  (assert! (.exists (io/file "src/missile_command/combat.cljc")) "missing combat")
  (assert! (.exists (io/file "src/missile_command/jvm/cli.clj")) "missing jvm.cli")
  (assert! (.exists (io/file "src/missile_command/jvm/telemetry.clj")) "missing telemetry")
  (assert! (.exists (io/file "src/missile_command/jvm/scenario.clj")) "missing scenario")
  (assert! (.exists (io/file "src/missile_command/testing.cljc")) "missing testing")

  (let [c (run! "arch" "bb arch-check")
        p (run! "property" "bb property")]
    (assert! (zero? (:exit c)) "arch failed")
    (assert! (zero? (:exit p)) "property failed"))

  ;; B: modular extraction static checks
  (let [combat (slurp "src/missile_command/combat.cljc")
        core (slurp "src/missile_command/core.cljc")
        testing (slurp "src/missile_command/testing.cljc")
        input (slurp "src/missile_command/jvm/input.clj")
        plans (slurp "docs/architecture/plans/README.md")
        jvm (slurp "src/missile_command/jvm/sketch.clj")
        br (slurp "src/missile_command/browser/main.cljs")]
    (assert! (re-find #"defn tick-enemies" combat) "B combat missing tick-enemies")
    (assert! (re-find #"defn tick-flyers" combat) "B combat missing tick-flyers")
    (assert! (re-find #"defn tick-playing-combat" combat)
             "B combat missing tick-playing-combat")
    (assert! (re-find #"defn tick-defensive" combat) "B combat missing tick-defensive")
    (assert! (re-find #"combat/tick-playing-combat" core)
             "B core must call combat/tick-playing-combat")
    (assert! (not (re-find #"defn- tick-enemy-missiles" core))
             "B core still has private enemy tick")
    (assert! (not (re-find #"defn- tick-flyers" core))
             "B core still has private flyer tick")
    (assert! (re-find #"not for production hosts" testing)
             "B testing missing host warning")
    (assert! (re-find #"route-enemy-through-point|defn route-" testing)
             "B testing missing route helpers")
    (assert! (re-find #"jvm\.cli|jvm\.scenario|jvm\.telemetry" input)
             "B input must require split modules")
    (assert! (re-find #"(?i)extract-combat.*Done" plans)
             "B plan should mark extract-combat Done")
    (assert! (re-find #"(?i)host-input-split.*Done" plans)
             "B plan should mark host-input-split Done")
    (assert! (not (re-find #"missile-command\.testing" jvm))
             "B jvm sketch must not require testing")
    (assert! (not (re-find #"missile-command\.testing" br))
             "B browser must not require testing"))

  ;; C: host scenario + fire still works after split/extraction
  (write-edn! "tmp/fme.edn" {:screen :playing :wave 1})
  (write-events! "tmp/fme.txt"
                 ["wait 0.15" "aim 400 200" "key z" "wait 0.2" "quit"])
  (let [r (launch! {:scenario-path "tmp/fme.edn"
                    :events-path "tmp/fme.txt"
                    :scores-path "tmp/fme-empty.edn"})
        a1 (first (filter #(and (= "playing" (field % "screen"))
                                (= 1 (long-field % "wave_attack")))
                          (:sims r)))
        fired (first (filter #(and (= "playing" (field % "screen"))
                                   (pos? (or (long-field % "missiles_in_flight") 0)))
                             (:sims r)))]
    (assert! a1 (str "C never attack 1: "
                     (mapv #(field % "wave_attack") (:sims r))))
    (assert! fired (str "C never missiles_in_flight after fire: "
                        (mapv #(field % "missiles_in_flight") (:sims r)))))

  (println "\nPASS: finish-modular-extraction automated QA (A–C)")
  (println "PASS: look-and-feel deferred per user (skip until further notice)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
