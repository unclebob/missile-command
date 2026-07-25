#!/usr/bin/env bb
;; Executable QA for enemy-missiles-impacts. Keep host waits short (~5s each).

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[clojure.java.io :as io])

(def procedure-path "qa/procedures/enemy-missiles-impacts.qa.md")
(def feature-path "features/enemy-missiles-impacts.feature")

(defn die! [msg]
  (binding [*out* *err*] (println (str "FAIL: " msg)))
  (System/exit 1))

(defn assert! [ok? msg] (when-not ok? (die! msg)))

(defn field [line key]
  (when-let [[_ v] (re-find (re-pattern (str key "=([^\\s]+)")) line)] v))

(defn lines-with [out prefix]
  (->> (str/split-lines out) (map str/trim) (filter #(str/starts-with? % prefix)) vec))

(defn write-events! [path lines]
  (io/make-parents path)
  (spit path (str (str/join "\n" lines) "\n")))

(defn run-doc! [label cmd]
  (println (str "==> " label ": " cmd) "\n")
  (flush)
  (let [r (p/shell {:out :string :err :string :continue true} "bash" "-lc" cmd)
        out (str (:out r) (:err r))]
    (print out) (flush)
    {:exit (:exit r) :out out}))

(defn launch!
  [{:keys [extra events timeout-ms] :or {timeout-ms 25000 events []}}]
  (let [path "tmp/qa-enemy-events.txt"
        _ (write-events! path (concat events ["quit"]))
        cmd (str "bb play 800 600 --qa-telemetry "
                 (when (seq extra) (str extra " "))
                 "--qa-events " path)]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (lines-with out "qa-sim ")})))

(defn -main [& _]
  (assert! (.exists (io/file procedure-path)) "missing procedure")
  (assert! (.exists (io/file feature-path)) "missing feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)--qa-enemy" readme) "README missing --qa-enemy")
    (assert! (re-find #"(?m)enemy_missiles=" readme) "README missing enemy telemetry"))

  (let [c (run-doc! "arch" "bb arch-check")]
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: city impact
  (let [r (launch! {:extra "--qa-enemy city:0" :events ["wait 5.5"]})
        last-sim (last (:sims r))]
    (assert! last-sim (str "no sim telemetry: " (:out r)))
    (assert! (= "0" (field last-sim "enemy_missiles"))
             (str "enemy should be gone: " last-sim))
    (assert! (= "5" (field last-sim "cities_alive"))
             (str "city 0 should die -> 5 alive: " last-sim)))

  ;; B: battery impact + cannot fire
  (let [r (launch! {:extra "--qa-enemy battery:left"
                    :events ["wait 5.5" "key 1"]})
        last-sim (last (:sims r))
        fires (lines-with (:out r) "qa-fire ")]
    (assert! (= "true" (field last-sim "battery_left_destroyed"))
             (str "left battery destroyed: " last-sim))
    (assert! (some #(re-find #"battery=none" %) fires)
             (str "key-fire left should be none: " fires)))

  ;; C: fireball on city-1 path (x≈218 at 800px) intercepts and saves city
  (let [r (launch! {:extra "--qa-enemy city:1 --qa-fireball 218,200,80"
                    :events ["wait 5.5"]})
        last-sim (last (:sims r))
        fates (filter #(re-find #"last_enemy_fate=fireball" %) (:sims r))]
    (assert! (seq fates) (str "expected fireball fate: " (take-last 3 (:sims r))))
    (assert! (= "0" (field last-sim "enemy_missiles")) "enemy gone after intercept")
    (assert! (= "6" (field last-sim "cities_alive"))
             (str "city should survive intercept: " last-sim)))

  ;; D: far fireball does not save city 0
  (let [r (launch! {:extra "--qa-enemy city:0 --qa-fireball 700,100,20"
                    :events ["wait 5.5"]})
        last-sim (last (:sims r))]
    (assert! (= "5" (field last-sim "cities_alive"))
             (str "city should die when fireball is far: " last-sim))
    (assert! (not-any? #(re-find #"last_enemy_fate=fireball" %) (:sims r))
             "should not intercept with far fireball"))

  (println "\nPASS: enemy-missiles-impacts automated QA (A–D)")
  (println "MANUAL PENDING: look-and-feel approval")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
