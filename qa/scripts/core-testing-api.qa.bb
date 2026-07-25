#!/usr/bin/env bb
;; Executable QA for core-testing-api + seedable sky RNG.

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

(defn origin-xs
  [line]
  (->> (re-seq #"enemy_origin_x=([^\s]+)" line)
       (map (fn [[_ v]] (Double/parseDouble v)))
       vec))

(defn attack1-line [sim-lines]
  (first (filter #(and (= "playing" (field % "screen"))
                       (= 1 (long-field % "wave_attack"))
                       (= 3 (long-field % "ballistic_missiles")))
                 sim-lines)))

(defn origins-for-seed [seed scores-path]
  (write-edn! (str "tmp/cta-seed-" seed ".edn")
              {:screen :playing :wave 1 :rng-seed seed})
  (write-events! (str "tmp/cta-seed-" seed ".txt") ["wait 0.25" "quit"])
  (let [r (launch! {:scenario-path (str "tmp/cta-seed-" seed ".edn")
                    :events-path (str "tmp/cta-seed-" seed ".txt")
                    :scores-path scores-path})
        line (attack1-line (:sims r))]
    (assert! line (str "never attack1 for seed " seed ": "
                       (mapv #(field % "wave_attack") (:sims r))))
    (let [xs (origin-xs line)]
      (assert! (= 3 (count xs)) (str "need 3 origins seed " seed ": " xs))
      (assert! (every? #(and (>= % 0.0) (< % 800.0)) xs)
               (str "origin out of range seed " seed ": " xs))
      xs)))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/core-testing-api.qa.md"))
           "missing procedure")
  (assert! (.exists (io/file "src/missile_command/testing.cljc"))
           "missing testing facade")
  (assert! (.exists (io/file "src/missile_command/rng.cljc"))
           "missing rng module")
  (assert! (.exists (io/file "docs/architecture/plans/pr-06-core-testing-api.md"))
           "missing testing plan")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)enemy_origin_x=" readme) "README missing enemy_origin_x="))

  (let [u (run! "unit" "bb test")
        a (run! "accept" "bb accept")
        c (run! "arch" "bb arch-check")
        p (run! "property" "bb property")]
    (assert! (zero? (:exit u)) "unit failed")
    (assert! (zero? (:exit a)) "accept failed")
    (assert! (zero? (:exit c)) "arch failed")
    (assert! (zero? (:exit p)) "property failed"))

  ;; B: facade + hosts avoid testing ns
  (let [testing (slurp "src/missile_command/testing.cljc")
        rng (slurp "src/missile_command/rng.cljc")
        jvm (slurp "src/missile_command/jvm/sketch.clj")
        br (slurp "src/missile_command/browser/main.cljs")
        jvm-in (slurp "src/missile_command/jvm/input.clj")]
    (assert! (re-find #"not for production hosts" testing)
             "B testing ns missing host warning")
    (assert! (re-find #"with-rng-seed" testing) "B testing missing with-rng-seed")
    (assert! (re-find #"route-enemy-through-point" testing)
             "B testing missing route helper")
    (assert! (re-find #"defn with-seed|defn seed" rng) "B rng missing seed API")
    (assert! (re-find #"next-sky-origin-x" rng) "B rng missing next-sky-origin-x")
    (assert! (not (re-find #"missile-command\.testing" jvm))
             "B jvm sketch must not require testing")
    (assert! (not (re-find #"missile-command\.testing" br))
             "B browser must not require testing")
    (assert! (re-find #"rng-seed|with-rng-seed" jvm-in)
             "B jvm scenario loader must support rng-seed"))

  ;; C: same seed → same origins
  (let [a (origins-for-seed 42 "tmp/cta-empty-a.edn")
        b (origins-for-seed 42 "tmp/cta-empty-b.edn")]
    (assert! (= a b) (str "C same seed must match: " a " vs " b)))

  ;; D: different seeds → different origins
  (let [a (origins-for-seed 42 "tmp/cta-empty-c.edn")
        d (origins-for-seed 99 "tmp/cta-empty-d.edn")]
    (assert! (not= a d) (str "D different seeds should differ: " a " vs " d)))

  (println "\nPASS: core-testing-api automated QA (A–D)")
  (println "PASS: look-and-feel deferred per user (skip until further notice)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
