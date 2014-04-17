; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode) -*-
;;;; See http://minecraft-ids.grahamedgecombe.com/ for all the blocks' ID numbers & pictures!


;;; [Oh, hell, another mistake.  The gravity-based dependencies should be :restore, not :destroy.]

(ns cljengine.block)



;;;; Block type-level dependencies
;; id=type-level

(do
  (define-block-subtype sand ::gravity-bound)
  (define-block-subtype gravel ::gravity-bound)
  (define-block-subtype anvil ::gravity-bound))

(do
  (define-subtype ::opaque ::load-bearing)
  (define-subtype ::any-torch ::load-bearing)
  (define-subtype ::gravity-bound ::load-bearing)
  (define-block-subtype snow ::load-bearing)
  (define-block-subtype vines ::load-bearing)
  (define-block-subtype sapling ::load-bearing))

(do
  (define-block-subtype slab ::slab)
  (define-block-subtype wood-slab ::slab))


(do
  (derive ::opaque ::torch-bearing)
  (derive ::glass ::torch-bearing)
  (derive ::packed-ice ::torch-bearing)
  (derive ::hopper ::torch-bearing)
  (derive ::end-portal ::torch-bearing))

(do
  (derive ::torch ::any-torch)
  (derive ::redstone-torch ::any-torch)
  (do
    (derive ::redstone-torch-on ::redstone-torch)
    (derive ::redstone-torch-off ::redstone-torch)))

(do
  (derive ::red-mushroom ::mushroom)
  (derive ::brown-mushroom ::mushroom))

