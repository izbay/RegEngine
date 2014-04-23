package com.github.izbay.regengine;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import com.github.izbay.util.Util;

//class for to make the block go from a block to serialization back to block
public class SerializedBlock implements Comparable<SerializedBlock> {
		
	private final int x, y, z;
	private final long date;
	private final String type, world;
	private final byte data;
	private final String[] signtext;
	private final SerializedInventory inventory;

	@SuppressWarnings("deprecation")
	//constructor for the block in order to go back to block from serialization
	public SerializedBlock(Block block){
		// Unfortunately the nested ternary is required to make the constructor the first instruction of the overloaded constructor.
		this(block.getWorld().getTime(), block.getWorld().getName(), block.getType().name(), block.getData(), (block.getType() == Material.SIGN)? ((Sign)block).getLines():null, (block.getState() instanceof Chest)?((Chest)block.getState()).getBlockInventory():(block.getState() instanceof InventoryHolder)?((InventoryHolder)block.getState()).getInventory():null, block.getX(), block.getY(), block.getZ());
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
		this.type = type;
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
	public void place(Location l) {
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
		
	}// place()
	
	//place the block in the world based off of the x,y,z coordinates of the world
	public void place()
	{	place(new Location(Bukkit.getWorld(this.world),x,y,z)); }// place()

	@Override
	//compare to the block
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

