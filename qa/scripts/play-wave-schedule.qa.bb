#!/usr/bin/env bb
;; Executable QA for play-wave-schedule (full wave activation).

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

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/play-wave-schedule.qa.md")) "missing procedure")
  (assert! (.exists (io/file "features/waves-and-rearm.feature")) "missing waves feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)wave_mirv_count=" readme) "README missing wave_mirv_count=")
    (assert! (re-find #"(?m)flyers_bomber=" readme) "README missing flyers_bomber=")
    (assert! (re-find #"(?m)mirv_parents=" readme) "README missing mirv_parents="))

  (let [u (run! "unit" "bb test")
        a (run! "accept" "bb accept")
        c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit u)) "unit failed")
    (assert! (zero? (:exit a)) "accept failed")
    (assert! (re-find #"(?i)waves[-_]?and[-_]?rearm" (:out a)) "accept missing waves")
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: wave 1 schedule — ballistics only
  (write-events! "tmp/pws-w1.txt" ["wait 0.1" "start" "wait 0.2" "quit"])
  (let [r (launch! {:events-path "tmp/pws-w1.txt"
                    :scores-path "tmp/pws-empty.edn"})
        p (first (filter #(= "playing" (field % "screen")) (:sims r)))]
    (assert! p (str "B never playing: " (map #(field % "screen") (:sims r))))
    (assert! (= 1 (long-field p "wave")) (str "B wave: " p))
    (assert! (= (long-field p "wave_enemy_count")
                (long-field p "ballistic_missiles"))
             (str "B ballistic count mismatch: " p))
    (assert! (= 0 (long-field p "wave_mirv_count")) (str "B mirv schedule: " p))
    (assert! (= 0 (long-field p "mirv_parents")) (str "B live mirv: " p))
    (assert! (= 0 (long-field p "wave_bomber_count")) (str "B bomber: " p))
    (assert! (= 0 (long-field p "flyers_bomber")) (str "B live bomber: " p))
    (assert! (= 0 (long-field p "wave_satellite_count")) (str "B sat: " p)))

  ;; C: wave 9 full schedule (stage playing so start does not wipe wave)
  (write-edn! "tmp/pws-w9.edn" {:wave 9 :screen :playing})
  (write-events! "tmp/pws-w9.txt" ["wait 0.25" "quit"])
  (let [r (launch! {:scenario-path "tmp/pws-w9.edn"
                    :events-path "tmp/pws-w9.txt"
                    :scores-path "tmp/pws-empty.edn"})
        p (first (filter #(= "playing" (field % "screen")) (:sims r)))]
    (assert! p (str "C never playing: " (map #(field % "screen") (:sims r))))
    (assert! (= 9 (long-field p "wave")) (str "C wave: " p))
    (assert! (= (long-field p "wave_enemy_count")
                (long-field p "ballistic_missiles"))
             (str "C ballistics: " p))
    (assert! (= (long-field p "wave_mirv_count")
                (long-field p "mirv_parents"))
             (str "C MIRVs: " p))
    (assert! (= (long-field p "wave_smart_bomb_count")
                (long-field p "smart_bombs"))
             (str "C smarts: " p))
    (assert! (= (long-field p "wave_bomber_count")
                (long-field p "flyers_bomber"))
             (str "C bomber: " p))
    (assert! (= (long-field p "wave_satellite_count")
                (long-field p "flyers_satellite"))
             (str "C satellite: " p))
    (assert! (pos? (long-field p "mirv_parents")) (str "C expect MIRVs on w9: " p))
    (assert! (pos? (long-field p "smart_bombs")) (str "C expect smarts on w9: " p))
    (assert! (= 1 (long-field p "flyers_bomber")) (str "C bomber count: " p))
    (assert! (= 1 (long-field p "flyers_satellite")) (str "C sat count: " p)))

  ;; D: clear wave 1 (one enemy) → banner → wave 2 schedule
  (write-edn! "tmp/pws-cont.edn"
              {:wave 1
               :batteries {:left {:ammo 0} :center {:ammo 0} :right {:ammo 0}}
               :enemies [{:target [:city 5]}]})
  (write-events! "tmp/pws-cont.txt" ["wait 0.15" "start" "wait 12" "quit"])
  (let [r (launch! {:scenario-path "tmp/pws-cont.edn"
                    :events-path "tmp/pws-cont.txt"
                    :scores-path "tmp/pws-empty.edn"
                    :timeout-ms 90000})
        all (:sims r)
        banner (first (filter #(= "wave-banner" (field % "screen")) all))
        w2 (first (filter #(and (= "playing" (field % "screen"))
                                (= 2 (long-field % "wave"))
                                (pos? (long-field % "enemy_missiles")))
                          (drop-while #(not= "wave-banner" (field % "screen")) all)))]
    (assert! banner (str "D never banner: " (map #(field % "screen") all)))
    (assert! w2 (str "D never wave2 schedule: " (last all)))
    (assert! (= (long-field w2 "wave_enemy_count")
                (long-field w2 "ballistic_missiles"))
             (str "D wave2 ballistics: " w2))
    (assert! (= 0 (long-field w2 "wave_mirv_count")) (str "D wave2 mirv: " w2)))

  (println "\nPASS: play-wave-schedule automated QA (A–D)")
  (println "PASS: look-and-feel approved (wave-9 full schedule density)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
