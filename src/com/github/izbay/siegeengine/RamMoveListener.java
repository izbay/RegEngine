package com.github.izbay.siegeengine;

//import org.bukkit.block.Block;
import org.bukkit.event.*;
import org.bukkit.event.vehicle.*;
import org.bukkit.util.*;
import org.bukkit.*;

import com.github.izbay.regengine.*;
import com.github.izbay.util.Util;

/**
 * @author J. Jakes-Schauer
 * 
 */
public class RamMoveListener implements Listener {
	/*
	public RamMoveListener(final SiegeEnginePlugin plugin) 
	{
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}*/
	RegEnginePlugin reg = (RegEnginePlugin)Bukkit.getServer().getPluginManager().getPlugin("RegEngine");
	

	@EventHandler
	public void ramMoveHandler(final VehicleMoveEvent ev) {
		if (ev.getVehicle().getType() == SiegeEnginePlugin.RAM_VEHICLE_ENTITY) {
			final BlockVector vFrom = Util.getBlockVector(ev.getFrom());
			final BlockVector vTo = Util.getBlockVector(ev.getTo());
			
			assert(vFrom.getX() == vTo.getX() || vFrom.getZ() == vTo.getZ());
			
			if( vFrom.getY() == vTo.getY()
					&& (vFrom.getX() == vTo.getX() || vFrom.getZ() == vTo.getZ())
					&& !Util.equal(vFrom, vTo)
					&& Util.isSolid(Util.getBlockBelow(vFrom)) 
					&& !Util.isSolid(Util.getBlockBelow(vTo)) 
					&& Util.isSolid(Util.getBlockBelow(Util.getBlockBelow(vTo))) 
			) {
				reg.alter(vFrom, Material.RAILS);
				reg.alter(Util.getBlockBelow(vTo), Material.RAILS);
			}// if
		}// if
	}// ramMoveHandler()
}// RamMoveListener
