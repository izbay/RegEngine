package com.github.izbay.regengine.block;

import org.bukkit.block.BlockFace;
import org.bukkit.material.Bed;
//import org.bukkit.material.Door;
import org.bukkit.material.PistonBaseMaterial;

import com.github.izbay.regengine.BlockImage;
import com.github.izbay.util.Util;

public class CompoundDependingBlock extends DependingBlock 
{
	public static enum Position {MASTER, SLAVE}; // Don't ask
	
	public final Position position;

	// Merge constructor.
	public CompoundDependingBlock(final CompoundDependingBlock d1, final CompoundDependingBlock d2)
	{
		super(d1, d2);
		if(d1.position != d2.position) throw new IllegalArgumentException("Bad matchup in dyadic CompoundDependingBlock ctor.");
		this.position = d1.position;
	}// ctor
	
	// Copy constructor.
	public CompoundDependingBlock(final CompoundDependingBlock rhs)
	{
		super(rhs.block, rhs.action());
		this.position = rhs.position;
	}// ctor
	

	/**
	 * A 'DESTROY' action in either block will override any type in the other.  
	 * TODO: Show that this method is commutative.
	 * @param d2
	 * @return
	 */
	@Override
	public CompoundDependingBlock mergeWith(final DependingBlock d2)
	{
		if(!(d2 instanceof CompoundDependingBlock)) throw new IllegalArgumentException(); 
		//if(this.coord() != d2.coord()) { throw new IllegalArgumentException(); }
		return new CompoundDependingBlock(this, (CompoundDependingBlock)(d2));
	}// mergeWith()


	/**
	 * Use DependingBlock.from().
	 * @param block
	 * @param action
	 */
	protected CompoundDependingBlock(final BlockImage b, final Action action/*, final Position pos*/) 
	{
		super(b, action);
		assert(BlockTypes.duple.contains(block.getType()));
		
		switch(block.getType())
		{
		case BED_BLOCK: // Somewhat surprisingly, the "head" of the bed is not dominating end.  When you place a bed, the foot goes in the target square, the bed facing towards you.
			final Bed bedData = (Bed)(block.getBlockState().getData());
			this.position = bedData.isHeadOfBed() ? Position.SLAVE : Position.MASTER; 
			break;
		case WOODEN_DOOR: // Less surprisingly but equally amusingly, the "bottom" of the door is the more important.
		case IRON_DOOR_BLOCK:
			//final Door doorData = (Door) (block.getBlockState().getData());
			// Since Door.isTopHalf() is deprecated--I don't know why--we use a simple workaround, namely, the fact that two doors cannot be stacked vertically.
			this.position = (Util.getBlockBelow(block).getType() == block.getType()) ? Position.SLAVE : Position.MASTER;
			break;
		case DOUBLE_PLANT: // The root dominates.  That makes some sense.
			this.position = (Util.getBlockBelow(block).getType() == block.getType()) ? Position.SLAVE : Position.MASTER;
			break;
		case PISTON_BASE:
		case PISTON_STICKY_BASE:
			this.position = Position.MASTER;
			break;
		case PISTON_EXTENSION:
			this.position = Position.SLAVE;
			break;
        default:
			throw new IllegalArgumentException("A block was passed to the CompoundDependingBlock constructor not classified within BlockTypes.duple .");
		}// switch
	}// ctor
	
	public DependingBlockSet getEntire()
	{
		CompoundDependingBlock other = null;
		final DependingBlockSet set = new DependingBlockSet(this);// Add first elt
		switch(this.getType())
		{
		case BED_BLOCK:
			assert(this.block.getBlockState().getData() instanceof Bed);
			final Bed bedData = (Bed)(block.getBlockState().getData());
			if(this.position == Position.MASTER)
				other = new CompoundDependingBlock(new BlockImage(Util.add(this.block.getLocation(), bedData.getFacing()).getBlock().getState()), this.action);
			else
				other = new CompoundDependingBlock(new BlockImage(Util.add(this.block.getLocation(), bedData.getFacing().getOppositeFace()).getBlock().getState()), this.action);
			break;
        // We might as well lump these three together, since they work identically:
		case DOUBLE_PLANT:
		case WOODEN_DOOR: 
		case IRON_DOOR_BLOCK:
			other = new CompoundDependingBlock(new BlockImage( Util.add(this.getLocation(), (this.position == Position.MASTER) ? BlockFace.UP : BlockFace.DOWN ).getBlock()), this.action);
			break;
		case PISTON_BASE:
		case PISTON_STICKY_BASE:
			assert(position == Position.MASTER);
			assert(this.block.getBlockState().getData() instanceof PistonBaseMaterial);
			final PistonBaseMaterial pb = (PistonBaseMaterial)(block.getBlockState().getData());
			if(pb.isPowered())
			{	
				other = new CompoundDependingBlock(new BlockImage(Util.add(this.getLocation(), pb.getFacing()).getBlock()), this.action); 
				assert(other.position == Position.MASTER);
			}// if
			break;
		case PISTON_EXTENSION:
			assert(position == Position.SLAVE);
			other = new CompoundDependingBlock(new BlockImage(Util.getAttachedBlock(Util.getBlockAt(this.getLocation()))), this.action);
			assert(other.position == Position.MASTER);
			break;
        default:
			throw new Error("Dropped out of the switch statement in CompoundDependingBlock.getEntire(); this should never happen.");	
		}// switch
		
		assert(this.action() == other.action());
		assert(this.position != other.position);
		
		if( other != null )
		{	set.add(other);	}// if
		return set;
	}// getEntire()
	
	/*
	public static DependingBlockSet getEntire(final )
	
	public static DependingBlock from(final BlockImage b, final Action act)
	{
		
	}
	*/
	

}// CompoundDependingBlock
