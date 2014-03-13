; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode) -*-
;(ns mc)
(ns cljengine.backup-restore
  (:use (clojure core repl pprint reflect)
        cljengine.mc)
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

(defonce *backed-up-region* ())
(defonce *backed-up-block* nil)
;; TODO: This should cause AIR not to have its state saved.  Unless the air blocks are going to have metadata, I don't see how this could be a problem.
(defonce ^:dynamic *ignore-air-blocks* true)

;; Old version, using Material i/s/o a BlockState:
#_(defn store-block [^Block block]
  "Makes a map object storing position & material of block."
  {:material (.getType block)
   :position (.. block getLocation toVector)})

#_(defn ^BlockState backup-block [^Block block]
  "Currently stores block in global var *backed-up-block*.  Then returns a block image, which currently is a BlockState.
Checks the global *ignore-air-blocks*; unless that is set, AIR isn't backed up.
Note: Currently *not* used by (backup-region)."
  (let [material (.getType block)]
    (when (or (not (= material Material/AIR))
              *ignore-air-blocks*)
      (let [block-state (.getState block)]
        (debug-println "Backing up" material "block at" (format-position (.getLocation block)))
        (def *backed-up-block* block-state)
        (assert (= *backed-up-block* block-state))
        (assert (= *backed-up-block* (.getState block)))
        block-state))))

(defn restore-block* [^BlockState old-state]
  "Impl. for (restore-block), though it can be used on its own."
  (let [old-mat-type (.getType old-state)
        target-block (get-block-at (.getLocation old-state))
        vec (get-vector target-block)]
    (assert (instance? Block target-block))
    (if (= old-state (.getState target-block))
      (do (debug-println "Block at" (format-vector vec) "already exactly matches stored state!"))
      (do
        (debug-println "Restoring block at" (format-vector vec)
                       "from material" (.getType target-block)
                       "to material" old-mat-type)
        (.update old-state true)))
    (assert (cond
             (= old-state (.getState target-block)) true
             (= (.getType target-block) (.getType old-state))
             (do (debug-println "States don't match, though materials do.")
                    true)
             :else (do (debug-println "Failing assert; type of block is still " (.getType target-block))
                       false)))
                                        ;(assert (= (.getType target-block) old-mat-type))
    )
  true)

(defn restore-block
  ([block] (restore-block* block))
  ([]
     "The block stored in *backed-up-block* is regenerated."
     (if (nil? *backed-up-block*)
       (do
         (when *debug-print* (println "No block backed up."))
         false)
       (restore-block* *backed-up-block*))))


(defn backup-target-block [^Player entity]; TODO ^Entity
  "Calls (backup-block) on (get-target-block)."
  (backup-block (get-target-block entity)))




;; Temporarily substituting the v. in regen.clj:
#_(defn backup-region [^Vector start-corner
                     ^Vector end-corner]
  "Now traverses (gen-region-vectors); less efficient than before but easier to read."
  (let [vectors (gen-region-vectors start-corner end-corner)]
    (def *backed-up-region* ())
    (doseq [v vectors]
      (let [target-block (get-block-at v)]
            (assert (instance? Block target-block))
            (debug-println "Backing up block at" (format-vector v))
            (let [block-state (.getState target-block)]
              (def *backed-up-region*
                (cons block-state *backed-up-region*))))))
  ;; nreverse:
  (def *backed-up-region* (reverse *backed-up-region*))
  true)


(defn restore-region [& [force]] ;
  "Uses BlockStates.  Experimental, using (restore-block*).
Unless 'force' is set, a block which already matches won't be regen'd."
  (if (and (empty? *backed-up-region*)
           (not force))
    (do
      (debug-println "No block region backed up.")
      false)
    (do
      (doseq [block-state *backed-up-region*]
        (assert (instance? BlockState block-state))
        (restore-block* block-state))
      true)))

;; May or may not be needed, I don't know.
#_(defn same-block? [b1 b2]
  )



;; Old version, using Material i/s/o a BlockState:
#_(defn backup-block [^Block block]
  "The 'block' is stored, by position and material, in the global *backed-up-block*."
  (def *backed-up-block* (store-block block))
  (assert (= (:material *backed-up-block*) (.getType block)))
  (when *print-debug*
    (println "Backed up block of type" (:material *backed-up-block*)
             "at position" (format-vector (:position *backed-up-block*)) "."))
  *backed-up-block*)

;; Old version, using Material i/s/o a BlockState:
#_(defn restore-block []
  "The block stored in *backed-up-block* is regenerated."
  (if (nil? *backed-up-block*)
    (do
      (when *print-debug* (println "No block backed up."))
      false)
    (do
      (let [{:keys [position material]} *backed-up-block*
            loc (.toLocation position (get-current-world))
            block (get-block-at loc)]
        (.setType block material)
        (assert (= (.getType block) material))
        (debug-println "Restoring block of type" material
                 "at location" (format-vector position)))
      true)))


(defn check-backed-up-region []
  "Prints about how the states stored in *backed-up-region* compare to
the blocks they reference.
Returns a duple: number of those that matched and those that didn't."
  (if (empty? *backed-up-region*) (println "No region in backup.")
      (with-local-vars [matching 0
                        non-matching 0]
        (doseq [state *backed-up-region*]
          (let [pos (get-vector state)
                block (get-block-at pos)]
            (when (or (not *ignore-air-blocks*)
                      (not= (.getType state) Material/AIR))
              (cond
               (= state (.getState block))
               (do
                 (println "Block at" pos "still matches:" (.getType state))
                 (var-set matching (inc (var-get matching)))),
               (= (.getType state) (.getType block))
               (do
                 (println "Block at" (format-vector pos) "is the right mat," (.getType block))
                 (var-set matching (inc (var-get matching)))),
                                        ;(println (format "Block at %s is the right material (%s), but not an exact match." (format-vector vec) (.getType state)))
               :else (do
                       (println "No match at" (format-vector pos)
                                (format "; %s != %s." (.getType block)
                                        (.getType state)))
                       (var-set non-matching (inc (var-get non-matching)))))
               #_(println (format "No match at %s: state has %s, block has %s."
                                                    (.getType state)
                                                    (.getType block))))))
        [@matching @non-matching])))
