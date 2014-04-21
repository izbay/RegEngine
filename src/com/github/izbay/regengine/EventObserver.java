package com.github.izbay.regengine;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;

public class EventObserver implements Listener{

	private boolean physicsBool = false;
	
	 public EventObserver(RegEnginePlugin plugin) {
	        plugin.getServer().getPluginManager().registerEvents(this, plugin);
	    }
	 
	 @EventHandler
	 public void onPhysics(BlockPhysicsEvent event) {
		 event.setCancelled(physicsBool);
	 	}
	 
	 public void setPhysics(boolean phybool)
	 {
		 if(phybool == true)
			 this.physicsBool = false;
		 else
			 this.physicsBool = true;
	 }
	 
}
