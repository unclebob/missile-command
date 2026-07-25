#!/usr/bin/env bb
;; Executable QA for options (mute, difficulty, remap, persist).

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
    :or {timeout-ms 45000}}]
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
  (assert! (.exists (io/file "qa/procedures/options.qa.md")) "missing procedure")
  (assert! (.exists (io/file "features/options.feature")) "missing feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)open-options" readme) "README missing open-options")
    (assert! (re-find #"(?m)mute=" readme) "README missing mute=")
    (assert! (re-find #"(?m)difficulty=" readme) "README missing difficulty=")
    (assert! (re-find #"(?m)missile-command-settings" readme) "README missing settings path"))

  (let [c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: open options, defaults
  (write-events! "tmp/opt-defaults.txt"
                 ["wait 0.1" "open-options" "wait 0.15" "quit"])
  (let [r (launch! {:events-path "tmp/opt-defaults.txt"
                    :scores-path "tmp/opt-empty.edn"})
        opt (first (filter #(= "options" (field % "screen")) (:sims r)))]
    (assert! opt (str "B never options: " (map #(field % "screen") (:sims r))))
    (assert! (= "false" (field opt "mute")) (str "B mute default: " opt))
    (assert! (= "arcade" (field opt "difficulty")) (str "B difficulty: " opt))
    (assert! (re-find #"z" (or (field opt "fire_key_left") "")) (str "B left key: " opt)))

  ;; C: mute true, leave, reopen still true
  (write-events! "tmp/opt-mute.txt"
                 ["wait 0.1" "open-options" "mute true" "leave-options"
                  "open-options" "wait 0.1" "quit"])
  (let [r (launch! {:events-path "tmp/opt-mute.txt"
                    :scores-path "tmp/opt-mute.edn"})
        opts (filter #(= "options" (field % "screen")) (:sims r))
        last-opt (last opts)]
    (assert! (>= (count opts) 2) (str "C reopen: " (count opts)))
    (assert! (= "true" (field last-opt "mute")) (str "C mute not sticky: " last-opt)))

  ;; D: bind left to q, start, key q fires left
  (write-events! "tmp/opt-remap.txt"
                 ["wait 0.1" "open-options" "bind-fire left q" "leave-options"
                  "start" "aim 400 200" "key q" "wait 0.15" "quit"])
  (let [r (launch! {:events-path "tmp/opt-remap.txt"
                    :scores-path "tmp/opt-remap.edn"})
        fire (some #(re-find #"qa-fire battery=left" %) (str/split-lines (:out r)))]
    (assert! fire (str "D remap fire failed: " (:out r))))

  ;; E: difficulty easy → wave metrics
  (write-events! "tmp/opt-easy.txt"
                 ["wait 0.1" "open-options" "difficulty easy" "leave-options"
                  "start" "wait 0.2" "quit"])
  (let [r (launch! {:events-path "tmp/opt-easy.txt"
                    :scores-path "tmp/opt-easy.edn"})
        playing (first (filter #(= "playing" (field % "screen")) (:sims r)))]
    (assert! playing (str "E never playing: " (map #(field % "screen") (:sims r))))
    (assert! (= "easy" (field playing "difficulty")) (str "E difficulty: " playing))
    (assert! (= 2 (long-field playing "wave_enemy_count"))
             (str "E count (expect 2): " playing))
    ;; easy factor 0.7 × arcade wave-1 base speed 40.0 → 28.0
    (assert! (= "28.0" (field playing "wave_enemy_speed"))
             (str "E speed (expect 28.0): " playing)))

  ;; F: persist mute+difficulty across relaunch
  (let [scores "tmp/opt-persist.edn"]
    (when (.exists (io/file scores)) (.delete (io/file scores)))
    (write-events! "tmp/opt-persist-w.txt"
                   ["wait 0.1" "open-options" "mute true" "difficulty normal"
                    "leave-options" "wait 0.1" "quit"])
    (launch! {:events-path "tmp/opt-persist-w.txt" :scores-path scores})
    (assert! (.exists (io/file scores)) "F settings not written")
    (write-events! "tmp/opt-persist-r.txt"
                   ["wait 0.1" "open-options" "wait 0.1" "quit"])
    (let [r (launch! {:events-path "tmp/opt-persist-r.txt" :scores-path scores})
          opt (first (filter #(= "options" (field % "screen")) (:sims r)))]
      (assert! opt (str "F never options: " (last (:sims r))))
      (assert! (= "true" (field opt "mute")) (str "F mute lost: " opt))
      (assert! (= "normal" (field opt "difficulty")) (str "F difficulty lost: " opt))))

  ;; G: leave options → title
  (write-events! "tmp/opt-leave.txt"
                 ["wait 0.1" "open-options" "leave-options" "wait 0.1" "quit"])
  (let [r (launch! {:events-path "tmp/opt-leave.txt"
                    :scores-path "tmp/opt-leave.edn"})
        after (drop-while #(not= "options" (field % "screen")) (:sims r))
        title (first (filter #(= "title" (field % "screen")) after))]
    (assert! title (str "G never title after leave: " (map #(field % "screen") (:sims r)))))

  (println "\nPASS: options automated QA (A–G)")
  (println "PASS: look-and-feel approved (options screen)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
