/**
 * Representation of a block in the REG EN queue.  This will act as an abstraction over the final form of the
 * block storage, which will be determined by where Alex goes, and on the real manner of restoration.
 * BlockStates work surprisingly well, all considered.
 */
package com.github.izbay.regengine;

import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockVector;

/**
 * The equality question. It is of great importance to have a working equivalence test on blocks.  The current one, which tests whether their BlockStates are equal(), works quite well, but I doubt it will do what we want in every possible case.
 * 
 * Issues noted with BlockState.update():
 * - Vehicles aren't removed when blocks around them are destroyed--they just fall.  Nor are they removed during regeneration--they're buried.
 * - It stores whether a Detector Rail is activated (and probably a pressure plate, too), which state depends on the Entity perched atop.  Stationary Vehicles I'm considering treating, but not creatures.
 * - There's an issue with a Cactus block at <-207,69,287> on my test server.  Sometimes it reports failure after regeneration when, by appearances, it regen'd fine.
 * 	 - Yeah, I can't reproduce this any more after messing up my block arrangement.
 * - It doesn't catch the contents of a Flowerpot.
 * - Bad one: Water source blocks aren't equal after regeneration; if the routine doesn't stop phys. events, it can loop forever.  This opens a breach in my restoration guarantee.
 * 
 * @author jdjs
 *
 */
public class BlockImage implements BlockState /* TODO This may not go so smoothly if I modify BlockImage to use SerializedBlock. */
{
	/**
	 * 
	 */
	private final Location blockLoc;
	private final BlockState stateImage;
	/**
	 * It may not be necessary or beneficial to keep a reference to the target restoration time with each block.
	 * On the other hand, if we start merging REG EN regions, it could be very useful.
	 */
	//private final int restorationTargetTime; // Not used yet
	

	public BlockImage(final Block block /*, final int regenTime*/) {
		super();
		blockLoc = block.getLocation();
		stateImage = block.getState();
	//	restorationTargetTime = regenTime;
	//	assert(regenTime >= blockLoc.getWorld().getFullTime());
	}// ctor
	
	public BlockImage(final BlockState bs /*, final int regenTime*/) {
		super();
		stateImage = bs;
		blockLoc = bs.getLocation().clone();
		//restorationTargetTime = regenTime;
		//assert(regenTime >= blockLoc.getWorld().getFullTime());
	}// ctor

	/**
	 * Copy constructor (shallow copy of BlockState, which cannot easily be cloned).
	 * @param b
	 */
	public BlockImage(final BlockImage b) {
		super();
		stateImage = b.stateImage;
		blockLoc = b.getLocation().clone();
	}// Copy c.

	/**
	 * @return the Location
	 */
	public Location getLocation() { return blockLoc; }

	public BlockVector getBlockVector() {
			return new BlockVector(blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ());
	}
	/**
	 * @return the BlockState
	 */
	public BlockState getBlockState() { return stateImage; }
	
	public boolean isRestored() { return stateImage.equals(blockLoc.getBlock().getState()) ; }// isRestored()
	
	public boolean equals(final BlockImage other) {
		return stateImage.equals(other.stateImage);
	}// equals()

	public boolean equals(final Block block) {
		return stateImage.equals(block.getState());
	}// equals()
	
	public boolean equals(final BlockState other) {
		return stateImage.equals(other);
	}// equals()

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
	
	public World getWorld() { return blockLoc.getWorld(); }

