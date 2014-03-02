package com.github.izbay.regengine;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public class RegEnginePlugin extends JavaPlugin {
		
		private HashMap<Location, Material> blockMap = new HashMap<Location,Material>();
		private HashMap<Location, Byte> dataMap = new HashMap<Location,Byte>();
	
		@Override
		public void onDisable() {
		}

		@Override
		public void onEnable() {
		}
		
		
		
		public void alter(Location l){
			alter(l, Material.AIR);
		}
		
		@SuppressWarnings("deprecation")
		public void alter(Location l, Material m){
			Material backup = l.getBlock().getType();
			
			if(backup != m && !blockMap.containsKey(l)){
				blockMap.put(l, backup);
				dataMap.put(l, l.getBlock().getData());
				l.getBlock().setType(m);
				regen(l);
			}
		}
		
		
		
		private void regen(final Location l){
			this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				@SuppressWarnings("deprecation")
				public void run() {
				      l.getBlock().setType(blockMap.get(l));
				      l.getBlock().setData(dataMap.get(l));
				      blockMap.remove(l);
				      dataMap.remove(l);
				  }
				}, 120L);
		}
}
