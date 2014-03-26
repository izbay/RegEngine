; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode) -*-

(ns cljengine.test
  (:require [cljengine.mc :as mc]
            [cljengine.regen :as regen]
            [cljengine.tasks :as tasks])
  (:use (clojure [core :exclude [map ]]
                 repl pprint reflect set)
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
           (org.bukkit.plugin Plugin
                              RegisteredListener)
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

(defn test-alter-region [l1 l2 & {:keys [delay, output-delay, monitor-events, suppress-physics, bottom-up]
                                                   :or {delay +regen-total-delay+,
                                                        suppress-physics true,
                                                        bottom-up true}}]
  (let [output-delay (or output-delay (* 1000 (+ (ticks-to-seconds delay)
                                                 10)))]
    (debug-println "Testing (alter-and-restore-region).  Will verify results after" output-delay "ms.")
      (when monitor-events
        (unregister-our-events)
        (register-all-events-feedback))
    (alter-and-restore-region l1 l2 :delay delay :suppress-physics suppress-physics :bottom-up bottom-up)
    (Thread/sleep output-delay)
    (let [retv (verify-altered-region)]
      (when monitor-events (unregister-our-events))
      retv)))

(defn test-alter-region-with-file-output [l1 l2 output-file-name & {:keys [delay, output-delay, monitor-events, suppress-physics, bottom-up]
                                                   :or {delay +regen-total-delay+,
                                                        suppress-physics true,
                                                        bottom-up true}}]
  ":delay is regular regen delay.  :output-delay is the number of milliseconds to be redirecting to 'output-file-name'.
If 'monitor-events' is true, we'll also load event handlers--and unload them after."
  (let [output-delay (or output-delay (* 1000 (+ (ticks-to-seconds delay)
                                                 10)))] (with-debug-print-to-file-timed output-file-name output-delay
            (when monitor-events
              (unregister-our-events)
              (register-all-events-feedback))
            (alter-and-restore-region l1 l2 :delay delay :suppress-physics suppress-physics :bottom-up bottom-up))
   (let [retv (verify-altered-region)]
     (when monitor-events (unregister-our-events))
     retv)))
