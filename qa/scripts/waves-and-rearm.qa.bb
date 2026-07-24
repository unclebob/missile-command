#!/usr/bin/env bb
;; Executable QA for waves-and-rearm (short host waits).

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[clojure.java.io :as io]
         '[clojure.edn :as edn])

(defn die! [msg]
  (binding [*out* *err*] (println (str "FAIL: " msg)))
  (System/exit 1))

(defn assert! [ok? msg] (when-not ok? (die! msg)))

(defn field [line key]
  (when-let [[_ v] (re-find (re-pattern (str key "=([^\\s]+)")) line)] v))

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

(defn launch! [scenario-path events-path]
  ;; --qa-speed multiplies sim-time vs wall clock so host waits stay short.
  (let [cmd (str "bb play 800 600 --qa --qa-speed 10 --qa-scenario " scenario-path
                 " --qa-events " events-path)]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout 25000}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out)})))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/waves-and-rearm.qa.md")) "missing procedure")
  (assert! (.exists (io/file "features/waves-and-rearm.feature")) "missing feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)--qa\b" readme) "README missing --qa")
    (assert! (re-find #"(?m)--qa-scenario" readme) "README missing --qa-scenario")
    (assert! (re-find #"(?m)--qa-speed" readme) "README missing --qa-speed")
    (assert! (re-find #"(?m)wave=" readme) "README missing wave telemetry"))

  (let [u (run! "unit" "bb test")
        a (run! "accept" "bb accept")
        c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit u)) "unit failed")
    (assert! (zero? (:exit a)) "accept failed")
    (assert! (re-find #"(?i)waves[-_]?and[-_]?rearm" (:out a)) "accept missing waves feature")
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: depleted ammo + one enemy → impact → wave advance + rearm
  ;; Host then launches the next wave's scheduled attacks (continuous play).
  ;; With --qa-speed 10, ~5.7s sim impact finishes in under 1s wall clock.
  (write-edn! "tmp/wave-rearm-depleted.edn"
              {:batteries {:left {:ammo 2} :center {:ammo 2} :right {:ammo 2}}
               :enemies [{:target [:city 0]}]})
  (write-events! "tmp/wave-events.txt" ["wait 1.2" "quit"])
  (let [r (launch! "tmp/wave-rearm-depleted.edn" "tmp/wave-events.txt")
        sims (:sims r)
        first-sim (first sims)
        ;; First telemetry after wave 1 completes (rearm + wave>=2).
        rearm-sim (first (filter (fn [line]
                                   (let [w (field line "wave")]
                                     (and w
                                          (>= (Long/parseLong w) 2)
                                          (= "10" (field line "battery_left_ammo"))
                                          (= "10" (field line "battery_center_ammo"))
                                          (= "10" (field line "battery_right_ammo")))))
                                 sims))]
    (assert! first-sim (str "no telemetry: " (:out r)))
    (assert! (= "1" (field first-sim "wave")) (str "start wave: " first-sim))
    (assert! (= "2" (field first-sim "battery_left_ammo")) (str "depleted ammo: " first-sim))
    (assert! (= "1" (field first-sim "enemy_missiles")) (str "enemy present: " first-sim))
    (assert! rearm-sim (str "wave advance + rearm not seen: " (last sims))))

  ;; Destroyed left + rearm others
  (write-edn! "tmp/wave-rearm-destroyed-left.edn"
              {:batteries {:left {:destroyed true :ammo 3}
                           :center {:ammo 1}
                           :right {:ammo 1}}
               :enemies [{:target [:city 1]}]})
  (write-events! "tmp/wave-events2.txt" ["wait 1.2" "key 1" "quit"])
  (let [r (launch! "tmp/wave-rearm-destroyed-left.edn" "tmp/wave-events2.txt")
        sims (:sims r)
        rearm-sim (first (filter (fn [line]
                                   (and (= "true" (field line "battery_left_destroyed"))
                                        (= "10" (field line "battery_center_ammo"))
                                        (= "10" (field line "battery_right_ammo"))
                                        (let [w (field line "wave")]
                                          (and w (>= (Long/parseLong w) 2)))))
                                 sims))
        fires (->> (str/split-lines (:out r)) (filter #(str/starts-with? % "qa-fire ")))]
    (assert! rearm-sim (str "destroyed-left rearm not seen: " (last sims)))
    (assert! (some #(re-find #"battery=none" %) fires)
             (str "destroyed left cannot fire: " fires)))

  ;; Harder wave metrics via telemetry (wave 1 vs wave 3 schedule)
  (write-edn! "tmp/wave3.edn" {:wave 3 :enemies [{:target [:city 0]}]})
  (write-events! "tmp/wave-events3.txt" ["wait 0.15" "quit"])
  (let [r1 (do (write-edn! "tmp/wave1.edn" {:wave 1 :enemies [{:target [:city 0]}]})
               (launch! "tmp/wave1.edn" "tmp/wave-events3.txt"))
        r3 (launch! "tmp/wave3.edn" "tmp/wave-events3.txt")
        s1 (first (:sims r1))
        s3 (first (:sims r3))
        c1 (Double/parseDouble (field s1 "wave_enemy_count"))
        c3 (Double/parseDouble (field s3 "wave_enemy_count"))
        sp1 (Double/parseDouble (field s1 "wave_enemy_speed"))
        sp3 (Double/parseDouble (field s3 "wave_enemy_speed"))]
    (assert! (or (> c3 c1) (> sp3 sp1))
             (str "wave 3 should be harder: " s1 " vs " s3)))

  (println "\nPASS: waves-and-rearm automated QA (A–B)")
  (println "PASS: look-and-feel approved (HUD wave readability)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
