(ns missile-command.browser.persist
  "Browser localStorage settings: pure encode/decode + host load/save."
  (:require [missile-command.core :as core]
            #?(:cljs [cljs.reader :as reader])))

(def storage-key "missile-command-settings")

#?(:clj (defonce ^:private settings-atom (atom nil)))

(defn encode
  [settings]
  (pr-str settings))

(defn decode
  [raw]
  (when (and raw (pos? (count (str raw))))
    #?(:clj (read-string raw)
       :cljs (reader/read-string raw))))

(defn save-settings!
  "Persist exported settings. JVM tests use an in-memory atom; CLJS uses localStorage."
  [state]
  (let [blob (encode (core/export-settings state))]
    #?(:clj (do (reset! settings-atom blob) blob)
       :cljs (do
               (when (exists? js/localStorage)
                 (.setItem js/localStorage storage-key blob))
               blob))))

(defn load-settings
  []
  #?(:clj (decode @settings-atom)
     :cljs (when (exists? js/localStorage)
             (decode (.getItem js/localStorage storage-key)))))

(defn load-into
  [state]
  (if-let [settings (load-settings)]
    (core/import-settings state settings)
    state))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T16:18:08.816663-05:00", :module-hash "2107514195", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "8794659"} {:id "def/storage-key", :kind "def", :line 6, :end-line 6, :hash "1823420441"} {:id "form/2/defonce", :kind "defonce", :line 8, :end-line 8, :hash "1922143144"} {:id "defn/encode", :kind "defn", :line 10, :end-line 12, :hash "1910926456"} {:id "defn/decode", :kind "defn", :line 14, :end-line 18, :hash "-297063616"} {:id "defn/save-settings!", :kind "defn", :line 20, :end-line 28, :hash "86262882"} {:id "defn/load-settings", :kind "defn", :line 30, :end-line 34, :hash "1690297278"} {:id "defn/load-into", :kind "defn", :line 36, :end-line 40, :hash "-837693620"}]}
;; clj-mutate-manifest-end
