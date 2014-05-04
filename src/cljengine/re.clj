; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode) -*-

(ns cljengine.re
;  (:refer-clojure :exclude [map])
                                        ;  (:require clojure.core)
  (:use (clojure [core :exclude [map]] repl pprint reflect set)) ;[string :exclude [join]] )
  (:require [cljengine.mc :as mc])
  (:refer cljengine.mc)
  (:import (com.github.izbay.regengine RegEnginePlugin
                                       BlockImage
                                       RegenBatch)
           (com.github.izbay.regengine.block Action
                                             DependingBlock
                                             VineDependingBlock
                                             DependingBlockSet)
           (com.github.izbay.util Util)))

;(derive BlockImage ::block-state-image)


#_(extend DependingBlockSet
  BukkitLocatable
  {:get-location (memfn getLocation)})

(derive DependingBlockSet ::bukkit-locatable)

(defmethod get-data BlockImage [block]
    (the* MaterialData (.. block getBlockState getData)))











#_(ns cljengine.re
  (:use (clojure core repl reflect pprint) )

  ;; TODO: These aren't transferring to the gen-class calls.
  #_(:import (org.bukkit Location
                       Material)
           (org.bukkit.plugin Plugin)
           (com.github.izbay.regengine RegEnginePlugin
                                       RegEngine))
  #_(:gen-class
   :main false
   :prefix "regen-"
   :methods [[testMethod [] String]])
  #_(:gen-class
   :constructors {[org.bukkit.plugin.Plugin] []}
   :init init
   :main false
   :state state
   :prefix "regen-"
   :methods [[alter [org.bukkit.Location] String]
                     [alter [org.bukkit.Location org.bukkit.Material] String]
                     [testMethod [] String]]))


(gen-class :name cljengine.re.ClojureRegen
           ;:implements [com.github.izbay.regengine.RegEngine]
           :constructors {[org.bukkit.plugin.Plugin] []}
           :init init
           :main false
           :state state
           :prefix "regen-"
           :methods [;[alter [org.bukkit.Location] String]
;                     [alter [org.bukkit.Location org.bukkit.Material] String]
                     [testMethod [] String]])

(defn regen-init [parent]
  [[] (ref {:parent parent})])

#_(defn regen-alter [this ^Location l & [^Material m]]
    ;; FIXME
    "alter stub.")

(defn regen-testMethod [this]
  "This is a test.")

(defn self-compile []
                                        ; (var-set *compile-path* "D:/Documents/minecraft/craftbukkit.jar")
  (binding [*compile-path* "D:/Documents/minecraft/classes" ]
    (compile 'cljengine.re)) )
