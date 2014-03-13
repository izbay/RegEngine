; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode) -*-

(ns cljengine.regen
  (:require [cljengine.mc :as mc])
  (:use (clojure [core :exclude [alter]]
                 repl pprint reflect)
        (cljengine mc tasks
                   events
;                   [backup-restore :exclude [*backed-up-region*]]
                    )
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
                      [player :only [send-msg]])
        )
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


;(load-file "clojure/backup-restore.clj")
;(load-file "clojure/events.clj")
;(load-file "clojure/tasks.clj")


#_(gen-interface :name com.github.izbay.regengine.RegenSectionIface
               ;:methods [[regen [Block] void]]
               )

#_(gen-interface :name com.github.izbay.regengine.RegeneratorIface
               :methods [ [^{:static true} regen [Block] com.github.izbay.regengine.RegenSectionIface]
                          [^{:static true} regen [Vector] com.github.izbay.regengine.RegenSectionIface]
                          [^{:static true} regen [long long long] com.github.izbay.regengine.RegenSectionIface]
                          ;; getIsFlagged() overloaded:
                          [^{:static true} getIsFlagged [Vector] boolean]
                          [^{:static true} getIsFlagged [long long long] boolean]])

;;; Was this fucking thing actually working??:
#_(gen-class :name com.github.izbay.regengine.Regenerator
           :implements [com.github.izbay.regengine.RegeneratorIface]
           )

;; TODO: This should cause AIR not to have its state saved.  Unless the air blocks are going to have metadata, I don't see how this could be a problem.
(defonce ^:dynamic *ignore-air-blocks* true)

;; TODO: We'll try to keep blocks from dropping their item equivalents.
; TODO: Further customization?
(defonce ^:dynamic *cancel-block-drops* true)

;; Determines whether MONITOR-level events should be loaded:
(defonce ^:dynamic *enable-warning-monitor-events* true)

#_(defonce backed-up-blocks (atom {:blocks ()
                                 :block-data {}})); Vectors as keys
                                        ;(defonce global-regen-list (atom ()))

(defonce ^{:doc "Used by (queue-for-regen)."
           :dynamic true}
  *blocks-queued-for-regen* (atom #{}))

;; FIXME: Pick a good default time.
;; TODO: Eventually these should be declared with (defonce).
(def ^{:doc "Default number of ticks between backup & restoration." :dynamic true}
  *regen-total-delay* (seconds-to-ticks 20))

(def ^{:doc "Number of ticks remaining before regeneration when players first get warned."
       :dynamic true}
  *regen-warning-delay* (seconds-to-ticks 15))

(def ^{:doc "Number of ticks that elapse between warning attempts"
           :dynamic true}
  *regen-warning-period* (seconds-to-ticks 5))

(def ^{:doc "TODO: Pick a distance; explain."
       :dynamic true}
  *regen-vfx-distance* 20)

(defonce ^{:doc "Used by (verify-region) and comp."}
  latest-regen-region
  (atom {}))

(defonce ^{:doc "Used by (alter-region), (test-alter-region)..."}
  block-regen-order-reversed
  (atom ()))

(defonce ^{:doc "Like block-regen-order-reversed, but set only at the end of the (test-alter-region) operation."}
  block-regen-order
  (atom []))

(def regen-warning-effect
  "We'll try displaying this as a warning that a block is about to reappear:"
  org.bukkit.Effect/MOBSPAWNER_FLAMES)

(defn visual-warning-at [pos]
  "A warning to players that the block is about to be overwritten.  The 'nil' arg depends on the effect type."
  (effect (.toLocation (get-vector pos) (get-current-world)) regen-warning-effect nil)
  #_(.playEffect (get-current-world) (if (instance? Location pos) pos
                                       (.toLocation (get-vector pos) (get-current-world)))
               regen-warning-effect nil))

(defn- play-effect-at [pos]
  (effect (.toLocation (get-vector pos)) regen-warning-effect nil))

 ;(defonce global-regen-list ()) )