(do
  (define-subtype ::dirt ::sapling-growing)
  (define-block-subtype ::grass-block ::sapling-growing)
  (assert* (every? find-block-type '[dirt podzol])))

(do
  (define-block-subtype ::watermelon ::melon)
  (define-block-subtype ::pumpkin ::melon)
  (define-block-subtype ::jack-o-lantern ::melon)
  (assert* (every? find-block-type '[watermelon, pumpkin, jack-o-lantern])))

(define-subtype ::solid ::bed-supporting)

(do
  (define-subtype ::solid ::rail-supporting)
  (define-subtype ::melon ::rail-supporting)
  (define-block-subtype ::hopper ::rail-supporting)
  (define-block-subtype ::redstone-block ::rail-supporting)
  (assert* (every? find-block-type '[hopper redstone-block])))

(do
  (define-block-subtype rail ::any-rail)
  (define-block-subtype powered-rail ::any-rail)
  (define-block-subtype activator-rail ::any-rail)
  (define-block-subtype detector-rail ::any-rail))

(do
  (define-block-subtype piston ::any-piston-base)
  (define-block-subtype sticky-piston ::any-piston-base)
  (define-subtype ::any-piston-base ::any-piston)
  (define-block-subtype piston-head ::any-piston))

(do
  (define-block-subtype dandelion ::flower)
  (define-block-subtype multi-flower ::flower))

(do
  (define-subtype ::dirt ::flower-supporting)
  (define-block-subtype grass-block ::farmland))

(do
  (define-subtype ::dirt ::mushroom-supporting)
  (define-block-subtype grass-block ::mushroom-supporting)
  (define-block-subtype farmland ::mushroom-supporting)
  (define-block-subtype mycelium ::mycelium))

(do
  (define-subtype ::opaque ::redstone-wire-supporting)
  (define-subtype ::stairs ::redstone-wire-supporting)
  (define-block-subtype glowstone ::redstone-wire-supporting)
  (define-block-subtype hopper ::redstone-wire-supporting))

(do
  (define-block-subtype wooden-door-block ::wooden-door)
  (define-block-subtype iron-door-block ::iron-door)
  (define-subtype ::wooden-door ::door)
  (define-subtype ::iron-door ::door) )


(do
  (define-subtype ::opaque ::door-supporting)
  (define-block-subtype hopper ::door-supporting))

;;; New:
(do
  (define-block-subtype bed-block ::bed))

(do
  (define-subtype ::door ::compound-block)
  (define-subtype ::bed ::compound-block)
  (define-subtype ::any-piston ::compound-block))

(do
  (define-subtype ::opaque ::ladder-supporting)
  (define-block-subtype jack-o-lantern ::ladder-supporting)
  (assert* (every? #(isa? % ::ladder-supporting) [::opaque ::jack-o-lantern])))

(do
  (define-block-subtype signpost ::sign)
  (define-block-subtype wall-sign ::sign ))

(do
  (define-block-subtype stone-pressure-plate ::pressure-plate)
  (define-block-subtype wood-pressure-plate ::pressure-plate)
  (define-block-subtype gold-pressure-plate ::pressure-plate)
  (define-block-subtype iron-pressure-plate ::pressure-plate))

(define-subtype ::solid ::pressure-plate-supporting)

(do
  (define-block-subtype wood-button ::button)
  (define-block-subtype stone-button ::button))

(do
  (define-subtype ::torch-bearing ::button-bearing)
  (define-block-subtype dispenser ::button-bearing)
  (define-block-subtype dropper ::button-bearing))

(do
  (define-block-subtype redstone-repeater-on ::redstone-repeater)
  (define-block-subtype redstone-repeater-off ::redstone-repeater))

(do
  (define-subtype ::opaque ::redstone-supporting)
  (define-subtype ::stairs ::redstone-supporting)
  (define-subtype ::slab ::redstone-supporting))

(do
  (define-block-subtype huge-mushroom-1 ::huge-mushroom)
  (define-block-subtype huge-mushroom-2 ::huge-mushroom))

;; NB: 'crops' is the official name for Wheat, so don't confuse it with '::crop'.
(do
  (define-block-subtype carrot ::crop)
  (define-block-subtype potato ::crop)
  (define-block-subtype wheat ::crop)
  (define-block-subtype melon-stem ::crop)
  (define-block-subtype pumpkin-stem ::crop))

(do
  (define-block-subtype redstone-comparator-on ::redstone-comparator)
  (define-block-subtype redstone-comparator-off ::redstone-comparator))


(do
  (define-block-subtype wood-stairs ::stairs)
  (define-block-subtype cobblestone-stairs ::stairs)
  (define-block-subtype brick-stairs ::stairs)
  (define-block-subtype smooth-stairs ::stairs)
  (define-block-subtype nether-brick-stairs ::stairs)
  (define-block-subtype sandstone-stairs ::stairs)
  (define-block-subtype spruce-wood-stairs ::stairs)
  (define-block-subtype birch-wood-stairs ::stairs)
  (define-block-subtype jungle-wood-stairs ::stairs)
  (define-block-subtype quartz-stairs ::stairs))

;;;; Even newer (10 April):


#_(do
  (define-dependency [::gravity-bound ::load-bearing] above?)

  (define-dependency [::cactus ::sand] above?)
  (define-dependency [::cactus ::cactus] above?))

(do
  (defmulti add-to-regen-batch dispatch-on-block-type)
  (defmethod add-to-regen-batch :default [block batch & {:keys [level] :or {level :strong}}]
    (debug-println-2 ":default in (add-to-regen-batch).")
    (assoc batch block level))
  (defmethod add-to-regen-batch ::bed [bed-block batch]
    (assoc (assoc batch (get-rest-of-bed bed-block) :strong)
      bed-block :strong))
  (defmethod add-to-regen-batch ::any-piston [piston-block batch]
    (let [batch' (assoc batch piston-block :strong)]
      (if-let [other-half (get-rest-of-piston piston-block)]
        (assoc batch' other-half :strong)
        batch')))
  ;; TODO:
  #_(defmethod add-to-regen-batch ::door [door-block batch]
    (assoc (assoc batch (get-rest)))))


(define-dependency [vine vine] (fn [v1 v2]
                                 (cond*
                                  [(and (below? v1 v2)
                                        )])))

(define-dependency [vine ::solid]
  (fn [v b]
    (comment "If there is a vine above, ")
                                    ))


(define-dependency [::gravity-bound ::load-bearing] above?)



#_(defmethod regen-batch-search dispatch-on-block-type)
#_(defmethod-on-block-type regen-batch-search vine [v batch]
    (if (and (above? ::vine v)
           (not (find batch (get-block-vector (get-block-above v)))))
    (map (fn [direction]
           (if (and (. isOnFace (get-data v))
                    (add v (get-mod-vector direction)))))
         [BlockFace/NORTH, BlockFace/SOUTH, BlockFace/EAST, BlockFace/WEST]))
  (cond*
   [()]))

#_(defmethod regen-batch-search :default [b batch]
;; TODO:
  )

#_(def +complex-block-types+ #{::bed, ::door, ::double-plant, })


#_(complex-block #(if ))

(defn add-dep [deps block & {:keys [action] :or {action :destroy}}]
  (assert* (map? deps))
  (assert* (instance? Block block))
  (assert* (keyword? action))
  (assoc deps block action))

#_(defn union-deps [& dep-maps]
  "The first takes precedence."
  (assert* (every? map? dep-maps) "All args to (union-deps) should be map types: %s" (map type dep-maps))
  (satisfying map? (apply merge dep-maps)))
#_(defn union-deps [& dep-maps]
  (assert* (every? map? dep-maps) "All args to (union-deps) should be map types: %s" (map type dep-maps))
  (satisfying map?
              (reduce (fn [acc deps-map]
                        (reduce (fn [acc' deps]
                                  )
                                acc deps-map))
                      {} dep-maps)))

(defn union-deps [& dep-maps]
  (assert* (every? map? dep-maps) "All args to (union-deps) should be map types: %s" (map type dep-maps))
  (satisfying map?
              (let [out (apply merge-with (fn [t1 t2]
                                            (if (or (= :destroy t1)
                                                    (= :destroy t2))
                                              :destroy
                                              :restore))
                               dep-maps)]
;                (when (some #(= (get-type %) Material/AIR) (keys out)) (debug-println "Caught AIR."))
                out)))

(defn diff-deps [map & rest-of-maps]
  (assert* (map? map))
  (assert* (every? map? rest-of-maps))
  (satisfying map? (apply dissoc map (mapcat keys rest-of-maps))))

(defn gen-deps [coll & {:keys [action] :or {action :destroy}}]
  "'coll' is a collection of Blocks."
  (when-not (coll? coll) (throw (IllegalArgumentException. (format "Arg %s (gen-deps) must be a collection." coll))))
  (satisfying map?
              (if (map? coll) coll
                  (zipmap coll (repeat action)))))


(defn basic-deps [& [action? & blocks :as all-blocks]]
  "If the first arg is a keyword, it's processed as an 'action' directive."
  (if-let [action (when (keyword? action?) action?)]
    (zipmap blocks (repeat action))
    (zipmap all-blocks (repeat :destroy))))

(defn empty-deps []
  {})

(defn dep-map? [obj]
  (map? obj))


(defn ensure-orig-deps [initial-coll]
  (let [initial-deps (satisfying map?
                                 (if (map? initial-coll) initial-coll
                                      ;; These starting blocks are the ones passed to R.E., so theirs is a "strong" dependency:
                                     (zipmap initial-coll (repeat :destroy))))]
    initial-deps))

;;;; Forward dependencies:
;; id=fwd

(defn attached-dependents [b]
  (satisfying map?
              (let [bs (filter #(= b (get-attached-block %)) (neighboring-blocks b))]
                (gen-deps bs))))

(def ^{:doc "Referred to by (support-dependency) and (support-rev-dep)."}
  +materials-needing-support+
  (set (map #(find-block-main-type-name % :error true)
        '#{sapling,
           cactus,
           bed-block,
           rail, powered-rail, detector-rail, activator-rail,
           long-grass,
           multi-flower,
           brown-mushroom, red-mushroom,
;           torch,
           fire,
           redstone-wire,
           wheat,
;           signpost, wall-sign,
           wooden-door-block, iron-door-block,
;           lever,
           stone-pressure-plate, wood-pressure-plate, iron-pressure-plate, gold-pressure-plate,
           redstone-ore, glowing-redstone-ore,
;           redstone-torch-on, redstone-torch-off,
;           stone-button, wood-button
           sugarcane,
           cake,
           redstone-repeater-on, redstone-repeater-off,
;           trapdoor,
           pumpkin-stem, melon-stem,
;           gate,
           lilypad,
           nether-wart,
;           enchantment-table, ;; TODO: ?
           ;brewing-stand,;; TODO: ?
           ;cauldron, ;; TODO: ?
           flowerpot,
           carrot, potato,
           ;mob-head,
;           anvil, ;; TODO: ?
           redstone-comparator-off, redstone-comparator-on,
;           daylight-detector,
           carpet,
           double-plant,
           tripwire})))

;(defprotocol )

(defrecord DependentBlock [block action])

(defrecord VineDependentBlock [block faces])

(defn adjacent-directions []
  [BlockFace/NORTH, BlockFace/SOUTH, BlockFace/EAST, BlockFace/WEST])

;(extend-type VineBlock DependentBlock [faces])

(defn vine-fwd-dependency [b]
  (assert* (isa? (class (block b)) :cljengine.mc/block-state-image))
  (let [typ (get-type b)]
    (cond*
     [(solid? b) ; TODO: "vine-supporting" material
      (let [dirs (adjacent-directions)
            adj-blocks (map #(add (block b) %) dirs)
            adj-vines (filter #(= (get-type %) Material/VINE) adj-blocks)]
        (map (fn [dir b']
               (VineDependencyBlock. b' {(.getFacing dir) #{:same}}))
             dirs
             adj-vines))
                                        ;(= typ Material/)
;FIXME:
      ])))

(def vine-rev-dependency [b]
  (assert* (isa? (class (block b)) :cljengine.mc/block-state-image))
  )

;; TODO: Vine
;; TODO: Tripwire
;; TODO: Portals

(defn complex-block-dependency [b]
  (assert* (isa? (class b) :cljengine.mc/block-state-image))
  (satisfying map?
              (let [type (get-type b)]
                (cond*
                 [(or (= type Material/WOODEN_DOOR)
                      (= type Material/IRON_DOOR))
                  (basic-deps b (get-rest-of-door b))]
                 [(= type Material/BED_BLOCK)
                  (basic-deps b (get-rest-of-bed b))]
                 [(some #(= % type)
                        [Material/PISTON_EXTENSION
                         Material/PISTON_STICKY_BASE
                         Material/PISTON_BASE])
                  (if-let [rest-of-p (get-rest-of-piston b)]
                    (basic-deps b rest-of-p)
                    (basic-deps b))]
                 [:else (basic-deps b)]))))

(defn supported-dependency [b]
  (assert* (isa? (class b) :cljengine.mc/block-state-image))
  (satisfying map?
              (let [b-above (get-block-above b)
                    t (get-type b-above)
                    nm (find-block-main-type-name t :error true)]
                (if (contains? +materials-needing-support+ nm)
                  (basic-deps b-above)
                  (empty-deps)))))

(defn gravitational-dependency [b]
  (assert* (isa? (class b) :cljengine.mc/block-state-image))
  (satisfying map?
              (let [b-above (get-block-above b)
                    t (get-type b-above)
                    nm (find-block-main-type-name t :error true)]
                (if (isa? nm ::gravity-bound)
                  (basic-deps :restore b-above)
                  (empty-deps)))))

(defn vine-dependency [b]
  (assert* (isa? (class b) :cljengine.mc/block-state-image))
  (cond*
   [(= (get-type b) Material/VINE)
    ]
   [:else (empty-deps)]))

(defn tripwire-dependency [b]
  (assert* (isa? (class b) :cljengine.mc/block-state-image))
  (satisfying dep-map?
              (let [typ (get-type b)]
                (cond*
                 [(= typ Material/TRIPWIRE_HOOK)
                  (if-not (.. b getData isActivated)
                    (empty-deps)
                    (basic-deps (get-block-at (add b (get-facing b)))))]
                 [(= typ Material/TRIPWIRE)
                  (if-not (.. b getData isActivated)
                    (empty-deps)
                    (gen-deps (filter #(and (= Material/TRIPWIRE (get-type %))
                                            (. (get-state %) isActive %))
                             (adjacent-blocks b))))]))))

#_(defn vine-dependency [b]
    (assert* (isa? (class b) :cljengine.mc/block-state-image))
    (satisfying dep-map?
              (let [typ (get-type b)]
                (cond*
                 [(= typ Material/TRIPWIRE_HOOK)
                  ]))))

(def +forward-dependencies+
  #{attached-dependents,
    complex-block-dependency,
    supported-dependency,
    gravitational-dependency
    tripwire-dependency})


(defn find-fwd-deps [b & {:keys [blocks]}]
  ":blocks parm defaults to 'the set containing b.'  Note that 'blocks' should always contain b."
  (let [s (or blocks (basic-deps b))]
    (assert* (isa? (class b) :cljengine.mc/block-state-image))
    (assert* (coll? s))
    (assert* (contains? s b))
    (satisfying map?
               (let [new (satisfying map? (apply union-deps ((apply juxt +forward-dependencies+) b)))
                     s' (satisfying map? (union-deps s new))]
                 (if (= s s') s'
                     (reduce #(find-fwd-deps (key %2) :blocks %1) s' new))))))

(defn find-all-fwd-deps [input-set]
  (let [input-set (satisfying map? (ensure-orig-deps input-set))]
    (reduce (fn [st next-block-entry]
              (assert* (isa? (class next-block-entry) :cljengine.mc/block-state-image) "Lambda failure.")
              (find-fwd-deps (key next-block-entry) :blocks st)) input-set input-set)))




;;;; Reverse dependency search.  These blocks need to be regenerated (if not present) at restoration-time, but they do not need to be destroyed!  Therefore they get tagged ':restore' instead of ':destroy'.
;; id=rev

(defn grav-rev-dep [b]
  (satisfying map?
              (if (isa? (find-block-main-type-name (get-type b)) ::gravity-bound)
                (basic-deps :restore (get-block-below b))
                (empty-deps))))

(defn att-rev-dep [b]
  (assert* (isa? (class b) :cljengine.mc/block-state-image))
  (satisfying map?
              (if (attachable? b)
                (basic-deps :restore (get-attached-block b))
                (empty-deps))))

(defn complex-block-rev-dep [b]
  "\"Softens\" the dependencies."
  (zipmap (keys (satisfying map? (complex-block-dependency b)))
          (repeat :restore)))

(defn support-rev-dep [b]
  (satisfying map?
              (let [t (get-type b)
                    nm (find-block-main-type-name t)]
                (if (contains? +materials-needing-support+ nm)
                  (basic-deps :restore (get-block-below b))
                  (empty-deps)))))

(defn tripwire-rev-dep [b
])

(def +reverse-dependencies+
  #{att-rev-dep,
;    complex-block-rev-dep,
    support-rev-dep,
    grav-rev-dep,
    tripwire-dependency})

(defn find-rev-deps [b & {:keys [blocks]}]
  (let [s (satisfying map? (or blocks (basic-deps b)))]
    (debug-println "In (find-rev-deps) with" b s)
    (when (empty? s) (throw (IllegalArgumentException. "Mustn't pass empty set to (find-rev-deps).")))
    (assert* (isa? (class b) :cljengine.mc/block-state-image) "Failure in (find-rev-deps) for arg %s of type %s which should be a ::block-state-image descendant but is not." b (class b))
    (satisfying map?
                (let [new (apply union-deps ((apply juxt +reverse-dependencies+) b))
                      s' (union-deps s new)]
                  s'))))

(defn find-rev-deps-recursively [b & {:keys [blocks]}]
  (let [s (satisfying map? (or blocks (basic-deps b)))]
;    (debug-println "In (find-rev-deps-recursively) with" b s)
    (when (empty? s) (throw (IllegalArgumentException. "Mustn't pass empty set to (find-rev-deps-recursively).")))
    (assert* (isa? (class b) :cljengine.mc/block-state-image) "Failure in (find-rev-deps-recursively) for arg %s of type %s which should be a ::block-state-image descendant but is not." b (class b))
    (satisfying map?
                (let [new (satisfying map? (apply union-deps ((apply juxt +reverse-dependencies+) b)))
                      s' (satisfying map? (union-deps s new))]
                  (if (= s s') s'
                      (do
                        (assert* (not (empty? new)))
                        (reduce #(find-rev-deps-recursively (key %2) :blocks %1) s' new)))))))


(defn find-all-rev-deps [initial-deps]
  (when-not (coll? initial-deps) (throw (IllegalArgumentException. "Arg to (find-all-rev-deps) must be a collection.")))
  (satisfying map?
              (let [initial-deps (ensure-orig-deps initial-deps)
                    output-deps (satisfying map? (reduce #(find-rev-deps-recursively (key %2) :blocks %1) initial-deps initial-deps))]
                ;(debug-println "Here.")
                ;(debug-println initial-deps)
                ;(debug-println output-deps)
                (assert* (every? #(= % :restore) (vals (diff-deps output-deps initial-deps))))
                output-deps)))



#_(defn find-all-deps [input-set]
  (when-not (coll? input-set) (throw (IllegalArgumentException. "Arg to (find-all-rev-deps) must be a collection.")))
  (find-all-block-deps (find-all-rev-deps input-set)))

;;; Modification: It's better to do forward searches first.
(defn find-all-deps [input-set]
  (when-not (coll? input-set) (throw (IllegalArgumentException. "Arg to (find-all-deps) must be a collection.")))
  (find-all-rev-deps (find-all-fwd-deps (ensure-orig-deps input-set))))
