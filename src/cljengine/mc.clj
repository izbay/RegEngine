; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode) -*-
;;;; Load this first of the Minecraft Clojure files.

(ns cljengine.mc
  "Testing Clojure in Minecraft."
;  (:require clojure.core)
  (:use (clojure core repl pprint reflect set)
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
  ;; Add some Enums...
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

(create-ns 'mc.block); Fwd dec
(alias 'bl 'mc.block)

(defonce player nil)

(defonce ^{:dynamic true
           :doc "To use in preference to clj-plugin."}
  *plugin*
  @clj-plugin)

;;;; **** <util.mc>
;;; TODO: Move utils to util.clj.

;;;; TODO: Turn debug-* funcs into macros for compile-time removability.
(defonce ^:dynamic *debug-print* true); TODO: Is dynamic the right sort?
(defn debug-println [& forms]
  "If *debug-print* is set, passes 'forms' to (println)."
  (when *debug-print* (apply #'println forms)))

(defn debug-msg [& forms]
  "If *debug-print* is set, passes 'forms' through (format) to (send-msg) to all players.
TODO: Specify which player."
  (when *debug-print* (doseq [pc (online-players)]
                        (send-msg pc (apply format forms)))))

(defn debug-announce [& forms]
  "Invokes both (debug-println) and (debug-msg) on 'forms', which are run through (format).  In turn, they output iff *debug-print* is true."
  (let [fmt-string (apply format forms)]
    (debug-println fmt-string)
    (debug-msg fmt-string)))

;;; id=math

(defn mean [coll]
  "Statistical average."
  (/ (apply + coll)
     (count coll)))

(defn mode [coll]
  "Statistical mode."
  (key (first (sort #(compare (val %1) (val %2)) (frequencies coll)))))

;; Absolute value wrapper:
;(def abs (memfn Math/abs)); TODO: This wasn't working.
(definline abs [num]
  `(Math/abs ~num))

(defn ns-syms [ns]
  "Wrapper.  Returns & prints (unqualified) names of symbols in namespace 'ns'.  Good for poking around--you can use Emacs's Cider smart completion to fill out a valid ns name."
  (let [keys (sort (keys (ns-interns ns)))]
    (pprint keys)
    keys))

;;;; **** </util.mc>

(defonce ^:dynamic *do-not-compile-assertions* false)
(defmacro assert* [expr & forms]
  "Modification to (assert) allowing static and dynamic control.  Setting *do-not-compile-assertions* at compile-time will have the same effect as compiling a regular (assert)ion with *assert* unset: removal.  However, if *do-not-compile-assertions* is false, then *assert* can be used dynamically to enable/disable assertion evaluation, at the slight cost of a boolean test.
Note that (assert*) uses an implicit (format).
3/3/14 update: Now sends a message to players if they're online!"
  (assert (var? #'*do-not-compile-assertions*))
  (if *do-not-compile-assertions*
    '(do)
    (let [rest-forms forms
          fmt-form (when rest-forms `(format ~@rest-forms))]
;      (debug-println rest-forms)
;      (debug-println fmt-form)
      `(if *assert*
         (try
           (assert ~expr ~@(if fmt-form [fmt-form] []))
           (catch AssertionError ass#; TODO: Assertion Exception type?
;             (debug-println "Caught exception!")
             (when-let [players# (online-players)]
               (doseq [player# players#]
                 (send-msg player# ~(if fmt-form `(format "Assert failed: %s" ~fmt-form) "Assertion failed!"))))
;             (debug-println "Re-throwing exception!")
             (throw ass#)))
         ;; If assertions are run-time disabled, do nada.
         (do)))))

(defmacro assert-seq [varname binding-form & assert-forms]
  "Passes 'assert-forms' to (assert*) with 'binding' in place via (doseq).  The advantage of doing a raw iteration over assertions is that this form will be compiled out if *do-not-compile-assertions* is set."
  (if *do-not-compile-assertions*
    '(do)
    `(doseq [~varname ~binding-form]
       (assert* ~@assert-forms))))

(defmacro assert-every? [func-form seq & assert-forms]
  "Returns nil on success."
  (if *do-not-compile-assertions*
    '(do)
    `(let [func# ~func-form
           seq# ~seq]
       (doseq [var# seq#]
         (assert* (func# var#) ~@(or assert-forms
                                     `[(format "(%s %s)." '~func-form 'var#)]))))))

(defmacro cond* [fst-clause & rest-of-clauses]
  "I have *had* it with Clojure's (cond).  The Scheme style seems utterly pointless; you could simply do with a variadic (if), like Shen.
This version accordingly requires that separate test clauses be enclosed as sequences.  Vectors, specifically, with '[]'; when in Clojure, do as Clojurians do."
  (let [[fst-test & fst-body] fst-clause]
    (letfn [(cond-recur
              ([] [])
              ([clause & clauses]
                 (let [[test & body] clause]
;                   (println "Got this far.")
                   `(~test (do ~@(or body []))
                           ~@(apply cond-recur clauses)))))]
 ;     (println rest-of-clauses)
      `(cond
         ~fst-test (do ~@(or fst-body []))
         ~@(if (nil? rest-of-clauses) []
               (apply cond-recur rest-of-clauses))))))

(def ^:dynamic *econd-error* nil)

(defmacro econd* [& all]
  "TODO: Need a better way to throw."
  `(cond* ~@all
          [:else
           ;(def *econd-error* '~(first all))
;\n impertinent form has been stored in '*econd-error*'.
          ;; TODO: RuntimeException?
           (assert* false (format "(econd*) fall-through: %s"
                                 (str '(econd* ~@all))))]))

(defn pluralizes? [n]
  "For use in format strings: my fixation on proper pluralization."
  (if (== n 1) "" "s"))

(defn listify [arg]
  "Utility; converts eigenvalues to lists."
  (if (list? arg) arg (list arg)))
(defn print-classpath []
  "Learned from http://pupeno.com/2008/11/26/printing-the-class-path-in-clojure/."
  (println (seq (.getURLs (java.lang.ClassLoader/getSystemClassLoader)))))

#_(defmacro typecase* [test-form & clauses]
  "Like CL's TYPECASE macro.  It has the '*' after the name to indicate that, like (econd*), it expects its clauses to be conses."
  `(let [test-form# ~test-form]
     (cond*
      ~@(let [xformed-clauses (map (fn [clause]
                                   (let [[type & body] clause]
                                     `[(instance? ~type ~test-form#) ~@body]))
                                 clauses)]
          xformed-clauses))))

#_(defmacro etypecase* [test-form & clauses]
  "Hopefully works like ETYPECASE."
  (let [typecase-form
        (macroexpand-1 `(typecase* ~test-form ~@clauses))
        cond*-form (concat typecase-form
                           `([:else (assert* false (format "(etypecase*) fall-through: %s failed to match any of %s."
                                                           ~test-form (map first '~clauses)))]))]
    cond*-form))




(defn ticks-to-seconds [t]
  (float (/ t 20)))

#_(defn seconds-to-ticks [s;;;;;;;; & {:keys [coerce] :or {coerce :truncate}}
                        ]
  (long (* s 20)))

;;;; id=world
(defn get-current-world []
  "Hopefully.  Returns a World instance."
  (first (worlds)))

(defn get-seed []
  "World seed."
  (.getSeed (get-current-world)))

(defn get-block-at
  ([^Number x ^Number y ^Number z]
     "Gets blocks at vector coords."
     (get-block-at (new Vector x y z)))
  ([pos]
     "Wrapper that calls (.getBlockAt) in the first world found.  The 'pos' arg may be a Vector or a Location."
     (cond*
      ((instance? Location pos) (.getBlockAt (get-current-world) pos))
      ((instance? Vector pos)
             (get-block-at (.toLocation pos (get-current-world))))
      ((instance? Block pos) pos)
      (:else (assert false (format "%s is not a valid type for (get-block-at)." (type pos)))))))


(defn get-first-player []
  "nil if no one is online."
  (first (online-players)))

(defn get-player []
  "Like (get-first-player), but stores the value in the 'player' globvar as well."
  (def player (get-first-player))
  player)

(definline get-first-player* []
  `(get-player))

;;;; id=pluginmanager
(defn get-plugin [^String name]
  "Wrapper."
  (.getPlugin (plugin-manager) name))

#_(defn get-plugins []
  "Not terribly useful unless you can print out a plugin list."
  (.getPlugins (plugin-manager)))



;;;; id=vector
(defn ^Vector get-vector [obj]
  "Wrapper.  Overloaded for type; returns a fresh vector.
TODO: A more specific exception type."
  ;; TODO: I would prefer a CL (cond).
  (cond*
   [(instance? Vector obj) (.clone obj)]
   [(instance? Location obj) (.toVector obj)]
        ;; Sadly, trying to get too clever only hurts clarity:
        ;((some-fn (partial instance? Block) (partial instance? Entity)) obj)
   [(or (instance? Block obj)
        (instance? BlockState obj)
        (instance? Entity obj))
    (get-vector (.getLocation obj))]
   [:else (throw
           (new RuntimeException (format "(get-vector) failed on unsupported type %s." (type obj))))]))



(defn ^BlockVector get-block-vector [obj]
  "Like (get-vector), but it returns a BlockVector with the coordinates *explicitly* truncated to integers.
Always returns a fresh vector."
  (let [vec (get-vector obj)]
    (BlockVector. (long (.getX vec))
                  (long (.getY vec))
                  (long (.getZ vec)) )))

(defn compare-vectors [^Vector v1 ^Vector v2]
  "(compare) for org.bukkit.util/Vector; may be useful for sorting.  Axial precedence in ordering is y>z>x."
  (let [y (compare (.getY v1) (.getY v2))]
    (if-not (zero? y) y
            (let [z (compare (.getZ v1) (.getZ v2))]
              (if-not (zero? z) z
                      (compare (.getX v1) (.getX v2)))))))

;;;; (compare-vector) tests:
(when-not *do-not-compile-assertions*
  (assert (== 0 (compare-vectors (new Vector 2 4 6) (new Vector 2 4 6))))
  (assert (== 1 (compare-vectors (new Vector 2 4 6) (new Vector 2 3 6))))
  (assert (== -1 (compare-vectors (new Vector 2 3 6) (new Vector 2 4 6))))
  (assert (== 1 (compare-vectors (new Vector 2 4 7) (new Vector 2 4 6))))
  (assert (== -1 (compare-vectors (new Vector 2 4 6) (new Vector 2 4 7))))
  (assert (== 1 (compare-vectors (new Vector 3 4 7) (new Vector 2 4 7))))
  (assert (== -1 (compare-vectors (new Vector 1 4 7) (new Vector 2 4 7)))))


(defn format-vector [^Vector vec]
  (format "(%s,%s,%s)"
           (.getX vec) (.getY vec) (.getZ vec)))

(defn format-position [pos]
  "'pos' should be a Vector or Location."
  (format-vector (if (instance? Vector pos) pos
                     (.toVector pos))))

;;; id=wrappers
(defn get-location [obj]
  "Wrapper."
  (cond*
   ((instance? Vector obj) (.toLocation obj (get-current-world)))
   (:else (.getLocation obj))))

#_(defn get-location [obj]
  "Wrapper."
  (.getLocation obj))

(defn add [pos x y z]
  "Wrapper for Location.add() that uses a fresh Location.
TODO: Overload for type."
  (cond*
   [(instance? Location pos) (.add (.clone pos) x y z)]
   [:else (add (get-location pos) x y z)]))

;;;; id=yaw
(defn get-yaw [obj]
  (cond*
   [(instance? Location obj)
    (.getYaw obj)]
   [:else (get-yaw (get-location obj))]))

(defn set-yaw [obj yaw]
  "TODO: This seems to have no effect."
  (cond*
   [(instance? Location obj)
    (.setYaw obj yaw)]
   [:else (set-yaw (get-location obj) yaw)]))

(defn get-state [block]
  "Wrapper.  'block' may be a Block, a BlockVector, or whatever, as long it can be associated with a BlockState.
NB: If passed a BlockState object, the result, the same object, may be out-of-touch with the world, as BlockStates can be."
  (cond*
   [(instance? BlockState block) (.clone block)]
   [(instance? Block block) (.getState block)]
   [:else (get-state (get-block-at (get-vector block)))]))

(defn get-type [obj]
  "Wrapper for Block.getType() that returns a Material/* enum.  Not to be confused with Clojure's own (type)."
  (cond*
   [(or (instance? Block obj)
        (instance? BlockState obj))
    (.getType obj)]
   [:else (get-type (get-block-at (get-block-vector obj)))]))

(defn set-type [block mat]
  "Wrapper for Block.setType()."
  (.setType block mat))

(defn force-update [^BlockState state]
  "Wrapper for (.update state true)."
  (. state BlockState/update true))

(defn get-players-block [player]
  "Returns the block that 'player' is standing on.  TODO: Should do an Entity.isOnGround() check?"
  (get-block-at (.add (.getLocation player)
                      0 -1 0)))

(defn destroy-players-block [^Player player]
  "Knocks the block out from under player's feet."
  (if-let [block (get-players-block player)]
    (do
      (.setType block Material/AIR)
      true)))

; (defn destroy-region [pos|block-1 pos|block-2] )

;(clojure.core/refer 'cljminecraft.core)
(defn spawn-creeper [loc]
  (let [player (get-first-player)
        world (.getWorld player)
        loc (.getLocation player)]
    (cljminecraft.player/send-msg player "Trying to spawn a creeper.")
    (.spawnEntity world loc EntityType/CREEPER)))

(defn get-target-block [entity]
  "Returns the block a player is \"looking at.\"  Attempting to write a replacement for the deprecated Entity.getTargetBlock()."
  (let [bit (new BlockIterator entity 20)]
    (loop []
      (if (.hasNext bit)
        (let [block (.next bit)]
          (if (= (.getType block) Material/AIR)
            (recur)
            block))
        nil))))


#_ (defn light-TNT []
  ;; TODO: Make sure the command-user is a player!
  (if (instance? sender Player)
    (do)
    (warn "That has to be called by a player!")))


#_(defn foo-handler [_]
     (cljminecraft.player/send-msg (get-first-player) "Trying to spawn a creeper.")
     {:msg "Handling!"})


;; Called on a player, it sets the 'flying' flag.
(defn fly
  ([] (fly (get-first-player) true))
  ([player] (fly player true))
  ([player bool] (.setFlying player bool)))

(defn make-platform
  ([player] (make-platform player 20))
  ([player size]
     "Makes a square stone platform under the player."
     (assert (instance? Player player))
     (let [center (.getLocation (get-players-block player))
           -half (- (int (/ size 2)))
           starting-point (.add (.clone center) -half 0 -half)]
       (dotimes [x size]
         (dotimes [z size]
           (let [block-pos (.add (.clone starting-point) x 0 z)
                 block (get-block-at block-pos)]
             ;; Debug messages:
             (comment (send-msg player "Making block at (%s,%s,%s)."
                                (.getX block-pos) (.getY block-pos) (.getZ block-pos)))
             (comment (println (format  "Making block at (%s,%s,%s)."
                                (.getX block-pos) (.getY block-pos) (.getZ block-pos))))
             (.setType block Material/STONE)
             (assert (= (.getType block)
                        Material/STONE))))))
     true))

(defn make-wall [^Location starting-loc length height & {:keys [inc mat]
                                               :or [^Vector inc (new BlockVector 0 0 1)
                                                    mat Material/OBSIDIAN]}]
  "Still needs kinks worked out, but the idea is to generate a wall 'length' by 'height'."
  ;(assert (instance? Location starting-loc))
  ;(assert (instance? Vector inc))
  {:pre [(pos? length)
         (pos? height)
         (instance? org.bukkit.Material mat)]}
  ;(assert (pos? length))
  ;(assert (pos? height))
  ;(assert (instance? org.bukkit.Material mat))
  (let [start-vec (.clone (.toVector starting-loc))]
    (dotimes [i length]
      (dotimes [j height]
        (let [vec (.add
                   (.add (.multiply (.clone inc) i)
                         start-vec)
                   (new Vector 0 j 0))
              world (get-current-world)
              loc (.toLocation vec world)
              block (get-block-at loc)]
          (assert (instance? Block block))
          (comment (println (format "Making block at (%s,%s,%s)."
                            (.getX vec) (.getY vec) (.getZ vec))))
          (.setType block mat)
          (assert (= (.getType block) mat))))))
  true)

(defn level-area
  ([^Player player] (level-area player 30))
  ([^Player player ^long sz]
     "Eliminates blocks in a cube above and around the player's location.  'sz' is a side of the cube.
TODO: Add a way to filter types of blocks."
     (let [world (get-current-world)
           half (/ sz 2)
           center-vec (.. player getLocation toVector)
           corner-vec (.subtract (.clone center-vec)
                                 (new BlockVector half 0 half))]
       (dotimes [x sz]
         (dotimes [z sz]
           (dotimes [y sz]
             (let [target-vec (.add (.clone corner-vec)
                                    (new BlockVector x y z))
                   target-loc (.toLocation target-vec world)
                   target-block (get-block-at target-loc)]
               (.setType target-block Material/AIR)
               (assert (= (.getType target-block) Material/AIR)))))))
     true))


(defn unregister-all
  ([] (unregister-all *plugin*))
  ([plugin]
     "Removes all event listeners declared by 'plugin', which defaults to value stored in global *plugin*.
TODO: reset registered-events."
     (assert (instance? Plugin plugin))
     (org.bukkit.event.HandlerList/unregisterAll plugin)))

(defn copy-block [block times direction]
  "Makes <times - 1> copies of 'block'; 'direction' should be :x, :y, or :z.  Whether the negative or positive direction is used is determined by the sign of "
  (assert (instance? Block block))
  (let [mat (.getType block)
        world (get-current-world)
        unit (if (neg? times) -1 1)
        unit-vec (cond (= direction :x) (new BlockVector unit 0 0)
                       (= direction :y) (new BlockVector 0 unit 0)
                       (= direction :z) (new BlockVector 0 0 unit))]
;    (println "Vector to add: " (format-vector unit-vec))
    (dotimes [i (dec (Math/abs times))]
      (let [new-vec (.add (.. block getLocation toVector)
                          (.multiply (.clone unit-vec) (inc i)))
            new-block (get-block-at (.toLocation new-vec world))]
        (.setType new-block mat)
        ;(println "Making a block at " (format-vector new-vec))
        (assert (= (.getType new-block) mat))))
    true))

(defn copy-targeted-block [^Player player ^long times direction]
  ;(assert (instance? Player player))
  (copy-block (get-target-block player) times direction))

(defn glow [pos]
  (.setType (get-block-at pos) Material/GLOWSTONE)
  (assert (= (.getType (get-block-at pos))
             Material/GLOWSTONE))
  pos)

(defn get-vectors-bounding-region [coll]
  "TODO: This could be done in one pass."
  (let [max-x (apply max (map (comp (memfn getX) get-vector) coll))
        max-y (apply max (map (comp (memfn getY) get-vector) coll))
        max-z (apply max (map (comp (memfn getZ) get-vector) coll))
        min-x (apply min (map (comp (memfn getX) get-vector) coll))
        min-y (apply min (map (comp (memfn getY) get-vector) coll))
        min-z (apply min (map (comp (memfn getZ) get-vector) coll))]
    [(new Vector min-x min-y min-z) (new Vector max-x max-y max-z)]))

;;;; Wrapper:
(defmacro store-state-of-first-players-targeted-block [varname]
  `(let [state# (get-state (get-target-block (get-first-player)))]
     (def ~varname state#)
     (get-type state#)))

;; TODO: Clojure lazy sequence?
(defn gen-region-vectors [start-corner end-corner]
    "Returns a linked list of all Vectors in the enclosed area, from bottom to top."
    (let [start-corner (get-block-vector start-corner); shadowing
          end-corner (get-block-vector end-corner)
          [x-min x-max] ((juxt min max)
                         (.getX start-corner) (.getX end-corner))
          [y-min y-max] (apply (juxt min max)
                               (map (memfn getY) [start-corner end-corner]))
          [z-min z-max] ((juxt min max)
                         (.getZ start-corner) (.getZ end-corner))]
      ;; Pretty ugly, but reasonable for Java translation:
      (with-local-vars [vector-list ()]
        ;; The loop goes from bottom to top, stepping through x-values before z-values.
        ;; I will opine that the LOOP facility does this more elegantly.
        (doseq [y (range y-min (inc y-max))]; for(int i=y-min; i<=y-max; ++i)...
          (doseq [z (range z-min (inc z-max))]
            (doseq [x (range x-min (inc x-max))]
              (var-set vector-list (cons (new BlockVector x y z)
                                         @vector-list)))))
        (reverse @vector-list))))


;;;; id=block
(defn gen-block-region [^Vector start-corner
                        ^Vector end-corner]
  "Returns a list of blocks."
  (map get-block-at (gen-region-vectors start-corner end-corner)))


(defn effect [location effect-id data & [radius]]
  "Edited from cljminecraft's world.clj.  The 'effect-id' should be either a Java Enum or a keyword as in 'effects'."
  (let [effect (if (instance? Effect effect-id) effect-id
                   (get effects effect-id))]
    (if radius
      (.playEffect (.getWorld location) location effect data radius)
      (.playEffect (.getWorld location) location effect data))))

(defn air? [block]
  "Returns true if 'block' is made of air or represents a block made of air.  If called with an arg that does not represent a block, returns nil."
  (if (or (instance? Block block)
          (instance? BlockState block))
    (= (.getType block) Material/AIR)
    nil))

(defn solid? [block]
  "True if the block specified, or its material, is classified as 'solid'.  The game knows."
  (econd*
   [(instance? Material block) (.isSolid block)]
   [(or (instance? Block block)
        (instance? BlockState block)) (.isSolid (.getType block))]))

(defn block-pos-eq? [obj1 obj2]
  "True if the positions of the given game objects are equal up to the nearest block.
TODO: Is this the best way to do this?"
;  (apply == (map (comp (memfn hashCode) (memfn toBlockVector) get-vector) [obj1 obj2]))
  (let [v1 (get-block-vector obj1)
        v2 (get-block-vector obj2)]
    (every? #(apply == (map (comp long %) [v1 v2]))
            [(memfn getX)
             (memfn getY)
             (memfn getZ)])))

(defn get-block-below [pos]
  (get-block-at (add (get-block-vector pos) 0 -1 0)))

(defn get-block-above [pos]
  (get-block-at (add (get-block-vector pos) 0 1 0)))

(defn block-state-verisimilitude? [^BlockState state]
  "Returns true iff 'state' is the same type as the block at its coordinates, i.e., if it's a quasi-accurate reflection of current state."
  (= (get-type state)
     (get-type (get-block-at (get-vector state)))))

(defn get-player-space [^Player pc]
  "Returns a Vector duple giving the positions of a player's upper and lower coords.  TODO: BlockVectors."
  (let [pc-pos (get-block-vector pc)]
    [pc-pos (add pc-pos 0 1 0)]))


(defn player-block-collision? [^Player pc block & {:keys [solid]}]
  "Log. true if 'pc' is standing or has his head in a block, else nil.
'block' should be a Block or BlockState.
When 'solid' is log. true, the block must also pass a (solid?) test."
  (assert* (instance? Player pc))
  (assert* (or (instance? Block block)
               (instance? BlockState block)))
;  (debug-println solid)
  (when (or (not solid) (solid? block))
    (some #(block-pos-eq? block %) (get-player-space pc))))


#_(defn test-some-shit []
;  (compile 'cljengine.re)
  (new cljengine.re.ClojureRegen)
)

;;;; id=time
(defn get-full-time []
  "Wrapper."
  (. (get-current-world) getFullTime))(print-classpath)

(println "Loaded mc.clj successfully.")