;; A set.
;(defonce backed-up-blocks (atom #{}))
;(defonce backed-up-blocks #{})

#_(defn tagged-for-regen? [^Vector v]
  "True if the block at vector v is already enrolled."
  (let [{:keys  [block-data]} @backed-up-blocks]
    (contains? block-data v)))

#_(defn- tag-for-regen [set & [^Block block]]
  "Returns new mapping; intended to be a callback to (swap! backed-up-blocks ...)."
  (let [current-time (get-game-time-in-ticks)
        time-to-regen (+ current-time *regen-total-delay*)
        {:keys [blocks block-data]} set]
    {:block-data (assoc block-data (get-vector block)
                        {:time-to-regen time-to-regen})
     :blocks (cons (.getState block) block-data)}))

#_(defn tag-for-regen! [^Block block]
  "Delegates to side-effect-free (tag-for-regen)."
  (swap! backed-up-blocks tag-for-regen block))

#_(defn backup-block [^Block block]
  (let [v (get-vector block)]
    (when-not (tagged-for-regen? v))
    (tag-for-regen! block)))

#_(defn block-break-handler [^BlockBreakEvent ev]
  "Suppresses block drop if *pcancel-block-drops* is set.  This requires cancelment of the event, which may or may not make other plugins unhappy.
TODO: Custom event covariant with BlockBreakEvent."
  (let [block (.getBlock ev)]
    (when (eligible-for-regen? block)
      (backup-block block)
      (when *cancel-block-drops*
        (.setCancelled ev true)
        *cancel-block-drops*))))


(defn block-break-monitor [^BlockBreakEvent ev]
  "If *enable-warning-monitor-events* is set, this will be added as a MONITOR-priority event listener to check whether the break-event remained cancelled, like doing an (assert)."
  (let [block (.getBlock ev)]
    (when (and *cancel-block-drops*
               (not (.isCancelled ev))
               *enable-warning-monitor-events*)
      (cljminecraft.logging/warn "** Block drop has been forcibly reenabled by a rogue plugin."))))

#_(defn do-commence-warning [pos]
  ;; Load an intervalic handler to make block blink
  ;; Check whether a player's already in danger; warn if so
  ;; Load a PlayerMoveEvent listener to warn if a player wanders into danger
  )

;; FIXME: Terrible name, for one thing:
#_(defn do-time-to-regen []
  (let [{:keys [blocks block-data]} @backed-up-blocks
        [block & rest] blocks
        block-vec (get-vector block)
        {:keys [time-to-regen]} (get block-data block-vec)]
    (assert* (contains? block-data block-vec))
    (assert* time-to-regen)
    (when <current time has reached time-to-regen>
          <regen>
          <do swap! operation))
  ;; Fire custom event
  ;; Remove block-blink handler
  ;; Remove, or deactivate, PlayerMoveEvent listener
  )


;; TODO: Registering all handlers:
#_(do
 ;; FIXME: (register-event @clj-plugin "block.block-break" #'block-break-handler Priority/LOW)


 ;; Register watcher:
#_(when *enable-warning-monitor-events*
   (register-event @clj-plugin "block.block-break" #'block-break-monitor
                   Priority/MONITOR)))



