(ns cljengine.siege
  (:use (clojure [core :exclude alter]
                 repl pprint reflect)
        (cljengine mc tasks events regen)
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



;;;; id=SIEGEN
(defn- minecart-collision-handler [^VehicleBlockCollisionEvent ev]
  "For SIEGEN testing."
  (when (= (.getType (.getVehicle ev))
           EntityType/MINECART)
    (send-msg (get-player) "Vehicle block collision.")
    (def *block-rammed* (.getBlock ev))
    (def *ram* (.getVehicle ev))))

(defonce ^:dynamic *minecart-handler-signal-only-transitions* true)


(defn- minecart-move-handler [^VehicleMoveEvent ev]
  "For SIEGEN testing."
  (when (= (.getType (.getVehicle ev))
           EntityType/MINECART)
    (let [v1 (get-block-vector (.getFrom ev))
          v2 (get-block-vector (.getTo ev))]

      ;; I need to know if either of these can ever hold:
;      (assert* (not (block-pos-eq? v1 v2)))
      (assert* (or (== (.getX v1) (.getX v2))
                   (== (.getZ v1) (.getZ v2))))
      ;(not *minecart-handler-signal-only-transitions*)

      (if (block-pos-eq? v1 v2)
        (debug-println (format "Movement between equal vectors %s and %s." (format-vector v1) (format-vector v2)))
        (do
          (send-msg (get-player) (format "Minecart moved from %s to %s." (format-vector v1) (format-vector v2)))
          (cond*
           [(and (== (.getY v1) (.getY v2))
                 (-> v1 get-block-below solid?)
                 (not (-> v2 get-block-below solid?))
                 (-> v2 get-block-below get-block-below solid?))
            (send-msg (get-player) "Laying downhill tracks from %s to %s." (format-vector v1)
                      (format-vector (get-vector (get-block-below v2))))
            (alter (get-block-at v1) :new-mat Material/RAILS :delay 200)
            (alter (get-block-below v2) :new-mat Material/RAILS :delay 200)
            ;(.setType (get-block-at v1) Material/RAILS)
            ;(.setType (get-block-below v2) Material/RAILS)
            ]
           )))
      #_(when (== (.getY v2) (dec (.getY v1)))
          (.setType (get-block-at v1) Material/RAILS)
          (.setType (get-block-at v2) Material/RAILS))
      #_(cond*
         [(and (-> v1 get-block-below solid?)
               (not (-> v2 get-block-below solid?))
               (-> v2 get-block-below get-block-below solid?))
          (send-msg (get-player) "Laying downhill top track at %s." (format-vector v1))
          (.setType (get-block-at v1) Material/RAILS)]
         [(and (== (.getY v2) (dec (.getY v1)))
               (-> v2 get-block-below solid?))
          (send-msg (get-player) "Laying downhill bottom track at %." (format-vector v2))
          (.setType (get-block-at v2) Material/RAILS)])
      #_(when ;(solid? (get-block-at (add v2 0 -1 0)))
            (send-msg (get-player) "Laying downhill tracks.")
          (doseq [% [;v1
                     v2]]
            (.setType (get-block-at %) Material/RAILS)
            (assert (= (get-type (get-block-at %)) Material/RAILS)))))))

(defn- load-minecart-move-handler []
  (unregister-our-events "vehicle.vehicle-move")
  (register-event *plugin* "vehicle.vehicle-move" minecart-move-handler))
