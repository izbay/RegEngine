; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode); eval: (cider '127.0.0.1 4005) -*-

(comment " Try running the following in Emacs Lisp to get the REPL going: "
         (progn
          (if (not (cider-connected-p)) (cider "127.0.0.1" 4005)
              (cider-interactive-eval-to-repl "(in-ns 'cljengine.regen)")
              (cider-switch-to-current-repl-buffer)
              (paredit-mode))))

(ns cljengine.regen
  "Testing Clojure in Minecraft."
  (:refer-clojure :exclude [map])
                                        ;  (:require clojure.core)
  (:use (clojure [core :exclude [map]] repl pprint reflect set))
  (:use clojure.java.io)
  (:use
   (cljminecraft core
                          entity
                          [bukkit :exclude [repeated-task ; (repeated-task) has a simple bug.  He must never have tried it.
                                            cancel-task]]
                          events
                          commands
                          logging
                          util
                          [world :exclude [effect]] ; (effect) has a simple bug.
                          ;; can't pull in all of cljminecraft.player without conflict:
                          [player :only [send-msg]]))
  (:require [cljengine.mc :as mc]
            [cljengine.events :as events]
            [cljengine.tasks :as tasks])
  (:use (cljengine mc
                     [tasks :exclude [map]]
              [block :exclude [map]]
              [events :exclude [map]]))
  (:import (java.util Map
                      Set))
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
                             EventPriority ; Enums
                             HandlerList
                             Listener)
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
           (org.bukkit.plugin.java JavaPlugin) ; subtype of Plugin
           (org.bukkit.scheduler BukkitScheduler
                                 BukkitTask)
           (cljminecraft BasePlugin
                         ClojurePlugin)
           (org.bukkit.util BlockIterator) )
  (:import (com.github.izbay.regengine RegEnginePlugin
                                       BlockImage
                                       RegenBatch)
           (com.github.izbay.regengine.block Action
                                             BlockTypes
                                             DependingBlock
                                             VineDependingBlock
                                             VineDependingBlock$Orientation
                                             DependingBlockSet)
           (com.github.izbay.siegeengine SiegeEnginePlugin
                                         Ram
                                         Weapon
                                         WeaponEventObserver)
           (com.github.izbay.util Util)) )

                                        ;(declare physics-blocking-handler, player-move-event-handler)


(do
  (defmulti regen-batch-destroying (fn [blocks? & _]
                                     (if (coll? blocks?) [(class (first blocks?))]
                                         (class blocks?))))
  (defmethod regen-batch-destroying :cljengine.mc/block-state-image [block delay]
    (regen-batch-destroying [block] delay))
  (defmethod regen-batch-destroying [:cljengine.mc/block-state-image] [blocks delay]
    (regen-batch-destroying *plugin* (map get-block-vector blocks) (get-current-world) delay))
  (defmethod regen-batch-destroying [Vector] [block-vecs delay]
    (RegenBatch/destroying *plugin* (get-current-world) block-vecs delay)) )


