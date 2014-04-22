package com.github.izbay.regengine.block;

//import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
//import java.util.Set;
//import java.util.LinkedList;
//import java.util.Iterator;
//import java.util.HashSet;
import java.util.Map;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
//import java.util.Set;
import org.bukkit.util.BlockVector;

import com.github.izbay.regengine.BlockImage;
//import com.github.izbay.util.Util;

public class DependingBlockSet implements Iterable<DependingBlock>
{
	public final Map<BlockVector,DependingBlock> blocks;
	
	public Iterator<DependingBlock> iterator()
	{	return blocks.values().iterator(); }
	
	// Default constructor:
	public DependingBlockSet()
	{
		super();
		this.blocks = new HashMap<BlockVector,DependingBlock>();
	}// ctor
	
	/**
	 * Copy constructor:
	 * @param rhs
	 */
	public DependingBlockSet(final DependingBlockSet rhs)
	{
		super();
		this.blocks = new HashMap<BlockVector,DependingBlock>();
		for(BlockVector v : rhs.blocks.keySet())
		{	this.blocks.put(v,rhs.blocks.get(v)); }
	}// ctor
	
	/**
	 * Construct from an already-extant collection.
	 * @param coll
	 */
	public DependingBlockSet(final Iterable<DependingBlock> coll)
	{
		super();
		this.blocks = new HashMap<BlockVector,DependingBlock>();
		for(DependingBlock b : coll)
		{ this.add(b); }// for
	}// ctor
	
	// Merging constructor.  Does not modify its args.
	public DependingBlockSet(final DependingBlockSet s1, final DependingBlockSet s2)
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
	 * Creates a singleton set.
	 * @param b
	 */
	public DependingBlockSet(final DependingBlock b)
	{
		this();
		this.add(b);
	}// ctor
	
	public static DependingBlockSet emptySet()
	{	return new DependingBlockSet(); }
	
	public DependingBlock get(final BlockVector key)
	{	return blocks.get(key); }
	
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
	
	public boolean contains(final BlockVector v)
	{ return blocks.containsKey(v); }
	
	public boolean isEmpty()
	{	return this.blocks.isEmpty(); }
	
	public int size()
	{	return blocks.size(); }
	
	public static DependingBlockSet union(final DependingBlockSet s1, final DependingBlockSet s2)
	{ return new DependingBlockSet(s1, s2); }
	
/*	public static DependingBlockSet union(final DependingBlockSet[] sets)
	{
		if(sets.length < 2) throw new IllegalArgumentException();

	}
	*/
		
