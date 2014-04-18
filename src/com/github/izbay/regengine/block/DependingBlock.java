package com.github.izbay.regengine.block;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;

import org.bukkit.material.Vine;

import com.github.izbay.regengine.BlockImage;
import com.github.izbay.regengine.block.VineDependingBlock.Orientation;
import com.github.izbay.util.Util;

public class DependingBlock {
		public final BlockImage block;
		public /*final*/ Action action;
		//public final BlockVector coord;

		/**
		 * @param block
		 * @param action
		 */
		public DependingBlock(BlockImage block, Action action) {
			super();
			this.block = block;
			this.action = action;
		}// ctor
		
		/**
		 * 'Merge' ctor.
		 * @param d1
		 * @param d2
		 */
		public DependingBlock(final DependingBlock d1, final DependingBlock d2)
		{
			this(d1.block, (d1.action == Action.DESTROY ? Action.DESTROY : (d2.action == Action.DESTROY ? Action.DESTROY : Action.RESTORE)) );
			if(!d1.coord().equals(d2.coord()))  { throw new IllegalArgumentException(); }
		}// ctor
		
		public BlockVector coord() {	return block.getBlockVector(); }
		
		public DependingBlock setAction(final Action act)
		{ return new DependingBlock(block, act); }
		
		public Material getType()
		{	return block.getType(); }
		
		/**
		 * A 'DESTROY' action in either block will override any type in the other.  
		 * TODO: Show that this method is commutative.
		 * @param d2
		 * @return new DependingBlock instance.
		 */
		public DependingBlock mergeWith(final DependingBlock d2)
		{
		//	if(!this.coord().equals(d2.coord()))  { throw new IllegalArgumentException(); }
			return new DependingBlock(this, d2);
		}
		
		/**
		 * Mutator version of mergeWith().
		 * @param d2
		 * @return
		 */
/*		protected DependingBlock assimilate(final DependingBlock d2)
		{
			if(!this.getType().equals(d2.getType()))
			{	throw new IllegalArgumentException(); }

			if(d2.action.equals(Action.DESTROY))
			{	this.action = Action.DESTROY; }
			return this;

		}// 
*/	
		public static DependingBlock from(final Block b, final Action act)
		{	return DependingBlock.from(new BlockImage(b), act);	}

/*		public static DependingBlock from(final Block b)
		{	return DependingBlock.from(new BlockImage(b));	}
		*/

		public static DependingBlock from(final BlockState b, final Action act)
		{	return DependingBlock.from(new BlockImage(b), act);	}

/*		public static DependingBlock from(final BlockState b)
		{	return DependingBlock.from(new BlockImage(b));	}
		*/

/*		public static DependingBlock from(final BlockImage b)
		{	return DependingBlock.from(b, Action.DESTROY);	}
		*/

		public static DependingBlock from(final BlockImage b, final Action act)
		{
			switch(b.getType())
			{
			case VINE:
				return new VineDependingBlock(b, act);
			default:
				return new DependingBlock(b, act);
			}// switch
		}// DependingBlock.from()
		
	
	//public DependingBlockSet 
	
	public DependingBlockSet doubleBlockFwdDependency(final DependingBlock b)
	{
		final DependingBlockSet set = new DependingBlockSet();
		// FIXME: Nonfunctional.
		

		return set;
	}// doubleBlockDependency()
	
	public DependingBlockSet attachmentFwdDependency()
	{
		final DependingBlock b = this;
		final DependingBlockSet set = new DependingBlockSet();

        for(Block bAdj: Util.getEnclosingBlocks(Util.getBlockAt(b.block.getLocation())))
        {
        	if(Util.isAttachable(bAdj) && (Util.getAttachedBlock(bAdj).getLocation().equals(b.block.getLocation())))
        	{ set.add(DependingBlock.from(bAdj, Action.DESTROY)); }// if
        }// for
		return set;
	}// attachmentFwdDependency()
	
	public DependingBlockSet attachmentRevDependency()
	{
		final DependingBlock b = this;
		final DependingBlockSet set = new DependingBlockSet();
		
		if(Util.isAttachable(b.block.getBlockState()))
		{	set.add(DependingBlock.from(Util.getAttachedBlock(b.block.getBlockState()), Action.RESTORE)); }// if
		
		return set;
	}// attachmentRevDependency()
	
	
	
	
	public DependingBlockSet portalFwdDependency()
	{
		final DependingBlock b = this;
		final DependingBlockSet set = new DependingBlockSet();
		if(b.getType() == Material.OBSIDIAN 
				|| b.getType() == Material.PORTAL)
		{
			for(Block bAdj: Util.getEnclosingBlocks(Util.getBlockAt(b.block.getLocation())))
			{
				if(bAdj.getType() == Material.PORTAL)
				{
					set.add(DependingBlock.from(bAdj, Action.DESTROY));
				}// if
			}// for
		}// if
		
		return set;
	}// portalFwdDependency()
	
