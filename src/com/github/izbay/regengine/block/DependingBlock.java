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

public class DependingBlock 
{
	protected final BlockImage block;
	protected /*final*/ Action action;
	//public final BlockVector coord;

	public BlockImage block() { return block; }

	public Action action() {	return this.action;	}


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
	 * Note that no copying is done here.
	 * @param block
	 * @param action
	 */
	protected DependingBlock(BlockImage block, Action action) {
		super();
		this.block = block;
		this.action = action;
		
		//assert(Util.areBlocksEquivalent(block, this));
	}// ctor
	
//	public boolean equals(final DependingBlock rhs)
	
	/**
	 * @param a1
	 * @param a2
	 * @return
	 * Claim: Commutative operation. $\forall a1,a2 \in Action. mergeActions(a1,a2) == mergeActions(a2,a1)$.
	 * Proof: From the commutativity of the logical OR operator used in these tests, it follows that the retval is independent of the parameters' order.
	 * Claim: Associative operation. $\forall a1,a2,a3 \in Action. mergeActions(a1, mergeActions(a2,a3)) == mergeActions(mergeActions(a1,a2), a3) $.
	 * Proof: By case:
	 * 		if a1, a2, or a3 = DESTROY, then mA(a1,mA(a2,a3)) = DESTROY and mA(mA(a1,a2),a3) = DESTROY
	 * 		Proof: Verify for a1, a2, & a3 separately.
	 * 		if none equals DESTROY, but one equals RESTORE_AFTER_LOSS, then mA(a1,mA(a2,a3)) = RESTORE_AFTER_LOSS and mA(mA(a1,a2),a3) = RESTORE_AFTER_LOSS
	 * 		Proof: Similar, using the second clause in the condition.
	 * 		Finish with the 'else' clause.
	 * Input data are thus completely partitioned; because the method is associative over all equiv. classes of input in the partitioning, it is associative overall; Q.E.D.
	 */
	public static Action mergeActions(final Action a1, final Action a2)
	{
		if(a1 == Action.DESTROY || a2 == Action.DESTROY) { 
			return Action.DESTROY;
		} else if (a1 == Action.RESTORE_AFTER_LOSS || a2 == Action.RESTORE_AFTER_LOSS) { 
				return Action.RESTORE_AFTER_LOSS;
		} else return Action.RESTORE;
	}// mergeActions()

	/**
	 * 'Merge' ctor.
	 * @param d1
	 * @param d2
	 * Correctness claim: The 'action' field of the new object will be DESTROY if either parameter's is.  If neither's is DESTROY and either's is RESTORE_AFTER_LOSS, the new object's will be RESTORE_AFTER_LOSS.  Otherwise its will be RESTORE.
	 * Proof: Directly from the implementation of mergeActions().
	 * Correctness claim: The new object is equivalent, modulo Action type, to both d1 and d2.
	 * 		This relationship is embodied in the Util.areBlocksEquivalent(DependingBlock,DependingBlock) method, and I will represent it locally using the '=' character.  There must be no mistaking block equivalence for the equals() function.
	 * Proof: The method will not return, due to exception, if d1 ≠ d2.  Because the <new object> shares structure with d1, it and d1 are also equivalent.  I.e., new DependingBlock(d1,d2).equals(new DependingBlock(d2,d1)).  Because Util.areBlocksEquivalent() is an equiv. relation (see util.java), by transitivity d1 = d2 = <new instance>.
	 * Claim: Commutative operation.  I.e., new DependingBlock(d1,d2).equals(new DependingBlock(d2,d1)).
	 * 		This form of equality is stricter than that described with Util.areBlocksEquivalent(), requiring the action fields also be equal.
	 * Proof: It may help to establish the lemma:
	 * 				Util.areBlocksEquivalent(d,d') && d.action == d'.action => d.equals(d')
	 * 		Proof: Util.areBlocksEquivalent(DependingBlock,DependingBlock) reduces to BlockImage.equals(BlockImage).  DependingBlock.equals(DependingBlock) also reduces to BlockImage.equals(BlockImage) && Action.equals(Action). ∎
	 * 	Let d' be the new DependingBlock constructed.  We know d' = d1 = d2 by the prior proof.  This leaves us with determining that d'.action comes out the same irrespective of parameter order.
	 * 	We get this from the commutativity of mergeActions(); this proves the commutativity of the ctor. ∎
	 * Claim: Associative operation.  $\forall d1,d2,d2 \in DependingBlock. (new DependingBlock(d1, new DependingBlock(d2,d3))).equals(new DependingBlock(new DependingBlock(d1,d2),d3))$
	 * Proof: We have d1.block = d2.block = d3.block = (new DependingBlock(d1, new DependingBlock(d2, d3))).block = (new DependingBlock(new DependingBlock(d1,d2), d3)).block by the associativity of the equivalence operator.  Assignment to the .action field obeys associativity by dint of mergeActions()'s associativity.∎ 
	 */
	public DependingBlock(final DependingBlock d1, final DependingBlock d2) {
		// Chain to "basic" constructor.
		this(d1.block, mergeActions(d1.action, d2.action));
		if(!Util.areBlocksEquivalent(d1,d2))  { throw new IllegalArgumentException("Block mismatch in DependingBlock() ctor."); }
		//if(!d1.coord().equals(d2.coord()))  { throw new IllegalArgumentException("Vector mismatch in DependingBlock() ctor."); }
	}// ctor

	public MaterialData getData() {	return block.getData(); }

	/**
	 * A 'DESTROY' action in either block will override any type in the other.  
	 * @param d2
	 * @return new DependingBlock instance.
	 * Claim: Commutative and associative operation, i.e., it does not matter which object is the caller.  
	 * Proof: Follows from these properties of DependingBlock(DependingBlock, DependingBlock).
	 */
	public DependingBlock mergeWith(final DependingBlock d2) {
		// The vector coordinates of the blocks must match; however, we don't need to test them here--the constructor called on the following line will.
		final DependingBlock dNew = new DependingBlock(this, d2);
		// A 'hard' dependency for either input should give a hard dependency on the output.
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
	
	public boolean equals(final DependingBlock rhs)	{	return this.action().equals(rhs.action()) && this.block().equals(rhs.block()); } 

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

	//public boolean equals(final DependingBlock rhs)

} // DependingBlock
