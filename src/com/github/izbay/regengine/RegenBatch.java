package com.github.izbay.regengine;

//import java.util.LinkedHashSet;
//import java.util.Set;

//import java.util.ArrayDeque;
import java.util.Collections;
//import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
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
	protected final World world;
	protected final long delay;
	public final DependingBlockSet blocks;
	public final Material newMaterial;
	public final Plugin plugin;
	//public Map<Location, SerializedBlock> blockMap;
	public final LinkedList<SerializedBlock> blockOrder = new LinkedList<SerializedBlock>();

	public static Set<RegenBatch> activeBatches = new HashSet<RegenBatch>();

	//	private BlockImage[] blocks; // Comment this out until I'm ready to use it

	/* (non-Javadoc)
	 * @see com.github.izbay.regengine.RegenBatchIface#getWorld()
	 */
	@Override
	public World world() { return world; }
	
	@Override
	public long delay() { return delay; }

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

	/**
	 * @param world
	 * @param targetTime
	 */
	public RegenBatch(final Plugin plugin, final Iterable<Vector> blockVectors, final World world, final long delay) {
		super();

		RegenBatch.activeBatches.add(this);

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
	}// ctor



	/* (non-Javadoc)
	 * @see com.github.izbay.regengine.RegenBatchIface#cancel()
	 */
	@Override
	public void cancel()
	{
		// TODO
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


	public void alterAndRestore()
	{
		batchAlter();
		queueBatchRestoration();
	}// destroyAndRestore()

	public void batchAlter()
	{
		if(!blocks.isEmpty())
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
	}// batchAlter()


	private void restore()
	{
		// TODO: Multiple loops, if necessary, to ensure proper replacement.
		// TODO: Pop-to-item the blocks being replaced.
		for(Iterator<SerializedBlock> i = this.blockOrder.descendingIterator(); i.hasNext(); )
			//		for(Map.Entry<Location,SerializedBlock> entry : blockMap.entrySet())
		{
			final SerializedBlock block = i.next();
			block.place();
			//entry.getValue().place(entry.getKey());
			//blockMap.remove(l);
			//		    dataMap.remove(l);
		}// for

		assert(RegenBatch.activeBatches.contains(this));
		RegenBatch.activeBatches.remove(this);
	}// restore()

	public void queueBatchRestoration()
	{
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() 
			{
				restore();
			}// run()
		}, this.delay);
	}// queueBatchRestoration()		
}// RegenBatch
