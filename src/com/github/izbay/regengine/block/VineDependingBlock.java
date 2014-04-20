package com.github.izbay.regengine.block;

import java.util.EnumMap;
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
	 * Use
	 * @param block
	 * @param action
	 */
	protected VineDependingBlock(BlockImage block, Action action) {
		super(block, action);
		if(block.getType() != Material.VINE)
		{	throw new IllegalArgumentException(); }
		vineCoveredFaces = new HashSet<BlockFace>();
		independentFaces =  new EnumMap<BlockFace,Set<Orientation>>(BlockFace.class);
		final Block bAbove = Util.getBlockAbove(block);
		final boolean isVineAbove = (bAbove.getType() == Material.VINE);
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
	
	// Merge constructor.
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
				
				// Handled by Normalize:
				/* if(this.independentFaces.get(dir).isEmpty())
				{	this.independentFaces.remove(dir);	}
				*/
			}// if
			else
			{	this.independentFaces.remove(dir); }// else
		}// for
			
		/*
        if(this.independentFaces.isEmpty())
        {	this.action = Action.DESTROY; }// if	
        */
		this.normalizeDestructively();
	}// ctor
	

	

	/**
	 * Copy constructor.  Deep copy.
	 * @param rhs
	 */
	public VineDependingBlock(final VineDependingBlock rhs)
	{
		super(rhs.block, rhs.action());
		this.vineCoveredFaces = new HashSet<BlockFace>(rhs.vineCoveredFaces);
		this.independentFaces = new EnumMap<BlockFace,Set<Orientation>>(rhs.independentFaces);
		// Deep-copy the sets:
		for(BlockFace dir : BlockFace.values())
		{
			if(this.independentFaces.containsKey(dir))
			{	this.independentFaces.put(dir, EnumSet.copyOf(rhs.independentFaces.get(dir))); }// if
		}// for

		assert(this.independentFaces != rhs.independentFaces);
		assert(this.independentFaces != rhs.independentFaces);
	}// ctor

	public Set<BlockFace> onFaces()
	{ return vineCoveredFaces; }// onFaces()

	/**
	 * A 'DESTROY' action in either block will override any type in the other.  
	 * TODO: Show that this method is commutative.
	 * @param d2
	 * @return
	 */
	@Override
	public VineDependingBlock mergeWith(final DependingBlock d2)
	{
		if(!(d2 instanceof VineDependingBlock)) throw new IllegalArgumentException(); 
		//if(this.coord() != d2.coord()) { throw new IllegalArgumentException(); }
		return new VineDependingBlock(this, (VineDependingBlock)(d2));
	}// mergeWith()
	
		
	/**
	 * Non-mutator.
	 * @param map
	 * @return
	 */
	public VineDependingBlock difference(final Map<BlockFace,Set<Orientation>> map)
	{
		final VineDependingBlock vb = new VineDependingBlock(this);
		for(BlockFace dir : map.keySet())
		{
			if(vb.independentFaces.containsKey(dir))
			{ vb.independentFaces.get(dir).removeAll(map.get(dir)); }// if
			
			// Handled with normalize() now:
			/*if(vb.independentFaces.get(dir).isEmpty())
			{	vb.independentFaces.remove(dir); }// if*/
		}// for
		
		/*if(vb.independentFaces.isEmpty())
        {	vb.action = Action.DESTROY; }// if	*/
		
		vb.normalizeDestructively();
		return vb;
	}// difference()

	// Compute: If there are no independent faces remaining, the block will have no support and should be destroyed.
	public VineDependingBlock normalize()
	{
		final VineDependingBlock vb =  new VineDependingBlock(this); // This does a deep copy, so modifying it should be OK
		vb.normalizeDestructively();
		
		return vb;
	}// normalize()

	
	/**
	 * Like normalize(), but operates solely for side-effects.  This is so it can be used to construct.
	 */
	public void normalizeDestructively()
	{
        for(BlockFace dir : Util.adjacentDirections())
		{
			if(independentFaces.containsKey(dir) && independentFaces.get(dir).isEmpty())
			{	independentFaces.remove(dir); }// if
		}// for
	
		if(independentFaces.isEmpty())
        {	this.action = Action.DESTROY; }// if	

	}// normalizeDestructively()
	
	public static Map<BlockFace,Set<Orientation>> subtrahendAdjacent(final BlockFace dir)
	{
        final Map<BlockFace,Set<Orientation>> subtrahend = new EnumMap<BlockFace,Set<Orientation>>(BlockFace.class);
        subtrahend.put(dir, EnumSet.of(Orientation.BESIDE));
        assert(subtrahend.size() == 1);
        return subtrahend;
	}

	public static Map<BlockFace,Set<Orientation>> subtrahendOverhead()
	{
				final Map<BlockFace,Set<Orientation>> subtrahend = new EnumMap<BlockFace,Set<Orientation>>(BlockFace.class);
				for(BlockFace dir : Util.adjacentDirections())
				{ subtrahend.put(dir, EnumSet.of(Orientation.ABOVE/*, Orientation.BESIDE*/)); }
				assert(subtrahend.size() == 4);
				return subtrahend;
	}// subtrahend()

}// VineDependingBlock