#_(defn do-regeneration-test [block]
  "Test func."
  (let [time-before (int (/ (- *regen-total-delay* *regen-warning-delay*)
                                           *regen-warning-period*))
        time-after (int (/ *regen-warning-delay*
                                           *regen-warning-period*))]
    (assert* (> time-before 0))
    (assert* (> time-after 0))
    (recursive-warning-regen (get-location block)
                             *regen-warning-period*
                             time-before
                             time-after
                             #(send-msg (get-first-player) "Regeneration.")))
  #_(schedule-regeneration block #(send-msg (get-first-player) "Regeneration.")))



#_(defn schedule-regeneration [block callback]
  "Test func."
  (assert* (> *regen-total-delay* *regen-warning-delay*))
  (let [^Location loc (get-location block)
        ^BukkitTask warning-task
        (repeated-task @clj-plugin
                       (fn []
                         (debug-println "Warning VFX playing.")
                         (send-msg (get-first-player) "Warning.")
                         (effect loc regen-warning-effect nil))
                       (- *regen-total-delay* *regen-warning-delay*)
                       *regen-warning-period*)
        warning-task-id (.getTaskId warning-task)
        ^BukkitTask regen-task (delayed-task @clj-plugin
                                             (fn []
                                               (debug-println "Regen task firing.")
                                               (assert* (or (running-task? warning-task-id)
                                                            (queued-task? warning-task-id)))
                                               (cancel-task warning-task-id)
                                               (assert* (not (queued-task? warning-task-id)))
                                               (callback))
                                             *regen-warning-delay*)
        regen-task-id (.getTaskId regen-task)]
    (assert* (or (running-task? warning-task-id)
                (queued-task? warning-task-id)))
    (assert* (or (running-task? regen-task-id)
                (queued-task? regen-task-id)))
    [warning-task, regen-task]))






#_(defn backup-region [start-corner end-corner]
  "Moving onward from the earlier version in backup-restore.clj."
  (let [vs (gen-region-vectors start-corner end-corner)
        vectors (if *ignore-air-blocks*
                  (remove #(air? (get-block-at %)) vs)
                   vs)]
    (def *backed-up-region* ())
    (doseq [v vectors]
      (let [target-block (get-block-at v)]
            (assert* (instance? Block target-block))
            (debug-println "Backing up block at" (format-vector v))
            (let [block-state (.getState target-block)]
              (def *backed-up-region*
                (cons block-state *backed-up-region*)))))))


(defn taxicab-distance [ob1 ob2]
"FIXME: This is a stub!"
  5)

(defonce player-move-event-declared (atom false))
(defonce player-hash-set (atom #{}))
(defonce blocks-to-regen (atom {}))

(defn player-move-event-declared? []
  "Currently unused."
  @player-move-event-declared)

(defn player-flagged-in-regen-area? [^Player pc]
  (contains? @player-hash-set pc))

(defn player-in-regen-area? [^Player pc]
  "True if either of pc's bounding blocks is within the blocks-to-regen set."
  (some #(contains? @blocks-to-regen (get-block-vector %))
   (get-player-space pc))
  #_(some (fn [^BlockVector pos]
          (let [block (get @blocks-to-regen pos)]
            (player-block-collision? pc block :solid false)))
          (keys @blocks-to-regen)))



(defn player-move-event-handler [^PlayerMoveEvent ev]
  "Block-regen PlayerMoveEvent handler: when a block is in queue, checks whether a player is moving in or out of the affected area... blah, blah.
TODO: Instead of looping through blocks, check just the player's space."
  (try
                                        ;(debug-println "Move event noted.")
    (let [pc (.getPlayer ev)]
      (cond*
       ;; Coming in:
       [(and (not (player-flagged-in-regen-area? pc))
             (player-in-regen-area? pc))
        (swap! player-hash-set #(conj % pc))
        (assert (contains? @player-hash-set pc))
        (send-msg pc "You are now in a REG EN zone.")]
       ;; Going out:
       [(and (player-flagged-in-regen-area? pc)
             (not (player-in-regen-area? pc)))
        (swap! player-hash-set #(disj % pc))
        (assert* (not (contains? @player-hash-set pc)))
        ;; TODO: Fix case where player gets trapped inside... but is told he is 'safely outside'.  Happens when the player is considered to have "left" the REG EN zone because the zone disappears.
        (send-msg pc "Safely outside the REG EN zone.")]))
    (catch Error e
      (send-msg (get-first-player) "Exception thrown within player-move warner.")
  ;    (cleanup-fn)
      (throw e))))

(defn instate-player-move-event-handler []
  "Used by (regen)."
  ;; TODO: Don't just re-initialize every time:
  (binding [*debug-print* false]
    (unregister-our-events "player.player-move"))
  (let [cnt (count (HandlerList/getRegisteredListeners *plugin*))]
    (register-event *plugin* "player.player-move"
                    player-move-event-handler)
    (let [count' (count (HandlerList/getRegisteredListeners *plugin*))]
      (assert* (= count' (inc cnt))
               "Error with loading the R.E. player-move handler; should have %s registered instead of %s." (inc cnt) count')))
  (debug-println "REG EN's player-move-event-handler loaded.")
  true)

(defn block-queued-for-regen? [obj]
  "Predicate; returns true iff obj (or its location Vector) is in blocks-to-regen."
  (cond*
   [(instance? BlockVector obj) (contains? @blocks-to-regen obj)]
   [:else (block-queued-for-regen? (get-block-vector obj))]))



(defn block-queue-regen [block & {:keys [start-time target-time]}]
  "Adds a block to the storage pile.  Doesn't invoke the machinery around it--use (regen) for that, and let it call this.  Has no effect if the block is already there.
If the block is queued, returns <its new map>."
  (assert* start-time)
  (assert* target-time)
  (econd*
   [(instance? BlockState block)
    (let [vec (get-block-vector block)
          new-entry {:state block
                     :starting-ordinal (count @latest-regen-region)
                     :regen-start-time start-time
                     :regen-target-time target-time
                     :actual-regen-time (promise)
                     :finishing-ordinal (promise)}]
      (swap! latest-regen-region #(assoc % vec new-entry))
      (swap! blocks-to-regen #(assoc % vec block))
      (assert* (block-queued-for-regen? block))
      new-entry)]
   [(instance? Block block) (block-queue-regen (get-state block) :start-time start-time :target-time target-time)]))

(defn block-dequeue [obj]
  "Removes a block from the queue; do not call directly."
  (assert* (block-queued-for-regen? obj))
  (cond*
   [(instance? BlockVector obj) (swap! blocks-to-regen #(dissoc % obj))]
   [:else (block-dequeue (get-block-vector obj))])
  (assert* (not (block-queued-for-regen? obj))))

(defn block-restore* [^BlockState old-state]
  "Impl. for (block-restore).  Should remove the block from the regen queue.  Do not call directly."
  (let [^Material old-mat-type (get-type old-state)
        ^Block target-block (get-block-at (get-location old-state))
        ^BlockVector vec (get-block-vector target-block)
        starting-ordinal (get-in @latest-regen-region [vec :starting-ordinal])]
    (assert* (instance? Block target-block))
    (if (= old-state (get-state target-block))
      (do
        (debug-println "Block at" (format-vector vec) "already exactly matches stored state!")
        ;; Remove from storage:
        (block-dequeue vec))
      (do
        (debug-println "Restoring block number" starting-ordinal "at" (format-vector vec)
                       "from material" (.getType target-block)
                       "to material" old-mat-type)
        ;; Force state change:
        (.update old-state true)
        ;(debug-println "Here")
        (deliver (get-in @latest-regen-region [vec :actual-regen-time])
                 (get-full-time))
        (assert* (realized? (get-in @latest-regen-region [vec :actual-regen-time])))
        (deliver (get-in @latest-regen-region [vec :finishing-ordinal]) (count @block-regen-order-reversed))
        (swap! block-regen-order-reversed #(cons starting-ordinal %))))
        ;; Remove from storage:
        (block-dequeue vec)
        (assert* (cond*
                  [(= old-state (get-state target-block)) true]
                  [(= (get-type target-block) (get-type old-state))
                   (debug-println "States don't match, though materials do.")
               true])
                 "Failing assert; type of block is still " (get-type target-block)))
  (assert* (not (block-queued-for-regen? old-state)))
  true)


(defn block-restore [obj]
  "Moves a block out of the queue and back into the world.  Error if the block isn't in queue."
  (cond*
   [(instance? BlockVector obj)
    (assert* (contains? @blocks-to-regen obj)); TODO: Better error type
    (let [block-state (get @blocks-to-regen obj)]
      (assert* (instance? BlockState block-state))
      ;; Delegate:
      (block-restore* block-state))]
   [:else
#_(or (instance? Block obj)
        (instance? BlockState))
    ;; Recurse:
    (block-restore (get-block-vector obj))]))


;;;; Very weird.
(defn purge-regen-blocks []
  "Empties the regen queue.  Strange, but using the #() lambda-func syntax was causing a compiler failure here."
    (swap! blocks-to-regen (constantly {})))

(defonce ^{:doc "Map added to by (regen); keys are BlockStates, vals are absolute time coordinates, in ticks, of when (regen) determined the blocks should be restored.  I.e., these are the times targeted by the tasks created.  Note that (alter-region) resets this var while (regen) doesn't."}
  regen-ending-times
  (atom {}))

(defn regen [block & {:keys [delay load-move-handler]
                      :or {load-move-handler true}}]
  "Entry point for regenerating a single block.  If :load-move-handler is true (default), then (instate-player-move-event-handler) is called.
Ideally we shall only have to do this if it was deactivated, which we shall learn to detect."
  #_(when-not (player-move-event-declared?)
      (register-event *plugin* "player.player-move"
                    player-move-event-handler
                    ;; TODO: EventPriority/MONITOR. Would that be breaking the rules, because it has side effects if an error is caught?
                                        ;:monitor
                                                          ); FIXME: set flag
      )
  (if (block-queued-for-regen? block)
    (debug-println "Tried to queue a block that's already scheduled!")
    (let [total-wait (or delay *regen-total-delay*); default
          cur-time (get-full-time)
          ;; NB: Decrement!
          end-time (+ cur-time total-wait) ; Absolute num of ticks
          ]
      ;; Make time note:
      (swap! regen-ending-times #(assoc % (get-block-vector block) end-time))
      (assert* total-wait)
      (when load-move-handler (instate-player-move-event-handler))
      (block-queue-regen block :start-time cur-time :target-time end-time)
      ;(block-queue-regen block)

      ;; TODO: Reinstate partial warnings:
      (comment [cur-time (get-full-time)
           end-time (+ cur-time *regen-warning-delay*) ; Absolute num of ticks
           wait (- *regen-total-delay* *regen-warning-delay*)
           total-wait (- end-time cur-time)])

      ;;(debug-println (format "Will begin warning after %ss; will regen block after %ss." (ticks-to-seconds wait) (ticks-to-seconds total-wait)))
;      (debug-println (format "Will regenerate block %s after %ss." block (ticks-to-seconds total-wait)))
      (let [t1 (promise)
            t2 (promise)
            ;; This is more concise than using a separate (letfn):
            abort-func (fn []
                                        ;               (send-msg (get-first-player) "Exception thrown within (queue-for-regen).")
                         ;; Empty ENTIRE regen queue:
                         (purge-regen-blocks)
                         (debug-println "*** block-queue-regen's abort routine invoked.")
                           ;; TODO: debug-msg
                           (send-msg (get-player) "*** Error: block-queue-regen's abort routine invoked.")
                           (binding [*debug-print* false]; Printing here can lag the game too much.
                             (unregister-our-events "player.player-move"))
                           ;(send-msg (get-first-player) "Cleanup func run.")
                           (doseq [t [t1 t2]]
                             (when (realized? t)
                               (let [t' (deref t)]
                                 (assert* (instance? BukkitTask t'))
                                 (when (task-active? t')
                                   (cancel-task t')
                                   (assert* (not (task-active? t')))))))
                           ;(swap! *blocks-queued-for-regen* #(disj % block))
                           @blocks-to-regen)]
        (try
          (let ;; Schedules the regeneration action itself.
              [t1' (delayed-task *plugin*
                                      (fn []
                                        (try
                                          ;(debug-println "Stopping regen task; OK.")
                                          ;(debug-println (type t2'))
                                          (assert* (realized? t2))
                                          (let [t2' (deref t2)]
                                            (assert* (instance? BukkitTask t2'))
                                            (assert* (task-active? t2'))
                                            (let [t2-id (.getTaskId t2')]
                                             ;; Shut down warner:
                                              (cancel-task t2-id)
                                              (assert* (not (task-active? t2-id)))
                                              ;; TODO: Need to not remove ALL the plugin's listeners for the event.
                                        ;(unregister-our-events "player.player-move")
                                        ;(.unregister (PlayerMoveEvent/getHandlerList) *plugin*)
                                              (block-restore block) ))
                                          (catch Error e
                                            (send-msg (get-first-player) "Exception thrown within the regeneration invoker!")
                                            (abort-func)
                                            (throw e))))
                                      ;; t3's delay:
                                      total-wait)
               ;; Warning loop.
               t2' (repeated-task
                         (fn []
                           (try
                             ;(debug-println "Checking for needed warnings.")
                             ;; Warn every player in zone:
                             (doseq [pc (online-players)]
                               ;; TODO: Solid?
                               (when (player-block-collision? pc block ;:solid true
                                                              )
                                 ;(debug-println "Warning" pc)
                                 (send-msg pc (let [sec (ticks-to-seconds (- end-time (get-full-time)))]
                                                (format "A block at your position is going to reappear in %s second%s." sec (pluralizes? sec))))))
                             ;; Show VFX to players "near enough":
                             (when (some #(< (taxicab-distance % block)
                                             *regen-vfx-distance*)
                                         (online-players))
                               (visual-warning-at block))
                             (catch Error e
                               (send-msg (get-first-player) "Exception thrown within the on-tick warner.")
                               (abort-func)
                               (throw e))))
                         ;; Task t2's timing params:
                         0 ;; TODO: Reinstate two-phase warning; for now we use no wait. ; wait
                         *regen-warning-period*
                         :exception-cancel false)]
            ;; All-important delivery: now that the tasks are created, we can make them available to (cleanup-fn) and the (catch) block:
            (deliver t1 t1')
            (deliver t2 t2')
            ;; Retval
            true)
          (catch Error e
                 (send-msg (get-first-player) "Error within (queue-for-regen).")
                 (abort-func)
                 (throw e)))))))




(defonce ^{:doc "Set of BlocksStates used by (verify-altered-region); use for debugging.  BlockStates are added hereto when (alter-region), by calling (alter), has tried to erase the block--most likely by changing it to AIR--and it hasn't changed properly.  This test is done *after* all blocks have changed; if physics isn't cancelled, the lag time in between may allow other blocks to move.  Water can flow, for example.  Comparing the these results with those of a test performed immediately after the switch, within (alter), would likely be worth thought.
By default this accumulates between (alter-region) calls, but that can be changed with a keyword arg.
If 'nil', it means (alter-region) has run without (verify-altered-region).  If an empty set, there were no failures!  Congrats!"}
  failures-of-alter-region
  (atom nil))


(defonce ^{:doc "Helper memo used by (alter-region), reset on every use."}
  latest-alter-region (atom nil))

(defn
  alter
  [block & {:keys [new-mat load-move-handler log-failure delay]
              :or {new-mat Material/AIR
                   load-move-handler true
                   log-failure true}}]
  "REG EN gateway.  \"Destroy\" a block and add it to the REG EN body.
If already of type 'new-mat', NOTHING IS DONE, and nil is returned; else returns true.
See (regen) for meaning of :load-move-handler.
If :log-failure is log. true, modifies (failures-of-alter-region)."
  (let [^Block block (cond*
                      [(instance? Block block) block]
                      [:else (get-block-at (get-vector block))])]
    (when (not (= new-mat (get-type block)))
      (let [retval (regen block :load-move-handler load-move-handler :delay delay)]
        (assert* (block-queued-for-regen? block))
        (set-type block new-mat)
        #_(when (and log-errors (not (= (get-type block) new-mat))) (swap! failures-of-alter-region #(conj % (get-state block))))
        (assert* (= (get-type block) new-mat) "(alter) could not change %s from %s to %s." block (get-type block) new-mat)
        retval))))

(defn verify-altered-region [& {:keys [new-mat]
                                 :or {new-mat Material/AIR}}]
  "A map containing three entries is returned: the number of hits, the number of misses, and a vector of misses.  Only makes sense if (alter-region) has been called, setting latest-alter-region, and regeneration has not transpired.  If it has regenerated, the number of misses will be inflated.
"
;  (debug-println "(verify-altered-region).")
  (if-not @latest-alter-region
    (debug-println "Called (verify-altered-region), but no region stored!")
    (do
      (swap! failures-of-alter-region (constantly #{}))
      (let [fcount (count @failures-of-alter-region)]
        (when-not (zero? fcount)
          (debug-announce "Warning: (verify-altered-region) found %s failure%s-to-destroy."
                          (if (== 1 fcount) "a" fcount) (pluralizes? fcount))))
;      (println @failures-of-alter-region)

      #_(doseq [x @latest-region]
         (let [v (get-block-vector x)
               cur-block (get-block-at v)
               cur-type (get-type cur-block)]
           (when-not (= cur-type new-mat)
             (swap! failures-of-alter-region #(conj % x))
             (assert* (= cur-type new-mat) "(alter-region) postcondition: Block at %s of type %s is not destroyed (%s)." v cur-type new-mat))))
      (let [partitions (group-by #(= (get-type (get-block-at (get-vector %))) new-mat) @latest-alter-region)
            failures (get partitions false)]
        (swap! failures-of-alter-region (constantly failures))
        {:blocks-hit (count (get partitions true))
         :blocks-missed (count (get partitions false))
         :missed-blocks (get partitions false)}))))


(defn alter-region [start-pos end-pos & {:keys [new-mat reset-failures delay]
                                         :or {new-mat Material/AIR
                                              reset-failures true}}]
  "Entry point for REG EN'ing (and destroying) a block section.  For efficiency, only calls (instate-player-move-event-handler) once, not once per (alter).
If :reset-failures is logical true, the failures-of-alter-region set is purged first."
  (assert* (instance? Material new-mat))
  (assert* (empty? @blocks-to-regen) "Called (alter-region) while blocks-to-regen isn't empty.")
  (try
    (swap! latest-regen-region (constantly {}))
    (swap! block-regen-order-reversed (constantly ()))
    (swap! block-regen-order (constantly []))
    (debug-println "Cleared 'latest-regen-region' and 'block-regen-order-reversed' in (alter-region).")
    (with-local-vars [latest-region []]
      (let [vectors (gen-region-vectors start-pos end-pos)]
        ;; By default we let failures accrue, but we can reset them:
        (when reset-failures (swap! failures-of-alter-region (constantly nil)))
        ;; Reset times map, to be set again within (regen):
        (def regen-ending-times (atom {}))
        (doseq [v vectors]
          (let [^BlockState cur-state (get-state (get-block-at v))
                ^Material cur-type (get-type cur-state)]
           (assert* (instance? Material cur-type))
           ;; Skip this block if the material already matches.
           (when-not (= cur-type new-mat)
             (alter v :new-mat new-mat :load-move-handler false :delay delay)
             (var-set latest-region (cons cur-state @latest-region)))
           ;; Note: this duplicates a check within (alter):
           (assert* (= new-mat (get-type (get-block-at (get-vector cur-state)))))))

       (swap! latest-alter-region (constantly @latest-region))
       ;; Test that last assignment:
       (assert* (= @latest-region @latest-alter-region))
                                        ;(assert-seq x @latest-region (= (get-type x) new-mat))
       ;; Check that no blocks of 'new-mat' accidentally were added:
       (assert* (not-any? #(= (get-type %) new-mat) (vals @blocks-to-regen)))
                                        ;      (debug-println "Here.")
       ;; Check whether the blocks all got changed properly!  This is most likely to fail.
       (verify-altered-region :new-mat new-mat)
       ;(assert* (zero? (:blocks-missed (verify-altered-region :new-mat new-mat))))

       ;; Move handler.  TODO: We should put this at the end and have some kind of check.
       (instate-player-move-event-handler)
       ;; Normal retval:
       true))
    ;; If all that happened is an assertion failed, we still want this to run.  For any other Error we cancel:
    (catch AssertionError a
      (debug-println "Caught assertion failure in (alter-region).")
      (instate-player-move-event-handler)
      (throw a))))


(defn verify-region []
  "Returns a triple-e map, in the fashion of (verify-altered-region)."
  (if (empty? @latest-regen-region)
    (debug-println "Called (verify-region), but no region stored!")
    (do
      ;(when reset-failures (swap! failures-of-alter-region ))
      (comment (doseq [x @latest-region]
                 (let [v (get-block-vector x)
                       cur-block (get-block-at v)
                       cur-type (get-type cur-block)]
                   (when-not (= cur-type new-mat)
                     (swap! failures-of-alter-region #(conj % x))
                     (assert* (= cur-type new-mat) "(alter-region) postcondition: Block at %s of type %s is not destroyed (%s)." v cur-type new-mat)))))
      (let [partitions (group-by block-state-verisimilitude? @latest-alter-region)
            failures (get partitions false)]
        {:blocks-regenerated (count (get partitions true))
         :blocks-failed (count (get partitions false))
         :failure-blocks failures}))))
