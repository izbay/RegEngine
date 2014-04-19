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
			final Iterator<DependingBlock> it = sSearch.values().iterator();
			final DependingBlock b = it.next();
			it.remove();

			final DependingBlockSet sD = b.allFwdDependencies();
			assert(!sSearch.containsKey(b.coord()));
			for(BlockVector v : sD.blocks.keySet())
			{
				final DependingBlock dDep = sD.blocks.get(v);
				if(!sOut.blocks.containsKey(v))
				{
					sOut.add(dDep);
					assert(!sSearch.containsKey(dDep.coord()));
					if(dDep.action().isHardDependency())
					{ sSearch.put(v, dDep); }
				}// if
				else // sOut contains dDep
				{
					if(!sSearch.containsKey(v))
					{	sSearch.put(v, dDep); }// if
					else
					{
						assert(sSearch.get(v).equals(sOut.blocks.get(v)));
						if(dDep.action().isHardDependency() 
								&& !sSearch.get(v).action().isHardDependency())
						{
							sSearch.put(v, dDep);
							sOut.blocks.put(v,dDep);
							assert(sSearch.get(v).action().isHardDependency());
							assert(sOut.blocks.get(v).action().isHardDependency());
						}// if
					}// else
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
		
		while(!sSearch.isEmpty())
		{
			final Iterator<DependingBlock> it = sSearch.blocks.values().iterator();
			final DependingBlock b = it.next();
			it.remove();
			
			final DependingBlockSet sD = b.allRevDependencies();
			for(Map.Entry<BlockVector,DependingBlock> entry : sD.blocks.entrySet())
			//for(BlockVector vDep : sD.blocks.keySet())
			{
				//final DependingBlock dDep = sD.blocks.get(vDep);
				assert(!entry.getValue().action().isHardDependency());
				if(!sSearch.contains(entry.getKey()))
				{
					sSearch.add(entry.getValue());
					if(!sOut.contains(entry.getKey()))
					{	sOut.add(entry.getValue());	}// if
				}// if
				else
				{	
					assert(!sOut.contains(entry.getKey()));
				}// else
			}// for
		}// while

		return sOut;
    }// doRevDepsSearch()
	
	public DependingBlockSet doFullDependencySearch()
	{ return this.doFwdDepsSearch().doRevDepsSearch(); }
}// DependingBlockSet
