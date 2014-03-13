; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode) -*-

(ns cljengine.test
  (:require [cljengine.mc :as mc]
            [cljengine.regen :as regen]
            [cljengine.tasks :as tasks])
  (:use (clojure [core :exclude [alter]]
                 repl pprint reflect)
        (cljengine mc
                   tasks
                   events
                   regen)
        (cljminecraft core
                      entity
                      [bukkit :exclude [repeated-task
                                        cancel-task]]
                      events
                      commands
                      logging
                      util
                      [world :exclude [effect]]; (effect) has a simple bug.
                      ;; can't pull in all of cljminecraft.player without conflict:
                      [player :only [send-msg]]))
  (:import (org.reflections Reflections)
           (org.bukkit Bukkit
                       Material
                       Location
                       World
                       Effect)
           (org.bukkit.block Block
                             BlockFace ; Enum
                             BlockState)
           (org.bukkit.entity Entity
                              EntityType
                              Player)
           (org.bukkit.metadata Metadatable)
           (org.bukkit.event Event
                             Cancellable
                             EventPriority; Enums
                             HandlerList)
           (org.bukkit.event.entity PlayerDeathEvent)
           (org.bukkit.event.player PlayerMoveEvent)
           (org.bukkit.event.block BlockEvent
                                   BlockPhysicsEvent
                                   BlockBreakEvent)
           (org.bukkit.event.vehicle VehicleBlockCollisionEvent
                                     VehicleMoveEvent)
           (org.bukkit.util Vector
                            BlockVector)
           (org.bukkit.plugin Plugin)
           (org.bukkit.plugin.java JavaPlugin); subtype of Plugin
           (org.bukkit.scheduler BukkitScheduler
                                 BukkitTask)
           (cljminecraft BasePlugin
                         ClojurePlugin)
           (org.bukkit.util BlockIterator)))

(def +test-vectors+ "Try these with (apply alter-region test-vectors).  There's one block which gets added to failures-of-alter-region because it starts as water and water flows into it with the first physics check after it's removed."
  [(BlockVector. 4978.0,63.0,5028.0)
   (BlockVector. 4875.0,98.0,4997.0)])

