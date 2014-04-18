package com.github.izbay.regengine;

//import java.util.LinkedHashSet;
//import java.util.Set;

import org.bukkit.World;
//import org.bukkit.Material;
//import org.bukkit.block.Block;
//import org.bukkit.block.BlockFace;
//import org.bukkit.block.BlockFace;
//import java.util.*;
//import org.bukkit.util.BlockVector;

//import com.github.izbay.regengine.block.Action;
//import com.github.izbay.regengine.block.DependingBlock;
//import com.github.izbay.regengine.block.DependingBlockSet;
//import com.github.izbay.util.Util;

//import com.github.izbay.util.Util;

public class RegenBatch implements RegenBatchIface 
{
	private final World world;
	private final int restorationTime;
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
	public int getRestorationTime() { return restorationTime; }

	/**
	 * @param world
	 * @param targetTime
	 */
	public RegenBatch(final BlockImage[] blocks, final World world, final int targetTime) {
		super();
//		this.blocks = blocks;
		this.world = world;
		this.restorationTime = targetTime;
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
	
}// RegenBatch
