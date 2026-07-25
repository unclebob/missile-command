(ns missile-command.browser-persist-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.browser.persist :as persist]
            [missile-command.core :as core]))

(defspec encode-decode-round-trip
  30
  (for-all [mute? gen/boolean
            diff (gen/elements [:arcade :normal :easy])]
    (let [settings (core/export-settings
                    (-> (core/new-game {:width 800 :height 600})
                        (core/set-mute mute?)
                        (core/set-difficulty diff)))
          round (persist/decode (persist/encode settings))]
      (and (= mute? (get-in round [:options :mute]))
           (= diff (get-in round [:options :difficulty]))))))

(defspec jvm-in-memory-store-round-trip
  20
  (for-all [mute? gen/boolean
            diff (gen/elements [:arcade :easy])]
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-mute mute?)
                    (core/set-difficulty diff))
          _ (persist/save-settings! state)
          restored (persist/load-into (core/new-game {:width 640 :height 480}))]
      (and (= mute? (core/mute? restored))
           (= diff (core/difficulty restored))
           (= 640 (core/playfield-width restored))))))
