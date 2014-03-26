; -*- eval: (clojure-mode); eval: (paredit-mode); (viper-mode) -*-

(defonce ^:dynamic *debug-print* true); TODO: Is dynamic the right sort?
(defn debug-println [& forms]
  "If *debug-print* is set, passes 'forms' to (println)."
  (when *debug-print* (apply #'println forms)))

;; Absolute value wrapper:
(definline abs [num]
  `(Math/abs ~num))

(defn ns-syms [ns]
  "Wrapper.  Returns & prints (unqualified) names of symbols in namespace 'ns'.  Good for poking around."
  (let [keys (sort (keys (ns-interns ns)))]
    (pprint keys)
    keys))

(defonce ^:dynamic *do-not-compile-assertions* false)
(defmacro assert* [& forms]
  "Modification to (assert) allowing static and dynamic control.  Setting *do-not-compile-assertions* at compile-time will have the same effect as compiling a regular (assert)ion with *assert* unset: removal.  However, if *do-not-compile-assertions* is false, then *assert* can be used dynamically to enable/disable assertion evaluation, at the slight cost of a boolean test."
  (assert (var? #'*do-not-compile-assertions*))
  (if *do-not-compile-assertions*
    '(do)
    `(if *assert* (assert ~@forms)
         (do))))


#_(defmacro cond*
  ([[test & body]]
     `(cond ~@test (do ~@body)))
  ([[fst-test & fst-clause] & rest]
     (letfn [(cond-recur
               ([[test* & body*]]
                  `(~test* (do ~@body*)))
               ([[test* & body*] & rest*]
                  `(~test* (do ~@body*)
                           ~@(apply cond-recur rest*))))]
       `(cond
         ~fst-clause (do ~@fst-test)
         ~@(apply cond-recur rest)))))
#_(defmacro cond*
  ([clause]
     (let [[test & body] clause]
       `(cond ~@test (do ~@body))))
  ([[fst-test & fst-clause] & rest]
     (letfn [(cond-recur
               ([[test* & body*]]
                  `(~test* (do ~@body*)))
               ([[test* & body*] & rest*]
                  `(~test* (do ~@body*)
                           ~@(apply cond-recur rest*))))]
       `(cond
         ~fst-clause (do ~@fst-test)
         ~@(apply cond-recur rest)))))
#_(defmacro cond*
  ([[test & body]]
     `(cond ~@test (do ~@body)))
  ([[fst-test & fst-clause] & rest]
     (letfn [(cond-recur
               ([[test* & body*]]
                  `(~test* (do ~@body*)))
               ([[test* & body*] & rest*]
                  `(~test* (do ~@body*)
                           ~@(apply cond-recur rest*))))]
       `(cond
         ~fst-clause (do ~@fst-test)
         ~@(apply cond-recur rest)))))

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

(assert
 (= (cond*
     [false 99]
     [false 200]
     [true 300]
     [true 20])
    300))
(assert
 (= (cond*
     [false 1]
     [false 2])
    nil))
(assert
 (= (cond*
     [false 1]
     [false 2]
     [:else 99])
    99))
