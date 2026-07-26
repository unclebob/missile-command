#!/usr/bin/env bb
;; Executable QA for hud.

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
  [{:keys [scenario-path events-path timeout-ms]
    :or {timeout-ms 45000}}]
  (let [cmd (str "bb play 800 600 --qa --no-keyfocus --qa-speed 8"
                 (when scenario-path (str " --qa-scenario " scenario-path))
                 (when events-path (str " --qa-events " events-path)))]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out)})))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/hud.qa.md")) "missing procedure")
  (assert! (.exists (io/file "features/hud.feature")) "missing feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)hud_score=|score=" readme) "README missing score telemetry"))

  (let [c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: fresh play HUD fields
  (write-events! "tmp/hud-fresh.txt" ["wait 0.1" "start" "wait 0.15" "quit"])
  (let [r (launch! {:events-path "tmp/hud-fresh.txt"})
        p (first (filter #(= "playing" (field % "screen")) (:sims r)))]
    (assert! p (str "never playing: " (first (:sims r))))
    (assert! (= 0 (long-field p "score")) (str "score: " p))
    (assert! (= 0 (long-field p "hud_score")) (str "hud_score: " p))
    (assert! (= 1 (long-field p "wave")) (str "wave: " p))
    (assert! (= 1 (long-field p "hud_wave")) (str "hud_wave: " p))
    (assert! (= 1 (long-field p "multiplier")) (str "mult: " p))
    (assert! (= 1 (long-field p "hud_multiplier")) (str "hud_mult: " p))
    (assert! (= 10 (long-field p "battery_left_ammo")) (str "ammo L: " p))
    (assert! (= 10 (long-field p "battery_center_ammo")) (str "ammo C: " p))
    (assert! (= 10 (long-field p "battery_right_ammo")) (str "ammo R: " p))
    (assert! (= 6 (long-field p "cities_alive")) (str "cities: " p))
    (assert! (= 6 (long-field p "hud_living_cities")) (str "hud cities: " p))
    (assert! (= 0 (long-field p "bonus_cities")) (str "bonus: " p))
    (assert! (= 0 (long-field p "hud_bonus_cities")) (str "hud bonus: " p))
    (assert! (= "true" (field p "hud_full")) (str "hud_full: " p)))

  ;; C/E: staged score/wave/cities/bonus via scenario then start (start resets!)
  ;; Use scenario after... start resets. So stage during playing via events only.
  ;; Destroy city via enemy impact + set score via scenario on end path is hard.
  ;; Fire ammo decrements:
  (write-events! "tmp/hud-ammo.txt"
                 ["wait 0.1" "start" "wait 0.1"
                  "key z" "wait 0.05"
                  "key x" "wait 0.05"
                  "key c" "wait 0.1" "quit"])
  (let [r (launch! {:events-path "tmp/hud-ammo.txt"})
        after (last (filter #(= "playing" (field % "screen")) (:sims r)))]
    (assert! after "missing playing telemetry")
    (assert! (= 9 (long-field after "battery_left_ammo"))
             (str "left ammo after z: " after))
    (assert! (= 9 (long-field after "battery_center_ammo"))
             (str "center ammo after x: " after))
    (assert! (= 9 (long-field after "battery_right_ammo"))
             (str "right ammo after c: " after)))

  ;; F: pause still reports HUD score/wave
  (write-events! "tmp/hud-pause.txt"
                 ["wait 0.1" "start" "wait 0.1" "pause" "wait 0.2" "quit"])
  (let [r (launch! {:events-path "tmp/hud-pause.txt"})
        p (first (filter #(= "paused" (field % "screen")) (:sims r)))]
    (assert! p (str "never paused: " (map #(field % "screen") (:sims r))))
    (assert! (some? (long-field p "hud_score")) (str "hud_score on pause: " p))
    (assert! (some? (long-field p "hud_wave")) (str "hud_wave on pause: " p))
    (assert! (= "true" (field p "hud_full")) (str "hud_full paused: " p)))

  ;; G: title — full HUD not required
  (write-events! "tmp/hud-title.txt" ["wait 0.15" "quit"])
  (let [r (launch! {:events-path "tmp/hud-title.txt"})
        t (first (:sims r))]
    (assert! (= "title" (field t "screen")) (str t))
    (assert! (= "false" (field t "hud_full"))
             (str "title should not force full HUD: " t)))

  (println "\nPASS: hud automated QA (A–G partial)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