(do
  (defmulti batch-intersection (fn [b1 b2] [(class b1) (class b2)]))
  (defmethod batch-intersection [RegenBatch RegenBatch] [bat1 bat2]
    (apply intersection (map #(set (keys (.. % blocks blocks))) [bat1 bat2])))
  (defmethod batch-intersection [RegenBatch Set] [b s]
    (intersection (set (keys (.. b blocks blocks))) s))
  (defmethod batch-intersection [Set Set] [s1 s2]
    (intersection s1 s2)))

(defn get-projected-restoration-time [#^RegenBatch rb]
  #_(+ (.delay rb) (get-full-time))
  (.getProjectedRestorationTime rb))

(defn get-possibly-conflicting-batches [bat & {:keys [running-only]}]
  (let [finish-time (get-projected-restoration-time bat)]
    (reverse (sort-by get-projected-restoration-time
                      (for [b (RegenBatch/activeBatches (.world bat))
                            :let [t_f_b (get-projected-restoration-time b)]
                            :when (and (not= b bat)
                                       (or (not running-only) (.isRunning b))
                                       (>= finish-time t_f_b))]
                        b)))))

(defn group-by-conflicts [bat possibly-conflicting-batches]
  (let [groupings (loop [remaining-batches possibly-conflicting-batches
                           remaining-blocks (set (keys (.. bat blocks blocks)))
                           acc {}]
                      (cond*
                       ((empty? remaining-blocks) acc)
                       ((empty? remaining-batches) (assoc acc bat remaining-blocks))
                       (:else (let [next-batch (first remaining-batches)
                                 isec (batch-intersection remaining-blocks (set (keys (.. next-batch blocks blocks))))]
                             (recur (rest remaining-batches)
                                    (difference remaining-blocks isec)
                                    (if (empty? isec) acc (assoc acc next-batch isec)))))))]
      groupings))

;(defn batch-destroy-region )

(defn queue-batch-restoration [bat]
  #_(.queueBatchRestoration bat)
  (let [;finish-time (.getRestorationTime bat)
        possibly-conflicting-batches (get-possibly-conflicting-batches bat)]
;; FIXME:
    ))

(defn alter-and-restore [bat]
  (.alterAndRestore bat))

(do
  (defmulti block-order class)
  (defmethod block-order RegenBatch [bat]
    (map (memfn getVector) (. bat blockOrder))))

(do
  (defmulti get-actions class)
  (defmethod get-actions RegenBatch [bat]
    (map (comp (memfn action) val) (.. bat blocks blocks) )))

;(load "dependencies")







(defonce ^{:doc "Master collection of all ongoing REG ENgine restoration operations, modified by (regenerate-region)."}
  regen-batch-regions-active (atom #{}))




                                        ;(defonce ^{:doc "Bound to the last PlayerMoveEvent listener registered to our plugin."} player-move-listener (atom nil))
(def +regen-warning-effect+
  "We'll try displaying this as a warning that a block is about to reappear:"
  org.bukkit.Effect/MOBSPAWNER_FLAMES)

(defn visual-warning-at [pos]
  "A warning to players that the block is about to be overwritten.  The 'nil' arg depends on the effect type."
  (effect (.toLocation (get-vector pos) (get-current-world)) +regen-warning-effect+ nil))







;; Redstone ore: replace glowing with regular
#_(do
  (defmethod-on-block-type restored? glowing-redstone-ore [block]
    (= (get-type (get-block-at block)) Material/REDSTONE_ORE))

  (defmethod-on-block-type restore glowing-redstone-ore [img]
   (let [block (get-block-at (the BlockImage img))]
     (set-type (the Block block) Material/REDSTONE_ORE)
     (assert* (= Material/REDSTONE_ORE (get-type block)))
     (assert* (restored? img))
     block)))

;; Burning furnace: TODO: Replacing with a furnace that isn't burning?
#_ (do
     (defmethod-on-block-type restored? glowing-redstone-ore [img]
       (= (get-type (get-block-at block)) Material/REDSTONE_ORE))

     (defmethod-on-block-type restore lit-furnace [img]
       (let [block (get-block-at (the BlockImage img))]
         (set-type (the Block block) Material/BURNING_FURNACE)
         (assert* (= Material/BURNING_FURNACE))
         (assert* (restored? img))
         block)))


(declare regenerate-region)

#_(defonce ^{:doc "Used by PC warner: tracks players' being in or out of the REG EN area."}
  players-in-regen-zone (atom #{}))

#_(defn player-flagged-in-regen-area? [^Player pc]
  (contains? @players-in-regen-zone pc))


#_(defn player-in-regen-area? [^Player pc]
  "True if either of pc's bounding blocks is within any of the active REG EN batches in the regen-batch-regions-active set."
  )

(declare regengine-event-handlers)

#_(defn player-move-event-handler [^PlayerMoveEvent ev]
  "Block-regen PlayerMoveEvent handler: when a block is in queue, checks whether a player is moving in or out of the affected area... blah, blah."
  (try
;(debug-println "Move event noted.")
    (when (get-in @regengine-event-handlers ["player.player-move" :active])
        (let [pc (.getPlayer ev)]
       (cond*
        ;; Coming in:
        [(and (not (player-flagged-in-regen-area? pc))
              (player-in-regen-area? pc))
         (swap! players-in-regen-zone conj pc)
         (assert (contains? @players-in-regen-zone pc))
         (send-msg pc "You are now in a REG EN zone.")
         ;; TODO: Also send the other warning, once I can easily identify the block timing.  I need the ending-time from the batch from the block, and it would be best if some already-used func like (player-in-regen-area?) could get it.
         ]
        ;; Going out:
        [(and (player-flagged-in-regen-area? pc)
              (not (player-in-regen-area? pc)))
         (swap! players-in-regen-zone disj pc)
         (assert* (not (contains? @players-in-regen-zone pc)))
         ;; TODO: Fix case where player gets trapped inside... but is told he is 'safely outside'.  Happens when the player is considered to have "left" the REG EN zone because the zone disappears.
         (send-msg pc "Safely outside the REG EN zone.")])))
    (catch Error e
      (debug-announce "Exception thrown within player-move warner.")
  ;    (cleanup-fn)
      (throw e))))

(defn physics-blocking-handler [^BlockEvent ev]
  "Installed by (ensure-regengine-handlers) in regen.clj.
If *log-physics-events* is logical true, will print a message with each event.
If *log-physics-events* is set to :record, this will also be stored into the *physics-event-log* global. "
  (try
    (assert (instance? BlockPhysicsEvent ev))
    (let [block (.getBlock ev)
          name (.getEventName ev)
          type (.getChangedType ev)]
                                        ;(debug-println "Logging?" *log-physics-events*)
      (when *log-physics-events*
        (debug-announce  name "; material" type "on block" block)
        (when (= *log-physics-events* :record)
          (swap! physics-events-log conj ev)))
      (when-not *suppress-physics*
        (when (get-in @cljengine.regen/regengine-event-handlers ["block.block-physics" :active])
          (if (.isCancelled ev) (debug-println "Already cancelled.")
              (do (.setCancelled ev true)
                  (debug-announce "Cancelled %s" name))))))
    true
    (catch Error e
      (debug-announce "Exception in (physics-blocking-handler)!")
      (throw e))))

;; TODO: Priority levels?
;; Gotcha: If you change either of the handler funcs, you may need to update this variable!
#_(defonce regengine-event-handlers (atom {"block.block-physics" {:handler physics-blocking-handler
                                                            :listener nil
                                                            :priority :normal
                                                            :active false},
                                     "player.player-move" {:handler player-move-event-handler
                                                           :listener nil
                                                           :priority :normal
                                                           :active false}}))

