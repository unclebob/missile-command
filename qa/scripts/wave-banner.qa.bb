#!/usr/bin/env bb
;; Executable QA for wave-banner (enter/exit motion, resume).

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
  (when-let [v (field line key)] (Long/parseLong v)))

(defn double-field [line key]
  (when-let [v (field line key)] (Double/parseDouble v)))

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
    :or {timeout-ms 60000}}]
  (let [cmd (str "bb play 800 600 --qa --no-keyfocus --qa-speed 10"
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
  (assert! (.exists (io/file "qa/procedures/wave-banner.qa.md")) "missing procedure")
  (assert! (.exists (io/file "features/wave-banner.feature")) "missing feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)wave-banner" readme) "README missing wave-banner")
    (assert! (re-find #"(?m)banner_text=" readme) "README missing banner_text=")
    (assert! (re-find #"(?m)banner_phase=" readme) "README missing banner_phase="))

  (let [c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B–E: finish final attack of wave 1 → WAVE 2 banner enter/exit → resume.
  ;; Stage attack 3 on :playing (start would wipe staged state).
  (write-edn! "tmp/wb.edn"
              {:wave 1
               :screen :playing
               :wave-attack 3
               :bonus-cities 6
               :batteries {:left {:ammo 0} :center {:ammo 0} :right {:ammo 0}}
               :enemies []})
  (write-events! "tmp/wb.txt" ["wait 12" "quit"])
  (let [r (launch! {:scenario-path "tmp/wb.edn"
                    :events-path "tmp/wb.txt"
                    :scores-path "tmp/wb-scores.edn"
                    :timeout-ms 90000})
        all (:sims r)
        banners (filter #(= "wave-banner" (field % "screen")) all)
        enters (filter #(= "enter" (field % "banner_phase")) banners)
        exits (filter #(= "exit" (field % "banner_phase")) banners)
        after-banner (drop-while #(not= "wave-banner" (field % "screen")) all)
        resumed (first (filter #(= "playing" (field % "screen")) after-banner))]
    (assert! (seq banners) (str "B never wave-banner: " (mapv #(field % "screen") all)))
    (let [b0 (first banners)]
      (assert! (= "WAVE_2" (field b0 "banner_text")) (str "B text: " b0))
      (assert! (= 2 (long-field b0 "banner_announced_wave")) (str "B announced: " b0)))
    (assert! (seq enters) "C never enter phase")
    (when (> (count enters) 1)
      (let [x0 (double-field (first enters) "banner_x")
            x1 (double-field (last enters) "banner_x")]
        (assert! (and x0 x1 (> x1 x0))
                 (str "C enter should move right toward center: " x0 " -> " x1))))
    (assert! (seq exits) "D never exit phase")
    (when (> (count exits) 1)
      (let [x0 (double-field (first exits) "banner_x")
            x1 (double-field (last exits) "banner_x")]
        (assert! (and x0 x1 (> x1 x0))
                 (str "D exit should move right off: " x0 " -> " x1))))
    (assert! (every? #(= 0 (long-field % "enemy_missiles")) banners)
             "E enemies advanced during banner")
    (assert! resumed (str "F never resumed playing: " (last all)))
    (assert! (= 2 (long-field resumed "wave")) (str "F wave not 2: " resumed)))

  (println "\nPASS: wave-banner automated QA (A–F)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
