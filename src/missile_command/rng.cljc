(ns missile-command.rng
  "Tiny portable PRNG for seedable sky origins (and future pure randomness).
  State: {:seed long :n long} — each next-u32 advances n."
  (:require [missile-command.waves :as waves]))

(defn seed
  "Build RNG state from a long seed."
  [s]
  {:seed (long s) :n 0})

(defn- mix64
  "SplitMix64-style next state; returns [u32 state']."
  [{:keys [seed n]}]
  (let [z (unchecked-add (long seed) (unchecked-multiply (inc (long n)) -7046029254386353131))
        z (unchecked-multiply (bit-xor z (unsigned-bit-shift-right z 30)) -4658895280553007687)
        z (unchecked-multiply (bit-xor z (unsigned-bit-shift-right z 27)) -7723592293110705685)
        z (bit-xor z (unsigned-bit-shift-right z 31))
        u (bit-and z 0xffffffff)]
    [u {:seed (long seed) :n (inc (long n))}]))

(defn next-unit
  "Return [u state'] where u is in [0.0, 1.0)."
  [rng]
  (let [[u s'] (mix64 (or rng (seed 0)))]
    [(/ (double (bit-and u 0xffffffff)) 4294967296.0) s']))

(defn unit-fn
  "Return a nullary fn that yields successive unit doubles, mutating an atom of rng state.
  Prefer threading state via next-unit in pure code."
  [rng-atom]
  (fn []
    (let [[u s'] (next-unit @rng-atom)]
      (reset! rng-atom s')
      u)))

(defn of-state
  "Optional RNG map from game state, or nil."
  [state]
  (:rng state))

(defn with-seed
  "Attach a seeded RNG to state."
  [state seed-value]
  (assoc state :rng (seed seed-value)))

(defn next-fn
  "Return [rand-fn state] where rand-fn is (fn [] double in [0,1)).
  Without :rng on state, uses clojure.core/rand and returns state unchanged."
  [state]
  (if-let [r (of-state state)]
    (let [box (atom r)
          f (fn []
              (let [[u s'] (next-unit @box)]
                (reset! box s')
                u))]
      ;; Capture final state after each spawn by reading atom when advance is called
      [f (fn advance [s] (assoc s :rng @box))])
    [rand identity]))

(defn next-sky-origin-x
  "Return [x state'] for a sky entry x in [0, width).
  Advances :rng when present; otherwise uses unseeded random."
  [state width]
  (if-let [r (of-state state)]
    (let [[u r'] (next-unit r)]
      [(waves/random-sky-origin-x width (constantly u))
       (assoc state :rng r')])
    [(waves/random-sky-origin-x width) state]))
