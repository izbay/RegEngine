package com.github.izbay.regengine;

//import java.util.LinkedHashSet;
//import java.util.Set;

//import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Arrays;
//import java.util.Deque;
//import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
//import java.util.LinkedHashMap;
import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
import java.util.Set;

//import net.minecraft.server.v1_7_R3.Block;
//import net.minecraft.server.v1_7_R3.Material;

//import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
//import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
//import java.util.Collection;


import com.github.izbay.regengine.block.Action;
import com.github.izbay.regengine.block.DependingBlock;
import com.github.izbay.regengine.block.DependingBlockSet;
//import org.bukkit.Material;
//import org.bukkit.block.Block;
//import org.bukkit.block.BlockFace;
//import org.bukkit.block.BlockFace;
//import java.util.*;
//import org.bukkit.util.BlockVector;
import com.github.izbay.util.Util;

//import com.github.izbay.regengine.block.Action;
//import com.github.izbay.regengine.block.DependingBlock;
//import com.github.izbay.regengine.block.DependingBlockSet;
//import com.github.izbay.util.Util;

//import com.github.izbay.util.Util;

public class RegenBatch implements RegenBatchIface 
{
	public static enum Status { UNUSED, ALTERED, PENDING_RESTORATION, DONE, CANCELLED };

	protected final World world;
	protected final long delay;
	public final DependingBlockSet blocks;
	public final Material newMaterial;
	public final Plugin plugin;
	//public Map<Location, SerializedBlock> blockMap;
	protected Status status;
	public final LinkedList<SerializedBlock> blockOrder = new LinkedList<SerializedBlock>();

	protected static Map<World,LinkedHashSet<RegenBatch>> activeBatches = new LinkedHashMap<World,LinkedHashSet<RegenBatch>>();

	//	private BlockImage[] blocks; // Comment this out until I'm ready to use it

	public static Map<World,LinkedHashSet<RegenBatch>> activeBatches() { return activeBatches; }
	public static Set<RegenBatch> activeBatches(final World world) { return activeBatches.get(world); }

	/* (non-Javadoc)
	 * @see com.github.izbay.regengine.RegenBatchIface#getWorld()
	 */
	@Override
	public World world() { return world; }

	@Override
	public long delay() { return delay; }

	public Status status() 
	{ 
		assert(activeBatches.containsKey(world));
		assert(status == Status.DONE || activeBatches.get(world).contains(this));
		return this.status; 
	}// status()

	public boolean isRunning() { return status == Status.PENDING_RESTORATION; }

	public boolean isActive() { return status != Status.DONE && status != Status.CANCELLED; }

	public boolean isDone() { return status == Status.DONE; }

	/* (non-Javadoc)
	 * @see com.github.izbay.regengine.RegenBatchIface#getRestorationTime()
	 */
	@Override
	public long getRestorationTime() { return delay + world.getFullTime(); }


	/**
	 * Factory method.
	 * @param blockVectors
	 * @param world
	 * @param targetTime
	 * @return
	 */
	public static RegenBatch destroying(final Plugin plugin, final Iterable<Vector> blockVectors, final World world, final long delay)
	{	return new RegenBatch(plugin, blockVectors, world, delay); }

	public static RegenBatch destroying(final Plugin plugin, final Vector[] blockVectors, final World world, final long delay)
	{	return new RegenBatch(plugin, Arrays.asList(blockVectors), world, delay); }

	/**
	/**
	 * @param world
	 * @param targetTime
	 */
	public RegenBatch(final Plugin plugin, final Iterable<Vector> blockVectors, final World world, final long delay) {
		super();

		if(!RegenBatch.activeBatches.containsKey(world)) RegenBatch.activeBatches.put(world, new LinkedHashSet<RegenBatch>());
		assert(RegenBatch.activeBatches.containsKey(world));

		assert(!RegenBatch.activeBatches.get(world).contains(this));
		RegenBatch.activeBatches.get(world).add(this);
		assert(!RegenBatch.activeBatches.get(world).contains(this));

		//		this.blocks = blocks;
		this.plugin = plugin;
		this.world = world;
		this.newMaterial = Material.AIR;
		this.delay = delay;
		final DependingBlockSet sIn = new DependingBlockSet();
		for(Vector v : blockVectors)
		{ 
			final Block b = Util.getBlockAt(v,world);
			if(b.getType() != newMaterial)
			{ sIn.add(DependingBlock.from(b, Action.DESTROY)); }// if
		}// for

		this.blocks = sIn.doFullDependencySearch();
		this.status = Status.UNUSED;
	}// ctor



	/* (non-Javadoc)
	 * @see com.github.izbay.regengine.RegenBatchIface#cancel()
	 */
	@Override
	public void cancel()
	{
		switch(this.status)
		{
		case UNUSED:
		case ALTERED:
			this.status = Status.CANCELLED;
			RegenBatch.removeFromActiveSet(this);
			break;
		case PENDING_RESTORATION:
			throw new Error("Cancelling mid-regeneration not yet implemented!");
		case DONE:
		case CANCELLED:
			assert(!this.isActive());
			assert(!this.isRunning());
			assert(!activeBatches.get(world).contains(this));
			break;
		default:
			throw new Error("Fell through RegenBatch.cancel(); shouldn't happen.");
		}// switch
	}// cancel

