(ns missile-command.generated-artifacts-policy
  "Generated build outputs that are disposable and never source-controlled.")

(def disposable-paths
  ["acceptance/generated"
   "build/acceptance"
   "resources/public/js"])

(def disposable-cache-paths
  [".cpcache"
   ".shadow-cljs"])

(defn all-disposable-paths
  []
  (concat disposable-paths disposable-cache-paths))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:39:31.271469-05:00", :module-hash "491138608", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "628031052"} {:id "def/disposable-paths", :kind "def", :line 4, :end-line 7, :hash "1743016669"} {:id "def/disposable-cache-paths", :kind "def", :line 9, :end-line 11, :hash "-1409876446"} {:id "defn/all-disposable-paths", :kind "defn", :line 13, :end-line 15, :hash "-1004091905"}]}
;; clj-mutate-manifest-end
