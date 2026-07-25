(ns missile-command.batteries-spec
  (:require [speclj.core :refer :all]
            [missile-command.batteries :as batteries]))

(describe "can-fire?"
  (it "allows a stocked intact battery"
    (should (batteries/can-fire? {:destroyed? false :missiles 3})))

  (it "rejects nil empty or destroyed batteries"
    (should-not (batteries/can-fire? nil))
    (should-not (batteries/can-fire? {:destroyed? false :missiles 0}))
    (should-not (batteries/can-fire? {:destroyed? true :missiles 5}))))

(describe "update-battery"
  (it "transforms only the matching battery"
    (let [batteries [{:id :left :missiles 10}
                     {:id :center :missiles 10}]
          updated (batteries/update-battery batteries :left batteries/spend-ammo)]
      (should= 9 (:missiles (first updated)))
      (should= 10 (:missiles (second updated))))))

(describe "restore"
  (it "clears destroyed and sets ammo"
    (let [restored (batteries/restore {:destroyed? true :missiles 0} 10)]
      (should-not (:destroyed? restored))
      (should= 10 (:missiles restored))
      (should (batteries/can-fire? restored)))))
