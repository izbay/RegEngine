package com.github.izbay.regengine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;

public class SerializedItem {
	
	/**
	 * @param args
	 */
	 //Standard Info 
	 private Material type;
	 private int amount;
	 private short durability;
	 private String[] lore;
	 
	 //Book Info
	 private boolean isBook = false;
	 private String name, author, title;
	 private String[] pages; 
	 
	 //Enchantment Info
	 private String[] enchant; //so tempted to name this array "hoes"
	 private Integer[] enchantLevels;

	public SerializedItem(ItemStack I){
	    
	    //Standard Info
		this.type = I.getType();
		this.amount = I.getAmount();		
		this.durability = I.getDurability();
		
		if(I.hasItemMeta()){
		    ItemMeta itemmeta = I.getItemMeta(); // Cache that shizz. Don't keep methodcalling! :D //< lol 3
		    this.name = (itemmeta.hasDisplayName())?itemmeta.getDisplayName():null;
		    this.lore = (String[]) ((itemmeta.hasLore())? itemmeta.getLore().toArray():null);
		    
		    //Book Info
		    if(type == Material.BOOK || type == Material.BOOK_AND_QUILL || type == Material.WRITTEN_BOOK){
		        // We can't be certain which of these fields would be missing while the item is still a book. Flag it.
		        isBook = true;
		        BookMeta bookmeta = (BookMeta)itemmeta;
		        this.author = (bookmeta.hasAuthor())?bookmeta.getAuthor():null;//Many ternaries. Such operators. Wow.
		        this.title = (bookmeta.hasTitle())?bookmeta.getTitle():null;
		        Object[] pageList = bookmeta.getPages().toArray();
		        this.pages = ((bookmeta.hasPages())?(Arrays.copyOf(pageList, pageList.length, String[].class)):null);
		    }//end IF book
		    
		    //Enchantment Info
            if(itemmeta.hasEnchants()){
                //I'm surprised that this doesn't need to be cast as an (Enchantment[])
                Enchantment[] enchantObjects = ((Enchantment[]) I.getItemMeta().getEnchants().keySet().toArray());

                this.enchant = new String[enchantObjects.length - 1];
                int i=0;
                for(Enchantment e: enchantObjects){
                    this.enchant[i++] = e.getName();
                }
                this.enchantLevels = ((Integer[])I.getItemMeta().getEnchants().values().toArray());
                
            }//end IF enchanted
		}//end IF hasMeta
        
	}//end SerializedItem CONSTRUCTOR
	
	//Alrighty. Time for de-objectifying.
	public ItemStack getItem(){
	    //Standard Info
	    ItemStack item = new ItemStack(type, amount, durability);
	    ItemMeta meta = item.getItemMeta();
	    
	    if(name != null)
	        meta.setDisplayName(name);
	    if (lore != null)
	        meta.setLore(Arrays.asList(lore));
	    if (isBook){
	        ((BookMeta)meta).setAuthor(author);
	        ((BookMeta)meta).setTitle(title);
	        ((BookMeta)meta).setPages(Arrays.asList(pages));
	    }
	    if(enchant != null){
	    	Map<Enchantment, Integer> map = new HashMap<Enchantment, Integer>();
	    
	    	for(int i=0; i<enchant.length; i++){
	    		map.put(Enchantment.getByName(enchant[i]), enchantLevels[i]);     
	    	}
	    	meta.getEnchants().putAll(map);
	    }
        item.setItemMeta(meta);
        return item;
    }
}//end SerializedItem CLASS