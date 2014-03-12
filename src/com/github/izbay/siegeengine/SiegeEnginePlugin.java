package com.github.izbay.siegeengine;

import org.bukkit.plugin.java.JavaPlugin;


public class SiegeEnginePlugin extends JavaPlugin {
	
	@Override
	public void onDisable() {
		
	}

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		getServer().getPluginManager().registerEvents(new WeaponListener(), this);
	}
	
}
