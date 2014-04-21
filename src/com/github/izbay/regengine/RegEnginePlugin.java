package com.github.izbay.regengine;

import java.util.HashMap;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/*
import clojure.lang.RT;
import clojure.lang.Compiler;
*/
//import clojure.lang.Var;

import com.github.izbay.util.*;

public class RegEnginePlugin extends JavaPlugin 
{
//		private HashMap<Location, Material> blockMap = new HashMap<Location,Material>();
		private HashMap<Location, SerializedBlock> blockMap = new HashMap<Location,SerializedBlock>();
//		private HashMap<Location, Byte> dataMap = new HashMap<Location,Byte>();
		private FileConfiguration config;
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
			//TODO: Write out every map to file.
		}

		@Override
		public void onEnable() {
			// Load config
			this.saveDefaultConfig();
			config = this.getConfig();
			
			// If configured to do so, check the latest version on BukkitDEV and
			// alert if user is out of date.
			if (this.config.getBoolean(Config.CHECK_UPDATE)) {
			      //new CheckUpdate(this, <INSERT ID HERE>);
			}
			doParticles = this.config.getBoolean(Config.DO_PARTICLES);
			clojureRegen = this.config.getBoolean(Config.USE_CLOJURE_REGEN);
			
			// Load Clojure REGENgine implementation:
			if (clojureRegen)
			{
				/*
				try {
					RT.loadResourceScript("cljengine/regen.clj");
					RT.var("cljengine.mc", "*debug-print*", true);// Set to false to make (debug-print) can it
					RT.var("cljengine.regen", "regen-total-delay", 200); // 10s default wait
					RT.var("cljengine.regen", "regen-warning-period", 20); // Shorter default period
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				*/
			}// if
		}// onEnable()
		
		
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
		
		/*
		public void alter(Plugin plugin, final Vector v, final Material m)
		{	alter(plugin, Util.getLocation(v), m); }
		
		public void alter(Plugin plugin, final Block b, final Material m)
		{	alter(plugin, b.getLocation(), m); }
		*/
		
		/*
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
		
			*/

		/*
	// Ad-hoc polymorphism.
		public void alter(Location l){
			alter(l, Material.AIR);
		}
		public void alter(final Vector v, final Material m)
		{	alter(Util.getLocation(v), m); }
		public void alter(final Block b, final Material m)
		{	alter(b.getLocation(), m); }
		*/

/*
		// It's ugly, but this is the simplest way to invoke Lisp functions from Java: convert the Java lvars into Clojure globals and follow up with eval().
		public void alterRestore(final Location loc, final Material m)
		{
			//RT.var("cljengine.mc", "*debug-print*", false);
			
			RT.var("cljengine.regen", "alter-restore-loc", loc);
			RT.var("cljengine.regen", "alter-restore-mat", m);
			final Object obj = RT.readString("(cljengine.regen/alter-and-restore cljengine.regen/alter-restore-loc :new-material cljengine.regen/alter-restore-mat)");
			assert(obj != null);
			Compiler.eval(obj);
		}// alterRestore()
		
		public void batchAlterRestore(/*final Collection<Location> final Location[] blocks, final Material m, final World w)
		{
			RT.var("cljengine.regen", "alter-restore-blocks", blocks);
			RT.var("cljengine.regen", "alter-restore-mat", m);
			// NB: At the moment, the :world keyword has no effect.
			RT.var("cljengine.regen", "alter-restore-world", w);
			final Object obj = RT.readString("(cljengine.regen/batch-alter-and-restore cljengine.regen/alter-restore-blocks :new-material cljengine.regen/alter-restore-mat :world cljengine.regen/alter-restore-world)");
			assert(obj != null);
			Compiler.eval(obj);
		}// batchAlterRestore()

		// Duplicating the preceding Clojure def. is pretty clumsy:
		public void batchAlterRestore(/*final Collection<BlockVector> final BlockVector[] blocks, final Material m, final World w)
		{
			RT.var("cljengine.regen", "alter-restore-blocks", blocks);
			RT.var("cljengine.regen", "alter-restore-mat", m);
			// NB: At the moment, the :world keyword has no effect.
			RT.var("cljengine.regen", "alter-restore-world", w);
			final Object obj = RT.readString("(cljengine.regen/batch-alter-and-restore cljengine.regen/alter-restore-blocks " +
					":new-material cljengine.regen/alter-restore-mat " +
					":world cljengine.regen/alter-restore-world)");
			assert(obj != null);
			Compiler.eval(obj);
		}// batchAlterRestore()
		
		
*/
		// The first prototype:
		private void regen(final Location l){
			
			
			if(doParticles && !blockMap.containsKey(1)){
				
				RestorationWarnings w = new RestorationWarnings(200L, l, this);
				w.start();
				
				/*
				for(int i=0; i<60; i+=10){
					this.getServer().getScheduler().scheduleSyncDelayedTask(this, new BukkitRunnable() {
						public void run() {
							l.getWorld().playEffect(l, Effect.MOBSPAWNER_FLAMES, 2004);
						}
					}, 200L-i);
				}
				*/
			}
			
			this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				public void run() {
					blockMap.get(l).place(l);
				    blockMap.remove(l);
		//		    dataMap.remove(l);
				}
			}, 200L);
		}
}// RegEnginePlugin


