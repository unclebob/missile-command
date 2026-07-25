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

(defn of-state
  "Optional RNG map from game state, or nil."
  [state]
  (:rng state))

(defn with-seed
  "Attach a seeded RNG to state."
  [state seed-value]
  (assoc state :rng (seed seed-value)))

(defn next-sky-origin-x
  "Return [x state'] for a sky entry x in [0, width).
  Advances :rng when present; otherwise uses unseeded random.
  Prefer this pure [x state'] form over host-side mutation."
  [state width]
  (if-let [r (of-state state)]
    (let [[u r'] (next-unit r)]
      [(waves/random-sky-origin-x width (constantly u))
       (assoc state :rng r')])
    [(waves/random-sky-origin-x width) state]))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-25T11:16:07.234324-05:00", :module-hash "952402890", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "-1532486878"} {:id "defn/seed", :kind "defn", :line 6, :end-line 9, :hash "2005159857"} {:id "defn-/mix64", :kind "defn-", :line 11, :end-line 19, :hash "-129466331"} {:id "defn/next-unit", :kind "defn", :line 21, :end-line 25, :hash "1184842547"} {:id "defn/of-state", :kind "defn", :line 27, :end-line 30, :hash "666072371"} {:id "defn/with-seed", :kind "defn", :line 32, :end-line 35, :hash "1266062505"} {:id "defn/next-sky-origin-x", :kind "defn", :line 37, :end-line 46, :hash "1843652997"}]}
;; clj-mutate-manifest-end
