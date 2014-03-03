package com.github.izbay.regengine;

import java.util.HashMap;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class RegEnginePlugin extends JavaPlugin {
		
		private HashMap<Location, Material> blockMap = new HashMap<Location,Material>();
		private HashMap<Location, Byte> dataMap = new HashMap<Location,Byte>();
		private FileConfiguration config;
		private Boolean doParticles;
		
		@Override
		public void onDisable() {
		}

		@Override
		public void onEnable() {
			// Load config
			this.saveDefaultConfig();
			config = this.getConfig();
			
			// If configured to do so, check the latest version on BukkitDEV and
			// alert if user is out of date.
			if (this.config.getBoolean("check-update")) {
			      //new CheckUpdate(this, <INSERT ID HERE>);
			}
			doParticles = this.config.getBoolean("do-particles");
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
			if(doParticles){
				for(int i=0; i<60; i+=10){
					this.getServer().getScheduler().scheduleSyncDelayedTask(this, new BukkitRunnable() {
						public void run() {
							l.getWorld().playEffect(l, Effect.MOBSPAWNER_FLAMES, 2004);
						}
					}, 200L-i);
				}
			}
			
			this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				@SuppressWarnings("deprecation")
				public void run() {
					l.getBlock().setType(blockMap.get(l));
				    l.getBlock().setData(dataMap.get(l));
				    blockMap.remove(l);
				    dataMap.remove(l);
				}
			}, 200L);
		}
}


