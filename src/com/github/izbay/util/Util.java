package com.github.izbay.util;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.Attachable;
import org.bukkit.material.MaterialData;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import com.github.izbay.regengine.*;
import com.github.izbay.regengine.block.Action;
import com.github.izbay.regengine.block.DependingBlock;
import com.github.izbay.regengine.serialize.*;

public abstract class Util 
{
	public static double toRadians(double yaw){
		return (270-yaw) * Math.PI / 180;
	}
		public static BlockVector getBlockVector(final Location l)		{ return new BlockVector((int) l.getX(), (int) l.getY(), (int) l.getZ()); }// getBlockVector() 
		public static BlockVector getBlockVector(final Block b)			{	return getBlockVector(b.getLocation()); }
		public static BlockVector getBlockVector(final BlockState b)	{	return getBlockVector(b.getLocation()); }
		// For equal standing:
		public static BlockVector getBlockVector(final BlockImage b)	{	return b.getBlockVector(); }

		/**
		 * @param n1
		 * @param n2
		 * @return  ∈ {-1,0,1}
		 */
		public static int compare(final double n1, final double n2) {
			if (n1 == n2)
				return 0;
			else if (n1 < n2)
				return -1;
			else
				return 1;
		}// compare()

		/**
		 * Imposes a monotonic order on Vectors, with the Y-axis taking highest priority, then the Z- (row-major).
		 * @param n1
		 * @param n2
		 * @return  ∈ {-1,0,1}
		 */
		public static int compare(final Vector v1, final Vector v2) {
			final int y = compare(v1.getY(), v2.getY());
			if (y == 0) {
                final int z = compare(v1.getZ(), v2.getZ());
                if (z == 0)
					return compare(v1.getX(), v2.getX());
				else
					return z;
			}// if
			else
				return y;
		}// compare()
		
		public static boolean equal(final Vector v1, final Vector v2)
		{	return compare(v1,v2) == 0; }// equal()
		
		public static boolean isSolid(final Material m) 	{	return m.isSolid(); }
		public static boolean isSolid(final Block b) 		{	return b.getType().isSolid(); }// isSolid()
		public static boolean isSolid(final BlockState b) 	{	return b.getType().isSolid(); }// isSolid()
		
		public static World getCurrentWorld()	{ return Bukkit.getServer().getWorlds().get(0); }
		
		public static Block getBlockAt(final Location l) 					{ return getCurrentWorld().getBlockAt(l); }
		public static Block getBlockAt(final Vector v, final World world)	{	return world.getBlockAt( v.getBlockX(), v.getBlockY(), v.getBlockZ()); }
		
		public static Location getLocation(final Vector v, final World w)	{ 	return v.toLocation(w); }
		public static Location getLocation(final Block b)					{ 	return b.getLocation(); }

		public static Location normalizeLocation(final Location l)
		{	return new Location(l.getWorld(), Math.floor(l.getX()), Math.floor(l.getY()), Math.floor(l.getZ()));}
		
		/**
		 * Non-mutating Vector addition.
		 * @return 
		 */
		public static Vector add(final Vector v, final double x, final double y, final double z)
		{	return new Vector(v.getX()+x, v.getY()+y, v.getZ()+z); }

		public static Location add(final Location l, final double x, final double y, final double z)
		{ return l.clone().add(x,y,z); }
		
		public static Location add(final Location l, final Vector v)
		{ return l.clone().add(v); }
		
		public static BlockVector add(final BlockVector v, final int x, final int y, final int z)
		{	return new BlockVector(v.getX()+x, v.getY()+y, v.getZ()+z); }
		
		public static BlockVector add(final BlockVector v, final BlockFace direction)
		{	return  Util.add(v, direction.getModX(), direction.getModY(), direction.getModZ()); }
		
		public static Location add(final Location l, final BlockFace direction)
		{	return  Util.add(l, direction.getModX(), direction.getModY(), direction.getModZ()); }

		public static Block add(final Block b, final int x, final int y, final int z)
		{	return getBlockAt(Util.add(b.getLocation(), x, y, z)); }

		public static Block add(final Block b, final BlockFace direction)
		{	return add(b, direction.getModX(), direction.getModY(), direction.getModZ()); }
		
		public static Block getBlockAbove(final Block b)
		{	return add(b, 0, 1, 0); }
		
		public static Block getBlockAbove(final BlockImage i)
		{	return getBlockAt(add(i.getLocation(), 0, 1, 0)); }
		
		public static Block getBlockBelow(final Vector v, final World world)
		{ return getBlockAt(Util.add(v, 0, -1, 0), world); }
		
		public static Block getBlockBelow(final BlockImage i)
		{	return getBlockAt(add(i.getLocation(), 0, -1, 0)); }
		
		public static Block getBlockBelow(final Location l)
		{	return getBlockAt(l.clone().add(0, -1, 0)); }
		
