
package com.github.izbay.regengine.block;

import java.util.Arrays;
import java.util.EnumSet;
//import java.util.HashSet;
//import java.util.Set;

import org.bukkit.Material;
//import org.bukkit.block.Block;
//import org.bukkit.block.BlockState;
//import org.bukkit.material.Bed;

//import com.github.izbay.util.Util;

public class BlockTypes
{
	public static final EnumSet<Material> needingSupport = EnumSet.copyOf(
Arrays.asList(new Material[]
		{
		Material.ACTIVATOR_RAIL,
		Material.STONE_PLATE,
		Material.SAPLING,
		Material.WOODEN_DOOR,
		Material.BED_BLOCK,
		Material.POWERED_RAIL,
		Material.CACTUS,
		Material.WATER_LILY,
		Material.RAILS,
		Material.WOOD_PLATE,
		Material.TRIPWIRE,
		Material.DETECTOR_RAIL,
		Material.FLOWER_POT,
		Material.PUMPKIN_STEM,
		Material.BROWN_MUSHROOM,
		Material.CAKE_BLOCK,
		Material.NETHER_WARTS,
		Material.CROPS,
		Material.REDSTONE_ORE,
		Material.RED_MUSHROOM,
		Material.REDSTONE_WIRE,
		Material.CARROT,
		Material.RED_ROSE,
		//nil,
		Material.DIODE_BLOCK_OFF,
		Material.GOLD_PLATE,
		Material.IRON_DOOR_BLOCK,
		Material.IRON_PLATE,
		Material.FIRE,
		Material.REDSTONE_COMPARATOR_ON,
		Material.MELON_STEM,
		Material.LONG_GRASS,
		Material.GLOWING_REDSTONE_ORE,
		Material.DIODE_BLOCK_ON,
		Material.SUGAR_CANE_BLOCK,
		Material.POTATO,
		Material.CARPET,
		Material.REDSTONE_COMPARATOR_OFF }));
	
	public static final EnumSet<Material> gravityBound = EnumSet.of(Material.SAND, Material.GRAVEL, Material.ANVIL);
		
	public static final EnumSet<Material> duple = EnumSet.of(Material.DOUBLE_PLANT, Material.BED_BLOCK, Material.WOODEN_DOOR, Material.IRON_DOOR_BLOCK, Material.PISTON_EXTENSION, Material.PISTON_STICKY_BASE, Material.PISTON_BASE);
	
	public static final EnumSet<Material> liquid = EnumSet.of(Material.WATER, Material.LAVA, Material.STATIONARY_WATER, Material.STATIONARY_LAVA);
	
	public static boolean needsWarnPlayerOnRegeneration(final Material m)
	{	return m.isSolid() || liquid.contains(m); }
	
	/*  Additions to CompoundDependingBlock.java may have made these unnecessary:
	public static BlockState[] getRestOfBed(final BlockState bs)
	{
		if(bs.getType() != Material.BED_BLOCK) throw new IllegalArgumentException();
		final Bed bedData = (Bed)(bs.getData());
		//final Block bOther = 
				
		if(bedData.isHeadOfBed()) 
		{	return new BlockState[] {bs, Util.add(bs.getLocation(),bedData.getFacing().getOppositeFace()).getBlock().getState() }; }// if
		else
		{	return new BlockState[] {Util.add(bs.getLocation(), bedData.getFacing()).getBlock().getState(), bs};	}// else
		//assert(bOther.getType() == Material.BED_BLOCK);
	}// getRestOfBed()
	
	public static BlockState[] getRestOfDoublePlant(final BlockState bs)
	{ }// getRestOfDoublePlant()
	*/
}// BlockTypes