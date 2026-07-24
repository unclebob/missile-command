(ns missile-command.jvm.window
  "Host window placement: open on the pointer's screen without stealing focus."
  (:import [java.awt GraphicsEnvironment MouseInfo Window Component]
           [javax.swing SwingUtilities]))

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
          ;; NEWT GLWindow duck-typing
          (.setPosition native (int x) (int y))
          true
          (catch Exception _ false)))))

(defn- disable-focus-steal!
  "Prefer opening without taking keyboard focus from the current app."
  [awt-window]
  (when (instance? Window awt-window)
    (try (.setAutoRequestFocus ^Window awt-window false) (catch Exception _))
    (try (.setFocusableWindowState ^Window awt-window false) (catch Exception _))
    (try (.setAlwaysOnTop ^Window awt-window false) (catch Exception _))))

(defn- reenable-focusable!
  [awt-window]
  (when (instance? Window awt-window)
    (try (.setFocusableWindowState ^Window awt-window true) (catch Exception _))))

(defn place-on-pointer-screen!
  "Move the Processing surface onto the screen under the mouse and avoid focus steal.
  surface must implement setLocation(int,int) and getNative() (PSurface)."
  [surface sketch-w sketch-h]
  (let [{:keys [x y]} (pointer-location)
        bounds (screen-bounds-containing x y)
        loc (centered-location bounds sketch-w sketch-h)
        lx (int (:x loc))
        ly (int (:y loc))
        native (try (.getNative surface) (catch Exception _ nil))
        awt (as-awt-window native)]
    (disable-focus-steal! awt)
    ;; Hide briefly so re-show with autoRequestFocus=false is less grabby.
    (try (.setVisible surface false) (catch Exception _))
    (try (.setLocation surface lx ly) (catch Exception _))
    (try-set-position! (or awt native) lx ly)
    (try (.setVisible surface true) (catch Exception _))
    ;; Allow the user to focus later by clicking the window.
    (future
      (try
        (Thread/sleep 200)
        (reenable-focusable! awt)
        (catch Exception _)))
    {:screen bounds :location loc :pointer {:x x :y y}}))
