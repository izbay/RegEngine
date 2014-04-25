; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode) -*-
;;;; Utilities for running experiments on the Minecraft event listeners.  Requires mc.clj and, of course, clj-minecraft.

(ns cljengine.events
  (:refer-clojure :exclude [map])
                                        ;  (:require clojure.core)
  (:use (clojure [core :exclude [map]] repl pprint reflect set))
  (:use (cljengine mc tasks )
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

;(load-file "mc.clj"); Still doesn't work

(defonce event-log ())
(defonce last-event nil)
(defonce ^:dynamic *physics-event-log* ())
(defonce ^:dynamic *log-physics-events* nil)

(defonce physics-events-log (atom []))

;; Used by (with-suppressed-physics).
(defonce ^:dynamic *cancel-event* false)

;;; As a batch:
(def block-events (disj (apply sorted-set (find-event "block."))
                        "block.block"
                        "block.block-piston"))
(def event-list
  "List of all event classes minus the abstract base ones."
  (disj (set (find-subclasses "org.bukkit" org.bukkit.event.Event))
        org.bukkit.event.block.BlockEvent
        org.bukkit.event.block.BlockPistonEvent
        org.bukkit.event.entity.EntityEvent
        org.bukkit.event.hanging.HangingEvent
        org.bukkit.event.painting.PaintingEvent
        org.bukkit.event.player.PlayerEvent
        org.bukkit.event.player.PlayerBucketEvent
        org.bukkit.event.server.PluginEvent
        org.bukkit.event.server.ServerEvent
        org.bukkit.event.server.ServiceEvent
        org.bukkit.event.vehicle.VehicleEvent
        org.bukkit.event.vehicle.VehicleCollisionEvent
        org.bukkit.event.weather.WeatherEvent
        org.bukkit.event.world.ChunkEvent
        org.bukkit.event.world.WorldEvent))

(def cljminecraft-event-list
  "String form acceptable by (register-event) &c."
  [;"block.block"
   "block.block-break"
   "block.block-burn"
   "block.block-can-build"
   "block.block-damage"
  "block.block-dispense"
  "block.block-exp"
  "block.block-fade"
  "block.block-form"
  "block.block-from-to"
  "block.block-grow"
  "block.block-ignite"
  "block.block-physics"
                                        ;"block.block-piston"
  "block.block-piston-extend"
  "block.block-piston-retract"
  "block.block-place"
  "block.block-redstone"
  "block.block-spread"
  "block.entity-block-form"
  "block.leaves-decay"
  "block.note-play"
  "block.sign-change"
  "enchantment.enchant-item"
  "enchantment.prepare-item-enchant"
  "entity.creature-spawn"
  "entity.creeper-power"
                                        ;"entity.entity"
  "entity.entity-break-door"
  "entity.entity-change-block"
  "entity.entity-combust"
  "entity.entity-combust-by-block"
  "entity.entity-combust-by-entity"
  "entity.entity-create-portal"
  "entity.entity-damage"
  "entity.entity-damage-by-block"
  "entity.entity-damage-by-entity"
  "entity.entity-death"
  "entity.entity-explode"
  "entity.entity-interact"
  "entity.entity-portal"
  "entity.entity-portal-enter"
  "entity.entity-portal-exit"
  "entity.entity-regain-health"
  "entity.entity-shoot-bow"
  "entity.entity-tame"
  "entity.entity-target"
  "entity.entity-target-living-entity"
  "entity.entity-teleport"
  "entity.entity-unleash"
  "entity.exp-bottle"
  "entity.explosion-prime"
  "entity.food-level-change"
  "entity.horse-jump"
  "entity.item-despawn"
  "entity.item-spawn"
  "entity.pig-zap"
  "entity.player-death"
  "entity.player-leash-entity"
  "entity.potion-splash"
  "entity.projectile-hit"
  "entity.projectile-launch"
  "entity.sheep-dye-wool"
  "entity.sheep-regrow-wool"
  "entity.slime-split"
                                        ;  "hanging.hanging"
  "hanging.hanging-break"
  "hanging.hanging-break-by-entity"
  "hanging.hanging-place"
  "inventory.brew"
  "inventory.craft-item"
  "inventory.furnace-burn"
  "inventory.furnace-extract"
  "inventory.furnace-smelt"
  "inventory.inventory"
  "inventory.inventory-click"
  "inventory.inventory-close"
  "inventory.inventory-creative"
  "inventory.inventory-drag"
  "inventory.inventory-interact"
  "inventory.inventory-move-item"
  "inventory.inventory-open"
  "inventory.inventory-pickup-item"
  "inventory.prepare-item-craft"
                                        ;"painting.painting"
  "painting.painting-break"
  "painting.painting-break-by-entity"
  "painting.painting-place"
  "player.async-player-chat"
  "player.async-player-pre-login"
                                        ;"player.player"
  "player.player-achievement-awarded"
  "player.player-animation"
  "player.player-bed-enter"
  "player.player-bed-leave"
                                        ;  "player.player-bucket"
  "player.player-bucket-empty"
  "player.player-bucket-fill"
  "player.player-changed-world"
  "player.player-channel"
  "player.player-chat"
  "player.player-chat-tab-complete"
  "player.player-command-preprocess"
  "player.player-drop-item"
  "player.player-edit-book"
  "player.player-egg-throw"
  "player.player-exp-change"
  "player.player-fish"
  "player.player-game-mode-change"
  "player.player-interact"
  "player.player-interact-entity"
  "player.player-inventory"
  "player.player-item-break"
  "player.player-item-consume"
  "player.player-item-held"
  "player.player-join"
  "player.player-kick"
  "player.player-level-change"
  "player.player-login"
  "player.player-move"
  "player.player-pickup-item"
  "player.player-portal"
  "player.player-pre-login"
  "player.player-quit"
  "player.player-register-channel"
  "player.player-respawn"
  "player.player-shear-entity"
  "player.player-statistic-increment"
  "player.player-teleport"
  "player.player-toggle-flight"
  "player.player-toggle-sneak"
  "player.player-toggle-sprint"
  "player.player-unleash-entity"
  "player.player-unregister-channel"
  "player.player-velocity"
  "server.map-initialize"
                                        ;"server.plugin"
  "server.plugin-disable"
  "server.plugin-enable"
  "server.remote-server-command"
                                        ;  "server.server"
  "server.server-command"
  "server.server-list-ping"
                                        ;  "server.service"
  "server.service-register"
  "server.service-unregister"
                                        ;  "vehicle.vehicle"
  "vehicle.vehicle-block-collision"
;  "vehicle.vehicle-collision"
  "vehicle.vehicle-create"
  "vehicle.vehicle-damage"
  "vehicle.vehicle-destroy"
  "vehicle.vehicle-enter"
  "vehicle.vehicle-entity-collision"
  "vehicle.vehicle-exit"
  "vehicle.vehicle-move"
  "vehicle.vehicle-update"
  "weather.lightning-strike"
  "weather.thunder-change"
;  "weather.weather"
  "weather.weather-change"
;  "world.chunk"
  "world.chunk-load"
  "world.chunk-populate"
  "world.chunk-unload"
  "world.portal-create"
  "world.spawn-change"
  "world.structure-grow"
;  "world.world"
  "world.world-init"
  "world.world-load"
  "world.world-save"
  "world.world-unload"])

;; Quick test to make sure we got 'em right:
(assert (== (count event-list) (count cljminecraft-event-list) 158))

(defn- clear-event-log []
  (def event-log (atom []))
  (def last-event nil)
  (swap! physics-events-log [])
;  (def physics-events-log ())
  (assert (= event-log ()))
  event-log)

(defn noisy-event-handler [ev]
  "Prints info about the occurring event, which can be of any type.  Also stores this last event in the 'last-event' global var AND prepends it to the 'event-log' .
The (register-all-events-feedback) func loads this into the listener for *every* event in the game.
TODO: Perhaps send a msg to the first player?  We can't use the clj-minecraft {:msg} automessaging; not all events have an associated player."
  (def last-event ev)
  (let [i (count @event-log)]
;    (def event-log (cons ev event-log))
    (swap! event-log conj ev)
    (assert (= (count @event-log) (inc i))))
  ;; NB: This prints on the server:
  (debug-announce (format "Event '%s' occured @ tick %s." (.getEventName ev) (get-full-time))))




#_(defn block-physics-handler [^BlockEvent ev]
  "Feedback handler.
Now also cancels events if *cancel-event* is true."
  (assert (instance? BlockPhysicsEvent ev))
  (let [block (.getBlock ev)
        name (.getEventName ev)
        type (.getChangedType ev)]
    (when
        (debug-println  name "; material" type "on block" block))
    (when (= *log-physics-events* :record)
      (swap! physics-events-log conj ev)) ); This is SO much more elegant with the proper syntax.
  (when *cancel-event*
    (if (.isCancelled ev) (debug-println "Already cancelled.")
        (do (.setCancelled ev true)
            (debug-announce "Cancelled %s" name))))
  true)

(defonce ^{:dynamic true
           :doc "A general override: when true, physics events will get suppressed even if 'regengine-event-handlers' is flagged active."}
  *suppress-physics* nil)
;(defonce physics-blocking-listener (atom nil))

;; TODO: I don't like this kind of forward dependency:
;(declare cljengine.regen/regengine-event-handlers)

#_(defn physics-blocking-handler [^BlockEvent ev]
  "Installed by (ensure-regengine-handlers) in regen.clj.
If *log-physics-events* is logical true, will print a message with each event.
If *log-physics-events* is set to :record, this will also be stored into the *physics-event-log* global. "
  (assert (instance? BlockPhysicsEvent ev))
  (let [block (.getBlock ev)
        name (.getEventName ev)
        type (.getChangedType ev)]
    (debug-println "Logging?" *log-physics-events*)
    (when *log-physics-events*
      (debug-announce  name "; material" type "on block" block)
      (when (= *log-physics-events* :record)
        (swap! physics-events-log conj ev)))
    (when-not *suppress-physics*
      (when (get-in @cljengine.regen/regengine-event-handlers ["block.block-physics" :active])
        (if (.isCancelled ev) (debug-println "Already cancelled.")
            (do (.setCancelled ev true)
                (debug-announce "Cancelled %s" name))))))
  true)

