(ns missile-command.desktop-host-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(describe "desktop host settings export"
  (it "exports and imports options across a fresh shell state"
    (let [prior (-> (core/new-game {:width 800 :height 600})
                    (core/set-mute true)
                    (core/set-difficulty :easy)
                    (core/add-high-score-entry "BOB" 500))
          blob (core/export-settings prior)
          restarted (-> (core/new-game {:width 1280 :height 720})
                        (core/import-settings blob))]
      (should= 1280 (core/playfield-width restarted))
      (should (core/mute? restarted))
      (should= :easy (core/difficulty restarted))
      (should= "BOB" (:initials (first (core/high-score-table restarted))))))

  (it "documents bb play as the desktop launch command"
    (let [readme (slurp (io/file "README.md"))]
      (should (str/includes? readme "bb play")))))
