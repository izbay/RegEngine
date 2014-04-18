package com.github.izbay.regengine;

import java.io.Serializable;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class SerializedBlock implements Serializable {
	
	private static final long serialVersionUID = 305489277751666964L;
	
	public final long date;
	public final String type, world;
	public final byte data;
	public final String[] signtext;
	public final SerializedInventory inventory;

	@SuppressWarnings("deprecation")
	public SerializedBlock(Block block){
		// Unfortunately the nested ternary is required to make the constructor the first instruction of the overloaded constructor.
		this(block.getWorld().getTime(), block.getWorld().getName(), block.getType().name(), block.getData(), (block.getType() == Material.SIGN)? ((Sign)block).getLines():null, (block.getState() instanceof Chest)?((Chest)block.getState()).getBlockInventory():(block.getState() instanceof InventoryHolder)?((InventoryHolder)block.getState()).getInventory():null);
	}
	
	public SerializedBlock(long date, String world, String type, byte data, String[] signtext, Inventory inventory) {
		this.date = date;
		this.world = world;
		this.type = type;
		this.data = data;
		this.signtext = signtext;
		this.inventory = new SerializedInventory(inventory);
	}

	public SerializedBlock(FileConfiguration rs) {
		date = rs.getLong("date");
		world = rs.getString("world");
		type = rs.getString("type");
		data = rs.getByteList("data").get(0);
		signtext = (String[]) rs.getStringList("signtext").toArray();
		inventory = new SerializedInventory((String[]) rs.getStringList("inventory").toArray());
	}

	@Override
	public String toString() {
		/**final StringBuilder msg = new StringBuilder();
		if (date > 0)
			msg.append(Config.formatter.format(date)).append(" ");
		if (playerName != null)
			msg.append(playerName).append(" ");
		if (signtext != null) {
			final String action = type == 0 ? "destroyed " : "created ";
			if (!signtext.contains("\0"))
				msg.append(action).append(signtext);
			else
				msg.append(action).append(materialName(type != 0 ? type : replaced)).append(" [").append(signtext.replace("\0", "] [")).append("]");
		} else if (type == replaced) {
			if (type == 0)
				msg.append("did an unspecified action");
			else if (ca != null) {
				if (ca.itemType == 0 || ca.itemAmount == 0)
					msg.append("looked inside ").append(materialName(type));
				else if (ca.itemAmount < 0)
					msg.append("took ").append(-ca.itemAmount).append("x ").append(materialName(ca.itemType, ca.itemData)).append(" from ").append(materialName(type));
				else
					msg.append("put ").append(ca.itemAmount).append("x ").append(materialName(ca.itemType, ca.itemData)).append(" into ").append(materialName(type));
			} else if (BukkitUtils.getContainerBlocks().contains(Material.getMaterial(type)))
				msg.append("opened ").append(materialName(type));
			else if (type == 64 || type == 71)
				// This is a problem that will have to be addressed in LB 2,
				// there is no way to tell from the top half of the block if
				// the door is opened or closed.
				msg.append("moved ").append(materialName(type));
			// Trapdoor
			else if (type == 96)
				msg.append((data < 8 || data > 11) ? "opened" : "closed").append(" ").append(materialName(type));
			// Fence gate
			else if (type == 107)
				msg.append(data > 3 ? "opened" : "closed").append(" ").append(materialName(type));
			else if (type == 69)
				msg.append("switched ").append(materialName(type));
			else if (type == 77 || type == 143)
				msg.append("pressed ").append(materialName(type));
			else if (type == 92)
				msg.append("ate a piece of ").append(materialName(type));
			else if (type == 25 || type == 93 || type == 94 || type == 149 || type == 150)
				msg.append("changed ").append(materialName(type));
			else if (type == 70 || type == 72 || type == 147 || type == 148)
				msg.append("stepped on ").append(materialName(type));
			else if (type == 132)
				msg.append("ran into ").append(materialName(type));
		} else if (type == 0)
			msg.append("destroyed ").append(materialName(replaced, data));
		else if (replaced == 0)
			msg.append("created ").append(materialName(type, data));
		else
			msg.append("replaced ").append(materialName(replaced, (byte)0)).append(" with ").append(materialName(type, data));
		if (loc != null)
			msg.append(" at ").append(loc.getBlockX()).append(":").append(loc.getBlockY()).append(":").append(loc.getBlockZ());
		*/
		return "lolbeans";
	}
}