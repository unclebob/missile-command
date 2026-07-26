#!/usr/bin/env bb
;; Executable QA for scoring-and-multiplier.

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
    (Long/parseLong v)))

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
  [{:keys [scenario-path events-path extra timeout-ms]
    :or {timeout-ms 45000 extra ""}}]
  (let [cmd (str "bb play 800 600 --qa --no-keyfocus --qa-speed 10"
                 (when (seq extra) (str " " extra))
                 (when scenario-path (str " --qa-scenario " scenario-path))
                 (when events-path (str " --qa-events " events-path)))]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out)})))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/scoring-and-multiplier.qa.md"))
           "missing procedure")
  (assert! (.exists (io/file "features/scoring-and-multiplier.feature"))
           "missing feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)--qa\b" readme) "README missing --qa")
    (assert! (re-find #"(?m)score=" readme) "README missing score= telemetry")
    (assert! (re-find #"(?m)multiplier=" readme) "README missing multiplier="))

  (let [c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: initial score/mult/wave
  (write-edn! "tmp/score-init.edn" {:screen :playing})
  (write-events! "tmp/score-init-events.txt" ["wait 0.15" "quit"])
  (let [r (launch! {:scenario-path "tmp/score-init.edn"
                    :events-path "tmp/score-init-events.txt"})
        s0 (first (:sims r))]
    (assert! s0 (str "no telemetry: " (:out r)))
    (assert! (= 0 (long-field s0 "score")) (str "score start: " s0))
    (assert! (= 1 (long-field s0 "multiplier")) (str "mult start: " s0))
    (assert! (= 1 (long-field s0 "wave")) (str "wave start: " s0)))

  ;; C: fireball kill awards 25 while another enemy remains
  ;; Stage :screen :playing — title freezes combat (no score / no motion).
  (write-edn! "tmp/score-kill.edn"
              {:screen :playing
               :wave 1
               :enemies [{:origin [217 0] :target [:city 1]}
                         {:origin [339 0] :target [:city 2]}]})
  (write-events! "tmp/score-kill-events.txt" ["wait 1.5" "quit"])
  (let [r (launch! {:scenario-path "tmp/score-kill.edn"
                    :events-path "tmp/score-kill-events.txt"
                    ;; Static fireball TTL≈1s; place near sky so enemy 1 enters it first.
                    :extra "--qa-fireball 217,40,50"})
        kill (first (filter (fn [line]
                              (and (= 25 (long-field line "score"))
                                   (let [n (long-field line "enemy_missiles")]
                                     (and n (pos? n)))
                                   (re-find #"last_enemy_fate=fireball" line)))
                            (:sims r)))]
    (assert! kill (str "expected score=25 fireball kill with enemy flying: "
                       (take-last 5 (:sims r)))))

  ;; C2: wave 3 kill awards 50 (2×)
  (write-edn! "tmp/score-kill-w3.edn"
              {:screen :playing
               :wave 3
               :enemies [{:origin [217 0] :target [:city 1]}
                         {:origin [339 0] :target [:city 2]}]})
  (write-events! "tmp/score-kill-w3-events.txt" ["wait 1.5" "quit"])
  (let [r (launch! {:scenario-path "tmp/score-kill-w3.edn"
                    :events-path "tmp/score-kill-w3-events.txt"
                    :extra "--qa-fireball 217,40,50"})
        kill (first (filter (fn [line]
                              (and (= 50 (long-field line "score"))
                                   (= 2 (long-field line "multiplier"))
                                   (re-find #"last_enemy_fate=fireball" line)))
                            (:sims r)))]
    (assert! kill (str "expected score=50 at mult 2: " (take-last 5 (:sims r)))))

  ;; D: wave-end bonus after attack 3 clears (3 ballistics hit → 3 cities left).
  ;; Score: 3 cities×100 + 30 ammo×5 = 450 at mult 1 (sequential 3-attack wave).
  (write-edn! "tmp/score-wave-end.edn"
              {:screen :playing
               :wave 1
               :wave-attack 3
               :batteries {:left {:ammo 10} :center {:ammo 10} :right {:ammo 10}}
               :enemies []})
  (write-events! "tmp/score-wave-end-events.txt" ["wait 2.5" "quit"])
  (let [r (launch! {:scenario-path "tmp/score-wave-end.edn"
                    :events-path "tmp/score-wave-end-events.txt"})
        awarded (first (filter (fn [line]
                                 (let [sc (long-field line "score")
                                       w (long-field line "wave")]
                                   (and sc (>= sc 450) w (>= w 2))))
                               (:sims r)))]
    (assert! awarded (str "expected wave-end score>=450: " (last (:sims r))))
    (assert! (= 450 (long-field awarded "score"))
             (str "exact wave-end 450: " awarded)))

  ;; D2: wave 3 attack 3 clear → 900 at 2× (3 cities×200 + 30 ammo×10)
  (write-edn! "tmp/score-wave-end-w3.edn"
              {:screen :playing
               :wave 3
               :wave-attack 3
               :batteries {:left {:ammo 10} :center {:ammo 10} :right {:ammo 10}}
               :enemies []})
  (write-events! "tmp/score-wave-end-w3-events.txt" ["wait 2.5" "quit"])
  (let [r (launch! {:scenario-path "tmp/score-wave-end-w3.edn"
                    :events-path "tmp/score-wave-end-w3-events.txt"})
        awarded (first (filter #(= 900 (long-field % "score")) (:sims r)))]
    (assert! awarded (str "expected wave-end 900: " (last (:sims r)))))

  ;; F: multiplier schedule via staged waves (short waits)
  (doseq [[w mult] [[1 1] [2 1] [3 2] [11 6] [13 6]]]
    (write-edn! (str "tmp/score-mult-" w ".edn")
                {:screen :playing
                 :wave w
                 :enemies [{:origin [96 0] :target [:city 0]}]})
    (write-events! "tmp/score-mult-events.txt" ["wait 0.12" "quit"])
    (let [r (launch! {:scenario-path (str "tmp/score-mult-" w ".edn")
                      :events-path "tmp/score-mult-events.txt"})
          s0 (first (:sims r))]
      (assert! s0 (str "no sim for wave " w))
      (assert! (= mult (long-field s0 "multiplier"))
               (str "wave " w " mult: " s0))))

  (println "\nPASS: scoring-and-multiplier automated QA (A–F)")
  (println "MANUAL PENDING: look-and-feel (score/mult HUD readability)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
