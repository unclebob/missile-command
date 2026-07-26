(ns missile-command.jvm.telemetry-emitter-spec
  (:require [speclj.core :refer :all]
            [missile-command.jvm.telemetry-emitter :as telemetry-emitter]))

(describe "sfx-line"
  (it "formats namespaced events without changing QA field names"
    (should= "qa-sfx type=impact/city played=true mute=false"
             (telemetry-emitter/sfx-line {:type :impact/city} false)))

  (it "reports muted playback"
    (should= "qa-sfx type=launch played=false mute=true"
             (telemetry-emitter/sfx-line {:type :launch} true))))
