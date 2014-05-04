package com.github.izbay.regengine.serialize;

import java.util.Arrays;



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

	 /*
	  * Pre-conditions:
	  * -create new serialized item from stack
	  * Post-conditions:
	  * -new serialized item is created
	  * 
	  * args
	  * -ItemStack I is a stack of the items within the inventory
	  */
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

                Object[] enchantList = I.getItemMeta().getEnchants().keySet().toArray();

                this.enchant = new String[enchantList.length];
                int i=0;
                for(Object o: enchantList){

                    this.enchant[i++] = ((Enchantment)o).getName();
                }
                Object[] eLvlList = I.getItemMeta().getEnchants().values().toArray();
                this.enchantLevels = Arrays.copyOf(eLvlList, eLvlList.length, Integer[].class);
                
            }//end IF enchanted
		}//end IF hasMeta
        
	}//end SerializedItem CONSTRUCTOR
	
	//Alrighty. Time for de-objectifying.
	 /*
	  * Pre-conditions:
	  * grab the item from the stack
	  * 
	  * Post-conditions:
	  * return the item from the stacks
	  */
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
	    
	    item.setItemMeta(meta);
	    
	    if(enchant != null){


	    	for(int i=0; i<enchant.length; i++){
	    		item.addEnchantment(Enchantment.getByName(enchant[i]), enchantLevels[i]);     
	    	}

	    }
	    

        return item;
    }
	
	
	//GETTERS FOR OTHER FILE TO STORE DATA AS A STRING
	public String getType(){
		return type.name();
	}
	public String getAmount(){
		return Integer.toString(amount);
	}
	public String getDurability(){
		return Short.toString(durability);
	}
	public String[] getLore(){
		return lore;
	}
	public String isBook(){
		return ((Boolean)isBook).toString();
	}
	public String getName(){
		return name;
	}
	public String getAuthor(){
		return author;
	}
	public String getTitle(){
		return title;
	}
	public String[] getPages(){
		return pages;
	}
	public String[] getEnchants(){
		return enchant;
	}
	public String[] getEnchantLevels(){
		return (enchantLevels!=null)?Arrays.copyOf(enchantLevels, enchantLevels.length, String[].class):null;
	}
	
}//end SerializedItem CLASS