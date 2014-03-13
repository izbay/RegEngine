; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode) -*-

(ns cljengine.re
  (:use (clojure core repl reflect pprint)
    ;    cljengine.mc
        )
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
