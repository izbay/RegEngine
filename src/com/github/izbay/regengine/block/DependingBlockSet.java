package com.github.izbay.regengine.block;

import java.util.HashMap;
//import java.util.HashSet;
import java.util.Map;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
//import java.util.Set;
import org.bukkit.util.BlockVector;

import com.github.izbay.regengine.BlockImage;
//import com.github.izbay.util.Util;

public class DependingBlockSet 
{
	public final Map<BlockVector,DependingBlock> blocks;
	
	// Merging constructor.  Does not modify its args.
	DependingBlockSet(final DependingBlockSet s1, final DependingBlockSet s2)
	{
		super();
		this.blocks = new HashMap<BlockVector,DependingBlock>(s1.blocks);
		for(BlockVector v : s2.blocks.keySet())
		{
			this.blocks.put(v, (this.blocks.containsKey(v) ? this.blocks.get(v).mergeWith(s2.blocks.get(v))
															: s2.blocks.get(v)));
		}// for
	}// ctor

	/**
	 * Warning: Mutator!
	 * @param b
	 * @return
	 */
	public DependingBlockSet add(final BlockImage b, final Action act)
	{ return this.add(DependingBlock.from(b, act)); }

	/**
	 * Warning: Mutator!
	 * @param b
	 * @return
	 */
	public DependingBlockSet add(final Block b, final Action act)
	{ return this.add(DependingBlock.from(b, act)); }

	/**
	 * Warning: Mutator!
	 * @param b
	 * @return
	 */
	public DependingBlockSet add(final BlockState b, final Action act)
	{ return this.add(DependingBlock.from(b, act)); }

	/**
	 * Warning: Mutator method!  The calling collection gets modified.
	 * @param b
	 * @return
	 */
	public DependingBlockSet add(final DependingBlock b)
	{
		final BlockVector v = b.coord();
		if(this.blocks.containsKey(v))
		{	this.blocks.put(v, this.blocks.get(v).mergeWith(b)); }
		else
		{	this.blocks.put(v, b); }
		
		return this;
	}// add()
	
	
}// DependingBlockSet
