; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode) -*-
;;;; Load this first of the Minecraft Clojure files.

(comment
  ())

(ns cljengine.mc
  "Testing Clojure in Minecraft."
  (:refer-clojure :exclude [map])
                                        ;  (:require clojure.core)
  (:use (clojure [core :exclude [map]] repl pprint reflect set)) ;[string :exclude [join]] )
  (:use clojure.java.io)
  (:use
   (cljminecraft core
                          entity
                          [bukkit :exclude [repeated-task
                                            cancel-task]]
                          events
                          commands
                          logging
                          util
                          [world :exclude [effect]] ; (effect) has a simple bug.
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
                    (org.bukkit.material MaterialData
                                         Attachable)
                    (org.bukkit.metadata Metadatable
                                         FixedMetadataValue
                                         LazyMetadataValue)
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
                    (org.bukkit.util BlockIterator)
                    )
  ;; Moving to re.clj:
  #_(:import (com.github.izbay.regengine RegEnginePlugin
                                       BlockImage
                                       RegenBatch)
           (com.github.izbay.regengine.block Action
                                             DependingBlock
                                             VineDependingBlock
                                             DependingBlockSet)
           (com.github.izbay.util Util)))




(create-ns 'mc.block); Fwd dec
(alias 'bl 'mc.block)

(defonce player nil)

(defonce ^{:dynamic true
           :doc "To use in preference to clj-plugin."}
  *plugin*
  @clj-plugin)



;;;; id=TYPES
;;; id=::block-state-image, id=block-state-image, id=derive

;;; id=Protocols
#_(defprotocol BukkitLocatable
  "Implements a get-location func."
  (get-location [obj]))

#_(derive BukkitLocatable ::bukkit-locatable)

;(derive ::block-state-image ::block)
#_(do
  (derive Block ::bukkit-locatable)
  (derive BlockState ::bukkit-locatable))

(derive ::block-state-image ::bukkit-locatable)

(do
  (derive Block ::block-state-image)
  (derive BlockState ::block-state-image)
  #_(derive BlockImage ::block-state-image))





#_(extend Block
  BukkitLocatable
  {:get-location (memfn getLocation)})
#_(extend BlockState
  BukkitLocatable
  {:get-location (memfn getLocation)})

;;;; **** <util.mc>
;; id=util
;;; TODO: Move utils to util.clj.