	public DependingBlockSet portalRevDependency()
	{
		final DependingBlock b = this;
		final DependingBlockSet set = new DependingBlockSet();
		if(b.getType() == Material.PORTAL)
		{
			for(Block bAdj: Util.getEnclosingBlocks(Util.getBlockAt(b.block.getLocation())))
			{
				set.add(DependingBlock.from(bAdj, Action.RESTORE));
			}// for
		}// if
		
		return set;
	}// portalRevDependency()
	
	
	
	public DependingBlockSet vineFwdDependency()
	{
		final DependingBlock b = this;
		final DependingBlockSet set = new DependingBlockSet();
		if(b.getType() == Material.VINE)
		{
			final Block bBel = Util.getBlockBelow(b.block.getLocation());
			if(bBel.getType() == Material.VINE)
			{
				final VineDependingBlock vb = (VineDependingBlock)(DependingBlock.from(bBel, Action.RESTORE));

				// Clojure equivalent of the following paragraph: (zipmap (adjacent-directions) (repeat #{Orientation/ABOVE, Orientation/BESIDE}))
				final Map<BlockFace,Set<Orientation>> subtrahend = new EnumMap<BlockFace,Set<Orientation>>(BlockFace.class);
				for(BlockFace dir : Util.adjacentDirections())
				{ subtrahend.put(dir, EnumSet.of(Orientation.ABOVE, Orientation.BESIDE)); }
				assert(subtrahend.size() == 4);

				set.add(vb.difference(subtrahend));
			}// if
		}// if
		// TODO: This may not be accurate enough, so find a better criterion for when a vine can be stuck to a block:
		else if(Util.isSolid(b.block.getBlockState()))
		{
			for(BlockFace dir : Util.adjacentDirections())
			{
				final Block bAdj = Util.add(Util.getBlockAt(b.block.getLocation()), dir);
				if(bAdj.getType() == Material.VINE)
				{
					final VineDependingBlock vb = (VineDependingBlock)(DependingBlock.from(bAdj, Action.RESTORE));

					final Map<BlockFace,Set<Orientation>> subtrahend = new EnumMap<BlockFace,Set<Orientation>>(BlockFace.class);
					subtrahend.put(dir, EnumSet.of(Orientation.BESIDE));
					assert(subtrahend.size() == 1);

					set.add(vb.difference(subtrahend));
				}// if
			}// for
		}// elif
		
		return set;
	}// vineFwdDependency()
	
	public DependingBlockSet vineRevDependency()
	{
		final DependingBlock b = this;
		final DependingBlockSet set = new DependingBlockSet();
		
		if(b.getType() == Material.VINE)
		{
			final Block bAbove = b.getBlockAbove();
			if(bAbove.getType() == Material.VINE)
			{ set.add(DependingBlock.from(bAbove, Action.RESTORE)); }
			
			for(BlockFace dir : Util.adjacentDirections())
			{
				final Block bAdj = Util.add(Util.getBlockAt(b.block.getLocation()),dir);
				if(Util.isSolid(bAdj) && ((Vine)(b.block.getBlockState().getData())).isOnFace(dir))
				{ set.add(DependingBlock.from(bAdj, Action.RESTORE)); }// if
			}// for
		}// if
		
		return set;
	}// vineRevDependency()

	public DependingBlockSet allFwdDependencies()
	{
		// TODO: All the rest!
		return DependingBlockSet.union(DependingBlockSet.union(this.attachmentFwdDependency(), this.portalFwdDependency()), this.vineFwdDependency());
	}// allFwdDependencies()
	
	public DependingBlockSet allRevDependencies()
		// TODO: All the rest!
	{ return DependingBlockSet.union(DependingBlockSet.union(this.attachmentRevDependency(), this.portalRevDependency()), this.vineRevDependency()); }
	
	public Block getBlockAbove()
	{ return Util.getBlockAbove(this.block); }// getBlockAbove()
			
	} // DependingBlock
