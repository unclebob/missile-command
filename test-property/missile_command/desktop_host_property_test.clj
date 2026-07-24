(ns missile-command.desktop-host-property-test
  "Pure settings export/import round-trips (desktop/browser host contract)."
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.core :as core]))

(defspec export-import-round-trip-preserves-options-and-scores
  30
  (for-all [mute? gen/boolean
            diff (gen/elements [:arcade :normal :easy])
            score (gen/elements [100 500 2500])
            initials (gen/elements ["AAA" "bob" "Z9X"])]
    (let [source (-> (core/new-game {:width 800 :height 600})
                     (core/set-mute mute?)
                     (core/set-difficulty diff)
                     (core/add-high-score-entry initials score))
          payload (core/export-settings source)
          restored (core/import-settings
                    (core/new-game {:width 1024 :height 768})
                    payload)]
      (and (= mute? (core/mute? restored))
           (= diff (core/difficulty restored))
           (= (core/high-score-table source) (core/high-score-table restored))
           (= (core/high-score-capacity source)
              (core/high-score-capacity restored))
           ;; shell dimensions come from the new host session, not settings
           (= 1024 (core/playfield-width restored))
           (core/title? restored)))))

(defspec import-nil-settings-keeps-defaults
  20
  (for-all []
    (let [state (core/import-settings
                 (core/new-game {:width 800 :height 600})
                 nil)]
      (and (not (core/mute? state))
           (= :arcade (core/difficulty state))
           (empty? (core/high-score-table state))))))
