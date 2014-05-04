package com.github.izbay.regengine.block;

//import java.lang.reflect.Method;
//import java.util.EnumMap;
//import java.util.EnumSet;
//import java.util.Map;
//import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;

import org.bukkit.material.MaterialData;
import org.bukkit.material.Vine;

import com.github.izbay.regengine.BlockImage;
//import com.github.izbay.regengine.block.VineDependingBlock.Orientation;
import com.github.izbay.util.Util;

public class DependingBlock {
		public final BlockImage block;
		protected /*final*/ Action action;
		//public final BlockVector coord;

		public static DependingBlock from(final Block b, final Action act)
		{	return DependingBlock.from(new BlockImage(b), act);	}

/*		public static DependingBlock from(final Block b)
		{	return DependingBlock.from(new BlockImage(b));	}
		*/

		public static DependingBlock from(final BlockState b, final Action act)
		{	return DependingBlock.from(new BlockImage(b), act);	}

/*		public static DependingBlock from(final BlockImage b)
		{	return DependingBlock.from(b, Action.DESTROY);	}
		*/
/*		public static DependingBlock from(final BlockState b)
		{	return DependingBlock.from(new BlockImage(b));	}
		*/

/*		public static DependingBlock from(final BlockImage b)
		{	return DependingBlock.from(b, Action.DESTROY);	}
		*/

		/**
		 * Constructor.  But: protected because the static factory meth DependingBlock.from() should be chosen instead in most cases.
		 * @param block
		 * @param action
		 */
		protected DependingBlock(BlockImage block, Action action) {
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
			this(d1.block, (d1.action == Action.DESTROY ? Action.DESTROY : 
					(d2.action == Action.DESTROY ? Action.DESTROY : 
						(d1.action == Action.RESTORE_AFTER_LOSS || d2.action == Action.RESTORE_AFTER_LOSS) ? Action.RESTORE_AFTER_LOSS : Action.RESTORE)) );
			if(!d1.coord().equals(d2.coord()))  { throw new IllegalArgumentException("Vector mismatch in DependingBlock() ctor."); }
		}// ctor
		
		public MaterialData getData()
		{	return block.getData(); }
		
		/**
		 * A 'DESTROY' action in either block will override any type in the other.  
		 * TODO: Show that this method is commutative.
		 * @param d2
		 * @return new DependingBlock instance.
		 */
		public DependingBlock mergeWith(final DependingBlock d2)
		{
		//	if(!this.coord().equals(d2.coord()))  { throw new IllegalArgumentException(); }
			final DependingBlock dNew = new DependingBlock(this, d2);
			// A 'hard' dependency for either input should give a hard on the output.
			assert(!((this.action().isHardDependency() || d2.action().isHardDependency()) && !dNew.action().isHardDependency()));
			return dNew;
		}// mergeWith()
		
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

		/**
		 * Factory method.
		 * @param b
		 * @param act
		 * @return
		 */
		public static DependingBlock from(final BlockImage b, final Action act)
		{
			switch(b.getType())
			{
			case VINE:
				return new VineDependingBlock(b, act);
			default:
				if(BlockTypes.duple.contains(b.getType()))
				{	return new CompoundDependingBlock(b, act); }// if
				else return new DependingBlock(b, act);
			}// switch
		}// DependingBlock.from()
		
		public Action action()
		{	return this.action;	}
	
	//public DependingBlockSet 
		
				
		public BlockVector coord() {	return block.getBlockVector(); }
		
		public DependingBlock setAction(final Action act)
		{ return new DependingBlock(block, act); }
		
		public Material getType()
		{	return block.getType(); }

		public Location getLocation()
		{	return block.getLocation(); }
		
	public DependingBlockSet gravityBoundFwdDependency()
	{
		final Block bAbove = Util.getBlockAbove(this.block);
		// TODO: Compare with the results of Material.hasGravity().
		if( BlockTypes.gravityBound.contains(bAbove.getType()) )
		{	return new DependingBlockSet(DependingBlock.from(bAbove, Action.RESTORE_AFTER_LOSS)); }// if
		else
		{	return DependingBlockSet.emptySet(); }// else
	}// gravityBoundFwdDependency()

	public DependingBlockSet gravityBoundRevDependency()
	{
		if( BlockTypes.gravityBound.contains(block.getType()) )
		{	return new DependingBlockSet(DependingBlock.from(Util.getBlockBelow(block), Action.RESTORE)); }
		else return DependingBlockSet.emptySet(); 	
	}// gravityBoundRevDependency()

