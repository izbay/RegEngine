package com.github.izbay.regengine;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class SerializedItem {
	
	private ItemStack item;
	
	public SerializedItem(ItemStack I){
		item = I;
	}
	
	public SerializedItem(String S){
		new ItemStack(Material.valueOf(S));
	}
	
	public ItemStack getItem(){
		return item;
	}

}
