package com.github.izbay.regengine;

import java.io.IOException;
import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import clojure.lang.RT;
//import clojure.lang.Compiler;


import com.github.izbay.util.*;

public class RegEnginePlugin extends JavaPlugin 
{
//		private HashMap<Location, Material> blockMap = new HashMap<Location,Material>();
		private HashMap<Location, SerializedBlock> blockMap = new HashMap<Location,SerializedBlock>();
//		private HashMap<Location, Byte> dataMap = new HashMap<Location,Byte>();
		private FileConfiguration config;
//		@SuppressWarnings("unused")
		private EventObserver eventobs = null;
		private static boolean active = true;
		/**
		 * Config-file keystrings, because I amuse myself idly by turning things like these into constants.
		 * Might be better as an enum.  This kind of design pattern gets so wearisome.
		 * @author jdjs
		 *
		 */
		public static abstract class Config 
		{
			public static final String CHECK_UPDATE = "check-update";
			public static final String DO_PARTICLES = "do-particles";
			public static final String USE_CLOJURE_REGEN = "use-clojure-regen";
		}// Config
		
		// TODO: Later we can go back to private if we want.  Having them public makes them more Clojure-accessible:
		public /*private*/ Boolean doParticles; 
		public Boolean clojureRegen;
		
		private static RegEnginePlugin instance; // Singleton-design pattern-related.
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
			active = false;
			/*LinkedList<SerializedBlock> writeOut = new LinkedList<SerializedBlock>();
			for(LinkedHashSet<RegenBatch> batchList: RegenBatch.activeBatches().values()){
				for(RegenBatch batch: batchList){
					writeOut.addAll(batch.blockOrder);
				}
			}
			XMLFromFile.BlocksToFile(writeOut);*/
		}
		
		public void loadClojure()
		{
			/*this.getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() 
			{
				public void run()
				{
				*/
					try {
						RT.loadResourceScript("cljengine/regen.clj");
						RT.var("cljengine.mc", "*debug-print*", true);// Set to false to make (debug-print) can it
						//RT.var("cljengine.regen", "regen-total-delay", 200); // 10s default wait
						//RT.var("cljengine.regen", "regen-warning-period", 20); // Shorter default period
					} catch (IOException e1) {
						// This is an auto-generated catch block.
						e1.printStackTrace();
					}// catch
					/*
				}// run()
			}, 60L);
			*/
		}// loadClojure()

		@Override
		//enable the plugin
		public void onEnable() {
			// Load config
			this.saveDefaultConfig();
			config = this.getConfig();
			eventobs = new EventObserver(this);
			
			// If configured to do so, check the latest version on BukkitDEV and
			// alert if user is out of date.
			if (this.config.getBoolean(Config.CHECK_UPDATE)) {
			      //new CheckUpdate(this, <INSERT ID HERE>);
			}
			doParticles = this.config.getBoolean(Config.DO_PARTICLES);
			clojureRegen = this.config.getBoolean(Config.USE_CLOJURE_REGEN);
			active = true;

			// Load Clojure REGENgine support:
			if (clojureRegen) {	loadClojure(); }// if
		}// onEnable()
		
		//disable world physics
		public void disablePhysics()
		{
			assert(this.eventobs != null);
			eventobs.setPhysics(false);
		}// disablePhysics()
		
		
		//enable world physics
		public void enablePhysics()
		{
			assert(this.eventobs != null);
			eventobs.setPhysics(true);
		}// enablePhysics()
		
/*
		@SuppressWarnings("deprecation")
		public void alter(Location l, Material m){
			if (clojureRegen)
			{ alterRestore(l, m); }
			else
			{
				Material backup = l.getBlock().getType();
				if(backup != m && !blockMap.containsKey(l)){
					blockMap.put(l, backup);
					dataMap.put(l, l.getBlock().getData());
					l.getBlock().setType(m);
					regen(l);
				}
			}// else
		}// alter()
*/		
		/*
		 * Preconditons:
		 * Perform an alteration for the world in order to place the block and have it implemented properly
		 * 
		 * Postconditions
		 * Places the block properly
		 * 
		 * Args
		 * Plugin, the plugin on which it will be run
		 * Location, location of blcok
		 * Material, material block
		 */
		public void alter(Plugin plugin, Location l, Material m){
			if(!active){return;}
			
			Location normal = Util.normalizeLocation(l);
			Material backup = normal.getBlock().getType();
			if(backup != m){
				if(!blockMap.containsKey(normal)){
					
					// turns physics off
					//eventobs.setPhysics(false);
					
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

		// The first prototype of our restorations
		private void regen(final Location l){
			
			
			if(doParticles && !blockMap.containsKey(1)){
				
				RestorationWarnings w = new RestorationWarnings(200L, l, this);
				w.start();
				
			}
			
			this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				public void run() {
					blockMap.get(l).place(l);
				    blockMap.remove(l);
				}
			}, 200L);
		}
}


