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

(declare load-settings)

(defn save-settings!
  "Write exported settings to path. Returns path."
  ([state]
   (save-settings! state (default-settings-path)))
  ([state path]
   (let [file (io/file path)]
     (io/make-parents file)
     (spit file (pr-str (merge (or (load-settings path) {})
                               (core/export-settings state))))
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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-27T13:34:22.67869-05:00", :module-hash "-1781296147", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "723552095"} {:id "def/default-settings-filename", :kind "def", :line 7, :end-line 7, :hash "-1411198985"} {:id "defn/default-settings-path", :kind "defn", :line 9, :end-line 13, :hash "2094294583"} {:id "form/3/declare", :kind "declare", :line 15, :end-line 15, :hash "1438499062"} {:id "defn/save-settings!", :kind "defn", :line 17, :end-line 26, :hash "-1541812423"} {:id "defn/load-settings", :kind "defn", :line 28, :end-line 38, :hash "1041803837"} {:id "defn/load-into", :kind "defn", :line 40, :end-line 47, :hash "-17450347"}]}
;; clj-mutate-manifest-end
