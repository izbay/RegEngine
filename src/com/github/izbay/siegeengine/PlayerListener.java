package com.github.izbay.siegeengine;

//import net.minecraft.server.v1_7_R1.EntityMinecartAbstract;
//import net.minecraft.server.v1_7_R1.NBTTagCompound;

import com.github.izbay.regengine.RegEnginePlugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
//import org.bukkit.World;
//import org.bukkit.craftbukkit.v1_7_R1.entity.CraftMinecart;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
//import org.bukkit.event.block.BlockPhysicsEvent;
//import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;

public class PlayerListener implements Listener{
	SiegeEnginePlugin plugin = (SiegeEnginePlugin)Bukkit.getServer().getPluginManager().getPlugin("SiegeEngine");
	RegEnginePlugin reg = (RegEnginePlugin)Bukkit.getServer().getPluginManager().getPlugin("RegEngine");
	
	@EventHandler
    private void playSound(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		if(e.getAction()==Action.RIGHT_CLICK_BLOCK && p.getItemInHand().getType() == Material.MINECART){
			p.getWorld().playSound(p.getLocation(), Sound.ANVIL_LAND, 1, 1);
			Minecart minecart = (Minecart) p.getWorld().spawnEntity(e.getClickedBlock().getLocation().add(0, 1.4, 0), EntityType.MINECART);
			//Minecart brace = (Minecart) p.getWorld().spawnEntity(e.getClickedBlock().getLocation().add(0,1.4,0), EntityType.MINECART);
			//setData(brace, Material.LOG, 8);
			//minecart.setPassenger(brace);
			minecart.setMaxSpeed(0.02);
		}
    }
	
	@EventHandler
	private void pushCart(VehicleBlockCollisionEvent e){
		Location v = e.getVehicle().getLocation();
		Location upLoc = e.getBlock().getLocation().add(0,1,0);
		
		if(e.getBlock().getLocation().getBlock().getType().isSolid()){
			if(!upLoc.getBlock().getType().isSolid()){
				reg.alter(v, Material.RAILS);
				reg.alter(upLoc, Material.RAILS);
			}
		}
	}
	
	@EventHandler
	private void nudgeCart(VehicleEntityCollisionEvent e){
		if(e.getEntity() instanceof Player){
			Vehicle v = e.getVehicle();
			Location l = v.getLocation();
			Player p = (Player)e.getEntity();
			
			l.setYaw(p.getLocation().getYaw()+90);
			
			/*double yaw = (p.getLocation().getYaw()+90) * Math.PI / 180;
			double mult = 0.05;
			Vector vec = new Vector(Math.cos(yaw)*mult, 0, Math.sin(yaw)*mult);
			l.add(vec);*/
			
			v.teleport(l);
		}
	}
	
	/**
	@EventHandler
	private void pushCart(VehicleEntityCollisionEvent e){
		if(e.getVehicle().getType() == EntityType.MINECART){
			Location l = e.getEntity().getLocation();
			Location v = e.getVehicle().getLocation();
			Location p = v.add(v).subtract(l);
			p.setY(v.getY()-1);
			World w = e.getVehicle().getWorld();
			Material m = w.getBlockAt(p).getType();
			if(m != Material.RAILS && m != Material.AIR && m != Material.SNOW){
				p.add(new Vector(0, 1, 0));
				m = w.getBlockAt(p).getType();
			} else if (m == Material.AIR){
				Material m2 = e.getVehicle().getWorld().getBlockAt(p.add(new Vector(0, -1, 0))).getType();
				if(m2 == Material.AIR || m2 == Material.SNOW)
					p.add(new Vector(0, -1, 0));
				m = w.getBlockAt(p).getType();
			}
			if(m != Material.RAILS && (m == Material.AIR || m == Material.SNOW)){
				reg.alter(p, Material.RAILS);
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	private void setData(Minecart minecart, Material block, int data)
	  {
	    
		if (!block.isBlock()) {
	      throw new IllegalArgumentException("The material of the minecart must be a block!");
	    }
	    EntityMinecartAbstract rawMinecart = getRaw(minecart);
	    NBTTagCompound minecartTag = getCompound(rawMinecart);
	    minecartTag.setByte("CustomDisplayTile", (byte)1);
	    minecartTag.setInt("DisplayTile", block.getId());
	    minecartTag.setInt("DisplayOffset", data);
	    
	    if(data > 0){
	    	//TODO: set rotation.
	    }
	    
	    setCompound(rawMinecart, minecartTag);
	  }
	
	  private static EntityMinecartAbstract getRaw(Minecart target)
	  {
	    return ((CraftMinecart)target).getHandle();
	  }
	  private static NBTTagCompound getCompound(EntityMinecartAbstract target)
	  {
	    NBTTagCompound tag = new NBTTagCompound();
	    target.c(tag);
	    return tag;
	  }
	private static void setCompound(EntityMinecartAbstract target, NBTTagCompound compound)
	  {
	    target.f(compound);
	  }*/
}
