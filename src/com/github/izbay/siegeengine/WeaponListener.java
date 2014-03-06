package com.github.izbay.siegeengine;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.util.Vector;

import com.github.izbay.regengine.RegEnginePlugin;

public class WeaponListener implements Listener {

	RegEnginePlugin reg = (RegEnginePlugin)Bukkit.getServer().getPluginManager().getPlugin("RegEngine");
	
	@EventHandler
	private void collideHandler(VehicleBlockCollisionEvent e){
		//TODO: Minecart/Weapon Type differentiation.
		if(e.getVehicle().getType() == EntityType.MINECART){
			Location v = e.getVehicle().getLocation();
			Location upLoc = e.getBlock().getLocation().add(0,1,0);
			if(v.getBlock().getType().isSolid()){
				e.getVehicle().teleport(v.add(0,1,0));
			}
			if(e.getBlock().getLocation().getBlock().getType().isSolid()){
				if(!upLoc.getBlock().getType().isSolid()){
					reg.alter(v, Material.RAILS);
					reg.alter(upLoc, Material.RAILS);
				} else {
					Double yaw = Weapon.toRadians(v.getYaw());
					int sin = (int) Math.round(Math.sin(yaw));
					int cos = (int) Math.round(Math.cos(yaw));
					
					//Generate a small cone. We'll worry about multipliers and non-right angles later.
					Vector[] vec = {
							new Vector(0,0,0),
							// Up Down
							new Vector(0,1,0), new Vector(0,-1,0),
							// Left Right
							new Vector(0,0,-sin), new Vector(-cos,0,0),
							new Vector(cos,0,0), new Vector(0,0,sin),
							// Forward
							new Vector(sin,0,0), new Vector(sin*2,0,0),
							new Vector(0,0,cos), new Vector(0,0,cos*2)};
					
					//TODO: Only remove 1 of the blocks and add sounds.
					for(int i=0; i<vec.length; i++){
						Location test = new Location(upLoc.getWorld(), upLoc.getX(), upLoc.getY(), upLoc.getZ());
						test = test.add(vec[i]);
						breakSound(test);
						reg.alter(test, Material.AIR);
						
					}
				}
			}
			
		}
	}
	
	private void breakSound(Location loc){
		Material mat = loc.getBlock().getType();
		if(mat==Material.AIR) return;
		String str = mat.toString();
		Sound sound = Sound.ITEM_BREAK;
		
		// It's unfortunate, but there's no classification to rely upon for sound effects.
		if(loc.getBlock().isLiquid())
			sound = Sound.SPLASH;
		else if(str.contains("WOOD") || str.contains("LOG"))
			sound = Sound.ZOMBIE_WOODBREAK;
		else if(str.contains("STONE") || str.contains("SMOOTH") || str.contains("ORE") || str.contains("BRICK") || str.contains("ROCK")){
			loc.getWorld().playSound(loc, Sound.EXPLODE, 1, 1);
			sound = Sound.FUSE;}
		else if(str.contains("GLASS"))
			sound = Sound.GLASS;
		else if(str.contains("GRASS") || str.contains("DIRT") || str.contains("SAND") || str.contains("GRAVEL"))
			sound = Sound.FUSE;
		else if(str.contains("RAIL") || str.contains("ANVIL") || str.contains("HOPPER") || str.contains("CAULDRON") || str.contains("IRON") ||
				str.contains("GOLD") || str.contains("DIAMOND") || str.contains("ICE"))
			sound = Sound.ANVIL_LAND;
		else if(str.contains("FENCE") || str.contains("TRAP_DOOR") || str.contains("WORKBENCH") || str.contains("SAPLING") || str.contains("CHEST") ||
				str.contains("BUSH") || str.contains("BOOK") || str.contains("SIGN") || str.contains("LADDER"))
			sound = Sound.ZOMBIE_WOODBREAK;
		else if(str.contains("QUARTZ") || str.contains("MONSTER") || str.contains("RACK") || str.contains("FURNACE") || str.contains("OBSIDIAN") ||
				str.contains("EMERALD") || str.contains("LAPIS") || str.contains("DISPENSER") || str.contains("DROPPER") || str.contains("PISTON") ||
				str.contains("STEP")){
			loc.getWorld().playSound(loc, Sound.EXPLODE, 1, 1);
			sound = Sound.FUSE;}
		else if(str.contains("SUGAR") || str.contains("VINE") || str.contains("SNOW") || str.contains("SOIL") || str.contains("MYCEL") ||
				str.contains("CROPS") || str.contains("FLOWER") || str.contains("ROSE") || str.contains("MUSHROOM") || str.contains("LEAVES"))
			sound = Sound.FUSE;
		else if(str.contains("CACTUS") || str.contains("WOOL") || str.contains("HAY") || str.contains("CARPET") || str.contains("HAY") ||
				str.contains("WARTS") || str.contains("REDSTONE") || str.contains("DIODE") || str.contains("CLAY") || str.contains("CAKE") ||
				str.contains("PUMPKIN") || str.contains("JACK") || str.contains("MELON") || str.contains("LILY") || str.contains("SPONGE"))
			sound = Sound.CREEPER_DEATH;

		loc.getWorld().playSound(loc, sound, 1, 1);
	}
}
