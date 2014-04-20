(defn rec-add-dependencies [b out]
  (let [ns (diff (neighboring-blocks b) out)]
    (reduce (fn [b' out']
              (cond*
               [<b' is attached to b>
                    (conj out' b')]
               [(and <b' is gravity-bound>
                     (above? b' b))
                (conj out' b')]
               [(and <b is a redstone power source>
                     <b' is a sticky piston powered by b>
                     <b' is not powered by any block not in out'>)
                (let [b'' <the block on the end of the piston's arm>]
                  <if b'' is the kind to get broken by a piston pull, )])) ns)))


(comment
  def recurse-deps b out
   let ns = (neighboring-blocks b) - out
    in
     fold (¦Ë b' out'.
             if b' is attached to b
             OR (b' is a gravity-affected block directly above b)
             then
               out' ¡È {b'}
             else if b is a redstone power source ¡Ä b' is an extended sticky piston ¡Ä there is no neighbor of b' not in out' that provides power to b' then
               if b' is a regular piston then)
      {} ns

)
