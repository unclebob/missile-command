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

(defn long-field [line key]
  (when-let [v (field line key)] (Long/parseLong v)))

(defn lines-with [out prefix]
  (->> (str/split-lines out) (map str/trim) (filter #(str/starts-with? % prefix)) vec))

(defn write-edn! [path data]
  (io/make-parents path)
  (spit path (pr-str data)))

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
  "Host run. Prefer events after start, or :screen :playing scenarios.
  Title freezes combat; start wipes staged scenario entities."
  [{:keys [scenario extra events timeout-ms]
    :or {timeout-ms 25000 events ["wait 2.0" "quit"] extra "" scenario nil}}]
  (let [scenario-path "tmp/qa-enemy-scenario.edn"
        events-path "tmp/qa-enemy-events.txt"
        _ (when scenario (write-edn! scenario-path scenario))
        _ (write-events! events-path (concat events ["quit"]))
        cmd (str "bb play 800 600 --qa --no-keyfocus --qa-speed 10 "
                 (when scenario (str "--qa-scenario " scenario-path " "))
                 (when (seq extra) (str extra " "))
                 "--qa-events " events-path)]
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

  ;; B: city impact — wave may continue after; assert first city loss.
  (let [r (launch! {:scenario {:screen :playing
                               :enemies [{:target [:city 0]}]}})
        hit (first (filter #(= 5 (long-field % "cities_alive")) (:sims r)))]
    (assert! hit (str "city 0 should die -> 5 alive: " (last (:sims r)))))

  ;; B: battery impact + cannot fire (start then enemy event; do not wipe via start after)
  (let [r (launch! {:events ["wait 0.1" "start" "enemy battery:left" "wait 2.0" "key 1"]})
        destroyed (first (filter #(= "true" (field % "battery_left_destroyed"))
                                 (:sims r)))
        fires (lines-with (:out r) "qa-fire ")]
    (assert! destroyed (str "left battery destroyed: " (last (:sims r))))
    (assert! (some #(re-find #"battery=none" %) fires)
             (str "key-fire left should be none: " fires)))

  ;; C: fireball on city-1 path intercepts staged enemy (wave may continue after).
  ;; Static fireball TTL≈1s — place near sky so the enemy enters it promptly.
  (let [r (launch! {:scenario {:screen :playing
                               :enemies [{:origin [217 0] :target [:city 1]}]}
                    :extra "--qa-fireball 217,40,50"
                    :events ["wait 1.5"]})
        fate (first (filter #(re-find #"last_enemy_fate=fireball" %) (:sims r)))]
    (assert! fate (str "expected fireball fate: " (take-last 3 (:sims r))))
    (assert! (= 6 (long-field fate "cities_alive"))
             (str "city should still be alive at intercept: " fate)))

  ;; D: far fireball does not save city 0
  (let [r (launch! {:scenario {:screen :playing
                               :enemies [{:target [:city 0]}]}
                    :extra "--qa-fireball 700,100,20"
                    :events ["wait 2.0"]})
        hit (first (filter #(= 5 (long-field % "cities_alive")) (:sims r)))]
    (assert! hit (str "city should die when fireball is far: " (last (:sims r))))
    (assert! (not-any? #(re-find #"last_enemy_fate=fireball" %) (:sims r))
             "should not intercept with far fireball"))

  (println "\nPASS: enemy-missiles-impacts automated QA (A–D)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
