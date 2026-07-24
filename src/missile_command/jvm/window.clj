(ns missile-command.jvm.window
  "Host window placement: open on the screen where bb was typed; no focus steal."
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
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

(defn- as-awt-window
  [native]
  (cond
    (nil? native) nil
    (instance? Window native) native
    (instance? Component native) (SwingUtilities/getWindowAncestor ^Component native)
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
  [awt-window]
  (when (instance? Window awt-window)
    (try (.setAutoRequestFocus ^Window awt-window false) (catch Exception _))
    (try (.setFocusableWindowState ^Window awt-window false) (catch Exception _))
    (try (.setAlwaysOnTop ^Window awt-window false) (catch Exception _))))

(defn- reenable-focusable!
  [awt-window]
  (when (instance? Window awt-window)
    (try (.setFocusableWindowState ^Window awt-window true) (catch Exception _))))

(defn place-on-launch-screen!
  "Center the Processing surface on the screen of launch-anchor {:x :y}.
  Avoids stealing keyboard focus. launch-anchor should be captured at process
  start (terminal where bb was typed), before the sketch appears."
  ([surface sketch-w sketch-h]
   (place-on-launch-screen! surface sketch-w sketch-h (capture-launch-anchor!)))
  ([surface sketch-w sketch-h launch-anchor]
   (let [anchor (or launch-anchor (capture-launch-anchor!))
         bounds (screen-bounds-containing (:x anchor) (:y anchor))
         loc (centered-location bounds sketch-w sketch-h)
         lx (int (:x loc))
         ly (int (:y loc))
         native (try (.getNative surface) (catch Exception _ nil))
         awt (as-awt-window native)]
     (disable-focus-steal! awt)
     (try (.setVisible surface false) (catch Exception _))
     (try (.setLocation surface lx ly) (catch Exception _))
     (try-set-position! (or awt native) lx ly)
     (try (.setVisible surface true) (catch Exception _))
     (future
       (try
         (Thread/sleep 200)
         (reenable-focusable! awt)
         (catch Exception _)))
     {:screen bounds :location loc :anchor anchor})))

(defn place-on-pointer-screen!
  [surface sketch-w sketch-h]
  (place-on-launch-screen! surface sketch-w sketch-h (pointer-location)))
