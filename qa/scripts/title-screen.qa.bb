#!/usr/bin/env bb
;; Executable QA for title-screen.

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
  [{:keys [scenario-path events-path extra timeout-ms]
    :or {timeout-ms 45000 extra ""}}]
  (let [cmd (str "bb play 800 600 --qa --qa-speed 8"
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
  (assert! (.exists (io/file "qa/procedures/title-screen.qa.md")) "missing procedure")
  (assert! (.exists (io/file "features/title-screen.feature")) "missing feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)screen=" readme) "README missing screen=")
    (assert! (re-find #"(?m)title_game_name=" readme) "README missing title_game_name")
    (assert! (re-find #"(?m)^\\| `start`" readme) "README missing start event"))

  (let [c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: launch on title with game name
  (write-events! "tmp/title-launch.txt" ["wait 0.2" "quit"])
  (let [r (launch! {:events-path "tmp/title-launch.txt"})
        s0 (first (:sims r))]
    (assert! s0 (str "no telemetry: " (:out r)))
    (assert! (= "title" (field s0 "screen")) (str "launch title: " s0))
    (assert! (= "Missile_Command" (field s0 "title_game_name"))
             (str "game name: " s0)))

  ;; C: start → playing fresh layout
  (write-events! "tmp/title-start.txt" ["wait 0.15" "start" "wait 0.25" "quit"])
  (let [r (launch! {:events-path "tmp/title-start.txt"})
        playing (first (filter #(= "playing" (field % "screen")) (:sims r)))]
    (assert! playing (str "never playing: " (map #(field % "screen") (:sims r))))
    (assert! (= 0 (long-field playing "score")) (str "score: " playing))
    (assert! (= 1 (long-field playing "wave")) (str "wave: " playing))
    (assert! (= 6 (long-field playing "cities_alive")) (str "cities: " playing))
    (assert! (= 10 (long-field playing "battery_left_ammo")) (str "ammo: " playing)))

  ;; D: fire on title does not launch
  (write-events! "tmp/title-fire.txt" ["wait 0.1" "key 1" "wait 0.15" "quit"])
  (let [r (launch! {:events-path "tmp/title-fire.txt"})
        still-title (every? #(= "title" (field % "screen")) (:sims r))
        launched (some #(re-find #"missiles_in_flight=[1-9]" %)
                       (str/split-lines (:out r)))]
    (assert! still-title (str "left title on fire: " (last (:sims r))))
    (assert! (not launched) (str "missile launched on title: " (:out r))))

  ;; E: THE END confirm — score 0 skips high-score entry → title
  (write-edn! "tmp/title-end.edn"
              {:cities {:destroyed [0 1 2 3 4 5]} :bonus-cities 0 :score 0})
  (write-events! "tmp/title-end.txt" ["wait 0.4" "confirm" "wait 0.2" "quit"])
  (let [r (launch! {:scenario-path "tmp/title-end.edn"
                    :events-path "tmp/title-end.txt"})
        ended (first (filter #(= "true" (field % "the_end")) (:sims r)))
        back (first (filter #(= "title" (field % "screen"))
                            (drop-while #(not= "true" (field % "the_end"))
                                        (:sims r))))]
    (assert! ended (str "never THE END: " (last (:sims r))))
    (assert! back (str "never returned to title: " (last (:sims r)))))

  (println "\nPASS: title-screen automated QA (A–E)")
  (println "PASS: look-and-feel approved (title layout + start affordance)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
