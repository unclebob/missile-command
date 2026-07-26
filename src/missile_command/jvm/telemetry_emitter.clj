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
  (let [t (core/sfx-event-type-name event)]
    (str "qa-sfx type=" t
         " played=" (if muted? "false" "true")
         " mute=" muted?)))

(defn emit-new-sfx!
  "Play new SFX clips and emit qa-sfx lines; honor mute for playback.
  Uses a host-owned SFX cursor."
  [ctx prev-state state]
  (when (and (core/title? prev-state) (core/playing? state))
    ((:stop-title! ctx)))
  (let [[fresh next-cursor] (core/sfx-take-new-with-cursor state @(:sfx-emitted-count ctx))
        muted? (core/mute? state)]
    ((:play-events! ctx) fresh muted?)
    (doseq [e fresh]
      (emit-line! (:launch-options ctx) (sfx-line e muted?)))
    (reset! (:sfx-emitted-count ctx) next-cursor)))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:35:48.331482-05:00", :module-hash "143226655", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "-1603225078"} {:id "defn/emit-line!", :kind "defn", :line 6, :end-line 10, :hash "965714201"} {:id "defn/emit-sim!", :kind "defn", :line 12, :end-line 16, :hash "-809269331"} {:id "defn/emit-telemetry-fire!", :kind "defn", :line 18, :end-line 20, :hash "2038362595"} {:id "defn/emit-fireball-phases!", :kind "defn", :line 22, :end-line 30, :hash "-686305833"} {:id "defn/sfx-line", :kind "defn", :line 32, :end-line 37, :hash "1333222773"} {:id "defn/emit-new-sfx!", :kind "defn", :line 39, :end-line 50, :hash "-1896570479"}]}
;; clj-mutate-manifest-end
