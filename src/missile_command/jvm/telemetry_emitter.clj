(ns missile-command.jvm.telemetry-emitter
  "JVM QA telemetry emission and SFX cursor coordination."
  (:require [missile-command.core :as core]
            [missile-command.jvm.input :as input]))

(defn emit-line!
  [launch-options line]
  (when (:qa-telemetry? @launch-options)
    (println line)
    (flush)))

(defn emit-sim!
  [ctx state]
  (emit-line! (:launch-options ctx)
              (input/format-sim-telemetry-line
               (assoc state :initials-draft @(:initials-draft ctx)))))

(defn emit-telemetry-fire!
  [ctx result]
  (emit-line! (:launch-options ctx) (input/format-telemetry-line result)))

(defn emit-fireball-phases!
  [ctx state]
  (let [[events next-map] (input/detect-fireball-phase-events
                           @(:fireball-phases ctx)
                           (core/fireballs state))]
    (reset! (:fireball-phases ctx) next-map)
    (doseq [e events]
      (emit-line! (:launch-options ctx)
                  (input/format-fireball-phase-line state (:fireball e) (:phase e))))))

(defn sfx-line
  [event muted?]
  (let [kw (:type event)
        t (if (namespace kw)
            (str (namespace kw) "/" (name kw))
            (name kw))]
    (str "qa-sfx type=" t
         " played=" (if muted? "false" "true")
         " mute=" muted?)))

(defn emit-new-sfx!
  "Play new SFX clips and emit qa-sfx lines; honor mute for playback.
  Uses sfx-take-new (cursor = previous log length)."
  [ctx prev-state state]
  (when (and (core/title? prev-state) (core/playing? state))
    ((:stop-title! ctx)))
  (let [prev (count (core/sfx-events prev-state))
        fresh (core/sfx-take-new state prev)
        muted? (core/mute? state)]
    ((:play-events! ctx) fresh muted?)
    (doseq [e fresh]
      (emit-line! (:launch-options ctx) (sfx-line e muted?)))
    (reset! (:sfx-emitted-count ctx) (count (core/sfx-events state)))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:25:57.998125-05:00", :module-hash "1760234680", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "-1603225078"} {:id "defn/emit-line!", :kind "defn", :line 6, :end-line 10, :hash "965714201"} {:id "defn/emit-sim!", :kind "defn", :line 12, :end-line 16, :hash "-809269331"} {:id "defn/emit-telemetry-fire!", :kind "defn", :line 18, :end-line 20, :hash "2038362595"} {:id "defn/emit-fireball-phases!", :kind "defn", :line 22, :end-line 30, :hash "-686305833"} {:id "defn/sfx-line", :kind "defn", :line 32, :end-line 40, :hash "389009004"} {:id "defn/emit-new-sfx!", :kind "defn", :line 42, :end-line 54, :hash "1729313920"}]}
;; clj-mutate-manifest-end
