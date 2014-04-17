package com.github.izbay.regengine.block;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
//import java.util.LinkedHashMap;
//import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Vine;
//import org.bukkit.util.BlockVector;

import com.github.izbay.regengine.BlockImage;
import com.github.izbay.util.Util;

	
/**
 * @author jdjs
 *
 */
public class VineDependingBlock extends DependingBlock
{
	public static enum Orientation { ABOVE, BESIDE };
	
	public final Set<BlockFace> vineCoveredFaces;
	public Map<BlockFace,Set<Orientation>> independentFaces; 

	/**
	 * @param block
	 * @param action
	 */
	public VineDependingBlock(BlockImage block, Action action) {
		super(block, action);
		if(block.getType() != Material.VINE)
		{	throw new IllegalArgumentException(); }
		vineCoveredFaces = new HashSet<BlockFace>();
		independentFaces = new HashMap<BlockFace,Set<Orientation>>();
		final Block bAbove = Util.getBlockAbove(block);
		final boolean isVineAbove = bAbove.getType() == Material.VINE;
		for(BlockFace dir : Util.adjacentDirections())
		{
			final MaterialData mat = block.getBlockState().getData();
			if( ((Vine) mat).isOnFace(dir) )
			{ 
				vineCoveredFaces.add(dir);
				independentFaces.put(dir, EnumSet.of(Orientation.BESIDE));
				assert(independentFaces.get(dir).contains(Orientation.BESIDE));
				// Check whether there's a vine above to provide support:
				if(isVineAbove && ((Vine)(bAbove.getState().getData())).isOnFace(dir))
				{	
					independentFaces.get(dir).add(Orientation.ABOVE);
					assert(independentFaces.get(dir).contains(Orientation.ABOVE));
				}// if
			}// if
		}// for
	}// ctor
	
	public VineDependingBlock(final VineDependingBlock d1, final VineDependingBlock d2)
	{	
		super(d1,d2); // merges the 'action' fields
		assert(d1.getType().equals(d2.getType()));
		assert(d1.independentFaces.equals(d2.independentFaces));
		assert( d1.independentFaces instanceof HashMap<?,?> );
		assert(d1.vineCoveredFaces.equals(d2.vineCoveredFaces));
		this.vineCoveredFaces = new HashSet<BlockFace>(d1.vineCoveredFaces);

		// The following is an intersection operation:
		//final LinkedHashMap<BlockFace,Set<Orientation>> m = (LinkedHashMap<BlockFace,Set<Orientation>>)
		this.independentFaces = new HashMap<BlockFace,Set<Orientation>>(d1.independentFaces);
		assert(this.independentFaces != d1.independentFaces);
		assert(this.independentFaces != d2.independentFaces);
		for(BlockFace dir : this.independentFaces.keySet())
		{
			if(d2.independentFaces.containsKey(dir))
			{
				this.independentFaces.put(dir, EnumSet.noneOf(Orientation.class)); // EnumSet.copyOf(d1.independentFaces.get(dir)));
				assert(this.independentFaces.get(dir) != d1.independentFaces.get(dir));
				for(Orientation o : Orientation.values())
				{
					if(this.independentFaces.get(dir).contains(o) 
							&& d2.independentFaces.get(dir).contains(o))
					{	this.independentFaces.get(dir).add(o);	}// if
					assert( !(this.independentFaces.get(dir).contains(o)
								&& d1.independentFaces.get(dir).contains(o)) );
					assert( !(this.independentFaces.get(dir).contains(o)
								&& d2.independentFaces.get(dir).contains(o)) );
				}// for
				
				if(this.independentFaces.get(dir).isEmpty())
				{	this.independentFaces.remove(dir);	}
			}// if
			else
			{	this.independentFaces.remove(dir); }// else
		}// for
			
		// Compute: If there are no independent faces remaining, the block will have no support and should be destroyed.
        if(this.independentFaces.isEmpty())
        {	this.action = Action.DESTROY; }// if	
	}// ctor

	public Set<BlockFace> onFaces()
	{ return vineCoveredFaces; }// onFaces()

	/**
	 * A 'DESTROY' action in either block will override any type in the other.  
	 * TODO: Show that this method is commutative.
	 * @param d2
	 * @return
	 */
	public VineDependingBlock mergeWith(final VineDependingBlock d2)
	{
		//if(this.coord() != d2.coord()) { throw new IllegalArgumentException(); }
		return new VineDependingBlock(this, d2);
	}
}// VineDependingBlock
