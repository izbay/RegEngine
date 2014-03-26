; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode) -*-
;;;; See http://minecraft-ids.grahamedgecombe.com/ for all the blocks' ID numbers & pictures!

;; TODO: Continue with changing '*...*' to '+...+' notation.

(ns mc.block
  (:use (clojure core set))
  (:use mc))

;; TODO: Should the metatag be ':set' i/s/o 'clojure.core/set'?
(defmacro def-blocktype-set [name & body]
  "Wrapper for 'def' that adds some metadata.  Metadata within the main declaration will override the addition, so it should be as safe as 'def'."
  `(do
     (def ~name ~@body)
     (alter-meta! #'~name (fn [current-meta# & rest#] (apply merge current-meta# rest#)) '{:semantic-type set})
     ~name))

(comment
  Block_list ::= [Block_entry*];
  Block_entry ::= Symbol | [Symbol Depends Attribute*];
  Depends ::= Nil_dep | Symbol | [Symbol+];
  Nil_dep ::= nil | [];
  Attribute ::= Keyword Value;

  "In this case paired parens can be used interchangeably with paired brackets.")
;; Rule: Sets are flattened.  If a set is specified as a member of another set, the union is taken.
;; There are 173 Blocks.

#_(defn keys* [seq]
  "Overloaded so that a list can be passed..."
  (econd*
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

(def-blocktype-set +block-types+
  "Primary set."
  (union '#{air
           stone
           (grass [] :common-name "Grass Block" :alias grass-block) ; http://minecraft.gamepedia.com/Grass_Block ; Without light, grass will turn to dirt in time.
           dirt
           cobblestone
           (wood nil :common-name "Wood planks")
           ;; Finally!  Our first dependency!
            (sapling [dirt podzol grass-block] :url "http://minecraft.gamepedia.com/Sapling")
            bedrock          ; There'd better not be dependencies here.
                                        ;(water stationary-water)
            (stationary-water [] :alias water-source)
                                        ;(lava stationary-lava)
            (stationary-lava [] :alias lava-source)
            (sand +sand-supporting+) ; Red Sand is not a separate block.
            (gravel +sand-supporting+)
            gold-ore
            iron-ore
            coal-ore
            (log nil :common-name "tree" :doc "Material #17.")
            (leaves log)
            ;*nondecaying-leaves* ;; TODO: Special
            sponge ;; We might as well back this up, even though it's not a regularly obtainable item.
            glass  ; http://minecraft.gamepedia.com/Glass
            lapis-ore
            lapis-block ; http://minecraft.gamepedia.com/Lapis_Lazuli_Block
            dispenser
            sandstone
            note-block      ; http://minecraft.gamepedia.com/Note_block
            (bed-block *bed-supporting* :common-name "Bed" :alias bed) ; http://minecraft.gamepedia.com/Bed
            (powered-rail *rail-supporting*) ; http://minecraft.gamepedia.com/Powered_rail
            (detector-rail *rail-supporting*)
            (piston-sticky-base [] :common-name "Sticky Piston" :alias sticky-piston )
            cobweb
            (long-grass grass-block :common-name "Grass" :url "http://minecraft.gamepedia.com/Grass")
            (dead-bush [sand red-sand podzol hardened-clay flower-pot])
            (piston-base [] :common-name "Piston" :alias piston)
            (piston-extension [piston sticky-piston] :common-name "Piston head" :doc "Tech block.  TODO: Is it the same extended piece for the sticky p. as the regular?")
            wool
                                        ;piston-moving-piece ; #36. TODO: This one is still a mystery.
            (yellow-flower [grass farmland podzol dirt flower-pot] :common-name "Dandelion" :url "http://minecraft.gamepedia.com/Flowers#Dandelion")
            (red-rose [grass farmland podzol dirt flower-pot] :common-name "Poppy") ; http://minecraft.gamepedia.com/Flowers#Poppy
            (brown-mushroom [dirt grass mycelium podzol flower-pot]) ; TODO: farmland?
            (red-mushroom [dirt grass mycelium podzol flower-pot]) ; TODO: farmland?
            gold-block   ; http://minecraft.gamepedia.com/Block_of_Gold
            iron-block
            (double-step [] :common-name "double slab" :doc "Presumably the result of combining upper and lower slabs in one location.")
            ;; 'Slab':
            (steps [] :common-name "slab" :alias slab :url "http://minecraft.gamepedia.com/Slabs")
            (brick [] :common-name "Bricks" :url "http://minecraft.gamepedia.com/Bricks")
            tnt
            bookshelf
            mossy-cobblestone ; http://minecraft.gamepedia.com/Moss_Stone
            obsidian
                                        ;          (torch *torch-supporting*) ; TODO: http://minecraft.gamepedia.com/Torch

            ;; Arguably we should only include Netherrack, since the others are unstable.
           (fire *burnable*)     ; http://minecraft.gamepedia.com/Fire
           (mob-spawner [] :doc "Tech block.")
                                        ;wood-stairs ; Handled via (union *stairs*)
                                        ;chest ; Handled via (union *chests*)
            (redstone-wire ; See 'Redstone related' at http://minecraft.gamepedia.com/Technical_blocks
             [*opaque* glowstone *stairs* hopper]); TODO: Slab?
            diamond-ore
            diamond-block
            (workbench [] :common-name "crafting table" :alias crafting-table)
            (crops [farmland] :common-name "wheat" :alias wheat) ; http://minecraft.gamepedia.com/Wheat
            (soil [water water-source] :common-name "farmland" :alias farmland :url "http://minecraft.gamepedia.com/Farmland"
                  :doc "To exist stably, farmland needs to be planted or water within X blocks.  Since water can't depend on farmland, it makes sense to put farmland after, though the dependency is not a very strict one.")
            furnace
                                        ; burning-furnace ?
           ;; TODO: *solid* type for sign anchoring?
            (sign-post *solid*)
            (wall-sign *solid*)

            ;; TODO: Is this right?
            (wooden-door [*opaque* hopper])

           ;; Ladders are tough.  They can be placed on Jack o'Lanterns, which are transparent, but not on Hoppers, Redstone Blocks, or other transparent stuff.
           (ladder [*opaque* jack-o-lantern]) ; http://minecraft.gamepedia.com/Ladders

           (rails *rail-supporting* :alias [rail, track])

           (lever *opaque*)
           (stone-plate *pressure-plate-supporting* :common-name "stone pressure plate")
           (iron-door-block [*opaque* hopper] :common-name "Iron door" :alias iron-door)
           (wood-plate *pressure-plate-supporting* :common-name "wood pressure plate")
           redstone-ore
           (stone-button *torch-supporting*)
           (snow *opaque* :common-name "Snow (cover)" :url "http://minecraft.gamepedia.com/Snow_(cover)")
           ice
           (snow-block [] :common-name "Snow" :doc "TODO: There may be a difference between a fully-stacked snow block and a partial in some cases.")
            (cactus [sand flower-pot] :doc "TODO: A cactus can go in a flowerpot, right?")
           (clay [] :common-name "Clay (block)" :alias clay-block)
           (sugar-cane-block [] :common-name "Sugar Cane" :alias sugar-cane)
           jukebox            ; http://minecraft.gamepedia.com/Jukebox
           fence
           pumpkin ; The melon family cannot be placed on non-solid blocks, but the solid block may be afterward removed.
           netherrack
           soul-sand
           glowstone
           (portal obsidian :common-name "Nether portal" :alias nether-portal)
           jack-o-lantern

           (cake-block :url "http://minecraft.gamepedia.com/Cake_Block")
           ;; TODO: I would like to consider the 'off' state the stable one, hence my giving it the primary name.
           (diode-block-off *redstone-supporting* :alias regenable-redstone-repeater :url "http://minecraft.gamepedia.com/Redstone_Repeater")
           (diode-block-on *redstone-supporting*)

           (trap-door *solid* :common-name "trapdoor" :alias trapdoor :url "http://minecraft.gamepedia.com/Trapdoor")
                                        ;monster-eggs; Non-regenable tech block.

           (huge-mushroom-1 [] :common-name "Huge Brown Mushroom")
           (huge-mushroom-2 [] :common-name "Huge Red Mushroom")
           (smooth-brick [] :common-name "Stone bricks")
           (iron-fence nil :common-name "Iron bars" :alias iron-bars)
           (thin-glass *solid* :common-name "glass pane")
           (melon-block [] :common-name "Melon" :alias melon)
           (pumpkin-stem farmland)
           (melon-stem farmland)
           (vine [*solid* *chests* crafting-table] :common-name "Vines" :alias vines
                 :doc "TODO: An individual vine block depends either on a solid block *or* a vine block above.  So the dependency is interesting: the Vine type has one dependency, while individual vine blocks may have another.  The progression is also from high to low, the opposite of what is usual.")
           (fence-gate nil :url "http://minecraft.gamepedia.com/Fence_gate")
           (mycel [] :common-name "Mycelium" :alias mycelium)
           (water-lily water-source :common-name "Lily pad" :alias lily-pad)
           nether-brick
           (nether-fence nil :common-name "Nether brick fence")
                                        ;          nether-brick-stairs ; in *stairs*
           (nether-warts soul-sand :common-name "Nether Wart")
           enchantment-table
           (brewing-stand [] :url "http://minecraft.gamepedia.com/Brewing_stand")
           cauldron

           (ender-portal ender-portal-frame :common-name "End portal" :alias end-portal)
           ender-portal-frame
           (ender-stone nil :common-name "End Stone")
           dragon-egg

            (redstone-lamp-on nil :common-name "Lit redstone lamp") ; TODO: Regenable?
            (redstone-lamp-off nil :common-name "Unlit redstone lamp" :alias regenable-redstone-lamp)
            (wood-double-step [] :common-name "double wood slab" :doc "Like the stone 'double slab'.")
            ; wood-step: in *stairs*
            (cocoa log :doc "Needs to be attached to a Jungle Wood tree.")
            emerald-ore
            (ender-chest nil :url "http://minecraft.gamepedia.com/Ender_Chest")
            (tripwire-hook *solid* :doc "It doesn't sound likely that non-solid blocks could anchor a wire." :url "http://minecraft.gamepedia.com/Tripwire_Hook")
            tripwire
            emerald-block
           (command-block nil :url "http://minecraft.gamepedia.com/Command_Block")
            beacon
            (cobble-wall nil :common-name "Cobblestone wall" :alias cobblestone-wall :url "http://minecraft.gamepedia.com/Cobblestone_wall")
            (flower-pot *solid*)
            (carrot farmland)
            (potato farmland)
            (wood-button *torch-supporting*)
            ;; TODO:
            (skull [] :common-name "mob head" :url "http://minecraft.gamepedia.com/Skull")
            (anvil +sand-supporting+ :url "http://minecraft.gamepedia.com/Anvil")
            (gold-plate *pressure-plate-supporting* :common-name "Weighted pressure plate (light)" :url "http://minecraft.gamepedia.com/Weighted_Pressure_Plate")
            (iron-plate *pressure-plate-supporting* :common-name "Weighted pressure plate (heavy)" :url "http://minecraft.gamepedia.com/Weighted_Pressure_Plate")
            ;; TODO: mask out the 'on' version?
            (redstone-comparator-off *redstone-supporting* :alias regenable-redstone-comparator)
            (redstone-comparator-on *redstone-supporting*)
            (daylight-detector *redstone-supporting* :doc "TODO:")
            redstone-block
            quartz-ore
            hopper
            quartz-block
            (activator-rail *rail-supporting*)
            dropper
            (stained-clay nil :url "http://minecraft.gamepedia.com/Stained_clay")
            hay-block
            stained-glass; #160
            (dark-leaves [dark-wood] :doc "Acacia & dark oak.")
            (dark-wood [] :doc "Acacia & dark oak.")
            ;; TODO: Forthcoming:
            ; (slime-block [])
            ; barrier
            ; iron trapdoor
            (carpet *carpet-supporting*)
            (hard-clay nil :common-name "Hardened clay" :url "http://minecraft.gamepedia.com/Stained_clay")
            coal-block
            packed-ice
            (sunflower [grass farmland podzol dirt] :doc "TODO:")
            (lilac [grass farmland podzol dirt] :doc "TODO:")
            (double-tallgrass [grass farmland podzol dirt] :doc "TODO:")
            (large-fern [grass farmland podzol dirt] :doc "TODO:")
            (rose-bush  [grass farmland podzol dirt] :doc "TODO:")
            (peony [grass farmland podzol dirt] :doc "TODO:")
           }
        ;; Unioned sets:
        *chests*
        *stairs*
        +torches+))

(defn get-entry-by-primary-name [name]
  "TODO: Name of +block-types+ set.  Or whatever.
TODO: Check aliases."
  (first (filter (fn [entry]
                   (econd*
                    [(coll? entry) (= name (first entry))]
                    [(symbol? entry) (= entry name)]))
                 +block-types+)))


(defn list-to-map [list]
  "Combines duplicate keys.  TODO: Might be more elegantly done with (reduce), if I can't find some natural function combination."
  (let [multimap; Not actually a multimap; a map of lists.
        ;; Convert nil to the empty set:
        (or (apply (partial merge-with #(concat (if (coll? %1) %1 [%1]) (if (coll? %2) %2 [%2])))
                (map (partial apply hash-map) (partition 2 list)))
            {})
;        keys (keys multimap)
;        vals (vals multimap)
;        map (zipmap keys (map second vals))
        ]
;    (println multimap)
    (assert* (map? multimap))
    (zipmap (keys multimap)
            ;; Convert singleton lists to singleton sets; wrap eigenvalues as sets.
            (map #(if (coll? %1) (set %1) (hash-set %1)) (vals multimap)))))

(defn all-aliases-of [name & {:keys [main?]
                              :or {main? true}}]
  "Passed the main symbol, returns a collection of any :alias entries.  Unless :main? is set false, the primary designator 'name' will appear first on the list.  Returns nil if nothing is found."
  (let [entry (get-entry-by-primary-name name)]
    (econd*
     [(nil? entry) nil]
     [(list? entry)
      (union (if main? #{name} #{}) (:alias (list-to-map (drop 2 entry))))]
     [(symbol? entry) (if main? #{name} #{})])))

(defn find-block-type
  "This is intended to be the main query function, examining both primary names and :alias fields.
If the :error keyword arg is set, an exception will be raised if no matching type is found."
  {:test #(do
            (assert* (= (find-block-type 'rail) (find-block-type 'rails)))
            (assert* (= (find-block-type 'pumpkin) 'pumpkin)))}
  ([name-or-alias & {:keys [error?]}]
     (if-let [match (first (filter (fn [entry]
                                (econd*
                                 [(coll? entry)
                                  (let [[name & [dependencies & rest]] entry
                                        all-names (all-aliases-of name) ;(set (conj (:alias (list-to-map rest)) name))
                                        ]
                                    ;(debug-println all-names)
                                    (contains? all-names name-or-alias))]
                                 [(symbol? entry) (= entry name-or-alias)]))
                              +block-types+))]
       match
       (when error? (throw (Exception. (format "No matching Block type found for '%s'." name-or-alias)))))))

(test #'find-block-type)


(defn dependencies [arg & {:keys [error?]}]
  "Argument 'arg' should either be a Block name or a collection of Block names, in which case their dependencies are union'd.  No other data.
NB: Returns 'nil' if no dependencies found unless the :error keyword arg is set, in which case an exception will be raised."
  (when-let [res (econd*
             [(symbol? arg) (second (listify (find-block-type arg :error? error?)))]
             [(or (list? arg)
                  (set? arg))
              (mapcat (fn [name]
                        (assert* (symbol? name))
                        (dependencies name)) arg)])]
    (if (coll? res) res #{res})))


;;;; ToDo:
; Anvils with ladders,signs,rails
; Slabs with ladders,signs, rails