	// Component blocks of compounds are mutually dependent.  The forward and reverse dependency checks are, in this case, the same.
	public DependingBlockSet dupleBlockDependency()
	{
		if(BlockTypes.duple.contains(this.getType()))
		{
			assert( this instanceof CompoundDependingBlock );
			if(!(this instanceof CompoundDependingBlock)) throw new IllegalArgumentException();
			return ((CompoundDependingBlock)this).getEntire();
		}// if
		else
			return DependingBlockSet.emptySet();
		/*
		final DependingBlockSet set = new DependingBlockSet();
		if(BlockTypes.duple.contains(b.getType()))
		{
		//	set.add(b);
			switch(b.getType())
			{
			case BED_BLOCK:
				set.add(DependingBlock.from(BlockTypes.getRestOfBed(b.block.getBlockState()), b.action);
				break;
			case WOODEN_DOOR:
					
			}
		}// if
		return set;
		*/
	}// dupleBlockDependency()
	
	public boolean isAttachable() { return Util.isAttachable(this.block); }
	
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
		
		if(Util.isAttachable(b.block.getData()))
		{	set.add(DependingBlock.from(Util.getAttachedBlock(b.block), Action.RESTORE)); }// if
		
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
				if(bAdj.getType() == Material.PORTAL || bAdj.getType() == Material.OBSIDIAN)
				{	set.add(DependingBlock.from(bAdj, Action.RESTORE));	}// if
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

				set.add(vb.difference(VineDependingBlock.subtrahendOverhead()));
			}// if
		}// if
		// TODO: This may not be accurate enough, so find a better criterion for when a vine can be stuck to a block:
		else if(getType().isSolid())
		{
			for(BlockFace dir : Util.adjacentDirections())
			{
				final Block bAdj = Util.add(Util.getBlockAt(b.block.getLocation()), dir);
				if(bAdj.getType() == Material.VINE)
				{
					final VineDependingBlock vb = (VineDependingBlock)(DependingBlock.from(bAdj, Action.RESTORE));

					set.add(vb.difference(VineDependingBlock.subtrahendAdjacent(dir.getOppositeFace())));
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
				if(Util.isSolid(bAdj) && ((Vine)(b.getData())).isOnFace(dir))
				{ set.add(DependingBlock.from(bAdj, Action.RESTORE)); }// if
			}// for
		}// if
		
		return set;
	}// vineRevDependency()
	public DependingBlockSet supportFwdDependency()
	{
		final Block bAbove = Util.getBlockAbove(this.block);
		if( BlockTypes.needingSupport.contains(bAbove.getType()) )
		{ return new DependingBlockSet(DependingBlock.from(bAbove, Action.DESTROY)); }
		else
		{	return DependingBlockSet.emptySet(); }// else
	}// supportFwdDependency()
	
	public DependingBlockSet supportRevDependency()
	{
		if( BlockTypes.needingSupport.contains(this.block.getType()) )
		{
			return new DependingBlockSet(DependingBlock.from(Util.getBlockBelow(block), Action.RESTORE));
		}// if
		else return DependingBlockSet.emptySet(); 
	}// supportRevDependency()

	public DependingBlockSet allFwdDependencies()
	{ return DependingBlockSet.union(DependingBlockSet.union(DependingBlockSet.union(DependingBlockSet.union(DependingBlockSet.union(this.dupleBlockDependency(), this.attachmentFwdDependency()), this.portalFwdDependency()), this.vineFwdDependency()), this.gravityBoundFwdDependency()), this.supportFwdDependency()); }// allFwdDependencies()

	//public static final Method[] forwardDependencies = { supportRevDependency };
	
	// NB: At the moment, the forward & reverse searches include a common phase.  Can we factor out the 'compound block' part of the search so we run it only once per block?
	public DependingBlockSet allRevDependencies()
	{ return DependingBlockSet.union(DependingBlockSet.union(DependingBlockSet.union(DependingBlockSet.union(DependingBlockSet.union(this.dupleBlockDependency(), this.attachmentRevDependency()), this.portalRevDependency()), this.vineRevDependency()), this.gravityBoundRevDependency()), this.supportRevDependency()); }
	
	public Block getBlockAbove()
	{ return Util.getBlockAbove(this.block); }// getBlockAbove()
	
	public Block getAttachedBlock() {
		return Util.getAttachedBlock(this.block);
	}// getAttachedBlock()

			
} // DependingBlock
