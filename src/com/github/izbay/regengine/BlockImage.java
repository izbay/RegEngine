/**
 * Representation of a block in the REG EN queue.  This will act as an abstraction over the final form of the
 * block storage, which will be determined by where Alex goes, and on the real manner of restoration.
 * BlockStates work surprisingly well, all considered.
 */
package com.github.izbay.regengine;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;

/**
 * @author jdjs
 *
 */
public class BlockImage 
{
	/**
	 * 
	 */
	private final Location blockLoc;
	private final BlockState stateImage;
	private final int restorationTargetTime; // Not used yet
	

	public BlockImage(final Block block, final int regenTime) {
		super();
		blockLoc = block.getLocation();
		stateImage = block.getState();
		restorationTargetTime = regenTime;
		assert(regenTime >= blockLoc.getWorld().getFullTime());
	}// ctor
	
	/**
	 * @return the Location
	 */
	public Location getLocation() { return blockLoc; }

	/**
	 * @return the BlockState
	 */
	public BlockState getBlockState() { return stateImage; }
	/**
	 * It may not be necessary or beneficial to keep a reference to the target restoration time with each block.
	 * On the other hand, if we start mergine REG EN regions, it could be very useful.
	 */
	
	public boolean isRestored() { return stateImage.equals(blockLoc.getBlock().getState()) ; }// isRestored()
	
	/**
	 * A block is placed back into the world.
	 * @return the calling object
	 */
	public BlockImage restore() {
		if( !this.isRestored() ) {
			stateImage.update(true);
			assert(this.isRestored());
			return this;
		}// if
		else throw new RuntimeException("Called BlockImage.restore() when already restored.");
	}// restore()
	
	public Material getType() { return stateImage.getType(); }
	
	public BlockVector getVector()
	{	return new BlockVector(blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ()); }
}// BlockImage
