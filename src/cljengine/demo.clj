; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode); eval: (cider '127.0.0.1 4005) -*-

(ns cljengine.demo
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
              [events :exclude [map]]
              [regen :exclude [map]]))
  ;; Add some Enums...
  (:import (javax.xml.parsers D   ocumentBuilderFactory
                              DocumentBuilder)
           (javax.xml.transform Transformer
                                TransformerFactory)
           javax.xml.transform.dom.DOMSource ;
           javax.xml.transform.stream.StreamResult
           (org.w3c.dom Attr             ;
                        Document;
                        NodeList;
                        Node;
                        Element;
                        Text))
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
                      BlockVector
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
           (com.github.izbay.regengine SerializedItem
                                       SerializedBlock
                                       SerializedInventory)
           (com.github.izbay.siegeengine SiegeEnginePlugin
                                         Ram
                                         Weapon
                                         WeaponEventObserver)
           (com.github.izbay.util Util)) )


(gen-region-vectors [-212.0 63.0 263.0] [-214.0 69.0 280.0])