	/*	public DependingBlockSet doFwdDependencies()
	{
		Queue<DependingBlock>
	}*/

	/*
	public void alter(Plugin plugin, Location l, Material m)
	{
		Location normal = Util.normalizeLocation(l);
		Material backup = normal.getBlock().getType();
		if(backup != m){
			if(!blockMap.containsKey(normal)){
				blockMap.put(normal, new SerializedBlock(normal.getBlock()));
				BlockState state = normal.getBlock().getState();
				if(state instanceof Chest){
					((Chest)state).getBlockInventory().clear();
				} else if(state instanceof InventoryHolder){
					((InventoryHolder)state).getInventory().clear();
				}
				normal.getBlock().setType(m);
				regen(normal);	
			}
		}
	}
	 */

	public RegenBatch alterAndRestore()
	{
		batchAlter();
		assert(status == Status.ALTERED);
		queueBatchRestoration();
		assert(this.isRunning());
		return this;
	}// destroyAndRestore()

	/**
	 * Does *not* queue for regeneration.  To cause that as well, use .alterAndRestore().
	 * @return
	 */
	public RegenBatch batchAlter()
	{
		if (status != Status.UNUSED)
		{	throw new Error("Called RegenBatch.batchAlter() on an already-altered Batch.  (I think.)"); }// if
		else if(!blocks.isEmpty())
		{
			//this.blockMap = new LinkedHashMap<Location,SerializedBlock>();
			//this.blockOrder = new ArrayDeque<SerializedBlock>();
			for(DependingBlock d : this.blocks)
			{
				assert(d.getType() != this.newMaterial);
				//final Location normal = Util.normalizeLocation(d.getLocation());
				final Block b = Util.getBlockAt(d.getLocation()); 
				this.blockOrder.add(new SerializedBlock(b));
				//				this.blockMap.put(normal, new SerializedBlock(b));
			}// for
			//			assert(this.blockMap.size() == this.blocks.size());
			assert(this.blockOrder.size() == this.blocks.size());

			// Mutating sort:
			Collections.sort(this.blockOrder);

			// Removal loop:
			//		for(Map.Entry<Location,SerializedBlock> entry : this.blockMap.entrySet())
			for(SerializedBlock sb : this.blockOrder)
			{
				final Block block = Util.getBlockAt(sb.getVector(),this.world);
				final BlockState state = block.getState();
				if(state instanceof Chest){
					((Chest)state).getBlockInventory().clear();
				} else if(state instanceof InventoryHolder){
					((InventoryHolder)state).getInventory().clear();
				}

				// We only back up, we don't remove, blocks marked 'Action.RESTORE' or the like.
				if(this.blocks.get(sb.getVector()).action() == Action.DESTROY)
				{	block.setType(newMaterial);	}// if
			}// for

		}// if

		status = Status.ALTERED;
		return this;
	}// batchAlter()

	public static void removeFromActiveSet(final RegenBatch batch)
	{
		assert(activeBatches.containsKey(batch.world));
		assert(activeBatches.get(batch.world).contains(batch));
		activeBatches.get(batch.world).remove(batch);
		assert(!activeBatches.get(batch.world).contains(batch));
	}// removeFromActiveSet()

	protected void disablePhysics()
	{	RegEnginePlugin.getInstance().disablePhysics(); }

	protected void enablePhysics()
	{	RegEnginePlugin.getInstance().enablePhysics(); }

	protected RegenBatch restore()
	{
		if(status != Status.PENDING_RESTORATION) throw new Error("Can only call RegenBatch.restore() on a batch 'PENDING_RESTORATION'.");

		try
		{
			disablePhysics();
			// TODO: Multiple loops, if necessary, to ensure proper replacement.
			// TODO: Pop-to-item the blocks being replaced.
			for(Iterator<SerializedBlock> i = this.blockOrder.descendingIterator(); i.hasNext(); )
				//		for(Map.Entry<Location,SerializedBlock> entry : blockMap.entrySet())
			{
				final SerializedBlock block = i.next();
				block.place();
			}// for
		}// try
		finally
		{
			enablePhysics();
		}// finally

		// Mark the batch as 'done', including removing it from the global map:
		this.status = Status.DONE;
		RegenBatch.removeFromActiveSet(this);

		return this;
	}// restore()

	public RegenBatch queueBatchRestoration()
	{
		if(status != Status.ALTERED)
		{	throw new Error("Can only call RegenBatch.queueBatchRestoration() with an 'ALTERED' status.  Did you run .batchAlter() first?"); }// if

		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() 
			{
				restore();
			}// run()
		}, this.delay);

		// TODO: Load warner
		//new RestorationWarnings

		this.status = Status.PENDING_RESTORATION;
		assert(this.isRunning());

		return this;
	}// queueBatchRestoration()		

}// RegenBatch
