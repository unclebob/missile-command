#!/usr/bin/env bb
;; Executable QA for extract-bonus-cities module extraction.

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

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/extract-bonus-cities.qa.md"))
           "missing procedure")
  (assert! (.exists (io/file "src/missile_command/bonus_cities.cljc"))
           "missing bonus_cities module")
  (assert! (.exists (io/file "docs/architecture/plans/pr-03-extract-bonus-cities.md"))
           "missing plan")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)bonus_cities=" readme) "README missing bonus_cities="))

  (let [c (run! "arch" "bb arch-check")
        p (run! "property" "bb property")]
    (assert! (zero? (:exit c)) "arch failed")
    (assert! (zero? (:exit p)) "property failed"))

  ;; B: extraction static checks
  (let [mod (slurp "src/missile_command/bonus_cities.cljc")
        core (slurp "src/missile_command/core.cljc")]
    (assert! (re-find #"defn apply-from-reserve" mod) "B module missing apply-from-reserve")
    (assert! (re-find #"defn sync-from-score" mod) "B module missing sync-from-score")
    (assert! (re-find #"bc/apply-from-reserve|apply-bonus-cities-from-reserve" core)
             "B core must re-export apply")
    (assert! (re-find #"bc/sync-from-score" core) "B core must call bc/sync-from-score")
    (assert! (not (re-find #"defn-? lowest-destroyed-city-id" core))
             "B core still has lowest-destroyed-city-id"))

  ;; C: mid-wave host — reserve held, cities not restored
  (write-edn! "tmp/ebc-mid.edn"
              {:screen :playing
               :wave 1
               :score 10000
               :bonus-cities 1
               :cities {:destroyed [0 1]}
               :enemies [{:target [:city 2]}]})
  (write-events! "tmp/ebc-mid.txt" ["wait 0.2" "quit"])
  (let [r (launch! {:scenario-path "tmp/ebc-mid.edn"
                    :events-path "tmp/ebc-mid.txt"
                    :scores-path "tmp/ebc-empty.edn"})
        p (first (filter #(= "playing" (field % "screen")) (:sims r)))]
    (assert! p (str "C never playing: " (last (:sims r))))
    (assert! (= 4 (long-field p "cities_alive"))
             (str "C mid-wave should stay 4 living: " p))
    (assert! (>= (long-field p "bonus_cities") 1)
             (str "C reserve should be held: " p)))

  (println "\nPASS: extract-bonus-cities automated QA (A–C)")
  (println "PASS: look-and-feel deferred per user (skip until further notice)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
