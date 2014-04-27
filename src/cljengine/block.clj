; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode) -*-
;;;; See http://minecraft-ids.grahamedgecombe.com/ for all the blocks' ID numbers & pictures!

;; TODO: Continue with changing '*...*' to '+...+' notation.

(comment " Try running the following in Emacs Lisp to get the REPL going: "
         (progn
          (if (not (cider-connected-p)) (cider "127.0.0.1" 4005)
              (cider-interactive-eval-to-repl "(in-ns 'cljengine.block)")
              (cider-switch-to-current-repl-buffer)
              (paredit-mode))))



(ns cljengine.block
  (:use (clojure [core :exclude [map]] repl pprint reflect set))
  (:use clojure.java.io)
  (:require ;[cljengine.regen :as reg]
            [cljengine.mc :as mc]
            [cljengine.tasks :as tasks])
  (:use
   (cljminecraft core
                          ;entity
                          [bukkit :exclude [repeated-task ; (repeated-task) has a simple bug.  He must never have tried it.
                                            cancel-task]]
                          ;events
                          ;commands
                          ;logging
                                        ;util
                                        ;[world :exclude [effect]] ; (effect) has a simple bug.
                          ;; can't pull in all of cljminecraft.player without conflict:
                                        ;[player :only [send-msg]]
                          )
   (cljengine [mc :exclude [map]]
              [tasks :exclude [map]]
              [events :exclude [map]]
              ))
  (:import (clojure.lang Keyword
                         Symbol)
           (org.reflections Reflections)
           (org.bukkit Bukkit
                       Material
                       Location
                       World
                       Effect)
           (org.bukkit.block Block
                             BlockFace ; Enum
                             BlockState)
           (org.bukkit.material MaterialData)
           (org.bukkit.entity Entity
                              EntityType
                              Player)
           (org.bukkit.metadata Metadatable)
           (org.bukkit.event Event
                             Cancellable
                             EventPriority ; Enums
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
           (org.bukkit.plugin.java JavaPlugin) ; subtype of Plugin
           (org.bukkit.scheduler BukkitScheduler
                                 BukkitTask)
           (cljminecraft BasePlugin
                         ClojurePlugin)
           (org.bukkit.util BlockIterator))
  (:import (com.github.izbay.regengine RegEnginePlugin
                                       BlockImage
                                       RegenBatch)
           (com.github.izbay.regengine.block Action
                                             DependingBlock
                                             VineDependingBlock
                                             VineDependingBlock$Orientation
                                             DependingBlockSet)
           (com.github.izbay.util Util)))

(def ABOVE VineDependingBlock$Orientation/ABOVE)
(def BELOW VineDependingBlock$Orientation/BESIDE)

(def DESTROY Action/DESTROY)
(def RESTORE Action/RESTORE)

(do
  (defmulti depending-block (fn [o & _] (class o)))
  (defmethod depending-block Vector [v & {:keys [action] :or {action Action/DESTROY}}]
    (DependingBlock/from (get-block-at v) action))
  (defmethod depending-block :cljengine.mc/block-state-image [block & {:keys [action] :or {action Action/DESTROY}}]
    (DependingBlock/from (get-block-at block) action))
  (defmethod depending-block BlockImage [block & {:keys [action] :or {action Action/DESTROY}}]
       (DependingBlock/from block action)))

(do
  (defmulti depending-block-set (fn [o & _] (class o)))
  #_(defmethod depending-block-set Iterable [blocks & {:keys [action] :or {action Action/DESTROY} :as rest} ]
    (DependingBlockSet. (map #(apply depending-block % rest) blocks)))
  (defmethod depending-block-set Iterable [blocks & {:keys [action] :or {action Action/DESTROY} :as rest}]
;; TODO: Why can't I use a 'rest' deployment?
    (let [dep-blocks (the Iterable (map #(depending-block % :action action) blocks))]
      (DependingBlockSet. dep-blocks)))
  (defmethod depending-block-set :default [block & {:keys [action] :or {action Action/DESTROY} :as rest} ]
    (debug-println-2 "Default method in (depending-block-set).")
    (DependingBlockSet. (the DependingBlock (apply depending-block (the Block (get-block-at block)) rest)))))

(do
  (defmulti all-rev-deps class)
  (defmethod all-rev-deps DependingBlock [b]
    (.allRevDependencies b))
  (defmethod all-rev-deps Block [b]
    (.allRevDependencies (depending-block b))))


(do
  (defmulti regen-batch-store class)
  (defmethod regen-batch-store :cljengine.mc/block-state-image [obj]
    (regen-batch-store [(get-block-vector obj)]))
  (defmethod regen-batch-store DependingBlock [obj]
    (regen-batch-store [(get-block-vector obj)]))
  (defmethod regen-batch-store Iterable [coll]
    (RegenBatch/storing *plugin* (get-current-world) coll)))

(declare deps)



(def vine-fwd-dependency (memfn vineFwdDependency))
(def gravity-bound-fwd-dependency (memfn gravityBoundFwdDependency))
(def portal-fwd-dependency (memfn portalFwdDependency))
(def portal-rev-dependency (memfn portalRevDependency))

(do
  (defmulti do-fwd-deps-search class)
  (defmethod do-fwd-deps-search DependingBlockSet [s]
    (.doFwdDepsSearch s))
  (defmethod do-fwd-deps-search DependingBlock [s]
    (.doFwdDepsSearch (depending-block-set s)))
  (defmethod do-fwd-deps-search Block [b]
    (.doFwdDepsSearch (depending-block-set b))))

(do
  (defmulti do-rev-deps-search class)
  (defmethod do-rev-deps-search DependingBlockSet [s]
    (.doRevDepsSearch s))
  (defmethod do-rev-deps-search Block [b]
    (.doRevDepsSearch (depending-block-set b))))

(do
  (defmulti do-full-deps-search class)
  (defmethod do-full-deps-search Block [b]
    (.doFullDependencySearch (depending-block-set b)))
  #_(defmethod do-full-deps-search ))

#_(defn proxy-do-full-deps-search [dbs]
  (proxy-rev-dependency (do-fwd-deps-search dbs))
  ;(do-fwd-deps-search (proxy-rev-dependency))
  )

(do
  (defmulti types class)
  (defmethod types DependingBlockSet [s]
    (map (memfn getType) (.. s blocks values))))

(do
  (defmulti acts class)
  (defmethod acts DependingBlock [b]
    [(.action b)])
  (defmethod acts DependingBlockSet [s]
    (map (memfn action) (.. s blocks values))))

(do
  (defmulti deps class)
  (defmethod deps DependingBlock [b]
    [(.getType b), (.action b)])
  (defmethod deps DependingBlockSet [s]
    (map (fn [b] [(.getType b), (.action b)]) (.. s blocks values))))

(do
  (defmulti fst class)
  (defmethod fst DependingBlockSet [s]
    (first (vals (.blocks s)))))

;; TODO: Should the metatag be ':set' i/s/o 'clojure.core/set'?
(defmacro def-blocktype-set [name & body]
  "Wrapper for 'def' that adds some metadata.  Metadata within the main declaration will override the addition, so it should be as safe as 'def'."
  `(do
     (def ~name ~@body)
     (alter-meta! #'~name (fn [current-meta# & rest#] (apply merge current-meta# rest#)) '{:semantic-type set})
     ~name))

(do
  (derive Symbol ::symbol)
  (derive Keyword ::symbol))

(comment
  Block_list ::= [Block_entry*];
  Block_entry ::= Symbol | [Symbol Depends Attribute*];
  Depends ::= Nil_dep | Symbol | [Symbol+];
  Nil_dep ::= nil | [];
  Attribute ::= Keyword Value;

  "In this case paired parens can be used interchangeably with paired brackets.")
;; Rule: Sets are flattened.  If a set is specified as a member of another set, the union is taken.

#_(defn keys* [seq]
  "Overloaded so that a list can be passed..."
  (mc/econd*
   [(map? seq)
    (map #(if (seq? %) (first %) %)
         seq)]
   ;; Else coerce seq to map.
   ))

;; http://minecraft.gamepedia.com/Sign
(def *sign* '[sign-post wall-sign])
(def *door* '[wooden-door iron-door])
#_(*redstone-torch* #{diode-block-on diode-block-off})
(def *fence* '#{fence iron-fence glass-pane nether-fence cobblestone-wall})


;; Based on http://jd.bukkit.org/rb/apidocs/org/bukkit/material/Attachable.html :
; This does not, however, address repeaters or comparators.
#_(def +attachable+ '(button cocoa ladder lever (comment "This is a tech block:" piston-extension-material) sign +torches+ trap-door))



(def ^{:doc "http://minecraft.gamepedia.com/Opacity#Types_of_transparent_blocks"}
  *opaque*
  '#{dirt
    grass-block
    double-slab
    double-wood-slab})

;; TODO: Perhaps I should use 'nil' for the nonexistent-dependency object.  Or have a reduction which maps []->nil.


;; TODO:
'(*pressure-plate-supporting* *solid*)

'(*pressure-plates* #{stone-plate wood-plate
                     ;; TODO: heavy, light
                     })

;;; Tech blocks: http://minecraft.gamepedia.com/Technical_blocks
;; Technical blocks that are not up for regeneration.
'(*non-regen-technical* #{piston-extension
                          piston-moving-piece
                          [glowing-redstone-ore [] :common-name "lit redstone ore"]
                                        ; TODO: lit redstone lamp?
                          [water stationary-water :common-name "flowing water"]
                          (lava stationary-lava :common-name "flowing lava")
                          [burning-furnace [] :common-name "lit furnace"]
                          monster-eggs})



;; TODO: We should partition all blocks into disjoint sets '*solid*' and '*nonsolid*'.
;; http://minecraft.gamepedia.com/Opacity#Types_of_transparent_blocks
;; TODO: (*nonsolid* #{air snow +torches+ sign fire nether-portal end-portal})
;;TODO:
#_(*transparent* ...)

;; TODO: Is it anything except 'air'?
;(def *carpet-supporting* )

;; There sure are a lot of these:
(def ^{:semantic-type 'set}
  *stairs* '#{wood-stairs
              cobblestone-stairs
              brick-stairs
              smooth-stairs
              nether-brick-stairs
              sandstone-stairs
              spruce-wood-stairs
              birch-wood-stairs
              jungle-wood-stairs
              quartz-stairs})

;; TODO: See the list at http://minecraft.gamepedia.com/Fire
#_(*burnable #{netherrack})

;; TODO: Anvils?  What else?
#_(def ^{:semantic-type 'set}
    +sand-supporting+ (union *opaque* +torches+ '#{snow vines})) ; Anything that can hold up sand
#_nondecaying-leaves ; Can we identify which leaves were player-placed?
;; TODO: Some overlap with *opaque*.
#_*solid*


(def-blocktype-set *chests* '#{chest trapped-chest ender-chest})

(def ^{:doc "At the block-level-dependency stage of regeneration: if the block underneath is AIR, consider changing it temporarily."}
  *melons* '#{pumpkin melon jack-o-lantern})

;; TODO: Rails are a hard item;
;; TODO: Leaves?
#_(def *rail-supporting* (union *solid* *melons* '#{redstone-block hopper}) ) ; Blocks that a rail or torch can be placed on

(def-blocktype-set +crops+ (union *melons* '#{carrot potato wheat}))

;; TODO:
(def-blocktype-set *torch-supporting*
  '#{*opaque*
                      ;*chests*
     glass; partial
                      ; Not ice.
                      ; Not leaves.
                      packed-ice
                      hopper ; partial
                      end-portal; surprising.
                      ;; TODO:  slab?
                      })

;; TODO: Cf. what supports redstone dust?  Should glowstone be on this list?  Stairs?.
#_(*redstone-supporting* #{*opaque* slab *stairs*})

#_(*piston-component* #{piston-sticky-base piston-base piston-extension piston-moving-piece}); Tech blocks serving as piston components.  These are _unstable_ and should not be regen'd.
(def-blocktype-set *mushroom* '#{brown-mushroom red-mushroom})

#_(def ^{:doc "TODO: "}
  *bed-supporting* *solid*)

;; TODO: Stairs?
#_(*partial-block* #{slab})

(def-blocktype-set
  +torches+ '#{(torch *torch-supporting*)
               (redstone-torch-on *torch-supporting*)
               (redstone-torch-off *torch-supporting* :common-name "unlit redstone torch" :alias unlit-redstone-torch)})

(def-blocktype-set *rail*  '#{rail powered-rail detector-rail activator-rail})

;;;; *Erased +old-block-types+.*


;;;; id=block-types
(def-blocktype-set +block-types+
  '((:cljengine.block/air [] :alias air :material Material/AIR :index 0)
    (:cljengine.block/stone [] :alias stone :material Material/STONE :index 1)
    (:cljengine.block/grass-block [] :common-name "Grass Block" :alias grass-block :alias grass :material Material/GRASS :index 2)
    (:cljengine.block/dirt [] :doc "NB: Includes podzol." :alias [dirt, regular-dirt, podzol] :material Material/DIRT :index 3)
    (:cljengine.block/cobblestone [] :alias cobblestone :material Material/COBBLESTONE :index 4)
    (:cljengine.block/wood nil :common-name "Wood planks" :alias wood :material Material/WOOD :index 5)
    (:cljengine.block/sapling [dirt, grass-block] :url "http://minecraft.gamepedia.com/Sapling" :alias sapling :material Material/SAPLING :index 6)
    (:cljengine.block/bedrock [] :alias bedrock :material Material/BEDROCK :index 7)
    (:cljengine.block/water stationary-water :common-name "flowing water" :alias water :material Material/WATER :index 8)
    (:cljengine.block/stationary-water [] :alias water-source :alias stationary-water :material Material/STATIONARY_WATER :index 9)
    (:cljengine.block/lava stationary-lava :common-name "flowing lava" :alias lava :material Material/LAVA :index 10)
    (:cljengine.block/stationary-lava [] :alias [lava-source, stationary-lava] :material Material/STATIONARY_LAVA :index 11)
    (:cljengine.block/sand +sand-supporting+ :alias sand :material Material/SAND :index 12)
    (:cljengine.block/gravel +sand-supporting+ :alias gravel :material Material/GRAVEL :index 13)
    (:cljengine.block/gold-ore [] :alias gold-ore :material Material/GOLD_ORE :index 14)
    (:cljengine.block/iron-ore [] :alias iron-ore :material Material/IRON_ORE :index 15)
    (:cljengine.block/coal-ore [] :alias coal-ore :material Material/COAL_ORE :index 16)
    (:cljengine.block/log nil :common-name "tree" :doc "Material #17." :alias log :material Material/LOG :index 17)
    (:cljengine.block/leaves log :alias leaves :material Material/LEAVES :index 18)
    (:cljengine.block/sponge [] :alias sponge :material Material/SPONGE :index 19)
    (:cljengine.block/glass [] :alias glass :material Material/GLASS :index 20)
    (:cljengine.block/lapis-ore [] :alias lapis-ore :material Material/LAPIS_ORE :index 21)
    (:cljengine.block/lapis-block [] :alias lapis-block :material Material/LAPIS_BLOCK :index 22)
    (:cljengine.block/dispenser [] :alias dispenser :material Material/DISPENSER :index 23)
    (:cljengine.block/sandstone [] :alias sandstone :material Material/SANDSTONE :index 24)
    (:cljengine.block/note-block [] :alias note-block :material Material/NOTE_BLOCK :index 25)
    (:cljengine.block/bed-block *bed-supporting* :common-name "Bed" #_(:alias bed) :alias bed-block :material Material/BED_BLOCK :index 26)
    (:cljengine.block/powered-rail *rail-supporting* :alias powered-rail :material Material/POWERED_RAIL :index 27)
    (:cljengine.block/detector-rail *rail-supporting* :alias detector-rail :material Material/DETECTOR_RAIL :index 28)
    (:cljengine.block/piston-sticky-base [] :common-name "Sticky Piston" :alias [sticky-piston, sticky-piston-base, piston-sticky-base] :material Material/PISTON_STICKY_BASE :index 29)
    (:cljengine.block/cobweb [] :alias cobweb :material Material/WEB :index 30)
    (:cljengine.block/long-grass grass-block :common-name "Grass" :url "http://minecraft.gamepedia.com/Grass" :alias long-grass :material Material/LONG_GRASS :index 31)
    (:cljengine.block/dead-bush [sand red-sand podzol hardened-clay flower-pot] :alias dead-bush :material Material/DEAD_BUSH :index 32)
    (:cljengine.block/piston-base [] :common-name "Piston" :alias [piston, piston-base] :material Material/PISTON_BASE :index 33)
    (:cljengine.block/piston-head [piston, sticky-piston] :common-name "Piston head" :doc "Tech block.  Same for the Sticky as the plain piston." :alias [piston-extension, piston-head] :material Material/PISTON_EXTENSION :index 34)
    (:cljengine.block/wool [] :alias wool :material Material/WOOL :index 35)
    (:cljengine.block/piston-moving-piece [] :alias piston-moving-piece :material Material/PISTON_MOVING_PIECE :index 36)
    (:cljengine.block/dandelion [grass-block farmland podzol dirt ;flower-pot ; I believe we take out the flowerpot because the plant in it does not occupy a separate block.
                                 ] :common-name "Dandelion" :url "http://minecraft.gamepedia.com/Flowers#Dandelion" :alias dandelion :alias yellow-flower :material Material/YELLOW_FLOWER :index 37)
    (:cljengine.block/multi-flower [grass-block farmland podzol dirt] :common-name "Poppy"
                                      :alias red-rose
                                      :alias blue-orchid
                                      :alias allium
                                      :alias azure-bluet
                                      :alias tulip
                                      :alias red-tulip
                                      :alias orange-tulip
                                      :alias white-tulip
                                      :alias pink-tulip
                                      :alias oxeye-daisy
                                      :alias multi-flower
                                      :material Material/RED_ROSE :index 38)
    (:cljengine.block/brown-mushroom [dirt grass-block mycelium podzol] :alias brown-mushroom :material Material/BROWN_MUSHROOM :index 39)
    (:cljengine.block/red-mushroom [dirt grass mycelium podzol flower-pot] :alias red-mushroom :material Material/RED_MUSHROOM :index 40)
    (:cljengine.block/gold-block [] :alias gold-block :material Material/GOLD_BLOCK :index 41)
    (:cljengine.block/iron-block [] :alias iron-block :material Material/IRON_BLOCK :index 42)
    (:cljengine.block/double-step [] :common-name "double slab" :doc "Presumably the result of combining upper and lower slabs in one location." :alias double-step :material Material/DOUBLE_STEP :index 43)
    (:cljengine.block/steps [] :common-name "slab" :alias slab :url "http://minecraft.gamepedia.com/Slabs" :alias steps :material Material/STEP :index 44)
    (:cljengine.block/brick [] :common-name "Bricks" :url "http://minecraft.gamepedia.com/Bricks" :alias brick :material Material/BRICK :index 45)
    (:cljengine.block/tnt [] :alias tnt :material Material/TNT :index 46)
    (:cljengine.block/bookshelf [] :alias bookshelf :material Material/BOOKSHELF :index 47)
    (:cljengine.block/mossy-cobblestone [] :alias mossy-cobblestone :material Material/MOSSY_COBBLESTONE :index 48)
    (:cljengine.block/obsidian [] :alias obsidian :material Material/OBSIDIAN :index 49)
    (:cljengine.block/torch *torch-supporting* :alias torch :material Material/TORCH :index 50)
    (:cljengine.block/fire *burnable* :alias fire :material Material/FIRE :index 51)
    (:cljengine.block/mob-spawner [] :doc "Tech block." :alias mob-spawner :material Material/MOB_SPAWNER :index 52)
    (:cljengine.block/wood-stairs [] :alias wood-stairs :material Material/WOOD_STAIRS :index 53)
    (:cljengine.block/chest [] :alias chest :material Material/CHEST :index 54)
    (:cljengine.block/redstone-wire [*opaque* glowstone *stairs* hopper] :alias redstone-wire :material Material/REDSTONE_WIRE :index 55)
    (:cljengine.block/diamond-ore [] :alias diamond-ore :material Material/DIAMOND_ORE :index 56)
    (:cljengine.block/diamond-block [] :alias diamond-block :material Material/DIAMOND_BLOCK :index 57)
    (:cljengine.block/workbench [] :common-name "crafting table" :alias crafting-table :alias workbench :material Material/WORKBENCH :index 58)
    (:cljengine.block/wheat [farmland] :common-name "wheat" :alias wheat :alias crops :material Material/CROPS :index 59)
    (:cljengine.block/soil [water water-source] :common-name "farmland" :alias farmland :url "http://minecraft.gamepedia.com/Farmland" :doc "To exist stably, farmland needs to be planted or water within X blocks.  Since water can't depend on farmland, it makes sense to put farmland after, though the dependency is not a very strict one." :alias soil :material Material/SOIL :index 60)
    (:cljengine.block/furnace [] :alias furnace :material Material/FURNACE :index 61)
    (:cljengine.block/lit-furnace [] :common-name "lit furnace" :alias [burning-furnace lit-furnace]  :material Material/BURNING_FURNACE :index 62)
    (:cljengine.block/sign-post *solid* :alias sign-post :alias signpost :material Material/SIGN_POST :index 63)
    (:cljengine.block/wooden-door-block [*opaque* hopper] :alias [wooden-door, wooden-door-block] :material Material/WOODEN_DOOR :index 64)
    (:cljengine.block/ladder [*opaque* jack-o-lantern] :alias ladder :material Material/LADDER :index 65)
    (:cljengine.block/rail *rail-supporting* :alias [rail track] :alias rails :material Material/RAILS :index 66)
    (:cljengine.block/cobblestone-stairs [] :alias cobblestone-stairs :material Material/COBBLESTONE_STAIRS :index 67)
    (:cljengine.block/wall-sign *solid* :alias wall-sign :material Material/WALL_SIGN :index 68)
    (:cljengine.block/lever *opaque* :alias lever :material Material/LEVER :index 69)
    (:cljengine.block/stone-plate *pressure-plate-supporting* :common-name "stone pressure plate" :alias [stone-pressure-plate stone-plate] :material Material/STONE_PLATE :index 70)
    (:cljengine.block/iron-door-block [*opaque* hopper] :common-name "Iron door" :alias iron-door :alias iron-door-block :material Material/IRON_DOOR_BLOCK :index 71)
    (:cljengine.block/wood-plate *pressure-plate-supporting* :common-name "wood pressure plate" :alias [wood-pressure-plate wood-plate] :material Material/WOOD_PLATE :index 72)
    (:cljengine.block/redstone-ore [] :alias redstone-ore :material Material/REDSTONE_ORE :index 73)
    (:cljengine.block/glowing-redstone-ore [] :common-name "lit redstone ore" :alias glowing-redstone-ore :material Material/GLOWING_REDSTONE_ORE :index 74)
    (:cljengine.block/redstone-torch-off *torch-supporting* :common-name "unlit redstone torch" :alias unlit-redstone-torch :alias redstone-torch-off :material Material/REDSTONE_TORCH_OFF :index 75)
    (:cljengine.block/redstone-torch-on *torch-supporting* :alias redstone-torch-on :material Material/REDSTONE_TORCH_ON :index 76)
    (:cljengine.block/stone-button *torch-supporting* :alias stone-button :material Material/STONE_BUTTON :index 77)
    (:cljengine.block/snow *opaque* :common-name "Snow (cover)" :url "http://minecraft.gamepedia.com/Snow_(cover)" :alias snow :material Material/SNOW :index 78)
    (:cljengine.block/ice [] :alias ice :material Material/ICE :index 79)
    (:cljengine.block/snow-block [] :common-name "Snow" :doc "TODO: There may be a difference between a fully-stacked snow block and a partial in some cases." :alias snow-block :material Material/SNOW_BLOCK :index 80)
    (:cljengine.block/cactus [sand; flower-pot
                              ] :doc "TODO: A cactus can go in a flowerpot, right?" :alias cactus :material Material/CACTUS :index 81)
    (:cljengine.block/clay [] :common-name "Clay (block)" :alias clay-block :alias clay :material Material/CLAY :index 82)
    (:cljengine.block/sugar-cane-block [water water-source] :common-name "Sugar Cane" :alias [sugar-cane, sugar-cane-block, sugarcane] :material Material/SUGAR_CANE_BLOCK :index 83)
    (:cljengine.block/jukebox [] :alias jukebox :material Material/JUKEBOX :index 84)
    (:cljengine.block/fence [] :alias fence :material Material/FENCE :index 85)
    (:cljengine.block/pumpkin [] :alias pumpkin :material Material/PUMPKIN :index 86)
    (:cljengine.block/netherrack [] :alias netherrack :material Material/NETHERRACK :index 87)
    (:cljengine.block/soul-sand [] :alias soul-sand :material Material/SOUL_SAND :index 88)
    (:cljengine.block/glowstone [] :alias glowstone :material Material/GLOWSTONE :index 89)
    (:cljengine.block/portal obsidian :common-name "Nether portal" :alias nether-portal :alias portal :material Material/PORTAL :index 90)
    (:cljengine.block/jack-o-lantern [] :alias [jack-o-lantern, jack-o'-lantern] :material Material/JACK_O_LANTERN :index 91)
    (:cljengine.block/cake-block [] :url "http://minecraft.gamepedia.com/Cake_Block" :alias [cake, cake-block] :material Material/CAKE_BLOCK :index 92)
    (:cljengine.block/diode-block-off *redstone-supporting* :alias [regenable-redstone-repeater
                                                                    redstone-repeater-off] :url "http://minecraft.gamepedia.com/Redstone_Repeater" :alias diode-block-off :material Material/DIODE_BLOCK_OFF :index 93)
    (:cljengine.block/diode-block-on *redstone-supporting* :alias [diode-block-on
                                                                   redstone-repeater-on] :material Material/DIODE_BLOCK_ON :index 94)
    (:cljengine.block/trap-door *solid* :common-name "trapdoor" :alias trapdoor :url "http://minecraft.gamepedia.com/Trapdoor" :alias trap-door :material Material/TRAP_DOOR :index 96)
    (:cljengine.block/monster-egg [] :alias [monster-egg, monster-eggs] :material Material/MONSTER_EGGS :index 97)
    (:cljengine.block/smooth-brick [] :common-name "Stone bricks" :alias smooth-brick :material Material/SMOOTH_BRICK :index 98)
    (:cljengine.block/huge-mushroom-1 [] :common-name "Huge Brown Mushroom" :alias huge-mushroom-1 :material Material/HUGE_MUSHROOM_1 :index 99)
    (:cljengine.block/huge-mushroom-2 [] :common-name "Huge Red Mushroom" :alias huge-mushroom-2 :material Material/HUGE_MUSHROOM_2 :index 100)
    (:cljengine.block/iron-fence nil :common-name "Iron bars" :alias iron-bars :alias iron-fence :material Material/IRON_FENCE :index 101)
    (:cljengine.block/thin-glass []         ;*solid* ; They don't need anything, do they?
                                 :common-name "glass pane" :alias thin-glass :material Material/THIN_GLASS :index 102
                                 )
    (:cljengine.block/watermelon [] :common-name "Melon" :alias melon :alias watermelon :alias melon-block :material Material/MELON_BLOCK :index 103)
    (:cljengine.block/pumpkin-stem farmland :alias pumpkin-stem :material Material/PUMPKIN_STEM :index 104)
    (:cljengine.block/melon-stem farmland :alias melon-stem :material Material/MELON_STEM :index 105)
    (:cljengine.block/vine [*solid* *chests* crafting-table] :common-name "Vines" :alias vines :doc "TODO: An individual vine block depends either on a solid block *or* a vine block above.  So the dependency is interesting: the Vine type has one dependency, while individual vine blocks may have another.  The progression is also from high to low, the opposite of what is usual." :alias vine :material Material/VINE :index 106)
    (:cljengine.block/fence-gate nil :url "http://minecraft.gamepedia.com/Fence_gate" :alias [gate, fence-gate] :doc "TODO: This does have a type-level dependency; it needs to be atop something." :material Material/FENCE_GATE :index 107)
    (:cljengine.block/brick-stairs [] :alias brick-stairs :material Material/BRICK_STAIRS :index 108)
    (:cljengine.block/smooth-stairs [] :alias smooth-stairs :material Material/SMOOTH_STAIRS :index 109)
    (:cljengine.block/mycel [] :common-name "Mycelium" :alias mycelium :alias mycel :material Material/MYCEL :index 110)
    (:cljengine.block/water-lily water-source :common-name "Lily pad" :alias [lilypad, lily-pad, water-lily] :material Material/WATER_LILY :index 111)
    (:cljengine.block/nether-brick [] :alias nether-brick :material Material/NETHER_BRICK :index 112)
    (:cljengine.block/nether-fence nil :common-name "Nether brick fence" :alias nether-fence :material Material/NETHER_FENCE :index 113)
    (:cljengine.block/nether-brick-stairs [] :alias nether-brick-stairs :material Material/NETHER_BRICK_STAIRS :index 114)
    (:cljengine.block/nether-wart soul-sand :common-name "Nether Wart" :alias [nether-wart, nether-warts] :material Material/NETHER_WARTS :index 115)
    (:cljengine.block/enchantment-table [] :alias enchantment-table :material Material/ENCHANTMENT_TABLE :index 116)
    (:cljengine.block/brewing-stand [] :url "http://minecraft.gamepedia.com/Brewing_stand" :alias brewing-stand :material Material/BREWING_STAND :index 117)
    (:cljengine.block/cauldron [] :alias cauldron :material Material/CAULDRON :index 118)
    (:cljengine.block/end-portal ender-portal-frame :common-name "End portal" :alias end-portal :alias ender-portal :material Material/ENDER_PORTAL :index 119)
    (:cljengine.block/ender-portal-frame [] :alias ender-portal-frame :material Material/ENDER_PORTAL_FRAME :index 120)
    (:cljengine.block/ender-stone nil :common-name "End Stone" :alias ender-stone :material Material/ENDER_STONE :index 121)
    (:cljengine.block/dragon-egg [] :alias dragon-egg :material Material/DRAGON_EGG :index 122)
    (:cljengine.block/redstone-lamp-off nil :common-name "Unlit redstone lamp" :alias regenable-redstone-lamp :alias redstone-lamp-off :material Material/REDSTONE_LAMP_OFF :index 123)
    (:cljengine.block/redstone-lamp-on nil :common-name "Lit redstone lamp" :alias redstone-lamp-on :material Material/REDSTONE_LAMP_ON :index 124)
    (:cljengine.block/wood-double-step [] :common-name "double wood slab" :doc "Like the stone 'double slab'." :alias wood-double-step :material Material/WOOD_DOUBLE_STEP :index 125)
    (:cljengine.block/wood-slab [] :common-name "Wood slab" :url "http://minecraft.gamepedia.com/Slabs" :alias wood-slab :alias wood-step :material Material/WOOD_STEP :index 126)
    (:cljengine.block/cocoa log :doc "Needs to be attached to a Jungle Wood tree." :alias cocoa :material Material/COCOA :index 127)
    (:cljengine.block/sandstone-stairs [] :alias sandstone-stairs :material Material/SANDSTONE_STAIRS :index 128)
    (:cljengine.block/emerald-ore [] :alias emerald-ore :material Material/EMERALD_ORE :index 129)
    (:cljengine.block/ender-chest nil :url "http://minecraft.gamepedia.com/Ender_Chest" :alias ender-chest :material Material/ENDER_CHEST :index 130)
    (:cljengine.block/tripwire-hook *solid* :doc "It doesn't sound likely that non-solid blocks could anchor a wire." :url "http://minecraft.gamepedia.com/Tripwire_Hook" :alias tripwire-hook :material Material/TRIPWIRE_HOOK :index 131)
    (:cljengine.block/tripwire [] :alias tripwire :material Material/TRIPWIRE :index 132)
    (:cljengine.block/emerald-block [] :alias emerald-block :material Material/EMERALD_BLOCK :index 133)
    (:cljengine.block/spruce-wood-stairs [] :alias spruce-wood-stairs :material Material/SPRUCE_WOOD_STAIRS :index 134)
    (:cljengine.block/birch-wood-stairs [] :alias birch-wood-stairs :material Material/BIRCH_WOOD_STAIRS :index 135)
    (:cljengine.block/jungle-wood-stairs [] :alias jungle-wood-stairs :material Material/JUNGLE_WOOD_STAIRS :index 136)
    (:cljengine.block/command-block nil :url "http://minecraft.gamepedia.com/Command_Block" :alias command-block :material Material/COMMAND :index 137)
    (:cljengine.block/beacon [] :alias beacon :material Material/BEACON :index 138)
    (:cljengine.block/cobble-wall nil :common-name "Cobblestone wall" :alias cobblestone-wall :url "http://minecraft.gamepedia.com/Cobblestone_wall" :alias cobble-wall :material Material/COBBLE_WALL :index 139)
    (:cljengine.block/flower-pot *solid* :alias [flowerpot, flower-pot] :material Material/FLOWER_POT :index 140)
    (:cljengine.block/carrot farmland :alias carrot :material Material/CARROT :index 141)
    (:cljengine.block/potato farmland :alias potato :material Material/POTATO :index 142)
    (:cljengine.block/wood-button *torch-supporting* :alias wood-button :material Material/WOOD_BUTTON :index 143)
    (:cljengine.block/skull [] :common-name "mob head" :url "http://minecraft.gamepedia.com/Skull" :alias [skull, mob-head] :material Material/SKULL :index 144)
    (:cljengine.block/anvil +sand-supporting+ :url "http://minecraft.gamepedia.com/Anvil" :alias anvil :material Material/ANVIL :index 145)
    (:cljengine.block/trapped-chest [] :alias trapped-chest :material Material/TRAPPED_CHEST :index 146)
    (:cljengine.block/gold-plate *pressure-plate-supporting* :common-name "Weighted pressure plate (light)" :url "http://minecraft.gamepedia.com/Weighted_Pressure_Plate" :alias gold-plate :alias gold-pressure-plate :material Material/GOLD_PLATE :index 147)
    (:cljengine.block/iron-plate *pressure-plate-supporting* :common-name "Weighted pressure plate (heavy)" :url "http://minecraft.gamepedia.com/Weighted_Pressure_Plate" :alias iron-plate :alias iron-pressure-plate :material Material/IRON_PLATE :index 148)
    (:cljengine.block/redstone-comparator-off *redstone-supporting* :alias regenable-redstone-comparator :alias redstone-comparator-off :material Material/REDSTONE_COMPARATOR_OFF :index 149)
    (:cljengine.block/redstone-comparator-on *redstone-supporting* :alias redstone-comparator-on :material Material/REDSTONE_COMPARATOR_ON :index 150)
    (:cljengine.block/daylight-detector *redstone-supporting* :doc "TODO:" :alias daylight-detector :material Material/DAYLIGHT_DETECTOR :index 151)
    (:cljengine.block/redstone-block [] :alias redstone-block :material Material/REDSTONE_BLOCK :index 152)
    (:cljengine.block/quartz-ore [] :alias quartz-ore :material Material/QUARTZ_ORE :index 153)
    (:cljengine.block/hopper [] :alias hopper :material Material/HOPPER :index 154)
    (:cljengine.block/quartz-block [] :alias quartz-block :material Material/QUARTZ_BLOCK :index 155)
    (:cljengine.block/quartz-stairs [] :alias quartz-stairs :material Material/QUARTZ_STAIRS :index 156)
    (:cljengine.block/activator-rail *rail-supporting* :alias activator-rail :material Material/ACTIVATOR_RAIL :index 157)
    (:cljengine.block/dropper [] :alias dropper :material Material/DROPPER :index 158)
    (:cljengine.block/stained-clay nil :url "http://minecraft.gamepedia.com/Stained_clay" :alias stained-clay :material Material/STAINED_CLAY :index 159)
    (:cljengine.block/hay-block [] :alias hay-block :material Material/HAY_BLOCK :index 170)
    (:cljengine.block/carpet *carpet-supporting* :alias carpet :material Material/CARPET :index 171)
    (:cljengine.block/hard-clay nil :common-name "Hardened clay" :url "http://minecraft.gamepedia.com/Stained_clay" :alias hard-clay :material Material/HARD_CLAY :index 172)
    (:cljengine.block/coal-block [] :alias coal-block :material Material/COAL_BLOCK :index 173)
    ;; TODO: Here are the ones that lack either indices or enums:
    (:cljengine.block/dark-wood [] :doc "Acacia & dark oak." :alias dark-wood :index 162)
    (:cljengine.block/stained-glass [] :alias stained-glass :index 95)
    (:cljengine.block/stained-glass-pane [] :alias thin-stained-glass :alias stained-glass-pane :index 160)
    (:cljengine.block/dark-leaves [dark-wood] :doc "Acacia & dark oak." :alias dark-leaves :index 161)
    (:cljengine.block/packed-ice [] :alias packed-ice :index 174)
    (:cljengine.block/double-plant [grass farmland podzol dirt] :doc "TODO:"
                                   :alias [double-plant, tall-plant, large-plant]
                                   :alias sunflower
                                   :alias lilac
                                   :alias large-fern
                                   :alias double-tallgrass
                                   :alias rose-bush
                                   :alias peony
                                   :index 175)))

(comment
  "Here are the unrepresented indices."
  #{163 164 165 166 167 168 169})

(comment
  "To retrieve a list of the Block types without index numbers, eval the following:"
  (remove #(:index (apply hash-map (nthrest % 2))) +block-types+))






(defn get-entry-by-primary-name [name & {:keys [domain] :or {domain +block-types+}}]
  "TODO: Name of +block-types+ set.  Or whatever.
TODO: Check aliases."
  (first (filter (fn [entry]
                   (mc/econd*
                    [(coll? entry) (= name (first entry))]
                    [(symbol? entry) (= entry name)]))
                 domain)))


(defn get-index [entry]
  "Shortcut to the :index key, if it exists."
  (:index (apply hash-map (nthrest entry 2))))

(do
  (defmulti get-material class)
  (defmethod get-material Block [block] (mc/get-type block))
  (defmethod get-material BlockState [block] (mc/get-type block))
  (defmethod get-material BlockImage [block]
    (mc/the Material (get-material (mc/the BlockState (.getBlockState block)))))
  (defmethod get-material :default [entry]
                                        ;   "Retrieves the :material key from a +block-type+.  Ugly."
    (mc/the* Material (eval (mc/satisfying symbol? (:material (apply hash-map (nthrest entry 2))))))))

(defn list-to-map [list]
  "Combines duplicate keys into sublists, so it does more than just a list->map conversion à la (apply hash-map).
TODO: Might be more elegantly done with (reduce), if I can't find some natural function combination."
  (let [multimap; Not actually a multimap; a map of lists.
        ;; Convert nil to the empty set:
        (or (apply (partial merge-with #(concat (if (coll? %1) %1 [%1]) (if (coll? %2) %2 [%2])))
                   (clojure.core/map (partial apply hash-map) (partition 2 list)))
            {})
                                        ;        keys (keys multimap)
                                        ;        vals (vals multimap)
                                        ;        map (zipmap keys (map second vals))
        ]
                                        ;    (println multimap)
    (mc/assert* (map? multimap))
    (zipmap (keys multimap)
            ;; Convert singleton lists to singleton sets; wrap eigenvalues as sets.
            (clojure.core/map #(if (coll? %1) (set %1) (hash-set %1)) (vals multimap)))))

(defn all-aliases-of [name & {:keys [main?, domain]
                              :or {main? true,
                                   domain +block-types+}}]
  "Passed the main symbol, returns a collection of any :alias entries.  Unless :main? is set false, the primary designator 'name' will appear first on the list.  Returns nil if nothing is found."
  (let [entry (get-entry-by-primary-name name :domain domain)]
    (mc/econd*
     [(nil? entry) nil]
     [(list? entry)
      (union (if main? #{name} #{}) (:alias (list-to-map (drop 2 entry))))]
     [(symbol? entry) (if main? #{name} #{})])))

(do
  (defmulti find-block-type (fn [o & _] (class o)))
  (defmethod find-block-type :cljengine.mc/block-state-image [img & options]
    (apply find-block-type (mc/the Material (get-material img)) options))
  (defmethod find-block-type Material [mat & {:keys [error, domain]
                                              :or {domain +block-types+}}]
    (if-let [match (some #(when (= (name (:material (apply hash-map (nthrest % 2)))) (.name mat))
                            %) domain)]
      match
      (when error (throw (Exception. (format "No matching Block type found for material '%s'." mat))))))
  (defmethod find-block-type :default [name-or-alias & {:keys [error, domain]
                                                        :or {domain +block-types+}}]
    ;; TODO: Don't use (filter); it doesn't short-c.
    (if-let [match (first (filter (fn [entry]
                                    (mc/econd*
                                     [(coll? entry)
                                      (let [[name & [dependencies & rest]] entry
                                            all-names (all-aliases-of name :domain domain) ;(set (conj (:alias (list-to-map rest)) name))
                                            ]
                                        ;(mc/debug-println all-names)
                                        (contains? all-names name-or-alias))]
                                     [(symbol? entry) (= entry name-or-alias)]))
                                  domain))]
      match
      (when error (throw (Exception. (format "No matching Block type found for '%s'." name-or-alias)))))))

#_(defn find-block-type
    "This is intended to be the main query function, examining both primary names and :alias fields.
If the :error keyword arg is set, an exception will be raised if no matching type is found."
    {:test #(do
              (mc/assert* (= (find-block-type 'rail) (find-block-type 'rails)))
                                        ;(mc/assert* (= (find-block-type 'pumpkin) 'pumpkin))
              )}
    ([name-or-alias & {:keys [error, domain]
                       :or {domain +block-types+}}]
       ;; TODO: Don't use (filter); it doesn't short-c.
       (if-let [match (first (filter (fn [entry]
                                       (mc/econd*
                                        [(coll? entry)
                                         (let [[name & [dependencies & rest]] entry
                                               all-names (all-aliases-of name :domain domain) ;(set (conj (:alias (list-to-map rest)) name))
                                               ]
                                        ;(mc/debug-println all-names)
                                           (contains? all-names name-or-alias))]
                                        [(symbol? entry) (= entry name-or-alias)]))
                                     domain))]
         match
         (when error (throw (Exception. (format "No matching Block type found for '%s'." name-or-alias)))))))


;;;; *********************** NEW ************************************
;; id=new, id=derive

(defn find-block-main-type-name [name-or-alias & options]
  "Given a primary name *or* an alias, returns the corresponding primary name, which should be a keyword, or nil; if :error is set, an exception will get thrown instead.  (See (find-block-type) for candidates to 'options'.)"
  (first (apply find-block-type name-or-alias options)) )


;; TODO: Make multimethod; overload for block instances.
(defn block-type-dependencies [arg & {:keys [error]}]
  "Argument 'arg' should either be a Block name or a collection of Block names, in which case their dependencies are union'd.  No other data.
NB: Returns 'nil' if no dependencies found unless the :error keyword arg is set, in which case an exception will be raised."
  (when-let [res (mc/econd*
                  [(symbol? arg) (second (mc/listify (find-block-type arg :error error)))]
                  [(or (list? arg)
                       (set? arg))
                   (mapcat (fn [name]
                             (mc/assert* (symbol? name))
                             (block-type-dependencies name)) arg)])]
    (if (coll? res) res #{res})))

#_(defmacro define-subtype [subtype-form supertype-form]
    (let [[subtype supertype :as types] (map find-block-main-type-name [subtype-form supertype-form])]
      (mc/assert* (every? #(or (nil? %) (keyword? %)) types))
      `(derive ~(or subtype (mc/satisfying keyword? subtype-form))
               ~(or supertype (mc/satisfying keyword? supertype-form)))))

(defmacro define-subtype [subtype-form supertype-form]
  "Wrapper for (derive) that allows aliases of Block types, or regular hierarchy keywords, to appear in the 'subtype-form' position.  The 'supertype-form' posittion *should* have a keyword."
  (let [subtype (find-block-main-type-name subtype-form)]
    (when-not (keyword? supertype-form) (throw (IllegalArgumentException. (format "Second arg to (define-subtype) should be a keyword, not '%s'." supertype-form))))
    (when (find-block-main-type-name supertype-form)
      (throw (Exception. (format "(define-subtype) should *not* get a Block type as a second arg: %s.  It should be a general hierarchy keyword." supertype-form))))
    (mc/assert* (or (nil? subtype) (keyword? subtype)))
    `(derive ~(or subtype (mc/satisfying keyword? subtype-form))
             ~supertype-form)))

(defmacro define-block-subtype [subtype-form supertype-form]
  "Like (define-subtype), but requires that the first arg't be a valid block-type main name or alias."
  (when-not (keyword? supertype-form) (throw (IllegalArgumentException. (format "Second arg to (define-subtype) should be a keyword, not '%s'." supertype-form))))
  (let [subtype (find-block-main-type-name subtype-form :error true)]; Error if search fails
    (when (find-block-main-type-name supertype-form)
      (throw (Exception. (format "(define-subtype) should *not* get a Block type as a second arg: %s.  It should be a general hierarchy keyword." supertype-form))))
    (mc/assert* (keyword? subtype))
    `(derive ~subtype ~supertype-form)))

(defmacro defmethod-on-block-type [name type-or-alias [& parms] & body]
  "Wrapper for (defmethod) that expects a keyword or symbol in the 'type-or-alias' position.  It is then looked up in +block-types+ to get the corresponding main name keyword, which is used as the dispatch val.  The purpose is to allow writing of multimethods specializing on Block types while allowing the freedom of aliases."
  (let [type (mc/satisfying keyword?
                            (find-block-main-type-name type-or-alias :error true))]
    `(defmethod ~name ~type [~@parms]
       ~@body)))



;; New and improved version:
(do
  (defmulti material-to-type #(mc/cond*
                               ((or (symbol? %)
                                    (keyword? %)) :name)
                                        ;((keyword? %) :keyword)
                               (:else (class %))))
  (defmethod material-to-type Material [mat]
    (mc/satisfying keyword?
                   (or (some #(let [mat' (get-material %)]
                                (when (= mat mat') (first %)))
                             +block-types+)
                       (throw (Exception. (format "(material-to-type) doesn't know how to handler Material %s." mat))))))
  (defmethod material-to-type :name [sym]
    (mc/satisfying keyword? (first (find-block-type sym :error true))))
  (defmethod material-to-type :default [obj]
    (mc/debug-println-2 ":default method in (material-to-type).")
    (throw (Exception. (format "(material-to-type) not defined for %s." obj)))))


#_(defmethod material-to-type :default [obj]
    (mc/debug-println-2 ":default method in (material-to-type).")
    (if (instance? Material obj) (throw (Exception. (format "(material-to-type) not defined for %s." obj)))
        (material-to-type (mc/get-type obj))))

#_(defmulti depends-on (fn [b1 b2 & [other]] (mc/satisfying vector?
                                                            (vec (concat (map class b1 b2)
                                                                         (if other [other] []))))))

;; TODO: It might be good to put Material, Symbol, & Keyword into a joint type.
(do
  (defmulti dispatch-on-block-type (fn [o & _] (class o)))
  (defmethod dispatch-on-block-type :cljengine.mc/block-state-image [block & _]
    (mc/satisfying keyword? (material-to-type (mc/the Material (mc/get-type block)))))
  (defmethod dispatch-on-block-type Material [mat & _]
    (mc/satisfying keyword? (material-to-type mat)))
  (defmethod dispatch-on-block-type Symbol [sym & _]
    (mc/satisfying keyword? (material-to-type sym)))
  (defmethod dispatch-on-block-type Keyword [kw & _]
    (mc/satisfying keyword? (material-to-type kw)))
  (defmethod dispatch-on-block-type :default [obj & _]
    (mc/debug-println-2 ":default method in (dispatch-on-block-type).")
    (class obj)))


;; What the--?  Where'd this come from?
#_(defn dispatch-on-block-type [obj & _]
    "Somewhat specialized dispatch function for writing multimethods.  If it can be reasonably certain that 'obj' is intended to identify a Block type, then we do a lookup in +block-types+ to get the primary name for that type.  If not, dispatch on (class) as usual.  This should allow multimethods that specialize on Block type hierarchy keywords.  This is more useful that dispatching off Material enum values because the latter can't be given to (derive) calls."
    (if (or (symbol? obj)
            (keyword? obj)
            (= (class obj) Material))
      (material-to-type obj)
      (class obj)))

(defn depends-on-dispatch [b1 b2 & [other]]
  (mc/satisfying vector?
                 (vec (concat (map #(if (keyword? %) % (material-to-type %)) [b1 b2])
                              (if other [other] [])))))

(defmulti depends-on depends-on-dispatch)
;;; NB: This is always risky:
(defmethod depends-on :default [& _]
  (mc/debug-println-2 ":default method in (depends-on).")
  false)

;; I've relaxed the requirement that the types satisfy (find-block-type) in case they refer to aggregate types.
(defmacro define-dependency [[dependent-type dependee :as blocks] func]
  (let [[main-type-1 main-type-2] (map #(let [name (find-block-main-type-name %)]
                                          (or name
                                              (when (keyword? %) %))) blocks)]
    (when-not (every? keyword? [main-type-1 main-type-2]) (throw (Exception. (format "Block type keywords not found for one of [%s, %s]." main-type-1 main-type-2))))
    `(defmethod depends-on [~main-type-1 ~main-type-2] [b1# b2#] (~func b1# b2#))))


                                        ; previously (dependency-on):
#_(defmacro define-dependency [[dependent-type dependee :as blocks] func]
    (let [[main-type-1 main-type-2] (map find-block-main-type-name blocks)]
      (when-not (every? keyword? [main-type-1 main-type-2]) (throw (Exception. (format "Block type keywords not found for one of [%s, %s]." main-type-1 main-type-2))))
      `(defmethod depends-on [~main-type-1 ~main-type-2] [b1# b2#] (~func b1# b2#))))



(do
  (defmulti above? (fn [x y] (vec (map class [x y]))))
  (defmethod above? :default [pos1 pos2]
    (mc/debug-println-2 ":default method in (above?).")
    (mc/block-pos-eq? pos1 (mc/add (mc/get-block-vector pos2) 0 1 0)))
  (defmethod above? [Object Keyword] [pos block-type]
    (= (mc/satisfying keyword? (material-to-type (mc/get-type (get-block-below pos))))
       block-type))
  (defmethod above? [Keyword Object] [block-type pos]
    (= (mc/satisfying keyword? (material-to-type (mc/get-type (get-block-above pos))))
       block-type)))

(definline below? [pos1 pos2]
  `(above? ~pos2 ~pos1))

;;;; Individual block dependencies:
;; id=indiv



(do
  (defn adjoining?
    {:test #(do (mc/assert* (adjoining? (BlockVector. 1 2 3)
                                        (BlockVector. 1 3 3)))
                (mc/assert* (not (adjoining? (BlockVector. 0 0 0)
                                             (BlockVector. 1 1 1)))))}
    [b1 b2]
    (let [v2 (mc/get-block-vector b2)]
      (some #(mc/block-pos-eq? b1 %)
            (map #(apply add v2 %) [[1 0 0]
                                    [-1 0 0]
                                    [0 1 0]
                                    [0 -1 0]
                                    [0 0 1]
                                    [0 0 -1]]))))
  (test #'adjoining?))

;; neighboring-blocks moved to mc

                                        ;(define-dependency [::leaves ::tree] )
(defmethod depends-on '[leaves leaves] [b1 b2 n]
  (mc/econd*
   ((zero? n) )))
                                        ;(define-dependency [leaves tree] adjoining?)

(definline depends-on? [b1 b2]
  `(depends-on ~b1 ~b2))

(definline depended-on-by? [b1 b2]
  `(depends-on? ~b2 ~b1))

(declare ≻ ≺)

(defn depends-on-indirectly? [b1 b2]
  "Partial-ordering relation; transitive version of (depends-on?)."
  (mc/debug-println (apply format "≻ comparison: %s, %s" (map (comp format-vector mc/get-block-vector) [b1 b2])) )
  (or (depends-on? b1 b2)
      (some (fn [%]
              (and (depends-on? % b2)
                   (≻ b1 %)))
            (surrounding-blocks b2))))

(definline ≻ [b1 b2]
  `(depends-on-indirectly? ~b1 ~b2))

(definline ≺ [b1 b2]
  `(≻ ~b2 ~b1))


(defn get-rest-of-bed [bed-block]
  "Given one half of a bed, returns the other."
  (mc/assert* (= (mc/get-type bed-block) Material/BED_BLOCK))
  (mc/assert* (isa? (find-block-main-type-name (get-material bed-block))
                 ::bed))
  (let [data (get-data bed-block)
        other-block (mc/the Block
                         (if (.isHeadOfBed data)
                           (let [foot (get-block-at (mc/add bed-block (get-mod-vector (.. data getFacing getOppositeFace))))]
                             foot)
                           (let [head (get-block-at (mc/add bed-block (get-mod-vector (.getFacing data))))]
                             head)))]
    (mc/assert* (= (mc/get-type other-block) Material/BED_BLOCK))
    (mc/assert* (not= bed-block other-block))
    other-block))

(defn get-rest-of-piston [piston-block]
  "Returns the other part of the piston--which will be nil if the piston isn't extended."
  (mc/econd*
   [(= (mc/get-type piston-block) Material/PISTON_EXTENSION)
    (mc/the Block (get-attached-block piston-block :error true))]
   [(or (= (mc/get-type piston-block)
           Material/PISTON_STICKY_BASE)
        (= (mc/get-type piston-block)
           Material/PISTON_BASE))
    (let [data (mc/the MaterialData (get-data piston-block))]
      (when (.isPowered data)
        (mc/the Block (get-block-at (mc/add piston-block (get-mod-vector (.getFacing data)))))))]))

(defn get-rest-of-double-plant [plant-block]
  (mc/the Block (let [under (get-block-below plant-block)]
               (if (= (mc/get-type under) Material/DOUBLE_PLANT)
                 under
                 (let [over (get-block-above plant-block)]
                   (mc/assert* (= (mc/get-type over) Material/DOUBLE_PLANT))
                   over)))))


;; TODO: The Door class isn't functional, according to Bukkit:
(defn get-rest-of-door [door-block]
  (mc/assert* (isa? (find-block-main-type-name (mc/get-type door-block)) ::door))
  (mc/the Block (let [under (get-block-below door-block)]
               (if (isa? (find-block-main-type-name (mc/get-type under)) ::door)
                 under
                 (let [over (get-block-above door-block)]
                   (mc/assert* (isa? (find-block-main-type-name (mc/get-type over)) ::door))
                   over)))))

;; Moved (dependency-action) to dependencies.clj.

;; TODO:
#_(defn calc-dependency-batch-expansion [initial-set]
  (loop [working-set initial-set]
    ))

#_(defn calc-dep-blocks [re-set]
  (mc/satisfying set?
              (letfn [(calc-dep-blocks' [b db]
                        (let [nearby-db (filter (partial depended-on-by? b) (difference (neighboring-blocks b) db))
                              db' (conj (union db nearby-db) b)]
                          (reduce (fn [cur-db b']
                                    (mc/satisfying set? (let [new-db (union cur-db (calc-dep-blocks b' cur-db))]
                                                       (mc/assert* (>= (count new-db) (count cur-db)))
                                                       new-db)))
                                        ;nearby-db
                                  db')))]
                (let [re-set' (reduce #(calc-dep-blocks' %2 %1) re-set re-set)]
                  (mc/assert* (>= (count re-set') (count re-set)))
                  re-set'))))

#_(defn calc-re-blocks [init-re-set]
  (mc/satisfying set?
              (letfn [(calc-re-blocks' [b acc]
                        (union acc
                               (reduce calc-re-blocks' (filter (partial depended-on-by? b)
                                                               (difference (set (neighboring-blocks b)) acc)))))]
                (reduce calc-re-blocks' #{} init-re-set))))

#_(defn calc-dep-blocks [initial-re-set]
    (letfn [(calc-dep-blocks' [b blocks]
              (let [nearby-db (filter (partial depended-on-by? b) (difference (neighboring-blocks b) blocks))
                    db' (conj (union blocks nearby-db) b)]
                (reduce (fn [cur-db b']
                          (union cur-db (calc-dep-blocks b' cur-db)))
                        db')))]
      (reduce #(calc-dep-blocks' %2 %1) initial-re-set initial-re-set)))

(do
  (defmulti is-block-type? (fn [b type & _] (class b)))
  (defmethod is-block-type? :cljengine.mc/block-state-image [b type]
    (isa? (mc/the Keyword (material-to-type (mc/the Material (get-material b)))) type))
  (defmethod is-block-type? Material [mat type]
    (isa? (mc/the Keyword (material-to-type mat)) type))
  (defmethod is-block-type? ::symbol [s type]
    (isa? (mc/satisfying keyword? (material-to-type s)) type)))


(comment
  "


* Prove: calc-dep-blocks terminates.

We define '≻' to mean
b₂≻ b₁⇔ (depends-on? b₂b₁) ∨ ∃b₃. (depends-on? b₂b₃) ∧ b₃≻ b₁
We can loosen it to a regular partial order by defining
b₂≽ b₁⇔ b₂= b₁∨ b₂≻ b₁


* Prove: ∀b ∈ W. b ∈ (calc-dep-blocks re-set) ⇔ ∃b'. b' ∈ re-set ∧ b ≽ b'

** First: Prove that b ∈ (calc-dep-blocks re-set) ⇒ ∃b'. b' ∈ re-set ∧ b ≽ b'
One possibility is that b is in initial-re-set.  It gets passed to calc-dep-blocks' via the (reduce) call.


calc-re-blocks init-re-set =
  let calc-re-blocks' b blocks = reduce (λ acc b' -> calc-re-blocks' b' cur ∪ acc) ({b} ∪ blocks ∪ filter (depended-on-by? b) (neighboring-blocks b - blocks))
  in
    reduce (λx y -> calc-re-blocks' y x) init-re-set init-re-set

Operators:
≺≻≼≽
≺≻≼≽⇔₁₂
⊆⊇
∩ ∪
→ ⇒
")

(comment
  (do
    (def bt2 (map #(if (coll? %) % [%]) +block-types+))
#_    (def bt2 (map identity +block-types+))
    #_(def bt3 (map #(if (> (count %) 1) % (concat % [[] :alias (first % )]))))
    (def bt3 (map #(concat (if (> (count %) 1) % (conj % []))
                           [:alias (first %)])
                  bt2))
    (def bt-names (map #(keyword "cljengine.block" (name (first %))) bt3))
    (def bt4 (map #(cons %1 (rest %2)) bt-names bt3))
    (mc/assert* (every? #(contains? (set %) :alias) bt4))
    (mc/assert* (every? coll? bt4))
    )

  (comment
    (with-open [f (writer "bts.clj")] (binding [*out* f *print-length* nil] (pr bt4)))) )

;  "Emacs materials_enum.txt regex:"
;"[[:space:]]*\\(\\S-*\\)(\\([[:digit:]]*\\).*$"

;"But this one actually worked, through Viper:"
;":%s/[[:space:]]*\(\S-*\)(\([[:digit:]]*\).*$/:material Material\/\1 :index \2/"


;;;; id=blink
(do
  (defmulti blink (fn [o & _] (class o)))
  (defmethod blink Iterable [coll & {:keys [seconds] :or {seconds 10}}]
    (let [blinker (repeated-task *plugin*
                                 #(doseq [block coll]
                                    (effect (.toLocation (get-block-vector block) (get-current-world)) org.bukkit.Effect/MOBSPAWNER_FLAMES nil))
                                 0 20)]
      (delayed-task *plugin* #(cancel-task blinker) (seconds-to-ticks seconds))) )
  (defmethod blink RegenBatch [bat & {:keys [seconds] :or {seconds 10}}]
    (blink (.blocks bat)))
  (defmethod blink :default [pos & {:keys [seconds] :or {seconds 10}}]
    (let [blinker (repeated-task *plugin* #(effect (.toLocation (get-block-vector pos) (get-current-world)) org.bukkit.Effect/MOBSPAWNER_FLAMES nil)
                                 0 20)]
      (delayed-task *plugin* #(cancel-task blinker) (seconds-to-ticks seconds)))))
