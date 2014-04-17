package com.github.izbay.regengine.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;

import com.github.izbay.regengine.BlockImage;

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
	} // DependingBlock
