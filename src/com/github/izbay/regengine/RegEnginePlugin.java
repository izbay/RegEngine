package com.github.izbay.regengine;

import java.util.HashMap;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.github.izbay.util.*;

public class RegEnginePlugin extends JavaPlugin {
		
		private HashMap<Location, SerializedBlock> blockMap = new HashMap<Location,SerializedBlock>();
		private FileConfiguration config;
		private Boolean doParticles;
		
		private static RegEnginePlugin instance;
		
		/**
		 * @return the RegEnginePlugin singleton
		 */
		public static RegEnginePlugin getInstance() {
			assert(instance != null);
			return instance; 
		}
		

		/**
		 * Default constructor; sets class singleton ref.
		 */
		public RegEnginePlugin()
		{
			super();
			instance = this;
		}// ctor
		
		@Override
		public void onDisable() {
			//TODO: Write out every map to file.
		}

		@Override
		public void onEnable() {
			// TODO: Read in maps from file to memory.
			
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
		
		
		
		public void alter(Plugin plugin, Location l){
			alter(plugin, l, Material.AIR);
		}
		
		public void alter(Plugin plugin, Location l, Material m){
			Location normal = Util.normalizeLocation(l);
			Material backup = normal.getBlock().getType();
			if(backup != m){
				if(!blockMap.containsKey(normal)){
					blockMap.put(normal, new SerializedBlock(normal.getBlock()));
					BlockState state = normal.getBlock().getState();
					if(state instanceof Chest){
						((Chest)state).getBlockInventory().clear();
					} else if(state instanceof InventoryHolder){
						((InventoryHolder)state).getInventory().clear();
					}
					normal.getBlock().setType(m);
					regen(normal);	
				}
			}
		}
		
		public void alter(Plugin plugin, final Vector v, final Material m)
		{	alter(plugin, Util.getLocation(v), m); }
		
		public void alter(Plugin plugin, final Block b, final Material m)
		{	alter(plugin, b.getLocation(), m); }
		
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
					SerializedBlock restore = blockMap.get(l);
					l.getBlock().setType(Material.getMaterial(restore.type));
				    l.getBlock().setData(restore.data);
				    if(restore.inventory.getInventory() != null){
				    	BlockState state = l.getBlock().getState();
				    	if(state instanceof Chest){
				    		((Chest) state).getBlockInventory().setContents(restore.inventory.getInventory());
				    	} else if(state instanceof InventoryHolder){
				    		((InventoryHolder) state).getInventory().setContents(restore.inventory.getInventory());
				    	}
				    }
				    blockMap.remove(l);
				}
			}, 200L);
		}
}