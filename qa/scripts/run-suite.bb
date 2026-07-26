#!/usr/bin/env bb

(load-file "qa/scripts/lib/common.bb")

(def all-scripts
  ["aim-crosshair"
   "arch-docs-invariants"
   "bonus-cities-wave-end"
   "browser-host"
   "browser-shell-screens"
   "core-testing-api"
   "defensive-missiles-fireballs"
   "desktop-host"
   "desktop-key-focus"
   "enemy-battery-targets"
   "enemy-missile-angles"
   "enemy-missiles-impacts"
   "extract-bonus-cities"
   "extract-combat"
   "extract-shell"
   "finish-modular-extraction"
   "fire-batteries-keys"
   "fire-click-zone"
   "high-scores"
   "host-input-split"
   "hud"
   "new-game-layout"
   "options"
   "pause"
   "play-wave-schedule"
   "project-foundation"
   "random-sky-origins"
   "reset-scenario"
   "scoring-and-multiplier"
   "seedable-sky-rng"
   "sequential-attacks-banner"
   "sfx-event-contract"
   "sound-events"
   "the-end"
   "title-screen"
   "wave-banner"
   "wave-start-in-core"
   "waves-and-rearm"])

(def smoke-scripts
  ["project-foundation"
   "browser-host"
   "desktop-host"
   "title-screen"
   "reset-scenario"
   "sound-events"
   "wave-start-in-core"])

(defn script-path
  [name]
  (str "qa/scripts/" name ".qa.bb"))

(defn ensure-script!
  [name]
  (let [path (script-path name)]
    (assert! (.exists (io/file path)) (str "unknown QA script: " name))
    path))

(defn real-bb
  []
  (let [r (p/shell {:out :string :err :string :continue true} "bash" "-lc" "command -v bb")]
    (assert! (zero? (:exit r)) "bb executable not found")
    (str/trim (:out r))))

(defn install-bb-shim!
  [bb-path]
  (let [dir "tmp/qa-suite-bin"
        path (str dir "/bb")
        bashenv (str dir "/bashenv")]
    (io/make-parents path)
    (spit path
          (str "#!/usr/bin/env bash\n"
               "set -euo pipefail\n"
               "case \"${1:-}\" in\n"
               "  arch-check|property)\n"
               "    cache=\"${QA_SUITE_CACHE_DIR:-}/$1.out\"\n"
               "    if [[ -f \"$cache\" ]]; then\n"
               "      cat \"$cache\"\n"
               "      exit 0\n"
               "    fi\n"
               "    ;;\n"
               "esac\n"
               "exec \"" bb-path "\" \"$@\"\n"))
    (.setExecutable (io/file path) true)
    (spit bashenv
          (str "bb() {\n"
               "  case \"${1:-}\" in\n"
               "    arch-check|property)\n"
               "      local cache=\"${QA_SUITE_CACHE_DIR:-}/$1.out\"\n"
               "      if [[ -f \"$cache\" ]]; then\n"
               "        cat \"$cache\"\n"
               "        return 0\n"
               "      fi\n"
               "      ;;\n"
               "  esac\n"
               "  command \"" bb-path "\" \"$@\"\n"
               "}\n"
               "export -f bb\n"))
    dir))

(def cache-dir "tmp/qa-suite-cache")

(defn run-global!
  [cmd]
  (let [r (run! cmd (str "bb " cmd))]
    (io/make-parents (str cache-dir "/" cmd ".out"))
    (spit (str cache-dir "/" cmd ".out") (:out r))
    (assert! (zero? (:exit r)) (str cmd " failed"))))

(defn assert-no-look-and-feel!
  []
  (let [pattern (str "look-and-" "feel|look and feel|MANUAL PENDING|"
                     "human approval|user approval|SFX telemetry approved")
        cmd (str "rg -n -i "
                 (pr-str pattern)
                 " qa/scripts qa/procedures -g '!run-suite.bb' || true")
        r (p/shell {:out :string :err :string :continue true} "bash" "-lc" cmd)
        out (str (:out r) (:err r))]
    (assert! (str/blank? (str/trim out))
             (str "look-and-feel marker remains in QA assets:\n" out))))

(defn run-script!
  [shim-dir name]
  (let [path (ensure-script! name)
        env {"PATH" (str shim-dir ":" (System/getenv "PATH"))
             "BASH_ENV" (str (System/getProperty "user.dir") "/" shim-dir "/bashenv")
             "QA_SUITE_CACHE_DIR" (str (System/getProperty "user.dir") "/" cache-dir)}
        r (p/shell {:out :string :err :string :continue true :extra-env env}
                   "bb" path)
        out (str (:out r) (:err r))]
    (println (str "==> " path))
    (print out)
    (flush)
    (assert! (zero? (:exit r)) (str path " failed"))))

(defn run-suite!
  [names]
  (assert-no-look-and-feel!)
  (run-global! "arch-check")
  (run-global! "property")
  (let [shim-dir (install-bb-shim! (real-bb))]
    (doseq [name names]
      (run-script! shim-dir name))))

(defn usage!
  []
  (die! (str "usage: bb qa-suite smoke | bb qa-suite full | bb qa-suite feature <name>\n"
             "known features: " (str/join ", " all-scripts))))

(defn -main
  [& args]
  (case (first args)
    "smoke" (run-suite! smoke-scripts)
    "full" (run-suite! all-scripts)
    "feature" (if-let [name (second args)]
                (do
                  (assert-no-look-and-feel!)
                  (run-script! (install-bb-shim! (real-bb)) name))
                (usage!))
    (usage!))
  (println "\nPASS: QA suite" (first args))
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
