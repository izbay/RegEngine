package com.github.izbay.siegeengine;

import net.minecraft.server.v1_7_R1.EntityMinecartAbstract;
import net.minecraft.server.v1_7_R1.NBTTagCompound;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftMinecart;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;


class Weapon {
	
	/** Types of Siege Engines */
	static protected enum WeaponType{
		
		/** Destroys walls when pushed up against them. */
		BatteringRam(Material.ANVIL, 5, Sound.IRONGOLEM_DEATH),
		
		/** Places blocks down when right clicked. */
		Wheelbarrow(Material.SAPLING, 0, Sound.ZOMBIE_WOOD);
		
		protected Material graphic;
		protected int offset;
		protected Sound spawnSound;
		
		private WeaponType(Material g, int o, Sound s){
			this.graphic = (!g.isBlock())?Material.AIR:g;
			this.offset = o;
			this.spawnSound = s;
		}
	}
	
	static protected double toRadians(double yaw){
		return (270-yaw) * Math.PI / 180;
	}	
	
	static private NBTTagCompound getCompound(EntityMinecartAbstract target){
	    NBTTagCompound tag = new NBTTagCompound();
	    target.c(tag);
	    return tag;
	}
	
	@SuppressWarnings("deprecation")
	static protected void setData(Minecart minecart, WeaponType type){
	    EntityMinecartAbstract rawMinecart = ((CraftMinecart)minecart).getHandle();
	    NBTTagCompound minecartTag = getCompound(rawMinecart);
	    
	    minecartTag.setByte("CustomDisplayTile", (byte)1);
	    minecartTag.setInt("DisplayTile", type.graphic.getId());
	    minecartTag.setInt("DisplayOffset", type.offset);
	    rawMinecart.f(minecartTag);
	}
	
	static protected void spawn(Location loc, WeaponType type){
		loc.setYaw(0);
		Minecart minecart = (Minecart) loc.getWorld().spawnEntity(loc, EntityType.MINECART);
		setData(minecart, type);
		loc.getWorld().playSound(loc, type.spawnSound, 1, 1);
	}
}