#_(defn event-cancelling-handler [^Event ev]
  (when *cancel-event*
    (if (instance? Cancellable ev)
      (if (.isCancelled ev) (debug-println ev "already cancelled.")
          (do (.setCancelled ev true)
              (debug-println "Cancelling " (.getEventName ev))
              true))
      (do
        (debug-println ev "not cancellable.")
        false))))

(defn register-handler [^String event-name
                        callback]
  "Wrapper currying the plugin iname."
  (cljminecraft.events/register-event *plugin* event-name callback)
  true)


;(defn get-registered-handlers []
(defn get-registered-listeners []
  "Returns list of all handlers declared by *plugin*."
  (HandlerList/getRegisteredListeners *plugin*))

(defn ^{:doc "Unregisters events associated with *plugin*.  Given no args, it unregisters all; given one, it should be a cljminecraft string as returned by (find-event)."}
  unregister-our-events
  ([^String eventname]
     (assert* *plugin*)
     (let [class (resolve (symbol (package-classname "org.bukkit.event" (str eventname "-event"))))]
       (eval `(.unregister (. ~class (getHandlerList)) *plugin*)); workaround for not being able to use a dynamically-resolved Class name to invoke a static method.  Note: There may be a Java reflection alternative.
       (debug-println (format "Unregistered %s for our plugin." eventname))
       true))
  ([]
     (assert* *plugin*)
     (org.bukkit.event.HandlerList/unregisterAll *plugin*)
     (debug-println "Removing all event handlers for our plugin.")
     (assert* (empty? (HandlerList/getRegisteredListeners *plugin*)))
     true))


#_(defn- register-physics-handler []
  "Currently unregisters all.  TODO: Finer control."
  (unregister-our-events)
  (def *physics-event-log* ())
  (register-event @clj-plugin "block.block-physics" #'block-physics-handler)
  (println "Registered BlockPhysicsEvent callback.")
  true)

(defmacro with-suppressed-physics [& body]
  `(do
    (unregister-our-events "block.block-physics")
    (register-handler "block.block-physics")
    (binding [*cancel-event* true]
      ~@body)))

(defn register-all-events-feedback []
  "For test purposes; registers with every known event a handler that announces every event that takes place (when *debug-print* is true)."
  (unregister-our-events)
  (doseq [evname cljminecraft-event-list]
    (println "Registering handler for" evname)
    (register-event *plugin* evname #'noisy-event-handler :monitor))
  true)

;(map #(register-event @clj-plugin % #'test-handler) block-events)
(defn- register-all-block-events []
  "Clears all plugin events & reloads those for block events; messy util func."
  (unregister-our-events)
  (doseq [evname block-events]
    (println "Registering handler for" evname)
    (register-event @clj-plugin evname #'noisy-event-handler))
  true)

;(org.bukkit.event.HandlerList/unregisterAll @clj-plugin)

(defn sort-events-by-pos [ev-coll]
  "Returns a collection sorted using (compare-vectors)."
  (sort (fn [s1 s2]
          (apply compare-vectors
                 (map (comp get-vector (memfn getBlock)) [s1 s2])))
        ev-coll))