;;;; Limit print length:
(alter-var-root #'clojure.core/*print-length* (constantly 51))

;;;; TODO: Turn debug-* funcs into macros for compile-time removability.
(defonce ^:dynamic *debug-print* true); TODO: Is dynamic the right sort?
(defonce ^:dynamic *debug-print-file* nil)
(defn debug-println [& forms; & {:keys [file]}
                     ]
  "If *debug-print* is set, passes 'forms' to (println)."
  (when *debug-print* (if-not *debug-print-file* (apply #'println forms)
                              (with-open [f (clojure.java.io/writer *debug-print-file* :append true)]
                                (binding [*out* f]
                                  (apply #'println forms))))))

(defonce ^:dynamic *debug-print-2* true)
(defn debug-println-2 [& forms]
  (when *debug-print-2* (apply #'println forms)))

(defn disable-debug-printing []
  "Wrapper."
  (alter-var-root #'*debug-print* (constantly false))
  (alter-var-root #'*debug-print-file* (constantly false))
  (alter-var-root #'*debug-print-2* (constantly (if *debug-print-2* false nil))))

(defn enable-debug-printing []
  "Wrapper.
Always re-enables *debug-print*.  But *debug-print-2* is treated a bit differently--it is only restored if its value is 'false'.  If it is 'nil', it is left that way."
  (alter-var-root #'*debug-print* (constantly true))
  (when (= *debug-print-2* false)
    (alter-var-root #'*debug-print-2* (constantly true))))

;; I did this wrong:
(defmacro with-debug-print-to-file-timed [filename time-limit & body]
  `(try
     (alter-var-root #'*debug-print-file* (constantly ~filename))
     (let [retval#
           (do ~@body)]
       (Thread/sleep ~time-limit)
       retval#
       (finally
         (alter-var-root #'*debug-print-file* (constantly nil)) ; We reset this by nullifying it, not inserting *out*.
         (assert* (nil? *debug-print-file*))
         (debug-println "Unblocking (with-debug-print-to-file-timed).")))))


#_(with-open [f# (writer ~filename :append true)]

     (let [retval#
           (do ~@body)]
       (Thread/sleep ~time-limit)
       (alter-var-root #'*debug-print-file* (constantly nil)); We reset this by nullifying it, not inserting *out*.
       (assert* (nil? *debug-print-file*))
       retval#))

(defn debug-msg [& forms]
  "If *debug-print* is set, passes 'forms' through (format) to (send-msg) to all players.
TODO: Specify which player."
  (when *debug-print* (doseq [pc (online-players)]
                        (send-msg pc (apply format forms)))))

(defn debug-announce [& format-forms]
  "Invokes both (debug-println) and (debug-msg) on 'forms', which are run through (format).  In turn, they output iff *debug-print* is true."
  (let [fmt-string (apply format format-forms)]
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

;; id=assert*
(defonce ^{:dynamic true
           :doc "If set _at compile time_, renders (assert*) forms null.  The same function is performed by the *assert* var for regular (assert)."} *do-not-compile-assertions* false)
(defonce ^{:dynamic true
           :doc "When true, (assert*) failure is broadcast to players' consoles as well as sent to stdout."}
  *msg-failed-assert* true)
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
             (when *msg-failed-assert*
               (when-let [players# (online-players)]
                 (doseq [player# players#]
                   (send-msg player# ~(if fmt-form `(format "Assert() failed: %s" ~fmt-form) "Assert()ion failed!")))))
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

(defmacro illegal-arg-check [form]
  `(when-not ~form (throw (IllegalArgumentException. (format "Illegal argument produced by form '%s.'" '~form)))))

(defmacro the [type-expr expr]
  "Attempted analogue of CL's (the) type-check macro.
NB: Clojure type equivalence is a little... unpredictable.  You might think that something reasonable like '(isa? 5 int)' would be true, but it's not.  Use caution when type-checking primitives."
  `(let [type-val# ~type-expr
         expr-val# ~expr]
    (assert* (or (isa? expr-val# type-val#)
                 (isa? (class expr-val#) type-val#)
                 (when (class? type-val#) (instance? type-val# expr-val#))) "Failure of (the) typecheck: Form '%s' did not match type specification '%s'." '~expr type-val#)
    expr-val#))

(definline the* ^{:doc "Deprecated.  I use the asterisk just because of my own custom of defining a macro with that name in Common Lisp."}
  [type-expr expr]
  `(the ~type-expr ~expr))

(defmacro satisfying [pred-expr test-expr]
  `(let [pred# ~pred-expr
         test-val# ~test-expr]
    (assert* (pred# test-val#) "Failure of (satisfying): Form '%s' did not satisfy '%s'." '~test-expr ~pred-expr)
    test-val#))

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

;;;; id=loop*
(defonce ^:dynamic *continue-loop* (agent true))
(defmacro loop* [binding-pairs & body]
  "Like (loop), but incorporates a way to break an infinite loop: setting var *continue-loop* to logical false."
  `(loop ~binding-pairs
     (econd*
      [@*continue-loop*
       ~@body]
      [(= @*break-loop false)
       (send *continue-loop* (constantly true))]
      [(= @*break-loop nil)])))


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


;(defmacro define-multimethod [name])


(defn ticks-to-seconds [t]
  (float (/ t 20)))

#_(defn seconds-to-ticks [s;;;;;;;; & {:keys [coerce] :or {coerce :truncate}}
                        ]
 (long (* s 20)))

;;;; TODO: This is not sturdy:
(defonce ^:dynamic *world* (agent (first (worlds))))

;;;; id=world
(defn get-current-world []
  "Hopefully.  Returns a World instance."
  @*world*)

(defn get-seed []
  "World seed."
  (.getSeed (get-current-world)))


;;; id=wrappers
#_(defn get-location [obj]
  "Wrapper."
  (cond*
   ((instance? Vector obj) (.toLocation obj (get-current-world)))
   (:else (.getLocation obj))))

(do
  (defmulti get-location class)
  (defmethod get-location ::bukkit-locatable [obj]
    "By deriving various Java classes from :cljengine.mc/bukkit-locatable, we provide access to their getLocation() methods via this multimethod instance."
    (.getLocation obj)))


#_(defn get-location [obj]
  "Wrapper."
  (.getLocation obj))

(do
  (defmulti get-world class)
  (defmethod get-world Location [loc] (.getWorld loc))
  (defmethod get-world ::bukkit-locatable [obj]
    (get-world (get-location obj))))

(defmulti get-block-at (fn
                         ([#^Number x #^Number y #^Number z]
                            (vec (clojure.core/map class [x y z])))
                         ([pos]
                            "Wrapper that calls (.getBlockAt) in the first world found.  The 'pos' arg may be a Vector, a Location, or a supported object that has a location."
                            (class pos))))
(defmethod get-block-at Location [loc] (.getBlock loc))
(defmethod get-block-at Vector [vec] (get-block-at (.toLocation vec (get-current-world))))
(defmethod get-block-at Block [block] block)
(defmethod get-block-at ::bukkit-locatable [im] (get-block-at (get-location im)))
(defmethod get-block-at :default [pos]
  (throw (IllegalArgumentException. (format "%s is not a valid type for (get-block-at)." (type pos)))))

#_(defn get-block-at
  ([^Number x ^Number y ^Number z]
     "Gets blocks at vector coords."
     (get-block-at (new Vector x y z)))
  ([pos]
     (cond*
      ((instance? Location pos) (.getBlockAt (get-current-world) pos))
      ((instance? Vector pos)
             (get-block-at (.toLocation pos (get-current-world))))
      ((instance? Block pos) pos)
      ((instance? BlockState pos) (get-block-at (get-location pos)))
      ((instance? BlockImage pos) (get-block-at (get-location pos)))
      (:else (assert false (format "%s is not a valid type for (get-block-at)." (type pos)))))))

(defn map [func seq & colls]
  "Important.  Shadowing the default (map) func in order to treat strictly any sequence starting with a BlockState (presumably, the whole sequence will be of BlockStates).  I encountered a very unpleasant bug due to Blocks' or BlockStates' changing material between the invocation of the (map) and the evaluation of a lazy sequence."
  (let [new-seq (apply clojure.core/map func seq colls)]
    (if (or (= func get-block-at)
            (and (not (empty? new-seq))
                 (let [fst (first new-seq)]
                   (or (instance? BlockState fst)
                       (instance? Block fst)))))
      (doall new-seq)
      new-seq)))


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
   [(or #_(instance? Block obj)
        #_(instance? BlockState obj)
        #_(instance? BlockImage obj)
        (isa? (class obj) ::block-state-image)
        (instance? Entity obj))
    (get-vector (.getLocation obj))]
   [:else (throw
           (new RuntimeException (format "(get-vector) failed on unsupported type %s." (type obj))))]))

(defn coordinates [obj]
  (when-let [v (get-vector obj)]
    [(.getX v) (.getY v) (.getZ v)]))


#_(defn ^BlockVector get-block-vector [obj]
  "Like (get-vector), but it returns a BlockVector with the coordinates *explicitly* truncated to integers.
Always returns a fresh vector."
  (let [vec (get-vector obj)]
    (BlockVector. (long (.getX vec))
                  (long (.getY vec))
                  (long (.getZ vec)) )))

;; id=get-block-vector
(do
  (defmulti get-block-vector
    "Like (get-vector), but it returns a BlockVector with the coordinates *explicitly* truncated to integers.
Always returns a fresh vector."
    class)
  (defmethod get-block-vector BlockVector [vec]
    (.clone vec))
  (defmethod get-block-vector Vector [vec]
    (.toBlockVector vec))
  (defmethod get-block-vector clojure.lang.PersistentVector [vec]
    (let [[x y z] vec]
      (new BlockVector (Math/floor x) (Math/floor y) (Math/floor z))))
  #_(defmethod get-block-vector DependingBlock [block]
      (get-block-vector (.block block)))
  (defmethod get-block-vector Location [loc]
    (BlockVector. (long (.getBlockX loc))
                    (long (.getBlockY loc))
                    (long (.getBlockZ loc)) ))
  ;(defmethod get-block-vector ::block-state-image [img])
  (defmethod get-block-vector ::bukkit-locatable [obj] (get-block-vector (get-location obj)))
  (defmethod get-block-vector :default [obj]
    (debug-println-2 ":default method in (get-block-vector).")
    (assert false "Default method for get-block-vector disabled!")
    ;(the* BlockVector (get-block-vector (get-location obj)))
    ))


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



(defn dispatch-for-add [pos ?? & _]
  [(class pos) (class ??)])

(declare get-mod-vector)

(do
  (defmulti add
    "Wrapper for Location.add() that uses a fresh Location."
    dispatch-for-add)
  (defmethod add [Location Number] [loc x y z]
    (.. loc clone (add x y z)))
  (defmethod add [Vector Number] [vec x y z]
    (Vector. (+ x (.getX vec)) (+ y (.getY vec)) (+ z (.getZ vec))))
  (defmethod add [Vector Vector] [v1 v2]
    (Vector. (apply + (map (memfn getX) [v1 v2]))
                  (apply + (map (memfn getY) [v1 v2]))
                  (apply + (map (memfn getZ) [v1 v2]))))
  (defmethod add [Block BlockFace] [b fc]
    (the Block (add b (the BlockVector (get-mod-vector fc)))))
  (defmethod add [Vector BlockFace] [v1 fc]
    (add v1 (the Vector (get-mod-vector fc))))
  (defmethod add [BlockFace Vector] [fc v1]
    (add v1 (the Vector (get-mod-vector fc))))
  (defmethod add [BlockVector BlockVector] [v1 v2]
    (BlockVector. (apply + (map (memfn getX) [v1 v2]))
                  (apply + (map (memfn getY) [v1 v2]))
                  (apply + (map (memfn getZ) [v1 v2]))))
  ;; TODO: I may not actually want to return a BlockVector here:
  #_(defmethod add [BlockVector Number] [vec x y z]
      (assert* (every? #(== % (int %)) [x y z]))
      (let [loc (the Location (.. (get-location vec) (add x y z)))
            [x y z] [(.getBlockX loc) (.getBlockY loc) (.getBlockZ loc)]]
        (BlockVector. x y z)))
  (defmethod add [Block Number] [block x y z]
    (get-block-at (add (get-location block) x y z)))
  (defmethod add [Block Vector] [block v]
    (get-block-at (add (get-block-vector block) v)))
  (defmethod add [Block BlockVector] [block v]
    (get-block-at (add (get-block-vector block) v)))
  (defmethod add [Object Number] [pos x y z]
                                        ;    (debug-println-2 ":default method in (add).")
    (add (the Location (get-location pos)) x y z))
                                        ;(.. (get-location pos) clone (add x y z))
  (defmethod add [Object Object] [o1 o2]
    (add (get-vector o1) (get-vector o2))))

(comment
  (defmethod add [Block Object] [block o]
    (get-block-at (add (get-block-vector block) o)))
  (defmethod add [Object Block] [o block]
    (add o (get-block-vector block)))
  ;#_(defmethod add [Object BlockFace] [o fc] (add o (the Vector (get-mod-vector fc))))
)

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
   [(instance? BlockState block) block]
   ;[(instance? BlockState block) (.clone block)]
   [(instance? Block block) (.getState block)]
   [:else (get-state (get-block-at (get-vector block)))]))

;;; id=get-type
(do
  (defmulti get-type
    "Wrapper for Block.getType() that returns a Material/* enum.  Not to be confused with Clojure's own (type)."
    class)
  (defmethod get-type ::block-state-image [block]
    (.getType block))
  (defmethod get-type Material [mat]
    mat)
  (defmethod get-type :default [obj]
    (debug-println-2 ":default method in (get-type).")
    (get-type (get-block-at (get-block-vector obj)))))

(do
  (defmulti set-type (fn [b _] (class b)))
  (defmethod set-type Block [block mat]
    "Wrapper for Block.setType()."
    (.setType block mat)
    (.getType block))
  (defmethod set-type Vector [vec ma]
    (set-type (get-location vec)))
  (defmethod set-type Location [loc mat]
    (set-type (the Block (get-block-at loc)) mat))
  (defmethod set-type ::bukkit-locatable [obj mat]
    (set-type (get-location obj) mat))
  #_(defmethod set-type :default [block? mat]
    (debug-println-2 ":default method in (set-type).")
    (set-type (get-block-at block?) mat)))

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

(defn fst-block []
  "Shorthand; returns block focussed by first player."
  (get-target-block (get-player)))


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

(defn glow [designator]
  (cond*
   ((coll? designator) (doall (map glow designator)))
   (:else
    (.setType (get-block-at designator) Material/GLOWSTONE)
    (assert* (= (.getType (get-block-at designator))
                Material/GLOWSTONE))
    designator)))

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
  `(let [state# (get-state (get-target-block (get-player)))]
     (def ~varname state#)
     (get-type state#)))
;"A perfectly good macro, but I'm moving it out in favor of (store-image-of-first-players-targeted-block)."

#_(defmacro store-image-of-first-players-targeted-block [varname]
  `(let [image# (new BlockImage (get-target-block (get-first-player)) 0)]
      (def ~varname image#)
      (get-type image#)))

;; TODO: Clojure lazy sequence?
(defn gen-region-vectors [start-corner end-corner & {:keys [start-at reversed]
                                                     :or {start-at :bottom}}]
    "Returns a linked list of all Vectors in the enclosed area, from bottom to top unless :reversed is true.
The :start-at keyword isn't implemented yet.  What it should do differs from :reversed in the ordering of X & Z axes.
NB: Don't use 'reverse' as the parm name!"
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
        ;; The list is already backwards, so we reverse it iff we need it the "normal" way:
        (if-not reversed (reverse @vector-list)
                @vector-list))))


;;;; id=block
(defn gen-block-region [start-corner
                        end-corner]
  "Returns a list of blocks."
  (map get-block-at (gen-region-vectors start-corner end-corner)))


(defn effect [location effect-id data & [radius]]
  "Edited from cljminecraft's world.clj.  The 'effect-id' should be either a Java Enum or a keyword as in 'effects'."
  (let [effect (if (instance? Effect effect-id) effect-id
                   (get effects effect-id))]
    (if radius
      (.playEffect (.getWorld location) location effect data radius)
      (.playEffect (.getWorld location) location effect data))))



(do
  (defmulti air?
    "New, spiffy multimethod version.
  Returns true if 'block' is made of air or represents a block made of air.  If called with an arg that does not represent a block, returns nil if :blocks-only is false; otherwise throws."
    (fn [block & _] (class block)))
  (defmethod air? ::block-state-image [block & _]
    (= (get-type block) Material/AIR))
  (defmethod air? :default [x & {:keys [blocks-only]
                                   :or {blocks-only true}}]
    (debug-println-2 ":default method in (air?).")
    (when blocks-only (throw (new IllegalArgumentException (format "Bad (air?) arg; %s cannot be \"coerced\" to yield a block." x))))))

#_(defn air? [block & {:keys [blocks-only]
                     :or {blocks-only true}}]
  "Returns true if 'block' is made of air or represents a block made of air.  If called with an arg that does not represent a block, returns nil if :blocks-only is false; otherwise throws."
  (if (or (instance? Block block)
          (instance? BlockState block))
    (= (.getType block) Material/AIR)
    (if-not blocks-only nil
            (throw (java.lang.IllegalArgumentException "Invalid arg to (air?).")))))



(do
  (defmulti solid? class)
  (defmethod solid? Material [mat] (.isSolid mat))
  (defmethod solid? ::block-state-image [block]
    (.isSolid (.getType block))))

#_(defn solid? [block]
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
  "Returns a Vector duple giving the positions of a player's lower and upper coords.  TODO: Make both BlockVectors."
  (let [pc-pos (get-block-vector pc)]
    [pc-pos (get-block-vector (add pc-pos 0 1 0))]))


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

(defn taxicab-distance [ob1 ob2]
"FIXME: This is a stub!"
  5)


;; TODO: Maybe add an option that forces a check of the source block?  In case the BlockState is outdated.
(do
  (defmulti get-data
    "Wrapper for BlockState.getData(), overloaded for other types."
    class)
  (defmethod get-data Block [block]
    (the* MaterialData (get-data (get-state block))))
  (defmethod get-data BlockState [block]
    (the* MaterialData (.getData block)))
  #_(defmethod get-data BlockImage [block]
    (the* MaterialData (.. block getBlockState getData)))
  (defmethod get-data MaterialData [mat]
    mat))

(definline get-facing [obj]
  `(.getFacing ~obj))

;;; id=attach
(do
  (defmulti attachable?
    "Returns true if arg refers to a block whose MaterialData is Attachable; overloaded for type."
    class)
  (defmethod attachable? MaterialData [mat]
    (instance? Attachable mat))
  (defmethod attachable? ::block-state-image [img]
    (attachable? (the MaterialData (get-data img))))
  (defmethod attachable? Material [mat]
    (throw (IllegalArgumentException. (format "TODO: (attachable?) currently isn't overloaded to match a Material arg to a MaterialData subclass.  Such an operation would require Java reflection, making it possible but expensive.  Offending argument: '%s'." mat)))))


(do
  (defmulti get-attached-face
    "Wrapper for .getAttachedFace(); will throw an exception if called for a block that doesn't get attached."
    class)
  (defmethod get-attached-face MaterialData [data]
    (the* BlockFace (.. data getAttachedFace)))
  (defmethod get-attached-face :default [block]
    (debug-println-2 ":default method in (get-attached-face).")
    (get-attached-face (the* MaterialData (get-data block))))
  (comment "Obsolete.  Now the overloaded (get-data) should take care of the polymorphism."
           (defmethod get-attached-face Block [block]
             (get-attached-face (get-state block)))
           (defmethod get-attached-face BlockState [block-state]
             (the* BlockFace (.. block-state getData getAttachedFace)))
           (defmethod get-attached-face BlockImage [img]
             (the* BlockFace (.. img getBlockState getData getAttachedFace)))))


(do
  (defmulti get-block-power class)
  (defmethod get-block-power Block [block]
    (.getBlockPower block)))


(defn powered? [block]
  (if (zero? (get-block-power block)) false true))


(defn get-power-levels [block]
  "This is mainly for examination purposes."
  (zipmap (reverse '[N S E W U D Self]) (reverse (map #(.getBlockPower block %) [BlockFace/NORTH, BlockFace/SOUTH, BlockFace/EAST, BlockFace/WEST, BlockFace/UP, BlockFace/DOWN, BlockFace/SELF]))))

(defn get-indirect-power [block]
  (zipmap (reverse '[N S E W U D Self]) (reverse (map #(if % 1 0) (map #(.isBlockFaceIndirectlyPowered block %) [BlockFace/NORTH, BlockFace/SOUTH, BlockFace/EAST, BlockFace/WEST, BlockFace/UP, BlockFace/DOWN, BlockFace/SELF])))))

(do
  (defmulti get-powering-blocks
    "Returns a "
    class)
  (defmethod get-powering-blocks Block [block]
    (if (zero? (get-block-power block)) []
        (map (comp get-block-at (partial add block)) (filter #(not (zero? (.getBlockPower block %))) [BlockFace/NORTH, BlockFace/SOUTH, BlockFace/EAST, BlockFace/WEST, BlockFace/UP, BlockFace/DOWN])))))

(do
 (defmulti get-mod-vector
   "Packages the (X,Y,Z) coords from a BlockFace obj."
   class)
 (defmethod get-mod-vector BlockFace [f]
   (BlockVector. (.getModX f) (.getModY f) (.getModZ f)))
 (defmethod get-mod-vector :default [x]
   (throw (IllegalArgumentException. (format "(get-mod-vector) is valid only for a BlockFace arg, not '%s'." x)))))


(defn neighboring-blocks [b & {:keys [include-self]}]
  "Returns a list of adjacent & cattycorner blocks."
  (let [blks (map #(get-block-at (add b %)) [BlockFace/NORTH, BlockFace/SOUTH, BlockFace/EAST, BlockFace/WEST,
                                             BlockFace/NORTH_EAST, BlockFace/NORTH_WEST, BlockFace/SOUTH_EAST, BlockFace/SOUTH_WEST])]
    (if include-self (conj blks b)
        blks)))

;;;; id=DIRECTIONS
(do
  (def NORTH BlockFace/NORTH)
  (def SOUTH BlockFace/SOUTH)
  (def EAST BlockFace/EAST)
  (def WEST BlockFace/WEST))

(defn surrounding-blocks [b]
  "The 27-block cube minus 'b' at its center.  TODO: Efficiency?  Should we return a list or a set?"
  (let [vb (get-block-vector b)
        neighboring-vecs (remove #(block-pos-eq? vb %) (gen-region-vectors (add vb -1 -1 -1) (add vb 1 1 1)))]
    (assert* (== (count neighboring-vecs) 26))
    (satisfying coll? (map get-block-at neighboring-vecs))))

(defn adjacent-blocks [b]
  "The four blocks in the cardinal directions."
  (let [vb (get-block-vector b)]
    (satisfying coll? (map get-block-at ((juxt #(add % 1 0 0) #(add % -1 0 0) #(add % 0 0 1) #(add % 0 0 -1)) vb)))))


(definline adjacent-directions []
 '[BlockFace/NORTH, BlockFace/SOUTH, BlockFace/EAST, BlockFace/WEST] )

#_(if (contains? (set (map (memfn getName) (.getMethods Util))) "getEnclosingBlocks")
  #_(definline enclosing-blocks [b]
    `(Util/getEnclosingBlocks ~b))
  #_(defn enclosing-blocks [b]
         (let [vb (get-block-vector b)]
           (satisfying coll? (map (partial add b) (concat (adjacent-directions) [BlockFace/UP, BlockFace/DOWN]))))))

(defn enclosing-blocks [b]
  "The six blocks sharing faces with 'b'."
         (let [vb (get-block-vector b)]
           (satisfying coll? (map (comp get-block-at (partial add (get-block-vector b))) (concat (adjacent-directions) [BlockFace/UP, BlockFace/DOWN])))))

(do
  (defmulti get-attached-block
   "Returns the block that the block represented by the argument is attached to.  If the arg is a Block, the result will be up-to-date; if the arg is a BlockState or BlockImage (q.v.), the coordinates from that snapshot will be used to retrieve the current World block.  There is therefore the chance of a senseless retval, in the case that the world has changed since the image was recorded.
Returns nil if there is no valid attachment.  Unless :error is set to true, throwing an exception.
If the :back keyword is set, behavior is somewhat different: all 26 neighbors are tested to see if something is attached *to* 'block'.  If more than one is found, a set of all is returned; if there is only one, it is returned by itself; otherwise nil is returned.  Thus the three possible outcomes have each a different return type."
   (fn [b & _] (class b)))
  (defmethod get-attached-block :cljengine.mc/block-state-image [block & {:keys [error, back]}]
    (cond*
     ((attachable? block)
      (the Block (add block (get-attached-face block))))
     (back
      (let [ns (surrounding-blocks block)
            attached-blocks (filter #(when (attachable? %)
                                       (get-attached-block % :back false))
                                    ns)]
        (cond*
         ((zero? (count attached-blocks))
          nil)
         ((== 1 (count attached-blocks))
          (first attached-blocks))
         (:else (set attached-blocks)))))
     (error (throw (Exception. (format "(get-attached-block) in strict mode found no attachment for %s." block))))))
  (defmethod get-attached-block :default [x & _]
    (throw (IllegalArgumentException. (format "Default method for (get-attached-block): non-block arg '%s'." x)))))

#_(do
  (defmulti attached?
   "Predicate; returns true if block b1, or that represented by b1, is attached to the block given by b2."
   (fn [b1 b2] (map class [b1 b2])))
  (defmethod attached? [:cljengine.mc/block-state-image
                        :cljengine.mc/block-state-image] [b1 b2]
    (get-attached-face b1)))

#_(defn test-some-shit []
;  (compile 'cljengine.re)
  (new cljengine.re.ClojureRegen)
)

;;;; id=time
(defn get-full-time []
  "Wrapper."
  (. (get-current-world) getFullTime))(print-classpath)







;;; id=abort
(def regengine-emergency-break (atom nil))

(defn abort-on-emergency-break! []
  (when @regengine-emergency-break
    (debug-announce "Emergency signal caught!")
    (swap! regengine-emergency-break (constantly false))
    (throw (RuntimeException. "Emergency break!"))))


(defn abort! []
  (swap! regengine-emergency-break (constantly true)))


;;;; id=metadata
(defn set-metadata [obj tag val & {:keys [plugin]}]
  "'plugin' defaults to the val in the *plugin* dynavar."
  ;(assert* (instance? Metadatable obj))
  (let [plugin (or plugin *plugin*)]
    (.setMetadata (the Metadatable obj) (the String tag) (FixedMetadataValue. plugin val))
    (assert* (.hasMetadata obj tag)))
  val)

(defn get-metadata [obj tag & {:keys [plugin]}]
  (let [plugin (or plugin *plugin*)]
    (when (.hasMetadata obj tag)
      (.value (some (fn [v]
                      (when (= (.getOwningPlugin v) plugin) v))
                    (. obj (getMetadata tag)))))))


;;;; id=reflection
(defn get-methods [obj]
  (vec (if (class? obj) (.getMethods obj)
           (.getMethods (class obj)))))



#_(defmacro in-my-ns [ns-name]
   `(ns ~ns-name
        "Testing Clojure in Minecraft."
                                        ;  (:require clojure.core)
        (:use (clojure
               [core :exclude [map]]
               repl pprint reflect set)
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
           (org.bukkit.util BlockIterator))))


(println "Loaded mc.clj successfully.")
