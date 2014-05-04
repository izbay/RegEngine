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

import com.github.izbay.regengine.serialize.SerializedBlock;

/**
 * The equality question. It is of great importance to have a working equivalence test on blocks.  The current one, which tests whether their BlockStates are equal(), works quite well, but I doubt it will do what we want in every possible case.
 * 
 * Issues noted with BlockState.update():
 * - Vehicles aren't removed when blocks around them are destroyed--they just fall.  Nor are they removed during regeneration--they're buried.
 * - It stores whether a Detector Rail is activated (and probably a pressure plate, too), which state depends on the Entity perched atop.  Stationary Vehicles I'm considering treating, but not creatures.
 * - Bad one: Water source blocks aren't equal after regeneration; if the routine doesn't stop phys. events, it can loop forever.  This opens a breach in my restoration guarantee.
 * 
 * @author jdjs
 *
 */
public class BlockImage implements BlockState 
{
	/**
	 * 
	 */
	protected final Location blockLoc;
	protected final SerializedBlock block;
//	protected final MaterialData blockData;
	/**
	 * It may not be necessary or beneficial to keep a reference to the target restoration time with each block.
	 * On the other hand, if we start merging REG EN regions, it could be very useful.
	 */
	//private final int restorationTargetTime; // Not used yet
	

	public BlockImage(final Block block /*, final int regenTime*/) {
		super();
		blockLoc = block.getLocation();
		this.block = new SerializedBlock(block);
		//stateImage = block.getState();
	//	restorationTargetTime = regenTime;
	//	assert(regenTime >= blockLoc.getWorld().getFullTime());
	}// ctor
	
	public BlockImage(final BlockState bs /*, final int regenTime*/) {
		super();
		//stateImage = bs;
		this.block = new SerializedBlock(bs);
		blockLoc = bs.getLocation().clone(); // Do I need the copy op?
		//restorationTargetTime = regenTime;
		//assert(regenTime >= blockLoc.getWorld().getFullTime());
	}// ctor

	/**
	 * Copy constructor.  As deep or shallow as the SerializedBlock() copy c.
	 * @param b
	 */
	public BlockImage(final BlockImage b) {
		super();
		//stateImage = b.stateImage;
		this.block = new SerializedBlock(b.block);
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
	 * I
	 * @return the BlockState
	 */
	//public BlockState getBlockState() { return stateImage; }
	
	/**
	 * Intention: return true if the in-world block corresponding to this snapshot is equivalent in its properties.
	 * @return
	 */
	public boolean isRestored() { 
		return block.isPlaced();
		//	.equals(blockLoc.getBlock().getState()) ; 
	}// isRestored()
	
	/**
	 * TODO I don't know if this is the right kind of equivalence for an equals() override.
	 * @param other
	 * @return
	 */
	/*
	public boolean equals(final BlockImage other) {
	//	return stateImage.equals(other.stateImage);
		return this.block.equals(other) && this.getLocation().equals(other.getLocation());
	}// equals()
	*/

	public boolean equals(final Block block) {
		return this.block.isBlockEquivalent(block.getState());
	}// equals()
	
	/*
	public boolean equals(final BlockState other) {
		return block.equals(other.block);
	}// equals()
	*/	

	/**
	 * A block is placed back into the world.  If the operation fails, an exception will be thrown per update(true).
	 * @return the calling object
	 */
	public BlockImage restore()
	{	
		update(true, false); 
		return this;
	}// restore()

	public Material getType() { return block.getType(); }
	
	public BlockVector getVector()
	{	return new BlockVector(blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ()); }
	
	public World getWorld() { return blockLoc.getWorld(); }

	/**
	 * FIXME: Stub
	 * @param metadataKey
	 * @param newMetadataValue
	 * @see org.bukkit.metadata.Metadatable#setMetadata(java.lang.String, org.bukkit.metadata.MetadataValue)
	 */
	public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
		//stateImage.setMetadata(metadataKey, newMetadataValue);
		throw new Error("BlockImage.setMetadata() stub.");
	}

	/**
	 * FIXME: Stub
	 * @param metadataKey
	 * @return
	 * @see org.bukkit.metadata.Metadatable#getMetadata(java.lang.String)
	 */
	public List<MetadataValue> getMetadata(String metadataKey) {
		throw new Error("BlockImage.getMetadata() stub.");
	}

	/**
	 * The block currently in the location identified by calling object's location record.
	 * @return
	 * @see org.bukkit.block.BlockState#getBlock()
	 */
	public Block getBlock() {
		return getLocation().getBlock();
	}

