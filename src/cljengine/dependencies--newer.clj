; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode) -*-
;;;; See http://minecraft-ids.grahamedgecombe.com/ for all the blocks' ID numbers & pictures!



(ns cljengine.block)



;;;; Block type-level dependencies
;; id=type-level

;; TODO: again

(defprotocol DependingBlock
  (coord [b])
  (block [b] "Retrieve inner block")
  (action [b])
  (set-action [b a]))

(defrecord BasicDependingBlock [block action]
  DependingBlock
  (block [this] (the :cljengine.mc/block-state-image (:block this)))
  (coord [this] (the BlockVector (get-block-vector (:block this))))
  (action [this] (satisfying keyword? (:action this)))
  (set-action [this act]
    (update-in this [:action] (satisfying keyword? act))))

(defn make-depending-block [block & {:keys [regengine-action]}]
  "Factory func."
  (BasicDependingBlock. block regengine-action))

(defprotocol VineDependingBlockProt
  (on-faces [b])
  (dissoc-map [b map]))

(defrecord VineDependingBlock [block action vine-covered-faces depending-faces]
  DependingBlock
  (block [this] (the :cljengine.mc/block-state-image (:block this)))
  (coord [this] (get-block-vector (:block this)))
  (action [this] (:action this))
  (set-action [this act] (update-in this [:action] act))
  VineDependingBlockProt
  (on-faces [this] (satisfying set? (:vine-covered-faces this))))


(do
  (defmulti make-vine-depending-block class)
  (defmethod make-vine-depending-block :cljengine.mc/block-state-image [block & {:keys [regengine-action]}]
    "Factory func."
    (when-not (= Material/VINE (get-type block)) (throw (IllegalArgumentException. "A non-vine block got passed to (make-vine-depending-block).")))
    (let [state (get-state block)]
      (VineDependingBlock. state regengine-action
                           (set (filter #(.isOnFace (get-data state) %) (adjacent-directions)))
                           {}))))

(declare merge-deps)

(defprotocol DependingBlockSetProt
  (blocks [s])
  (add-block [s b])
  (find-block [s v]))
(defrecord DependingBlockSet [block-map]
  DependingBlockSetProt
  (blocks [this] (satisfying map? (:block-map this)))
  (find-block [this v]
    (illegal-arg-check (instance? Vector v))
    (when-let [entry (find (blocks this) (get-block-vector v))]
      (the ::depending-block (val entry)))))

#_(add-block [this b]
    (illegal-arg-check (isa? (class b)
                             :cljengine.mc/block-state-image))
    (assoc (satisfying map? block-map) (coord b)
           (if-let [b' (find-block this (coord b) (merge-deps ))]
             (merge-deps ))))

(defn make-depending-block-set [coll]
  (if-not (coll? coll) (make-depending-block-set #{coll})
          (DependingBlockSet. (zipmap (map coord coll) coll))))

(derive DependingBlockSet ::depending-block-set)


(do
 (defmulti merge-deps (fn
                        ([o1 o2] [(class o1) (class o2)])
                        ([o1 _ & _] (class o1))))
 (defmethod merge-deps Object [s1 & rest]
   (reduce merge-deps s1 rest))
 (defmethod merge-deps [::depending-block-set ::depending-block-set] [s1 s2]
   (DependingBlockSet. (merge-with (fn [b1 b2]
                                     (assert* (= (block b1) (block b2)))
                                     ;; Delegate polymorphically:
                                     (merge-deps b1 b2)))))
 (defmethod merge-deps [::depending-block ::depending-block] [b1 b2]
   (illegal-arg-check (= (coord b1) (coord b2)))
   (make-depending-block (block b1)
                         :regengine-action (satisfying keyword?
                                                       (let [a1 (action b1)
                                                             a2 (action b2)]
                                                         (if (= a1 :destroy)
                                                           (do
                                                             (when-not a2 (throw (Error. "Nil :action in merge-deps...")))
                                                             :destroy)
                                                           (do
                                                             (when-not (= a1 :restore) (throw (Error. "Another illegal arg, a1.")))
                                                             (if (= a2 :destroy)
                                                               :destroy
                                                               (do
                                                                 (when-not (= a2 :restore) (throw (Error. "Another illegal arg, with a2.")))
                                                                 :restore)))))))))



(derive BasicDependingBlock ::depending-block)
(derive VineDependingBlock ::depending-block)





; (defn tripwire-fwd-dependency [b] )
