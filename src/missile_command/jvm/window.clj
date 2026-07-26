(ns missile-command.jvm.window
  "Host window placement: open on the screen where bb was typed; no focus steal."
  (:require [clojure.string :as str])
  (:import [java.awt GraphicsEnvironment MouseInfo Window Component]
           [javax.swing SwingUtilities]
           [java.lang ProcessHandle]))

(defn pointer-location
  []
  (let [p (.getLocation (MouseInfo/getPointerInfo))]
    {:x (.x p) :y (.y p)}))

(defn screen-bounds-containing
  "Graphics bounds of the screen that contains global point (x,y), else default."
  [x y]
  (let [ge (GraphicsEnvironment/getLocalGraphicsEnvironment)
        devices (.getScreenDevices ge)
        match (first (filter (fn [d]
                               (let [b (.getBounds (.getDefaultConfiguration d))]
                                 (.contains b x y)))
                             devices))
        device (or match (.getDefaultScreenDevice ge))
        b (.getBounds (.getDefaultConfiguration device))]
    {:x (.x b) :y (.y b) :width (.width b) :height (.height b)}))

(defn centered-location
  "Top-left for a width×height window centered on the given screen bounds."
  [bounds width height]
  {:x (+ (:x bounds) (quot (- (:width bounds) width) 2))
   :y (+ (:y bounds) (quot (- (:height bounds) height) 2))})

