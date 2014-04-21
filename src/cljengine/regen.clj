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
  ;; Add some Enums...
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
           (com.github.izbay.util Util)))

                                        ;(declare physics-blocking-handler, player-move-event-handler)


(do
  (defmulti regen-batch-destroying (fn [blocks? & _]
                                     (if (coll? blocks?) [(class (first blocks?))]
                                         (class blocks?))))
  (defmethod regen-batch-destroying :cljengine.mc/block-state-image [block delay]
    (regen-batch-destroying [block] delay))
  (defmethod regen-batch-destroying [:cljengine.mc/block-state-image] [blocks delay]
    (RegenBatch/destroying *plugin* (map get-block-vector blocks) (get-current-world) delay))
  (defmethod regen-batch-destroying [Vector] [block-vecs delay]
    (RegenBatch/destroying *plugin* block-vecs (get-current-world) delay)) )

(defn queue-batch-restoration [bat]
  (.queueBatchRestoration bat))

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

(defonce ^{:doc "Default number of ticks between backup & restoration." :dynamic true}
  regen-total-delay (seconds-to-ticks 20))

(defonce ^{:doc "Number of ticks that elapse between warning attempts"
                                        ;  :dynamic true
           }
  regen-warning-period (seconds-to-ticks 5))

(def +regen-warning-effect+
  "We'll try displaying this as a warning that a block is about to reappear:"
  org.bukkit.Effect/MOBSPAWNER_FLAMES)