(defonce latest-altered-region (atom nil))

(do
  (defmulti batch-alter (fn [blocks & _]
                        ;  (when-not (coll? blocks) (throw (IllegalArgumentException. "(batch-alter) requires a collection for its first argument.")))
                          (if-not (empty? blocks) (let [class-vec (vec (set (clojure.core/map class blocks)))]
                                                    (doall class-vec)
                                                    class-vec)
                                  [Block]))) ; Lazy seq is OK here
  ;; I don't know how (isa?) handles sets.  I know that it does vectors recursively.
  (defmethod batch-alter [Location] [loc-list & options]
    (apply batch-alter (map get-block-at loc-list) options))
  (defmethod batch-alter [Vector] [loc-list & options]
    (apply batch-alter (map get-block-at loc-list) options))
  (defmethod batch-alter [:cljengine.mc/block-state-image] [block-list & {:keys [new-material, suppress-physics, top-down]
                                                               :or { ;delay regen-total-delay,
                                                                    top-down true
                                                                    suppress-physics true,
                                                                    new-material Material/AIR}}]
    "foo"
   ))

(do
  (defmulti alter-region
    "Now delegates to batch-alter."
    (fn [l1 l2 & _] (vec (map class [l1 l2]))))
  (defmethod alter-region [Location, Location] [^Location l1 ^Location l2 & {:keys [new-material, suppress-physics, top-down] :as options
                                        :or {;delay regen-total-delay,
                                             top-down true
                                             suppress-physics true,
                                             new-material Material/AIR}}]
"foo")
  (defmethod alter-region [::mc/block-state-image ::mc/block-state-image] [block1 block2 & rest]
    (debug-println (list* block1 block2 rest))
    (apply alter-region (get-location block1) (get-location block2) rest)))

(do
  (defmulti alter-and-restore-region (fn [l1 l2 & _] (vec (map class [l1 l2]))))
  (defmethod alter-and-restore-region [Location, Location] [^Location l1 ^Location l2 & options]
    (assert* (= (get-world l1) (get-world l2)))
    ;(assert* (and (integer? delay) (> delay 0)))
    (let [images (apply alter-region l1 l2 options)]
      (apply regenerate-region images options)))
  (defmethod alter-and-restore-region [::mc/block-state-image ::mc/block-state-image] [block1 block2 & rest]
    (debug-println (list* block1 block2 rest))
    (apply alter-and-restore-region (get-location block1) (get-location block2) rest)))

;(declare instate-player-move-event-handler)
(declare ensure-regengine-handlers)

#_(defn find-failures []
  (remove #(restored? %) @latest-altered-region))

#_(defn verify-altered-region []
  (cond*
   ((every? #(restored? %) @latest-altered-region)
          (println "No failures!")
          true)
   (:else (println "*** Failures!")
          (find-failures) )))


(defonce backed-up-regions (atom {}))

#_(defn backup-region [start end & [tag]]
  (let [region (gen-region-vectors start end)
        tag? (or tag (count @backed-up-regions))
        tag (some #(when (not (contains? @backed-up-regions %)) %) (iterate inc tag?))]))














(defn cancel-all-regen-ops [& {:keys [events] :or {events true}}]
  (cancel-all-tasks)
  (when events
    (unregister-our-events))
  (swap! regen-batch-regions-active (constantly #{})))
