; -*- eval: (clojure-mode); eval: (paredit-mode); eval: (viper-mode) -*-
;;;; See http://minecraft-ids.grahamedgecombe.com/ for all the blocks' ID numbers & pictures!



(ns cljengine.block)



;;;; Block type-level dependencies
;; id=type-level

;; TODO: Yet again!

(defn empty-set []
  (new DependingBlockSet))

(defn vine-fwd-dependency [b]
  (assert* (isa? (class (block b)) :cljengine.mc/block-state-image))
  (let [typ (get-type b)]
    (cond*
     [(solid? b) ; TODO: "vine-supporting" material
      (let [dirs (adjacent-directions)
            adj-blocks (map #(add (block b) %) dirs)
            adj-vines (filter #(= (get-type %) Material/VINE) adj-blocks)]
        (map (fn [dir b']
               (DependingBlock/from b' Action/RESTORE)
               (VineDependencyBlock. b' {(.getFacing dir) #{:same}}))
             dirs
             adj-vines))
                                        ;(= typ Material/)
;FIXME:
      ])))


(defn portal-fwd-dependency [b]
  (the DependingBlockSet
       (let [t (get-type b)]
         (cond*
          [(= t Material/OBSIDIAN)
           (let [st (new DependingBlockSet)]
             (doseq [d (for [dir (Util/adjacentDirections)
                             :let [b' (Util/add (get-block-vector b) dir)
                                   mat (get-type b)]
                             :when (= mat Material/PORTAL)]
                         (DependingBlock/from b' Action/DESTROY))]
               (.add st d))
             st)]
          [(= t Material/PORTAL)
           (let [st (new DependingBlockSet)]
             (doseq [d (map #(DependingBlock/from % Action/DESTROY)
                            (filter #(= (get-type %) Material/PORTAL) (enclosing-blocks (.block b))))]
               (.add st d))
             st)]
          [:else (empty-set)]))))

(defn portal-rev-dependency [b]
  (the DependingBlockSet
       (let [t (get-type b)]
         (cond*
          [(= t Material/PORTAL)
           (map #(DependingBlock/from % Action/RESTORE) (enclosing-blocks))]
          [:else
           (empty-set)]))))


(defn alter-blocks [block-dep-set]
  (illegal-arg-check (instance? DependingBlockSet block-dep-set))
  (with-local-vars [out-coll #{}]
    (doseq [b block-dep-set]
      (if <b is VINE>
          (econd*
           [<action is DESTROY>
            remove-block b
            add b to out-coll]
           [<action is RESTORE])
          (cond*
           [(= (.action b) Action/DESTROY)
         (remove-block (.block b))
         (var-set #'out-coll (conj @out-coll (.block b))) ]
        [(= (.action b) Action/RESTORE)
         (var-set #'out-coll (conj @out-coll (.block b)))])))))
