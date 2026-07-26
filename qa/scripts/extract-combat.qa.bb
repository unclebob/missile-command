#!/usr/bin/env bb
;; Executable QA for extract-combat (defensive/fireball phase).

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
  [{:keys [events-path scores-path timeout-ms]
    :or {timeout-ms 45000}}]
  (let [cmd (str "bb play 800 600 --qa --no-keyfocus --qa-speed 8"
                 (when scores-path (str " --scores-file " scores-path))
                 (when events-path (str " --qa-events " events-path)))]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out)})))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/extract-combat.qa.md"))
           "missing procedure")
  (assert! (.exists (io/file "src/missile_command/combat.cljc"))
           "missing combat module")
  (assert! (.exists (io/file "docs/architecture/plans/pr-04-extract-combat.md"))
           "missing plan")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)missiles_in_flight=" readme)
             "README missing missiles_in_flight="))

  (let [c (run! "arch" "bb arch-check")
        p (run! "property" "bb property")]
    (assert! (zero? (:exit c)) "arch failed")
    (assert! (zero? (:exit p)) "property failed"))

  ;; B: extraction static
  (let [mod (slurp "src/missile_command/combat.cljc")
        core (slurp "src/missile_command/core.cljc")]
    (assert! (re-find #"defn tick-defensive" mod) "B combat missing tick-defensive")
    (assert! (re-find #"defn tick-fireballs" mod) "B combat missing tick-fireballs")
    (assert! (re-find #"defn destroy-targets-in-fireballs" mod)
             "B combat missing destroy-targets-in-fireballs")
    (assert! (re-find #"tick-defensive-phase|tick-defensive" core)
             "B core must call combat defensive path")
    (assert! (not (re-find #"defn- tick-defensive-missiles" core))
             "B core still defines tick-defensive-missiles")
    (assert! (not (re-find #"defn- tick-fireballs" core))
             "B core still defines private tick-fireballs"))

  ;; C: host fire path still works
  (write-events! "tmp/ec-fire.txt"
                 ["wait 0.1" "start" "aim 400 200" "key z" "wait 0.15" "quit"])
  (let [r (launch! {:events-path "tmp/ec-fire.txt"
                    :scores-path "tmp/ec-empty.edn"})
        fired (first (filter #(and (= "playing" (field % "screen"))
                                   (pos? (or (long-field % "missiles_in_flight") 0)))
                             (:sims r)))]
    (assert! fired (str "C never missiles_in_flight after fire: "
                        (mapv #(field % "missiles_in_flight") (:sims r)))))

  (println "\nPASS: extract-combat automated QA (A–C)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
