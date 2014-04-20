

;;;; Consider that we have a map sorted using the following total order:

(defn regen-compare [b1 b2]
  "If "
  (let [time-comparison (compare (get-target-time b1) (get-target-time b2))]
    (if-not (zero? time-comparison) time-comparison
            (compare-vectors b1 b2))))

;;; Block images are sorted, firstly, according to their projected restoration times, and, after that, according to axial position.  We go bottom-to-top, since that is a good default guess for satisfying most Block dependencies.
