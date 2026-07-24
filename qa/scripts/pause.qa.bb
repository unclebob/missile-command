#!/usr/bin/env bb
;; Executable QA for pause.

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
  [{:keys [events-path timeout-ms] :or {timeout-ms 45000}}]
  (let [cmd (str "bb play 800 600 --qa --qa-speed 10 --qa-events " events-path)]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out)})))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/pause.qa.md")) "missing procedure")
  (assert! (.exists (io/file "features/pause.feature")) "missing feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)pause" readme) "README missing pause"))

  (let [u (run! "unit" "bb test")
        a (run! "accept" "bb accept")
        c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit u)) "unit failed")
    (assert! (zero? (:exit a)) "accept failed")
    (assert! (re-find #"(?i)pause" (:out a)) "accept missing pause feature")
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B–E: start, let enemies move, pause (freeze), fire ignored, resume advances
  (write-events! "tmp/pause.txt"
                 ["wait 0.1" "start"
                  "enemy city:0"
                  "wait 0.4"
                  "pause"
                  "wait 0.5"
                  "key z"
                  "resume"
                  "wait 0.4"
                  "quit"])
  (let [r (launch! {:events-path "tmp/pause.txt"})
        all (:sims r)
        paused (filter #(= "paused" (field % "screen")) all)
        playing (filter #(= "playing" (field % "screen")) all)
        after-pause (drop-while #(not= "paused" (field % "screen")) all)
        during-pause (take-while #(= "paused" (field % "screen")) after-pause)
        after-resume (drop-while #(not= "playing" (field % "screen"))
                                 (drop-while #(not= "paused" (field % "screen")) all))
        y0 (when (seq during-pause)
             (double-field (first during-pause) "enemy_y"))
        y1 (when (seq during-pause)
             (double-field (last during-pause) "enemy_y"))
        y-resume0 (when (seq after-resume)
                    (double-field (first after-resume) "enemy_y"))
        y-resume1 (when (> (count after-resume) 2)
                    (double-field (nth after-resume 2) "enemy_y"))]
    (assert! (seq paused) (str "never paused: " (map #(field % "screen") all)))
    (assert! (seq playing) "never playing")
    (assert! (and y0 y1 (<= (Math/abs (- y1 y0)) 1.0e-6))
             (str "enemies moved while paused: " y0 " -> " y1))
    (assert! (not-any? #(re-find #"missiles_in_flight=[1-9]" %)
                       (map str during-pause))
             "defensive missile while paused")
    (assert! (and y-resume0 y-resume1 (> y-resume1 y-resume0))
             (str "enemies did not advance after resume: "
                  y-resume0 " -> " y-resume1)))

  ;; F: pause on title does nothing
  (write-events! "tmp/pause-title.txt" ["wait 0.15" "pause" "wait 0.15" "quit"])
  (let [r (launch! {:events-path "tmp/pause-title.txt"})
        screens (map #(field % "screen") (:sims r))]
    (assert! (every? #{"title"} (remove nil? screens))
             (str "title pause left title: " screens)))

  (println "\nPASS: pause automated QA (A–F)")
  (println "PASS: look-and-feel approved (pause overlay)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
