package com.github.izbay.regengine;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SerializedInventory {
    
    public SerializedItem[] inventory;
    
    public SerializedInventory(Inventory inventory){
        if(inventory != null){
			this.inventory = new SerializedItem[inventory.getSize()];
			int i = 0;
			for(ItemStack I : inventory){
				this.inventory[i++] = (I != null)?new SerializedItem(I):null;
			}
		} else {
			this.inventory = null;
		}
    }
    
    public ItemStack[] getInventory(){
        if(this.inventory != null){
            ItemStack[] inventory = new ItemStack[this.inventory.length];
			int i = 0;
			for(SerializedItem I: this.inventory){
			    inventory[i++] = (I!=null)?I.getItem():null;
			}
			return inventory;
        } else {
            return new ItemStack[0];
        }
    }
    
    public SerializedItem[] getSerializedInventory(){
    	return inventory;
    }
}//end SerializedInventory CLASS