	public DependingBlockSet doFwdDepsSearch()
	{
		final DependingBlockSet sIn = this;
		final DependingBlockSet sOut = new DependingBlockSet(sIn); 
		assert(sIn.blocks.size() == sOut.blocks.size());
		final LinkedHashMap<BlockVector,DependingBlock> sSearch = new LinkedHashMap<BlockVector,DependingBlock>();
		//final DependingBlockSet sSearch = new DependingBlockSet();
		for(BlockVector v : sIn.blocks.keySet())
		{
			final DependingBlock d = sIn.blocks.get(v);
			if(d.action().isHardDependency()) sSearch.put(v,d);
		}// for
		
		while(!sSearch.isEmpty())
		{
			// "Pop" the next block off the search "queue".
			final Iterator<DependingBlock> it = sSearch.values().iterator();
			final DependingBlock b = it.next();
			it.remove();

			final DependingBlockSet sD = b.allFwdDependencies();
			assert(!sSearch.containsKey(b.coord()));
			// We can't say this: assert(!sD.contains(b.coord())); // because the set returned by the compound-block check will contain its caller.
			for(BlockVector v : sD.blocks.keySet())
			{
				final DependingBlock dDep = sD.blocks.get(v);
				// Because sOut keeps track of the "visited" blocks, if a block can't be found in sOut, it's a first-time encounter.  Therefore it certainly gets added now...
				if(!sOut.contains(v))
				{
					sOut.add(dDep);
					assert(!sSearch.containsKey(dDep.coord()));
					// ... and it gets added to the search queue if it's a 'hard' dependency--i.e., it's guaranteed to be moved|destroyed and to propagate the dependency search along. 
					if(dDep.action().isHardDependency())
					{ sSearch.put(v, dDep); }
				}// if
				// Otherwise it has been encountered before--though the search may not have branched off its path yet.
				else // sOut contains dDep
				{
/*					if(!sSearch.containsKey(v))
					{	sSearch.put(v, dDep); }// if
					else
					{
						assert(sSearch.get(v).equals(sOut.blocks.get(v)));

					*/
					// If the block is in the search queue already, we perhaps will need to _update_ that value.  We now compare the new with the value currently in the output set:
                        final DependingBlock prevVal = sOut.blocks.get(v);
						// This merge operation is polymorphic: in the basic case it only checks the values of the action() fields, overriding a soft with a hard.  But in the Vine case, for example, it also calculates whether there is enough "evidence" to _upgrade_ a vine from a soft to a hard dependency.
						final DependingBlock newVal = prevVal.mergeWith(dDep);

					// Now what do we do with that?  The values in sOut and also sSearch--if prevVal is also in the search queue--need to be updated.  However, if the block has already been searched, but there has been a qualitative change, it must go _back_ in the queue.  This is how we handle the complicated Vine relationships.
					if(sSearch.containsKey(v))
					{
						// TODO: Assert that the sSearch and sOut objects are the same?
						sSearch.put(v, newVal);
					}// if
					else if(newVal.action().isHardDependency() 
								&& !prevVal.action().isHardDependency())
					{
							sSearch.put(v, newVal);
							assert(sSearch.get(v).action().isHardDependency());
					}// elif 
					
					// Now, update sOut in either case: 
                    sOut.blocks.put(v,newVal);
					/*}// else*/
				}// else
				assert(sOut.blocks.containsKey(dDep.coord()));
			}// for
		}// while
		
		return sOut;
	}// doFwdDepsSearch()

	public DependingBlockSet doRevDepsSearch()
	{
		final DependingBlockSet sIn = this;
		final DependingBlockSet sOut = new DependingBlockSet(sIn); 
		final DependingBlockSet sSearch = new DependingBlockSet(sIn);
		
		//int loop_counter = 0;
		while(!sSearch.isEmpty())
		{
			//++loop_counter;
			//if(loop_counter > 1000000000) throw new Error("Infinite loop detected in doRevDepsSearch().");
			
			final Iterator<DependingBlock> it = sSearch.blocks.values().iterator();
			final DependingBlock b = it.next();
			it.remove();
			
			final DependingBlockSet sD = b.allRevDependencies();
			for(Map.Entry<BlockVector,DependingBlock> entry : sD.blocks.entrySet())
			//for(BlockVector vDep : sD.blocks.keySet())
			{
				//final DependingBlock dDep = sD.blocks.get(vDep);
				// We can't assert this: assert(!entry.getValue().action().isHardDependency()); // because the dupleBlockDependency() check will return both parts with Action.DESTROY if one of them has it.
				if(!sOut.contains(entry.getKey()))
				{
					// TODO: Is it better to use add() here, which does a full merge check, or a plain put()?  Does it matter semantically or concerns it only efficiency?
					sSearch.add(entry.getValue());
					if(!sOut.contains(entry.getKey()))
					{	sOut.add(entry.getValue());	}// if
				}// if
				/*
				else
				{	
					assert(!sOut.contains(entry.getKey()));
				}// else
				*/
			}// for
		}// while

		return sOut;
    }// doRevDepsSearch()
	
	public DependingBlockSet doFullDependencySearch()
	{ return this.doFwdDepsSearch().doRevDepsSearch(); }
}// DependingBlockSet
