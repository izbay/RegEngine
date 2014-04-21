package com.github.izbay.regengine;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class RestorationWarnings {
	
	private long timer;
	private Location location;
	private Plugin plugin;
	
	private BukkitRunnable br = new BukkitRunnable() {
		@Override
		public void run() {
			timer = timer / 2;
			runRunnable(timer, location);
		}
	};
	
	
	
	private void runRunnable(long time, final Location l)
	{
		if(time < 10 )
		{
			//System.out.println("RESTORED!");
			return;
		}
		//System.out.println("inside" + timer);
		
		
		//List<Player> list = new ArrayList<Player>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (p.getLocation().distance(l) < 1) {
				//list.add(p);
				p.sendMessage("Warning! Block Restoration in " + (time*2)/20 + " seconds!");
			}
		}
		
		l.getWorld().playEffect(l, Effect.MOBSPAWNER_FLAMES, 2004);
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,br, time);
	}
	
	
	public RestorationWarnings(long time, final Location l, Plugin plugin) {
		this.timer = time/2;
		this.location = l;
		this.plugin = plugin;
	}
	
	public void start()
	{
		this.runRunnable(this.timer, this.location);
	}
	

}








/*
package com.github.izbay.regengine;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

public class RestorationWarnings {
	
	private long timerTick = 1;
	private long FullTime;
	private Location location;
	private RegEnginePlugin plugin;
	
	private BukkitRunnable br = new BukkitRunnable() {
		@Override
		public void run() {
			timerTick = timerTick * 2;
			runRunnable(timerTick, location);
		}
	};
	
	
	
	private void runRunnable(long time, final Location l)
	{
		if(time > FullTime )
			return;
		
		System.out.println("inside - FLAME " + time);
		l.getWorld().playEffect(l, Effect.MOBSPAWNER_FLAMES, 2004);
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,br, time);
	}
	
	
	public RestorationWarnings(long FullTime, final Location l, RegEnginePlugin plugin) {
		this.FullTime = FullTime;
		this.location = l;
		this.plugin = plugin;
	}
	
	public void start()
	{
		this.runRunnable(this.timerTick, this.location);
	}
	

}
*/