(defn parse-point-csv
  "Parse \"x,y\" (optional whitespace) into {:x :y}, or nil."
  [s]
  (when-let [[_ xs ys] (re-matches #"\s*(-?\d+)\s*,\s*(-?\d+)\s*" (str s))]
    {:x (Integer/parseInt xs)
     :y (Integer/parseInt ys)}))

(defn normalize-tty
  "Normalize tty paths for comparison: ttys009, /dev/ttys009 → /dev/ttys009."
  [tty]
  (when (and tty (not (str/blank? tty)) (not= "?" tty) (not= "??" tty) (not= "not a tty" tty))
    (let [t (str/trim tty)]
      (if (str/starts-with? t "/dev/")
        t
        (str "/dev/" t)))))

(defn- run-process
  [args]
  (try
    (let [pb (doto (ProcessBuilder. ^java.util.List (map str args))
               (.redirectErrorStream true))
          proc (.start pb)
          out (str/trim (slurp (.getInputStream proc)))
          code (.waitFor proc)]
      (when (zero? code)
        (when-not (str/blank? out) out)))
    (catch Exception _ nil)))

(defn- run-osascript
  [source]
  (run-process ["osascript" "-e" source]))

(defn- tmux-socket
  "Parse TMUX env (/path/to/sock,pid,session) → socket path."
  []
  (when-let [tmux (System/getenv "TMUX")]
    (first (str/split tmux #","))))

(defn tmux-client-tty
  "When running inside tmux, the real terminal client tty (where the user typed)."
  []
  (when-let [socket (tmux-socket)]
    (normalize-tty
     (run-process ["tmux" "-S" socket "display-message" "-p" "#{client_tty}"]))))

(defn process-controlling-tty
  "Walk this process and parents for a controlling TTY (pane tty inside tmux)."
  []
  (try
    (loop [ph (ProcessHandle/current) depth 0]
      (when (and ph (< depth 12))
        (let [pid (.pid ph)
              tty (some-> (run-process ["ps" "-p" (str pid) "-o" "tty="])
                          str/trim
                          normalize-tty)]
          (if tty
            tty
            (when-let [parent (.orElse (.parent ph) nil)]
              (recur parent (inc depth)))))))
    (catch Exception _ nil)))

(defn terminal-app-window-center-for-tty
  "macOS Terminal.app: center of the window whose tab owns tty."
  [tty]
  (when-let [tty (normalize-tty tty)]
    (let [short (str/replace tty #"^/dev/" "")
          script (str
                  "tell application \"Terminal\"\n"
                  "  repeat with w in windows\n"
                  "    repeat with t in tabs of w\n"
                  "      try\n"
                  "        set tabTty to tty of t as text\n"
                  "        if tabTty is \"" tty "\" or tabTty ends with \"" short "\" then\n"
                  "          set p to position of w\n"
                  "          set s to size of w\n"
                  "          set x to (item 1 of p) + (item 1 of s) / 2\n"
                  "          set y to (item 2 of p) + (item 2 of s) / 2\n"
                  "          return (x as integer as text) & \",\" & (y as integer as text)\n"
                  "        end if\n"
                  "      end try\n"
                  "    end repeat\n"
                  "  end repeat\n"
                  "end tell\n"
                  "error \"no matching Terminal tab\"\n")]
      (try
        (parse-point-csv (run-osascript script))
        (catch Exception _ nil)))))

(defn frontmost-window-center
  "macOS: center of the frontmost app's first window."
  []
  (try
    (parse-point-csv
     (run-osascript
      (str "tell application \"System Events\"\n"
           "  tell (first process whose frontmost is true)\n"
           "    if (count of windows) is 0 then error \"no window\"\n"
           "    set p to position of window 1\n"
           "    set s to size of window 1\n"
           "    set x to (item 1 of p) + (item 1 of s) / 2\n"
           "    set y to (item 2 of p) + (item 2 of s) / 2\n"
           "    return (x as integer as text) & \",\" & (y as integer as text)\n"
           "  end tell\n"
           "end tell")))
    (catch Exception _ nil)))

(defn capture-launch-anchor!
  "Point identifying the screen where the bb launch command was typed.
  Order: tmux client tty window → process tty window → frontmost window → mouse."
  []
  (or (when-let [tty (tmux-client-tty)]
        (terminal-app-window-center-for-tty tty))
      (when-let [tty (process-controlling-tty)]
        (terminal-app-window-center-for-tty tty))
      (frontmost-window-center)
      (pointer-location)))

(defn frontmost-app-name
  "macOS: name of the frontmost process (Terminal, etc.) before the sketch opens."
  []
  (try
    (run-osascript
     "tell application \"System Events\" to get name of first application process whose frontmost is true")
    (catch Exception _ nil)))

(defn restore-frontmost-app!
  "Return keyboard focus to the app that was frontmost at launch (no steal)."
  [app-name]
  (when (and app-name (not (str/blank? app-name)))
    ;; Escape embedded quotes in process name
    (let [safe (str/replace app-name "\"" "\\\"")]
      (run-osascript
       (str "tell application \"System Events\"\n"
            "  try\n"
            "    set frontmost of first process whose name is \"" safe "\" to true\n"
            "  end try\n"
            "end tell")))))

(defn- as-awt-window
  [native]
  (cond
    (nil? native) nil
    (instance? Window native) native
    (instance? Component native) (or (SwingUtilities/getWindowAncestor ^Component native)
                                     (try
                                       (let [m (.getMethod (class native) "getFrame" (make-array Class 0))
                                             frame (.invoke m native (object-array []))]
                                         (when (instance? Window frame)
                                           frame))
                                       (catch Exception _ nil)))
    :else nil))

(defn- try-set-position!
  [native x y]
  (when native
    (or (try (.setLocation ^Window native (int x) (int y)) true
             (catch Exception _ false))
        (try
          (.setPosition native (int x) (int y))
          true
          (catch Exception _ false)))))

(defn- disable-focus-steal!
  "Visible window, but do not auto-grab keyboard focus on show.
  Avoid setFocusableWindowState(false) / re-activating Terminal — that can
  bury the game behind other windows."
  [native]
  (let [awt (as-awt-window native)]
    (when (instance? Window awt)
      (try (.setAutoRequestFocus ^Window awt false) (catch Exception _))
      (try (.setAlwaysOnTop ^Window awt false) (catch Exception _))
      (try (.setFocusable ^Window awt true) (catch Exception _))
      (try (.setFocusableWindowState ^Window awt true) (catch Exception _)))
    (when (and native (not (instance? Window awt)))
      (try
        (let [m (.getMethod (class native) "setAlwaysOnTop"
                            (into-array Class [Boolean/TYPE]))]
          (.invoke m native (into-array Object [false])))
        (catch Exception _)))))

(defn make-non-focusable!
  "QA-only: keep the Processing window visible but unable to take keyboard focus."
  [native]
  (let [awt (as-awt-window native)]
    (when (instance? Component native)
      (try (.setFocusable ^Component native false) (catch Exception _)))
    (when (instance? Window awt)
      (try (.setAutoRequestFocus ^Window awt false) (catch Exception _))
      (try (.setFocusable ^Window awt false) (catch Exception _))
      (try (.setFocusableWindowState ^Window awt false) (catch Exception _))
      (try (.setAlwaysOnTop ^Window awt false) (catch Exception _)))))

(defn show-non-focusable-surface!
  "Show an AWT Processing surface without calling PSurfaceAWT.setVisible, which
  immediately requests canvas focus."
  [surface]
  (let [native (try (.getNative surface) (catch Exception _ nil))
        awt (as-awt-window native)]
    (make-non-focusable! native)
    (if (instance? Window awt)
      (try (.setVisible ^Window awt true) (catch Exception _))
      (try (.setVisible surface true) (catch Exception _)))
    (make-non-focusable! native)))

(defn place-on-launch-screen!
  "Center the Processing surface on the screen of launch-anchor {:x :y}.
  Keeps the window visible; setAutoRequestFocus(false) so typing stays in the
  terminal until the user clicks the game. restore-focus-app is ignored for
  visibility (re-fronting Terminal was hiding the sketch)."
  ([surface sketch-w sketch-h]
   (place-on-launch-screen! surface sketch-w sketch-h (capture-launch-anchor!) nil))
  ([surface sketch-w sketch-h launch-anchor]
   (place-on-launch-screen! surface sketch-w sketch-h launch-anchor nil))
  ([surface sketch-w sketch-h launch-anchor restore-focus-app]
   (place-on-launch-screen! surface sketch-w sketch-h launch-anchor restore-focus-app false))
  ([surface sketch-w sketch-h launch-anchor restore-focus-app no-keyfocus?]
   (let [anchor (or launch-anchor (capture-launch-anchor!))
         bounds (screen-bounds-containing (:x anchor) (:y anchor))
         loc (centered-location bounds sketch-w sketch-h)
         lx (int (:x loc))
         ly (int (:y loc))
         native (try (.getNative surface) (catch Exception _ nil))
         awt (as-awt-window native)]
     (if no-keyfocus?
       (make-non-focusable! native)
       (disable-focus-steal! native))
     (try (.setLocation surface lx ly) (catch Exception _))
     (try-set-position! native lx ly)
     (try (.setVisible surface true) (catch Exception _))
     ;; Ensure on-screen without keyboard grab: order by location only.
     (when (instance? Window awt)
       (try (.setAutoRequestFocus ^Window awt false) (catch Exception _)))
     (when no-keyfocus?
       (make-non-focusable! native)
       (restore-frontmost-app! restore-focus-app))
     {:screen bounds :location loc :anchor anchor})))

(defn place-on-pointer-screen!
  [surface sketch-w sketch-h]
  (place-on-launch-screen! surface sketch-w sketch-h (pointer-location)))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:04:13.630917-05:00", :module-hash "-1733619857", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 6, :hash "1007685043"} {:id "defn/pointer-location", :kind "defn", :line 8, :end-line 11, :hash "-307681220"} {:id "defn/screen-bounds-containing", :kind "defn", :line 13, :end-line 24, :hash "1048841510"} {:id "defn/centered-location", :kind "defn", :line 26, :end-line 30, :hash "231400435"} {:id "defn/parse-point-csv", :kind "defn", :line 32, :end-line 37, :hash "-524147827"} {:id "defn/normalize-tty", :kind "defn", :line 39, :end-line 46, :hash "-1697798455"} {:id "defn-/run-process", :kind "defn-", :line 48, :end-line 58, :hash "1056657960"} {:id "defn-/run-osascript", :kind "defn-", :line 60, :end-line 62, :hash "403540014"} {:id "defn-/tmux-socket", :kind "defn-", :line 64, :end-line 68, :hash "1971745431"} {:id "defn/tmux-client-tty", :kind "defn", :line 70, :end-line 75, :hash "1356283202"} {:id "defn/process-controlling-tty", :kind "defn", :line 77, :end-line 91, :hash "-1584411454"} {:id "defn/terminal-app-window-center-for-tty", :kind "defn", :line 93, :end-line 118, :hash "-844435020"} {:id "defn/frontmost-window-center", :kind "defn", :line 120, :end-line 136, :hash "-467981581"} {:id "defn/capture-launch-anchor!", :kind "defn", :line 138, :end-line 147, :hash "-1740903649"} {:id "defn/frontmost-app-name", :kind "defn", :line 149, :end-line 155, :hash "1402997965"} {:id "defn/restore-frontmost-app!", :kind "defn", :line 157, :end-line 168, :hash "1261717427"} {:id "defn-/as-awt-window", :kind "defn-", :line 170, :end-line 182, :hash "677262048"} {:id "defn-/try-set-position!", :kind "defn-", :line 184, :end-line 192, :hash "167591262"} {:id "defn-/disable-focus-steal!", :kind "defn-", :line 194, :end-line 210, :hash "11651988"} {:id "defn/make-non-focusable!", :kind "defn", :line 212, :end-line 222, :hash "1162789388"} {:id "defn/show-non-focusable-surface!", :kind "defn", :line 224, :end-line 234, :hash "1916848146"} {:id "defn/place-on-launch-screen!", :kind "defn", :line 236, :end-line 267, :hash "-324537841"} {:id "defn/place-on-pointer-screen!", :kind "defn", :line 269, :end-line 271, :hash "1900442351"}]}
;; clj-mutate-manifest-end