		public static Block getBlockBelow(final Block b)
		{	return add(b.getLocation(), BlockFace.DOWN).getBlock(); }
		
		public static BlockFace[] adjacentDirections()
		{ return new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST }; }
		
		public static Set<Block> getAdjacentBlocks(final Block b)
		{
			HashSet<Block> s = new HashSet<Block>();
			for(BlockFace dir : adjacentDirections())
			{ s.add(Util.add(b,dir)); }
			assert(s.size() == 4);
			return s;
		}// getAdjacentBlocks()
		
		public static Set<Block> getEnclosingBlocks(final Block b)
		{
			HashSet<Block> s = new HashSet<Block>();
			for(BlockFace dir : adjacentDirections())
			{ s.add(add(b,dir)); }
			s.add(add(b, BlockFace.UP));
			s.add(add(b, BlockFace.DOWN));
			assert(s.size() == 6);
			return s;
		}// getEnclosingBlocks()
		
		public static boolean isAttachable(final BlockState b)		{ return (b.getData() instanceof Attachable); }
		public static boolean isAttachable(final Block b)			{ return (b.getState().getData() instanceof Attachable); }
		public static boolean isAttachable(final MaterialData dat)	{ return (dat instanceof Attachable); }
		
		public static Block getAttachedBlock(final Block b)
		{	return getAttachedBlock(b.getState()); }// getAttachedFace
	
		public static Block getAttachedBlock(final BlockState b)
		{
			if(isAttachable(b))
			{
				return add(getBlockAt(b.getLocation()),((Attachable) b.getData()).getAttachedFace());
			}
			else
			{	return null; }
		}// getAttachedBlock()
		
	public static String[] getPossibleSignText(final BlockState bs)
	{ return (bs.getType() == Material.SIGN) ? ((Sign)bs).getLines() : null; }

	public static Inventory getPossibleBlockInventory(final BlockState bs) {
		if(bs instanceof Chest) { 
			return ((Chest)bs).getBlockInventory();
		} else if(bs instanceof InventoryHolder) {
			return ((InventoryHolder)bs).getInventory();
		} else return null;
	}// getPossibleBlockInventory()

	/**
	 * A notion of observable equivalence.
	 * @param bs
	 * @return
	 * The retval is a conjunction of various calls to equals() overloaded for preexisting types; we assume these qualify as equivalence relations.
	 * Because boolean conjunction is associative & commutative, it should be possible to show that permutation makes for equivalence.
	 */
	public static boolean areBlocksEquivalent(final SerializedBlock sb, final BlockState bs)
	{
		// TODO: .date field?
		return sb.getBlockWorld().equals(bs.getWorld().getName())
			&& sb.getBlockType().equals(bs.getType())
			&& sb.getVector().equals(Util.getBlockVector(bs))
			//&& this.getX() == bs.getX() && this.getY() == bs.getY() && this.getZ() == bs.getZ()
			&& sb.getData().equals(bs.getData())
			//&& this.data == bs.getData().getData()
			&& sb.getSignText().equals(getPossibleSignText(bs))
			&& sb.getInventory().equals(getPossibleBlockInventory(bs)) ;
	}// isBlockEquivalent()
	public static boolean areBlocksEquivalent(final BlockState bs, final SerializedBlock sb)		{	return areBlocksEquivalent(sb, bs); }
	public static boolean areBlocksEquivalent(final BlockState b1, final BlockState b2)				{	return b1.equals(b2); }
	public static boolean areBlocksEquivalent(final BlockImage b1, final BlockImage b2)				{	return b1.equals(b2); }
	public static boolean areBlocksEquivalent(final BlockImage b1, final BlockState bs)				{	return b1.equals(bs); }
	public static boolean areBlocksEquivalent(final BlockState bs, final BlockImage b1)				{	return b1.equals(bs); }
	/**
	 * @param b1
	 * @param b2
	 * @return
	 * Claim: Equivalence relation.
	 * Proof: The predefined equals() method is an equiv. rel.
	 */
	public static boolean areBlocksEquivalent(final SerializedBlock b1, final SerializedBlock b2)	{	return b1.equals(b2); }
	/**
	 * @param b1
	 * @param b2
	 * @return
	 * Note that block equivalence for DependingBlocks is *not* the same as an equals() test!  Comparison of the action() field is deliberately omitted.
	 * Claim: Equivalence relation.
	 * Proof: BlockImage.equals(BlockImage) is an equivalence relation; see BlockImage.java.
	 */
	public static boolean areBlocksEquivalent(final DependingBlock b1, final DependingBlock b2)	
	{	return /*b1.action() == b2.action() &&*/ b1.block().equals(b2.block()); }
	
	//public boolean areBlocksEquivalent(final )
		
	private Util() {}
}// Util
