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
  (let [cmd (str "bb play 800 600 --qa --no-keyfocus --qa-speed 10 --qa-scenario " scenario-path
                 " --qa-events " events-path)]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout 45000}
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

  (let [c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: final attack staged with one remaining enemy (empty list + one spawn).
  ;; wave-attack 3 + empty sky would start a full salvo; stage one enemy so
  ;; impact completes the last attack → banner → rearm on wave 2.
  (write-edn! "tmp/wave-rearm-depleted.edn"
              {:screen :playing
               :wave 1
               :wave-attack 3
               :bonus-cities 6
               :batteries {:left {:ammo 2} :center {:ammo 2} :right {:ammo 2}}
               :enemies []})
  ;; Apply single enemy via a second scenario path: use enemies after attack set.
  ;; Scenario applies enemies then wave-attack (order may spawn full attack).
  ;; Prefer: wave-attack 3 with one enemy only — if attack replace wipes, use empty
  ;; enemies and rely on wait for natural... Use enemies only (no wave-attack) after
  ;; banner path: stage playing attack 3 empty, wait for ensure? No — empty + attack
  ;; 3 means attack already set. If enemies empty and attack 3, attack-cleared? may
  ;; complete wave immediately.
  (write-events! "tmp/wave-events.txt" ["wait 4.0" "quit"])
  (let [r (launch! "tmp/wave-rearm-depleted.edn" "tmp/wave-events.txt")
        sims (:sims r)
        first-sim (first (filter #(= "playing" (field % "screen")) sims))
        rearm-sim (first (filter (fn [line]
                                   (let [w (field line "wave")]
                                     (and w
                                          (>= (Long/parseLong w) 2)
                                          (= "playing" (field line "screen"))
                                          (= "10" (field line "battery_left_ammo"))
                                          (= "10" (field line "battery_center_ammo"))
                                          (= "10" (field line "battery_right_ammo")))))
                                 sims))]
    (assert! first-sim (str "no playing telemetry: " (:out r)))
    (assert! (= "1" (field first-sim "wave")) (str "start wave: " first-sim))
    (assert! (= "2" (field first-sim "battery_left_ammo")) (str "depleted ammo: " first-sim))
    (assert! rearm-sim (str "wave advance + rearm not seen: " (last sims))))

  ;; Destroyed left before rearm: cannot fire (battery=none); after wave
  ;; rearm, destroyed bases return with full ammo (US-08).
  (write-edn! "tmp/wave-rearm-destroyed-left.edn"
              {:screen :playing
               :wave 1
               :wave-attack 3
               :bonus-cities 6
               :batteries {:left {:destroyed true :ammo 3}
                           :center {:ammo 1}
                           :right {:ammo 1}}
               :enemies []})
  (write-events! "tmp/wave-events2.txt" ["wait 0.2" "key 1" "wait 4.0" "key 1" "quit"])
  (let [r (launch! "tmp/wave-rearm-destroyed-left.edn" "tmp/wave-events2.txt")
        sims (:sims r)
        pre-fires (->> (str/split-lines (:out r))
                       (filter #(str/starts-with? % "qa-fire "))
                       vec)
        rearm-sim (first (filter (fn [line]
                                   (and (= "false" (field line "battery_left_destroyed"))
                                        (= "10" (field line "battery_left_ammo"))
                                        (= "10" (field line "battery_center_ammo"))
                                        (= "10" (field line "battery_right_ammo"))
                                        (= "playing" (field line "screen"))
                                        (let [w (field line "wave")]
                                          (and w (>= (Long/parseLong w) 2)))))
                                 sims))]
    (assert! (some #(re-find #"battery=none" %) pre-fires)
             (str "destroyed left cannot fire pre-rearm: " pre-fires))
    (assert! rearm-sim (str "destroyed-left rearm restore not seen: " (last sims))))

  ;; Harder wave metrics via telemetry (wave 1 vs wave 3 schedule)
  (write-edn! "tmp/wave3.edn" {:screen :playing :wave 3})
  (write-events! "tmp/wave-events3.txt" ["wait 0.25" "quit"])
  (let [r1 (do (write-edn! "tmp/wave1.edn" {:screen :playing :wave 1})
               (launch! "tmp/wave1.edn" "tmp/wave-events3.txt"))
        r3 (launch! "tmp/wave3.edn" "tmp/wave-events3.txt")
        s1 (first (filter #(= "playing" (field % "screen")) (:sims r1)))
        s3 (first (filter #(= "playing" (field % "screen")) (:sims r3)))
        c1 (Double/parseDouble (field s1 "wave_enemy_count"))
        c3 (Double/parseDouble (field s3 "wave_enemy_count"))
        sp1 (Double/parseDouble (field s1 "wave_enemy_speed"))
        sp3 (Double/parseDouble (field s3 "wave_enemy_speed"))]
    (assert! s1 (str "missing wave1 playing: " (last (:sims r1))))
    (assert! s3 (str "missing wave3 playing: " (last (:sims r3))))
    (assert! (or (> c3 c1) (> sp3 sp1))
             (str "wave 3 should be harder: " s1 " vs " s3)))

  (println "\nPASS: waves-and-rearm automated QA (A–B)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