(defonce ^{:doc "Master collection of all ongoing REG ENgine restoration operations, modified by (regenerate-region)."}
  regen-batch-regions-active (atom #{}))

(defn regengine-done? []
  "True iff no regen batch regions are queued."
  (empty? @regen-batch-regions-active))

#_(defn regen-regions-active? []
    ;; I know the Clojure docs don't want you using (empty?) for this check, but I want a simple retval:
    (not (empty? @regen-batch-regions-active)))

                                        ;(defonce ^{:doc "Bound to the last PlayerMoveEvent listener registered to our plugin."} player-move-listener (atom nil))

(defn visual-warning-at [pos]
  "A warning to players that the block is about to be overwritten.  The 'nil' arg depends on the effect type."
  (effect (.toLocation (get-vector pos) (get-current-world)) +regen-warning-effect+ nil))

(defn alter-block [block & {:keys [new-material]}]
  (let [new-material (or new-material Material/AIR)]
    (set-type block new-material)
  ;; Kludgy:
    (let [block' (get-block-at (get-location block))]
      (assert* (= (get-type block') new-material) "Type %s should be type %s!" (get-type block') new-material)
      block')))

(do
  (defmulti restore
    "Wrapper for BlockImage.restore() that allows overloading for particular types.  Note: If you add a specialization clause to (restore), you should probably add one to (restored?) as well."
    dispatch-on-block-type)
  (defmethod restore BlockImage [b]
    (.restore b)))

(do
  (defmulti restored?
    "Wrapper for BlockImage.isRestored(), parallel to (restore)."
    dispatch-on-block-type)
  (defmethod restored? BlockImage [i]
    (.isRestored i)))

;; Redstone ore: replace glowing with regular
(do
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

;; Monster eggs: Retrieve the texture material for the block; replace with a regular block of that material type.
(do
  ;; TODO: Overload on type?
  (defmethod-on-block-type restored? monster-egg [img]
    (assert* (instance? BlockImage img))
    (= (get-type (the Material (get-block-at img)))
       (the Material (.. img getBlockState getData getMaterial))))

  (defmethod-on-block-type restore monster-egg [img]
    (let [block (the Block (get-block-at (the BlockImage img)))
          texture (the Material (.. img getBlockState getData getMaterial))]
      (set-type block texture)
      (assert* (= get-type block texture))
      (assert* (restored? img))
      block)))

(declare regenerate-region)

(defonce ^{:doc "Used by PC warner: tracks players' being in or out of the REG EN area."}
  players-in-regen-zone (atom #{}))

(defn player-flagged-in-regen-area? [^Player pc]
  (contains? @players-in-regen-zone pc))


(defn player-in-regen-area? [^Player pc]
  "True if either of pc's bounding blocks is within any of the active REG EN batches in the regen-batch-regions-active set."
  (let [occ-blocks (get-player-space pc)]
    (some (fn [region]
            (assert* (map? region) "Excp. in (player-in-regen-area?).")
            (not (empty? (select-keys region occ-blocks))))
          @regen-batch-regions-active)))

(declare regengine-event-handlers)

(defn player-move-event-handler [^PlayerMoveEvent ev]
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
(defonce regengine-event-handlers (atom {"block.block-physics" {:handler physics-blocking-handler
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
   (when-not (coll? block-list)
     (throw (IllegalArgumentException. "Bad arg to (batch-alter).")))
   (let [blocks block-list]
     (if (empty? blocks) (debug-println "Empty blocklist passed to (batch-alter).")
         (let [legit-blocks (remove #(= (get-type %) new-material)
                                    blocks)]
           (assert* (not-any? #(= (get-type %) new-material) legit-blocks))
           (if (empty? legit-blocks) (debug-announce "The %s block%s in <target region> %s already %s!" (count blocks) (pluralizes? (count blocks)) (if (> (count blocks) 1) "are" "is") new-material)
               (let [images (doall (map #(BlockImage. %) legit-blocks))
                     initial-set (DependingBlockSet. (map #(DependingBlock/from % Action/DESTROY) images)) ]
                 (let [full-set (.. (the DependingBlockSet initial-set) doFullDependencySearch )
                       icnt (.size full-set)]
                   ;(assert* (== (count legit-blocks) icnt))
                   ;(assert* (not-any? #(= (.getType %) new-material) images) "Bad image!")
                   (swap! latest-altered-region (constantly images)) ; Make a note in case we fail the operation
                   (assert* (== icnt (count @latest-altered-region)))
                   (debug-announce "Altering %s block%s: %s." icnt (pluralizes? icnt) (get-full-time))
                   (doseq [b full-set]
                     (let [i (the BlockImage (.block (the DependingBlock b)))]
                       (abort-on-emergency-break!)
                                        ;(debug-println "Altering block.")
                       (alter-block (get-block-at (.getLocation i)) :new-material new-material)
                       (debug-println (format "@ %s: Block at %s, changing %s -> %s." (get-full-time) (format-vector (get-vector i)) (get-type i) new-material))
                       (assert* (= (.getType (get-block-at (.getLocation i))) new-material))))
                   (debug-announce "Altered %s block(s): %s." icnt (get-full-time))
                   (assert* (== icnt (count images) (count @latest-altered-region)))
                   ;; Massive assert:
                   (assert* (do
                              (when (not-every? #(= new-material (get-type (get-block-at (get-location %))))
                                                images)
                                (debug-announce "Warning from (alter-region)--not all blocks were successfully changed!"))
                              true))
                   images))))))))

(do
  (defmulti alter-region
    "Now delegates to batch-alter."
    (fn [l1 l2 & _] (vec (map class [l1 l2]))))
  (defmethod alter-region [Location, Location] [^Location l1 ^Location l2 & {:keys [new-material, suppress-physics, top-down] :as options
                                        :or {;delay regen-total-delay,
                                             top-down true
                                             suppress-physics true,
                                             new-material Material/AIR}}]
    (assert* (= (get-world l1) (get-world l2)))
    (let [ vectors (gen-region-vectors l1 l2 :reversed top-down)
          blocks (map #(let [l (get-location %)
                             block (get-block-at l)]
                         block)
                      vectors)]
      (assert* (not (empty? blocks)))
      (apply batch-alter blocks options)))
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

(defn batch-alter-and-restore [block-list & {:keys [delay, warn-players, suppress-physics, bottom-up, new-material, world]
                                             :or {delay regen-total-delay,
                                                  warn-players true,
                                                  bottom-up true,
                                                  suppress-physics true,
                                                  new-material Material/AIR}}]
  (assert* (coll? (vec block-list)) "Type %s should be a collection." (type block-list))
  (assert* (and (integer? delay) (> delay 0)))
  (let [images (batch-alter block-list :new-material new-material :suppress-physics suppress-physics)]
    (assert* (do ; Possibly expensive assertion
               (doall (map (fn [i j]
                             (assert* (= i j) "BlockImage %s should equal %s." i j)) images @latest-altered-region ))
               true))
    (regenerate-region (set images) :delay delay :suppress-physics suppress-physics :bottom-up bottom-up :wold world)))





(do
  (defmulti regenerate-region
   "Regeneration component only."
   (fn [blocks & _]
     (if (empty? blocks) [BlockImage]  ; hack
       (distinct (vec (map class blocks))))))
  (defmethod regenerate-region [BlockImage] [images & {:keys [delay, warn-players, suppress-physics, bottom-up, world, plugin]
                                                         :or {delay regen-total-delay,
                                                              warn-players true,
                                                              bottom-up true,
                                                              suppress-physics true}}]
    "Expects a collection of BlockImages.
If :warn-players is true (default), the PC-warner system is used.  TODO: The timing should be better controlled.  If the restoration is nowhere near in time, we don't need to warn constantly.
If :bottom-up is log.true, we'll sort the blocks lowest-to-highest--other factors notwithstanding, this is most likely to meet dependencies.
If :suppress-physics is set, we'll try to do just that...
:world isn't used, but needs to be.
:plugin is set to *plugin* by default."
    ;; If they aren't going, we start (1) the physics suppressor; (2) the player on-move warner:
    ;(debug-announce "Warning? %s")
    (let [plugin (or plugin *plugin*)]
      (let [images (if-not bottom-up images
                           (sort (fn [i1 i2] (apply compare-vectors (map get-vector [i1 i2]))) images))]
        (ensure-regengine-handlers)
        (if warn-players
          (do(swap! regengine-event-handlers assoc-in ["player.player-move" :active] true)
             (assert* (get-in @regengine-event-handlers ["player.player-move" :active])))
          (swap! regengine-event-handlers assoc-in ["player.player-move" :active] false))
        ;; Make sure that, for now, this is disabled:
        (swap! regengine-event-handlers assoc-in ["block.block-physics" :active] false)
        (assert* (not (get-in @regengine-event-handlers ["block.block-physics" :active])))
        (assert* (every? (partial instance? BlockImage) images) "Bad types: %s" images)
              (assert* (integer? delay))
              (assert* (> delay 0))
                                        ;(debug-println images)
              (cond*
               [(empty? images)
                (debug-announce "Can't regen empty list of blocks.")
                nil]
               [:else
                ;; It starts for real at this point:
                (let [regen-blocks-map (zipmap (map (comp get-block-vector get-location) images) images)
                      cur-time (get-full-time)
                      regen-time (+ cur-time delay)]
                  (assert* (not (contains? @regen-batch-regions-active regen-blocks-map)) "Uh-oh--this set of items is still in the REG EN set!")
                  ;; Modifying global:
                  (swap! regen-batch-regions-active conj regen-blocks-map)
                  (assert* (contains? @regen-batch-regions-active regen-blocks-map))
                  (debug-announce "@ %s: Queueing restoration task to fire in %s ticks." (get-full-time) delay)
                  ;; Concurrency?
                  (let [warning-task (promise)]
                    ;; Primary restoration handler:
                    (delayed-task plugin
                                  (fn []
                                    (try
                                      (debug-announce "@ %s: Firing regen task." (get-full-time))
                                     ;; TODO: This might be a good spot to try a transaction...
                                     (assert* (contains? @regen-batch-regions-active regen-blocks-map))
                                     (when suppress-physics
                                       (swap! regengine-event-handlers assoc-in ["block.block-physics" :active] true)
                                       (assert* (get-in @regengine-event-handlers ["block.block-physics" :active])))
                                     (debug-println "Regen map:" regen-blocks-map)
                                     (def latest-regen-map regen-blocks-map)
                                     (loop* [n 1]
                                            (abort-on-emergency-break!)
                                            (debug-announce "Starting restoration pass %s: %s." n (get-full-time))
                                            (let [vals-to-check (vals regen-blocks-map)]
                                              (debug-println "Vals:" vals-to-check)
                                              (if (not-every? true?
                                                              (doall (map (fn [b]
                                                                            (try
                                                                              (debug-announce "@ %s: Image of type %s at %s.  Restored? %s" (get-full-time) (.getType b) (format-vector (get-vector b)) (restored? b))
                                                                              (if (restored? b) true
                                                                                  (do
                                                                                    (debug-announce "Restoring %s: %s -> %s." b (get-type (get-block-at (get-location b))) (get-type b))
                                                                                    (restore b)
                                                                                    false))
                                                                              (catch Error e
                                                                                (debug-announce "Exception thrown within the map func in the restoration block loop!")
                                                                                (throw e))))
                                                                          vals-to-check)))
                                                (recur (inc n))
                                                (debug-announce "Done with restoration loop after %s passes!" n))))
                                     (debug-println "Dequeueing this REG EN batch.")
                                     (swap! regen-batch-regions-active disj regen-blocks-map)
                                     (when warn-players
                                       (cond*
                                        ((not (realized? warning-task))
                                         (debug-announce "Warning: The warning-issuing repeated-task seems not to have been started."))
                                        ((not (task-active? @warning-task))
                                         (debug-announce "Warning: The warner got stopped."))
                                        (:else (cancel-task @warning-task)))
                                       ;; If all regen regions are done, we can deactivate the warning stuff.  TODO: Move this outside of the individual batch handler.
                                       (when (regengine-done?)
                                         (swap! regengine-event-handlers assoc-in ["player.player-move" :active] false)
                                         (assert* (not (get-in @regengine-event-handlers ["player.player-move" :active]))))
                                       (swap! regengine-event-handlers assoc-in ["block.block-physics" :active] false)
                                       (assert* (not (get-in @regengine-event-handlers ["block.block-physics" :active])))
                                       (assert* (not (contains? @regen-batch-regions-active regen-blocks-map)))
                                       ;; Remove players from considertation for the movement warner if they're no longer in a REG EN zone:
                                       (doseq [pc (online-players)]
                                         (when (and (player-flagged-in-regen-area? pc)
                                                    (not (player-in-regen-area? pc)))
                                           (swap! players-in-regen-zone disj pc)
                                           (assert* (not (player-flagged-in-regen-area? pc))))))
                                     (catch Error e
                                       (debug-announce "Exception in regeneration task!")
                                       (throw e))))
                                  delay)

                    ;; Unless deinstructed, start the warner loops if they aren't running:
                    (when warn-players
                      (debug-println "(Re)starting player warner.")
                      (swap! regengine-event-handlers assoc-in ["player.player-move" :active] true)
                      (assert* (get-in @regengine-event-handlers ["player.player-move" :active]))
                      ;; Always keep a promise!
                      (assert* (not (realized? warning-task)))
                      (let [^BukkitTask warning-task' (repeated-task plugin
                                                                     (fn []
                                                                       (try
                                                                         ;; Warn every player in zone:
                                                                         (doseq [pc (online-players)]
                                                                           ;; TODO: Do we use a check to only warn players if the impending block is *solid*?
                                                                           ;; TODO: This also could be done more efficiently with vectors.
                                                                           (let [occ-blocks (get-player-space pc)
                                                                                 isec-map (select-keys regen-blocks-map occ-blocks)
                                                                                 ;; Ignore air blocks:
                                                                                 isec-not-air (remove air? (vals isec-map))]
                                                                             (assert* (every? (partial instance? Vector) occ-blocks))
                                                                             (when-not (empty? isec-not-air)
                                                                               (send-msg pc (let [sec (ticks-to-seconds (- regen-time (get-full-time)))]
                                                                                              (format "A block at your position is going to reappear in %s second%s." sec (pluralizes? sec)))))
                                                                             ;; TODO: Show VFX to players "near enough":
                                                                             #_(when (some #(< (taxicab-distance % block)
                                                                                               *regen-vfx-distance*)
                                                                                           ;; TODO: Check player's world.
                                                                                           (online-players))
                                                                                 ;; NB: I'm referring directly to a member var here:
                                                                                 (when (.. RegEnginePlugin (getInstance) doParticles)
                                        ;(.. RegEnginePlugin (getInstance) doParticles)
                                        ;(debug-announce "VFX!")
                                                                                   (visual-warning-at block)))))
                                                                         ;; Viz warning as well:
                                                                         (when (.. RegEnginePlugin (getInstance) doParticles)
                                                                           (doall (map (comp visual-warning-at get-state) ;(remove air? (vals regen-blocks-map))
                                                                                       (vals regen-blocks-map) )))
                                                                         (catch Error e
                                                                           (debug-announce "Exception thrown within the on-tick warner.")
                                        ;(abort-func)
                                                                           (throw e))))
                                                                     ;; Task t2's timing params:
                                                                     0 ;; TODO: Reinstate two-phase warning; for now we don't use; wait
                                                                     regen-warning-period)]
                        (assert* (instance? BukkitTask warning-task'))
                        (deliver warning-task warning-task')))))
                ;; retval
                [(count images) delay]]))))
  (defmethod regenerate-region :default [blocks? & rest]
    (debug-println-2 ":default method in (regenerate-region).")
    (assert* (not-empty blocks?))
    (let [images (clojure.core/map #(BlockImage. (get-state %)) blocks?)
          class-vec (vec (set (map class images)))]
      (doall images)
      (doall class-vec)
      (def foo-images images)
      (assert* (isa? class-vec [BlockImage]) "Dammit, wrong type: %s, %s" images class-vec)
      (apply regenerate-region images rest))))

#_(defn alter-and-restore [loc & options]
  "Single-block wrapper."
  ;(apply alter-and-restore-region loc loc rest)
  (apply batch-alter-and-restore [loc] options))

(defn find-failures []
  (remove #(restored? %) @latest-altered-region))

(defn verify-altered-region []
  (cond*
   ((every? #(restored? %) @latest-altered-region)
          (println "No failures!")
          true)
   (:else (println "*** Failures!")
          (find-failures) )))



;;;; PC Warning Subsystem
;; id=warner, id=warning





(defn ensure-regengine-handlers []
  "NB: By default they are not turned on."
  (assert* (map? @regengine-event-handlers))
  (doseq [[event {:keys [handler, listener, priority, active]}] (seq @regengine-event-handlers)]
    (assert* (instance? String event))
    ;(assert* (bound? (var handler)))
    (let [listeners (set (get-registered-listeners))
          old-c (count listeners)]
      (cond*
       ((and listener (contains? listeners listener))
        (debug-println event "handler still in place."))
       (:else
        (register-event *plugin* event handler priority)
        (assert* (== (count (get-registered-listeners))
                     (inc old-c)))
        (let [diff (difference (set (get-registered-listeners)) listeners)]
          (assert* (== 1 (count diff)) "Number %s should be 1." (count diff))
          (swap! regengine-event-handlers assoc-in [event :listener] (first diff)))
        (debug-println "REG EN's" event "handler loaded."))))
  ;  (when-not active (swap! regengine-event-handlers assoc-in [event :active] true))
    )
  ;(assert* (every? #(get % :active) (vals @regengine-event-handlers))); Check one more time whether all are turned on
  @regengine-event-handlers)






(defn cancel-all-regen-ops [& {:keys [events] :or {events true}}]
  (cancel-all-tasks)
  (when events
    (unregister-our-events))
  (swap! regen-batch-regions-active (constantly #{})))
