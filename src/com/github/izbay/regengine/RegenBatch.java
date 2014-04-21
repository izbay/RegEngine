package com.github.izbay.regengine;

//import java.util.LinkedHashSet;
//import java.util.Set;

import java.util.LinkedHashMap;
import java.util.Map;

//import net.minecraft.server.v1_7_R3.Block;
//import net.minecraft.server.v1_7_R3.Material;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;


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
	private final World world;
	private final long delay;
	public final DependingBlockSet blocks;
	public final Material newMaterial;
	public final Plugin plugin;
	public Map<Location, SerializedBlock> blockMap;

	//	private BlockImage[] blocks; // Comment this out until I'm ready to use it

	/* (non-Javadoc)
	 * @see com.github.izbay.regengine.RegenBatchIface#getWorld()
	 */
	@Override
	public World getWorld() { return world; }

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
	public static RegenBatch altering(final Plugin plugin, final Vector[] blockVectors, final World world, final long delay)
	{	return new RegenBatch(plugin, blockVectors, world, delay); }

	/**
	 * @param world
	 * @param targetTime
	 */
	public RegenBatch(final Plugin plugin, final Vector[] blockVectors, final World world, final long delay) {
		super();
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


	public void destroyAndRestore()
	{
		batchAlter();
		queueBatchRestoration();
	}// destroyAndRestore()

	public void batchAlter()
	{
		if(!blocks.isEmpty())
		{
			this.blockMap = new LinkedHashMap<Location,SerializedBlock>();
			for(DependingBlock d : this.blocks)
			{
				assert(d.getType() != this.newMaterial);
				final Location normal = Util.normalizeLocation(d.getLocation());
				final Block b = Util.getBlockAt(normal); 
				this.blockMap.put(normal, new SerializedBlock(b));
			}// for
			assert(this.blockMap.size() == this.blocks.size());
		}// if

		// TODO: Sort here.

		// Removal loop:
		for(Map.Entry<Location,SerializedBlock> entry : this.blockMap.entrySet())
		{
			final Block block = Util.getBlockAt(entry.getKey());
			final BlockState state = block.getState();
			if(state instanceof Chest){
				((Chest)state).getBlockInventory().clear();
			} else if(state instanceof InventoryHolder){
				((InventoryHolder)state).getInventory().clear();
			}
			block.setType(newMaterial);
		}// for
	}// batchAlter()
	
	
//		private void regen(final Location l){
		
	void queueBatchRestoration()
	{
		// TODO: Sort map.
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				public void run() 
				{
					for(Map.Entry<Location,SerializedBlock> entry : blockMap.entrySet())
					{
						entry.getValue().place(entry.getKey());
						//blockMap.remove(l);
		//		    dataMap.remove(l);
					}// for
				}// run()
			}, this.delay);
	}// queueBatchRestoration()		
}// RegenBatch