	/**
	 * FIXME Bad news: This needs to return an instance of the most specific applicable MaterialData subclass.
	 * @return
	 * @see org.bukkit.block.BlockState#getData()
	 */
	public MaterialData getData() { return block.getData(); }

	/**
	 * FIXME Stub.
	 * @param metadataKey
	 * @return
	 * @see org.bukkit.metadata.Metadatable#hasMetadata(java.lang.String)
	 */
	public boolean hasMetadata(String metadataKey) {
		throw new Error("BlockImage.hasMetadata() stub.");
	}

	/**
	 * FIXME Stub.
	 * @return
	 * @deprecated
	 * @see org.bukkit.block.BlockState#getTypeId()
	 */
	public int getTypeId() {
		throw new Error("BlockImage.getTypeId() stub.");
	}

	/**
	 * FIXME Stub.
	 * @return
	 * @see org.bukkit.block.BlockState#getLightLevel()
	 */
	public byte getLightLevel() {
		throw new Error("BlockImage.getLightLevel() stub.");
	}

	/**
	 * FIXME Stub.
	 * @param metadataKey
	 * @param owningPlugin
	 * @see org.bukkit.metadata.Metadatable#removeMetadata(java.lang.String, org.bukkit.plugin.Plugin)
	 */
	public void removeMetadata(String metadataKey, Plugin owningPlugin) {
		throw new Error("BlockImage.removeMetadata() stub.");
	}

	/**
	 * @return
	 * @see org.bukkit.block.BlockState#getX()
	 */
	public int getX() {
		return getLocation().getBlockX();
	}

	/**
	 * @return
	 * @see org.bukkit.block.BlockState#getY()
	 */
	public int getY() {
		return getLocation().getBlockY();
	}

	/**
	 * @return
	 * @see org.bukkit.block.BlockState#getZ()
	 */
	public int getZ() {
		return getLocation().getBlockZ();
	}

	/**
	 * FIXME Stub.
	 * @param loc
	 * @return
	 * @see org.bukkit.block.BlockState#getLocation(org.bukkit.Location)
	 */
	public Location getLocation(Location loc) {
		throw new Error("BlockImage.getLocation/1 stub.");
	}

	/**
	 * @return
	 * @see org.bukkit.block.BlockState#getChunk()
	 */
	public Chunk getChunk() { return block.getChunk(); }

	/**
	 * FIXME: Stub
	 * @param data
	 * @see org.bukkit.block.BlockState#setData(org.bukkit.material.MaterialData)
	 */
	public void setData(MaterialData data) {
		throw new Error("The mutator semantics of BlockImage.setData() are unacceptable for implementation.");
		//stateImage.setData(data);
	}

	/**
	 * FIXME: Stub
	 * @param type
	 * @see org.bukkit.block.BlockState#setType(org.bukkit.Material)
	 */
	public void setType(Material type) {
		throw new Error("Stub mutator BlockImage.setType() unacceptable.");
		//stateImage.setType(type);
	}

	/**
	 * FIXME: Stub
	 * @param type
	 * @return
	 * @deprecated
	 * @see org.bukkit.block.BlockState#setTypeId(int)
	 */
	public boolean setTypeId(int type) {
		throw new Error("Stub mutator BlockImage.setTypeId() unacceptable.");
//		return stateImage.setTypeId(type);
	}

	/**
	 * @return
	 * @see org.bukkit.block.BlockState#update()
	 */
	public boolean update() { return block.place(); }

	/**
	 * If 'force' is true, will generate an exception if the update failed.
	 * @param force
	 * @return
	 * @see org.bukkit.block.BlockState#update(boolean)
	 */
	public boolean update(final boolean force) {
		final boolean res = update();
		if(force && !this.isRestored()) { throw new Error("Failure in BlockImage.update(true)."); }
		return res;
	}

	/**
	 * @param force
	 * @param applyPhysics
	 * @return
	 * @see org.bukkit.block.BlockState#update(boolean, boolean)
	 */
	public boolean update(final boolean force, final boolean applyPhysics) {
		if(applyPhysics)
		{	return update(force); }// if
		else
		{	
			RegEnginePlugin.getInstance().doWithDisabledPhysics(new Runnable() { public void run() {
					update(force);
				}// lambda
			});
				
			return isRestored();
		}// else
	}// update()

	/**
	 * @return
	 * @deprecated
	 * @see org.bukkit.block.BlockState#getRawData()
	 */
	public byte getRawData() {
		return block.getRawData();
	}

	/**
	 * FIXME Stub
	 * @param data
	 * @deprecated
	 * @see org.bukkit.block.BlockState#setRawData(byte)
	 */
	public void setRawData(byte data) {
		throw new Error("BlockImage.setRawData() stub.");
	}
	
	
}// BlockImage
