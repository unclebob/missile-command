(ns missile-command.jvm.persist
  "File-backed high scores and options for the desktop host."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [missile-command.core :as core]))

(def default-settings-filename "missile-command-settings.edn")

(defn default-settings-path
  "Project-local tmp settings by default (override via MC_SETTINGS_PATH)."
  []
  (or (System/getenv "MC_SETTINGS_PATH")
      (.getAbsolutePath (io/file "tmp" default-settings-filename))))

(defn save-settings!
  "Write exported settings to path. Returns path."
  ([state]
   (save-settings! state (default-settings-path)))
  ([state path]
   (let [file (io/file path)]
     (io/make-parents file)
     (spit file (pr-str (core/export-settings state)))
     path)))

(defn load-settings
  "Read settings map from path, or nil when missing/unreadable."
  ([]
   (load-settings (default-settings-path)))
  ([path]
   (let [file (io/file path)]
     (when (.isFile file)
       (try
         (edn/read-string (slurp file))
         (catch Exception _
           nil))))))

(defn load-into
  "Import settings file onto state when present."
  ([state]
   (load-into state (default-settings-path)))
  ([state path]
   (if-let [settings (load-settings path)]
     (core/import-settings state settings)
     state)))
