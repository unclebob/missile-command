#!/usr/bin/env bb
;; Executable QA for high-scores (qualify, entry, table, persist).

(load-file "qa/scripts/lib/common.bb")
(require '[clojure.edn :as edn])

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/high-scores.qa.md")) "missing procedure")
  (assert! (.exists (io/file "features/high-scores.feature")) "missing feature")
  (let [readme (slurp "README.md")
        feature (slurp "features/high-scores.feature")
        procedure (slurp "qa/procedures/high-scores.qa.md")]
    (assert! (re-find #"(?m)high_score_count=" readme) "README missing high_score_count=")
    (assert! (re-find #"(?m)pending_high_score=" readme) "README missing pending_high_score=")
    (assert! (re-find #"(?m)open-high-scores" readme) "README missing open-high-scores")
    (assert! (re-find #"(?m)desktop Esc quits" readme) "README missing desktop Esc high-score behavior")
    (assert! (re-find #"(?m)initials" readme) "README missing initials event")
    (assert! (re-find #"(?m)scores\.edn" readme) "README missing scores.edn path")
    (assert! (re-find #"(?m)--scores-file" readme) "README missing --scores-file")
    (assert! (re-find #"(?m)Escape does not close the high score table" feature)
             "feature missing Escape high-score scenario")
    (assert! (re-find #"(?m)desktop `Esc` quits" procedure)
             "procedure missing desktop Esc high-score behavior"))

  (let [c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: full table, score below lowest → confirm → title (no entry)
  (write-edn! "tmp/hs-nonqual.edn"
              {:high-score-capacity 3
               :high-scores [{:initials "AAA" :score 1000}
                             {:initials "BBB" :score 900}
                             {:initials "CCC" :score 800}]
               :score 700
               :cities {:destroyed [0 1 2 3 4 5]}
               :bonus-cities 0})
  (write-events! "tmp/hs-nonqual.txt" ["wait 0.5" "confirm" "wait 0.2" "quit"])
  (let [r (launch! {:scenario-path "tmp/hs-nonqual.edn"
                    :events-path "tmp/hs-nonqual.txt"
                    :scores-path "tmp/hs-empty-scores.edn"})
        all (:sims r)
        ended (first (filter #(= "true" (field % "the_end")) all))
        after (drop-while #(not= "true" (field % "the_end")) all)
        title (first (filter #(= "title" (field % "screen")) after))
        entry? (some #(= "high-score-entry" (field % "screen")) after)]
    (assert! ended (str "B never THE END: " (last all)))
    (assert! title (str "B never title: " (map #(field % "screen") after)))
    (assert! (not entry?) (str "B should not enter high-score-entry: " after)))

  ;; C: qualifying score → high-score-entry with pending score
  (write-edn! "tmp/hs-qual.edn"
              {:high-score-capacity 3
               :high-scores [{:initials "AAA" :score 1000}
                             {:initials "BBB" :score 900}]
               :score 950
               :cities {:destroyed [0 1 2 3 4 5]}
               :bonus-cities 0})
  (write-events! "tmp/hs-qual.txt" ["wait 0.5" "confirm" "wait 0.2" "quit"])
  (let [r (launch! {:scenario-path "tmp/hs-qual.edn"
                    :events-path "tmp/hs-qual.txt"
                    :scores-path "tmp/hs-empty-scores.edn"})
        all (:sims r)
        entry (first (filter #(= "high-score-entry" (field % "screen")) all))]
    (assert! entry (str "C never high-score-entry: " (map #(field % "screen") all)))
    (assert! (= 950 (long-field entry "pending_high_score"))
             (str "C pending: " entry)))

  ;; D+E+F: submit initials, ordered, cap, normalize
  (write-edn! "tmp/hs-submit.edn"
              {:high-score-capacity 3
               :high-scores [{:initials "AAA" :score 1000}
                             {:initials "BBB" :score 900}
                             {:initials "CCC" :score 800}]
               :score 850
               :cities {:destroyed [0 1 2 3 4 5]}
               :bonus-cities 0})
  (write-events! "tmp/hs-submit.txt"
                 ["wait 0.5" "confirm" "wait 0.15" "initials new" "wait 0.2" "quit"])
  (let [r (launch! {:scenario-path "tmp/hs-submit.edn"
                    :events-path "tmp/hs-submit.txt"
                    :scores-path "tmp/hs-submit-scores.edn"})
        all (:sims r)
        entry (first (filter #(= "high-score-entry" (field % "screen")) all))
        after (drop-while #(not= "high-score-entry" (field % "screen")) all)
        title (first (filter #(= "title" (field % "screen")) after))
        last-title (last (filter #(= "title" (field % "screen")) all))]
    (assert! entry (str "D never entry: " (map #(field % "screen") all)))
    (assert! title (str "D never returned to title: " (map #(field % "screen") all)))
    (assert! last-title (str "D no title telemetry: " (last all)))
    (assert! (= 3 (long-field last-title "high_score_count"))
             (str "D/E cap: " last-title))
    (assert! (= "NEW" (field last-title "hs_rank3_initials"))
             (str "D rank3 initials: " last-title))
    (assert! (= 850 (long-field last-title "hs_rank3_score"))
             (str "D rank3 score: " last-title))
    (assert! (= "NEW" (field last-title "submitted_initials"))
             (str "F submitted: " last-title))
    ;; Cap dropped 800
    (assert! (not (some #(= "800" (field % "hs_rank3_score"))
                        (filter #(= "title" (field % "screen")) after)))
             (str "E lowest 800 should be dropped: " last-title)))

  ;; G: open high scores from title
  (write-edn! "tmp/hs-view.edn"
              {:high-score-capacity 10
               :high-scores [{:initials "AAA" :score 1000}
                             {:initials "BBB" :score 500}]})
  (write-events! "tmp/hs-view.txt"
                 ["wait 0.15" "open-high-scores" "wait 0.15" "close-high-scores"
                  "wait 0.1" "quit"])
  (let [r (launch! {:scenario-path "tmp/hs-view.edn"
                    :events-path "tmp/hs-view.txt"
                    :scores-path "tmp/hs-view-scores.edn"})
        all (:sims r)
        view (first (filter #(= "high-scores" (field % "screen")) all))
        back (first (filter #(= "title" (field % "screen"))
                            (drop-while #(not= "high-scores" (field % "screen")) all)))]
    (assert! view (str "G never high-scores view: " (map #(field % "screen") all)))
    (assert! (= "AAA" (field view "hs_rank1_initials")) (str "G rank1: " view))
    (assert! (= 1000 (long-field view "hs_rank1_score")) (str "G rank1 score: " view))
    (assert! (= "BBB" (field view "hs_rank2_initials")) (str "G rank2: " view))
    (assert! back (str "G never closed to title: " (map #(field % "screen") all))))

  ;; H: Escape boundary is covered by Gherkin/unit host-input policy; the JVM
  ;; real-key path exits on unhandled Escape.

  ;; I: persist across relaunch
  (let [scores-path "tmp/hs-persist-scores.edn"]
    (when (.exists (io/file scores-path))
      (.delete (io/file scores-path)))
    (write-edn! "tmp/hs-persist.edn"
                {:high-score-capacity 10
                 :high-scores []
                 :score 1234
                 :cities {:destroyed [0 1 2 3 4 5]}
                 :bonus-cities 0})
    (write-events! "tmp/hs-persist-write.txt"
                   ["wait 0.5" "confirm" "wait 0.15" "initials ZED" "wait 0.2" "quit"])
    (let [w (launch! {:scenario-path "tmp/hs-persist.edn"
                      :events-path "tmp/hs-persist-write.txt"
                      :scores-path scores-path})
          title (last (filter #(= "title" (field % "screen")) (:sims w)))]
      (assert! title (str "H write never title: " (last (:sims w))))
      (assert! (= "ZED" (field title "hs_rank1_initials"))
               (str "H write rank: " title))
      (assert! (.exists (io/file scores-path)) "H scores file not written")
      (let [disk (edn/read-string (slurp scores-path))
            table (or (:high-scores disk) [])]
        (assert! (= "ZED" (:initials (first table)))
                 (str "H disk content: " disk))))
    (write-edn! "tmp/hs-persist-reread.edn" {})
    (write-events! "tmp/hs-persist-read.txt"
                   ["wait 0.15" "open-high-scores" "wait 0.15" "quit"])
    (let [r (launch! {:scenario-path "tmp/hs-persist-reread.edn"
                      :events-path "tmp/hs-persist-read.txt"
                      :scores-path scores-path})
          view (first (filter #(= "high-scores" (field % "screen")) (:sims r)))]
      (assert! view (str "H relaunch never view: " (map #(field % "screen") (:sims r))))
      (assert! (= "ZED" (field view "hs_rank1_initials"))
               (str "H relaunch missing ZED: " view))
      (assert! (= 1234 (long-field view "hs_rank1_score"))
               (str "H relaunch score: " view))))

  (println "\nPASS: high-scores automated QA (A–I)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
