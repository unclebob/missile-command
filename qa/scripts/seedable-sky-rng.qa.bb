#!/usr/bin/env bb
;; Executable QA for seedable-sky-rng.

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

(defn launch! [{:keys [scenario-path events-path scores-path timeout-ms]
                :or {timeout-ms 45000}}]
  (let [cmd (str "bb play 800 600 --qa --no-keyfocus --qa-speed 8"
                 (when scores-path (str " --scores-file " scores-path))
                 (when scenario-path (str " --qa-scenario " scenario-path))
                 (when events-path (str " --qa-events " events-path)))]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out)})))

(defn origin-xs [line]
  (->> (re-seq #"enemy_origin_x=([^\s]+)" line)
       (map (fn [[_ v]] (Double/parseDouble v)))
       vec))

(defn attack1-line [sim-lines]
  (first (filter #(and (= "playing" (field % "screen"))
                       (= 1 (long-field % "wave_attack"))
                       (= 3 (long-field % "ballistic_missiles")))
                 sim-lines)))

(defn origins-for-seed [seed scores]
  (write-edn! (str "tmp/ssr-" seed ".edn")
              {:screen :playing :wave 1 :rng-seed seed})
  (write-events! (str "tmp/ssr-" seed ".txt") ["wait 0.25" "quit"])
  (let [r (launch! {:scenario-path (str "tmp/ssr-" seed ".edn")
                    :events-path (str "tmp/ssr-" seed ".txt")
                    :scores-path scores})
        line (attack1-line (:sims r))]
    (assert! line (str "no attack1 seed " seed))
    (let [xs (origin-xs line)]
      (assert! (= 3 (count xs)) (str "need 3 origins: " xs))
      xs)))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/seedable-sky-rng.qa.md")) "missing procedure")
  (assert! (.exists (io/file "src/missile_command/rng.cljc")) "missing rng")
  (assert! (.exists (io/file "docs/architecture/plans/pr-08-seedable-sky-rng.md")) "missing plan")

  (let [c (run! "arch" "bb arch-check")
        p (run! "property" "bb property")]
    (assert! (zero? (:exit c)) "arch failed")
    (assert! (zero? (:exit p)) "property failed")
    (assert! (re-find #"(?i)rng" (:out p)) "property missing rng suite"))

  (let [rng (slurp "src/missile_command/rng.cljc")
        scenario (slurp "src/missile_command/jvm/scenario.clj")]
    (assert! (re-find #"next-sky-origin-x" rng) "B rng missing next-sky-origin-x")
    (assert! (re-find #"rng-seed" scenario) "B scenario loader missing rng-seed"))

  (let [a (origins-for-seed 42 "tmp/ssr-a.edn")
        b (origins-for-seed 42 "tmp/ssr-b.edn")
        d (origins-for-seed 99 "tmp/ssr-d.edn")]
    (assert! (= a b) (str "C same seed: " a " vs " b))
    (assert! (not= a d) (str "C different seeds: " a " vs " d)))

  (println "\nPASS: seedable-sky-rng automated QA (A–C)")
  (println "PASS: look-and-feel deferred per user")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