(def +test-vector-ending-times+
  "The +test-vectors+ produced the following values for their regeneration times, which I acquired using '(sort (keys (group-by val @regen-ending-times)))'.  This is giving each block in its own task.  Note that there are 155 values for 119808 blocks.  The most assigned to a single tick was 1259, and the fewest was zero--the range is 165, so ten ticks were left out."
  '(1673498 1673499 1673500 1673501 1673502 1673503 1673504 1673505 1673506 1673507 1673508 1673509 1673510 1673511 1673512 1673513 1673514 1673515 1673516 1673517 1673518 1673519 1673520 1673521 1673522 1673523 1673524 1673525 1673526 1673527 1673528 1673529 1673530 1673531 1673532 1673533 1673534 1673535 1673536 1673537 1673538 1673539 1673540 1673541 1673542 1673543 1673544 1673545 1673546 1673547 1673548 1673549 1673550 1673551 1673552 1673553 1673554 1673555 1673556 1673557 1673558 1673559 1673560 1673561 1673562 1673563 1673564 1673565 1673566 1673567 1673568 1673569 1673570 1673571 1673572 1673573 1673574 1673575 1673576 1673577 1673578 1673579 1673580 1673581 1673582 1673583 1673584 1673585 1673586 1673587 1673588 1673589 1673590 1673591 1673592 1673593 1673594 1673595 1673596 1673597 1673598 1673599 1673600 1673601 1673602 1673603 1673604 1673605 1673606 1673607 1673608 1673609 1673610 1673611 1673612 1673613 1673614 1673615 1673616 1673617 1673618 1673619 1673620 1673621 1673622 1673623 1673624 1673625 1673626 1673627 1673628 1673629 1673630 1673631 1673632 1673633 1673634 1673635 1673636 1673637 1673638 1673639 1673640 1673641 1673642 1673644 1673645 1673646 1673647 1673650 1673651 1673654 1673655 1673659 1673663))

(defn test-alter-region [v1 v2 & {:keys [reset-failures new-mat suppress-physics delay coll-max]
                                  :or {reset-failures true
                                       new-mat Material/AIR
                                       suppress-physics false
                                       coll-max 20}}]
  "Uses *regen-total-delay* for the time.
'reset-failures' defaults to TRUE.  The 'max-output' keyword imposes a limit on the length of a collection that is returned or printed.  (These can be long.)"
  (assert* (empty? @blocks-to-regen) "(test-alter-region) attempted while blocks are still queued.
Either wait until a regen op finishes, or clear blocks-to-regen.")
  (let [delay (or delay *regen-total-delay*)]
    (try
      (let [region (gen-region-vectors v1 v2)
            num-blocks-in-region (count region) ; Total number; some of them may not need to be altered.
            attempted (count (remove #(= new-mat (get-type (get-block-at %)))
                                     region))]
        (def results! {:seed (get-seed)
                       :corners [v1 v2]
                       :delay delay
                       :new-material new-mat
                       :physics-suppressed? suppress-physics
                       :num-blocks-selected num-blocks-in-region
                       :destruction (promise)
                       :regeneration (promise)})

                                        ; (try
        (alter-region v1 v2 :new-mat new-mat :reset-failures reset-failures
                      :delay delay) ; No longer fails assertion b/c/o (verify-altered-region).
                                        ; (catch AssertionError _) )

        (assert* (not (empty? @latest-regen-region)))
        ;; This should work, I hope:
        (let [failures @failures-of-alter-region
              ending-times-map (zipmap (keys @latest-regen-region) (map :regen-target-time (vals @latest-regen-region))) ;@regen-ending-times
              ending-times (keys (group-by val ending-times-map)) ]
          ;(debug-println "Here.")
          (let [prelim-results {:num-blocks-tried attempted
                                :num-blocks-failed (count failures)
                                :failing-types (distinct (map get-type failures))
                                ;; As per the global tick counter:
                                :num-distinct-ending-times (count ending-times)
                                ;; This should be zero, in theory:
                                :ending-time-range (- (apply max (vals ending-times-map)) (apply min (vals ending-times-map)))
                                ;; Frequency of mode of ending times:
                                :ending-time-highest-frequency (apply max (map (comp count val) (group-by val ending-times-map)))}]
            (assert* (integer? attempted))
            (assert* (integer? (count failures)))
            (assert* (<= attempted num-blocks-in-region))
            ;(assert* (>= attempted num-blocks-in-region) "How can you try %s block(s) and get %s?!" attempted num-blocks-in-region)
            (assert* (<= (count failures) attempted))
            ;(debug-println "Here too.")
            (deliver (:destruction results!) prelim-results)
            (when-not (== attempted (count ending-times-map))
              (debug-announce "Number of blocks tried, %s, should equal the number of ending times recorded, %s." attempted (count ending-times-map)))
;            (assert* (== attempted (count ending-times-map)) "Number of blocks tried, %s, should equal the number of ending times recorded, %s." attempted (count ending-times-map))
            )))
      (finally
        ;; Follow-up:
        (delayed-task *plugin* (fn []
                                 (swap! regen/block-regen-order (constantly (reverse @regen/block-regen-order-reversed)))
                                 (let [ending-time-discrepancies (map #(- (:regen-target-time %) (deref (:actual-regen-time %))) (vals @latest-regen-region))
                                       {:keys [blocks-regenerated blocks-failed failure-blocks]} (verify-region)
                                       final-results {:num-blocks-regenerated blocks-regenerated
                                                      :num-blocks-failed blocks-failed
                                                      :failing-types (distinct (map get-type failure-blocks))
                                                      :max-target-time-error (apply max (map abs ending-time-discrepancies))
                                                      :mean-target-time-error (float (mc/mean ending-time-discrepancies))
                                                      :mode-target-time-error (mc/mode ending-time-discrepancies)
                                                      :block-permutation (if (< (count @regen/block-regen-order) coll-max) @regen/block-regen-order
                                                                             (take coll-max @regen/block-regen-order))}]
                                   (deliver (:regeneration results!) final-results))
                                 (debug-println "Finalising promises.")
                                 (assert* (empty? @blocks-to-regen) "End of (alter-region) regen routine was reached with %s block%s leftover."
                                          (count @blocks-to-regen) (pluralizes? (count @blocks-to-regen))))
                      (+ delay (seconds-to-ticks 2)))
        results!)))
  results!)

(defn write-test-to-file [filename & [^String comment]]
  (when comment (spit filename (println-str comment)))
  (spit filename (with-out-str (pprint results!)) :append true)
  (spit (str "order_" filename) @regen/block-regen-order))


#_(defn make-test-form [seed
                          ])
