#!/usr/bin/env bb
;; Executable QA for sequential-attacks-banner (3 salvos then WAVE N banner).

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
    :or {timeout-ms 90000}}]
  (let [cmd (str "bb play 800 600 --qa --no-keyfocus --qa-speed 12"
                 (when scores-path (str " --scores-file " scores-path))
                 (when scenario-path (str " --qa-scenario " scenario-path))
                 (when events-path (str " --qa-events " events-path)))]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out)})))

(defn first-attack [sims n]
  (first (filter #(and (= "playing" (field % "screen"))
                       (= n (long-field % "wave_attack")))
                 sims)))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/sequential-attacks-banner.qa.md"))
           "missing procedure")
  (assert! (.exists (io/file "features/wave-banner.feature")) "missing wave-banner feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)wave_attack=" readme) "README missing wave_attack=")
    (assert! (re-find #"(?m)wave_attacks_per_wave=" readme) "README missing attacks_per_wave"))

  (let [c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: wave 1 attack 1 ballistic only
  (write-events! "tmp/seq-a1.txt" ["wait 0.1" "start" "wait 0.2" "quit"])
  (let [r (launch! {:events-path "tmp/seq-a1.txt"
                    :scores-path "tmp/seq-empty.edn"
                    :timeout-ms 45000})
        a1 (first-attack (:sims r) 1)]
    (assert! a1 (str "B never attack 1: " (mapv #(field % "wave_attack") (:sims r))))
    (assert! (= 1 (long-field a1 "wave")) (str "B wave: " a1))
    (assert! (= 3 (long-field a1 "ballistic_missiles")) (str "B ballistics: " a1))
    (assert! (= 0 (long-field a1 "mirv_parents")) (str "B no MIRV on a1: " a1))
    (assert! (= 0 (long-field a1 "flyers_bomber")) (str "B no bomber on a1: " a1)))

  ;; C: natural advance 1 → 2 after first salvo clears
  (write-events! "tmp/seq-a2.txt" ["wait 0.1" "start" "wait 20" "quit"])
  (let [r (launch! {:events-path "tmp/seq-a2.txt"
                    :scores-path "tmp/seq-empty.edn"
                    :timeout-ms 90000})
        a1 (first-attack (:sims r) 1)
        a2 (first-attack (:sims r) 2)]
    (assert! a1 "C missing attack 1")
    (assert! a2 (str "C missing attack 2 after clear: "
                     (mapv #(field % "wave_attack") (:sims r))))
    (assert! (= 0 (long-field a2 "mirv_parents")) (str "C a2 specials: " a2)))

  ;; D: wave 9 final attack staged with specials
  (write-edn! "tmp/seq-w9-a3.edn" {:wave 9 :screen :playing :wave-attack 3})
  (write-events! "tmp/seq-w9-a3.txt" ["wait 0.25" "quit"])
  (let [r (launch! {:scenario-path "tmp/seq-w9-a3.edn"
                    :events-path "tmp/seq-w9-a3.txt"
                    :scores-path "tmp/seq-empty.edn"
                    :timeout-ms 45000})
        a3 (first-attack (:sims r) 3)]
    (assert! a3 (str "D never a3: " (mapv #(field % "wave_attack") (:sims r))))
    (assert! (pos? (long-field a3 "mirv_parents")) (str "D a3 need MIRVs: " a3))
    (assert! (pos? (long-field a3 "smart_bombs")) (str "D a3 need smarts: " a3))
    (assert! (= 1 (long-field a3 "flyers_bomber")) (str "D a3 bomber: " a3))
    (assert! (= 1 (long-field a3 "flyers_satellite")) (str "D a3 sat: " a3)))

  ;; E: banner after final attack clears — stage attack 3; keep reserve so
  ;; city losses cannot skip to THE END before banner.
  (write-edn! "tmp/seq-banner.edn"
              {:wave 1
               :screen :playing
               :wave-attack 3
               :bonus-cities 6
               :batteries {:left {:ammo 0} :center {:ammo 0} :right {:ammo 0}}
               :enemies []})
  (write-events! "tmp/seq-banner.txt" ["wait 20" "quit"])
  (let [r (launch! {:scenario-path "tmp/seq-banner.edn"
                    :events-path "tmp/seq-banner.txt"
                    :scores-path "tmp/seq-empty.edn"
                    :timeout-ms 90000})
        all (:sims r)
        a3 (first-attack all 3)
        banner (first (filter #(= "wave-banner" (field % "screen")) all))]
    (assert! a3 (str "E never a3: " (mapv #(field % "wave_attack") all)))
    (assert! banner (str "E never banner after last attack: "
                         (mapv #(field % "screen") all)))
    (assert! (= "WAVE_2" (field banner "banner_text")) (str "E banner: " banner)))

  (println "\nPASS: sequential-attacks-banner automated QA (A–E)")
  (println "PASS: look-and-feel approved (3 salvos then WAVE banner)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
