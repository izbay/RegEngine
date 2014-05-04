package com.github.izbay.regengine.serialize;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.MaterialData;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import com.github.izbay.util.Util;

//class for to make the block go from a block to serialization back to block
public class SerializedBlock implements Comparable<SerializedBlock> {
		
	private final int x, y, z;
	private final long date;
	private final String type, world;
	public final Material dType;
	public final World dWorld;
	private final byte data;
	private final String[] signtext;
	private final SerializedInventory inventory;
	
	// Assistance functions for making constructor chaining more readable!
	protected static String[] possibleSignText(final BlockState bs)
	{ return (bs.getType() == Material.SIGN) ? ((Sign)bs).getLines() : null; }
	protected static Inventory possibleBlockInventory(final BlockState bs) {
		if(bs instanceof Chest) { 
			return ((Chest)bs).getBlockInventory();
		} else if(bs instanceof InventoryHolder) {
			return ((InventoryHolder)bs).getInventory();
		} else return null;
	}// possibleBlockInventory()
	
	/**
	 * A notion of observable equivalence.
	 * @param bs
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public boolean isBlockEquivalent(final BlockState bs)
	{
		// TODO: .date field?
		return getBlockWorld().equals(bs.getWorld().getName())
			&& getBlockType().equals(bs.getType())
			&& this.getVector().equals(Util.getBlockVector(bs))
			//&& this.getX() == bs.getX() && this.getY() == bs.getY() && this.getZ() == bs.getZ()
			&& this.data == bs.getData().getData()
			&& signtext.equals(possibleSignText(bs))
			&& inventory.equals(possibleBlockInventory(bs)) ;
	}// isBlockEquivalent()
	
	public boolean isBlockEquivalent(final SerializedBlock sb)
	{	return this.equals(sb); }
	
	public Material getType() { return dType; }
	public World getWorld() { return dWorld; }
	@Deprecated
	public byte getRawData() { return data; }

	@SuppressWarnings("deprecation")
	public MaterialData getData()
	{	
		//return new MaterialData(getType(), getRawData()); 
		return getType().getNewData(getRawData());
	}// getData()

	public Chunk getChunk() { return getWorld().getChunkAt(getX(), getZ()); }

	public SerializedBlock(final Block block){
		this(block.getState());
	}// ctor
	
	//constructor for the block in order to go back to block from serialization
	@SuppressWarnings("deprecation")
	public SerializedBlock(final BlockState bs) {
		// Unfortunately the nested ternary is required to make the constructor the first instruction of the overloaded constructor.
		// 	- Not anymore! --JJ-S
		this(bs.getWorld().getTime(), bs.getWorld().getName(), bs.getType().name(), bs.getData().getData(), possibleSignText(bs), possibleBlockInventory(bs), bs.getX(), bs.getY(), bs.getZ());
	}// ctor
	
	/**
	 * Shallow copy constructor.
	 * @param rhs
	 */
	public SerializedBlock(final SerializedBlock rhs) {
//		this(rhs.date, rhs.world, rhs.type, rhs.data, rhs.signtext, rhs.inventory, rhs.x, rhs.y, rhs.z);
		this.date = rhs.date;
		this.world = rhs.world;
		this.type = rhs.type;
		this.dWorld = rhs.dWorld;
		this.dType = rhs.dType;
		this.data = rhs.data;
		this.signtext = rhs.signtext;
		this.inventory = rhs.inventory;
		this.x = rhs.x;
		this.y = rhs.y;
		this.z = rhs.z;
	}// copy c.
	
	public SerializedBlock(long date, final World world, final Material type, byte data, String[] signtext, Inventory inventory, int x, int y, int z) {
		this.date = date;
		this.dWorld = world;
		this.world = world.getName();
		this.dType = type;
		this.type = type.name();
		this.data = data;
		this.signtext = signtext;
		this.inventory = (inventory!=null)?new SerializedInventory(inventory):null;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	/*
	 * Pre Conditions
	 * Construct a serialized block element from the given data
	 * 
	 * Post Conditions
	 * serialized block has been created
	 * 
	 * Args
	 * long date-data for the world
	 * string world-world name
	 * string type -type of block
	 * byte data- metadata of the block
	 * string signtext- text if the block is a sign
	 * Invenetory - inventory if the block is an inventory
	 * int x - x coord
	 * int y - y coord
	 * int z - z coord
	 * 
	 */
	public SerializedBlock(long date, String world, String type, byte data, String[] signtext, Inventory inventory, int x, int y, int z) {
		this.date = date;
		this.world = world;
		this.dWorld = Bukkit.getWorld(this.world);
		this.type = type;
		this.dType = Material.getMaterial(type);
		this.data = data;
		this.signtext = signtext;
		this.inventory = (inventory!=null)?new SerializedInventory(inventory):null;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@SuppressWarnings("deprecation")
	/*
	 * Precondtions
	 * place the location from the srialized block back in to the world based off the location the same way does it
	 * 
	 * Post Conditions
	 *item is placed within the world
	 *
	 * 
	 * Args
	 * location is the X,Y,Z coordinates needed
	 * 
	 */
	public boolean place(Location l) {
		//get the type
		l.getBlock().setType(Material.getMaterial(type));
		//get the data
	    l.getBlock().setData(data);
	    //check for inventory
	    if(inventory != null){
	    	BlockState state = l.getBlock().getState();
	    	if(state instanceof Chest){
	    		((Chest) state).getBlockInventory().setContents(inventory.getInventory());
	    	} else if(state instanceof InventoryHolder){
	    		((InventoryHolder) state).getInventory().setContents(inventory.getInventory());
	    	} else if(state instanceof Sign){
	    		int i = 0;
	    		for (String str: signtext){
	    			((Sign) state).setLine(i++, str);
	    		}
	    	}
	    }
		
	    return isPlaced();
	}// place()
	
	public boolean isPlaced()
	{	return this.isBlockEquivalent(getWorld().getBlockAt(getX(), getY(), getZ()).getState()); }
	
	//place the block in the world based off of the x,y,z coordinates of the world
	public boolean place()
	{	return place(new Location(Bukkit.getWorld(this.world),x,y,z)); }// place()

	/*
	 * Imposes a total order on blocks' vector coordinates for minimizing dependency conflicts during REGENgine restoration.  Mostly this means moving in the positive Y direction.
	 *  (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(SerializedBlock other) {
		return Util.compare(new Vector(x, y, z), new Vector(other.getX(), other.getY(), other.getZ()));
	}

	
	//GETTERS FOR THE OTHER CLASSES
	private int getX() {
		return x;
	}
	
	private int getY() {
		return y;
	}
	
	private int getZ() {
		return z;
	}
	
	public BlockVector getVector()
	{	return new BlockVector(x,y,z); }

	public String getBlockDate()
	{
	   String s = Long.toHexString(date);
	   return s;
	}
	
	public String getBlockType()
	{
	   return type;
	}
	
	public String getBlockWorld()
	{
	   return world;
	}
	
	public String getBlockData()
	{
        return Byte.toString(data);
	}
	
	public String[] getBlockSignText()
	{
	   return signtext;
	}
	
	public SerializedInventory getBlockChestInventory()
	{
	   return inventory;
	}
	public String getBlockXLoc()
	{
	    return Integer.toString(x);
	}
	
	public String getBlockYLoc()
	{
	    return Integer.toString(y);
	}
	
	public String getBlockZLoc()
	{
	    return Integer.toString(z);
	}
}// SerializedBlock