	/**
	 * @param metadataKey
	 * @param newMetadataValue
	 * @see org.bukkit.metadata.Metadatable#setMetadata(java.lang.String, org.bukkit.metadata.MetadataValue)
	 */
	public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
		stateImage.setMetadata(metadataKey, newMetadataValue);
	}

	/**
	 * @param metadataKey
	 * @return
	 * @see org.bukkit.metadata.Metadatable#getMetadata(java.lang.String)
	 */
	public List<MetadataValue> getMetadata(String metadataKey) {
		return stateImage.getMetadata(metadataKey);
	}

	/**
	 * @return
	 * @see org.bukkit.block.BlockState#getBlock()
	 */
	public Block getBlock() {
		return stateImage.getBlock();
	}

	/**
	 * @return
	 * @see org.bukkit.block.BlockState#getData()
	 */
	public MaterialData getData() {
		return stateImage.getData();
	}

	/**
	 * @param metadataKey
	 * @return
	 * @see org.bukkit.metadata.Metadatable#hasMetadata(java.lang.String)
	 */
	public boolean hasMetadata(String metadataKey) {
		return stateImage.hasMetadata(metadataKey);
	}

	/**
	 * @return
	 * @deprecated
	 * @see org.bukkit.block.BlockState#getTypeId()
	 */
	public int getTypeId() {
		return stateImage.getTypeId();
	}

	/**
	 * @return
	 * @see org.bukkit.block.BlockState#getLightLevel()
	 */
	public byte getLightLevel() {
		return stateImage.getLightLevel();
	}

	/**
	 * @param metadataKey
	 * @param owningPlugin
	 * @see org.bukkit.metadata.Metadatable#removeMetadata(java.lang.String, org.bukkit.plugin.Plugin)
	 */
	public void removeMetadata(String metadataKey, Plugin owningPlugin) {
		stateImage.removeMetadata(metadataKey, owningPlugin);
	}

	/**
	 * @return
	 * @see org.bukkit.block.BlockState#getX()
	 */
	public int getX() {
		return stateImage.getX();
	}

	/**
	 * @return
	 * @see org.bukkit.block.BlockState#getY()
	 */
	public int getY() {
		return stateImage.getY();
	}

	/**
	 * @return
	 * @see org.bukkit.block.BlockState#getZ()
	 */
	public int getZ() {
		return stateImage.getZ();
	}

	/**
	 * @param loc
	 * @return
	 * @see org.bukkit.block.BlockState#getLocation(org.bukkit.Location)
	 */
	public Location getLocation(Location loc) {
		return stateImage.getLocation(loc);
	}

	/**
	 * @return
	 * @see org.bukkit.block.BlockState#getChunk()
	 */
	public Chunk getChunk() {
		return stateImage.getChunk();
	}

	/**
	 * @param data
	 * @see org.bukkit.block.BlockState#setData(org.bukkit.material.MaterialData)
	 */
	public void setData(MaterialData data) {
		stateImage.setData(data);
	}

	/**
	 * @param type
	 * @see org.bukkit.block.BlockState#setType(org.bukkit.Material)
	 */
	public void setType(Material type) {
		stateImage.setType(type);
	}

	/**
	 * @param type
	 * @return
	 * @deprecated
	 * @see org.bukkit.block.BlockState#setTypeId(int)
	 */
	public boolean setTypeId(int type) {
		return stateImage.setTypeId(type);
	}

	/**
	 * @return
	 * @see org.bukkit.block.BlockState#update()
	 */
	public boolean update() {
		return stateImage.update();
	}

	/**
	 * @param force
	 * @return
	 * @see org.bukkit.block.BlockState#update(boolean)
	 */
	public boolean update(boolean force) {
		return stateImage.update(force);
	}

	/**
	 * @param force
	 * @param applyPhysics
	 * @return
	 * @see org.bukkit.block.BlockState#update(boolean, boolean)
	 */
	public boolean update(boolean force, boolean applyPhysics) {
		return stateImage.update(force, applyPhysics);
	}

	/**
	 * @return
	 * @deprecated
	 * @see org.bukkit.block.BlockState#getRawData()
	 */
	public byte getRawData() {
		return stateImage.getRawData();
	}

	/**
	 * @param data
	 * @deprecated
	 * @see org.bukkit.block.BlockState#setRawData(byte)
	 */
	public void setRawData(byte data) {
		stateImage.setRawData(data);
	}
	
	
}// BlockImage
