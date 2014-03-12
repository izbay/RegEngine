package com.github.izbay.siegeengine;

import com.github.izbay.siegeengine.Weapon.WeaponType;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerListener implements Listener {
	
	@EventHandler
    private void spawnWeapon(PlayerInteractEvent e) {	
		if(e.getAction()==Action.RIGHT_CLICK_BLOCK){
			Player p = e.getPlayer();
			Location target = e.getClickedBlock().getLocation().add(0, 0, 0);
			
			/** Battering Ram */
			if(	e.getClickedBlock().getType() == Material.ANVIL &&
			e.getClickedBlock().getLocation().add(0,-1,0).getBlock().getType() == Material.CAULDRON &&
			p.getItemInHand().getType() == Material.FLINT_AND_STEEL){
				e.getClickedBlock().setType(Material.AIR);
				e.getClickedBlock().getLocation().add(0,-1,0).getBlock().setType(Material.AIR);
				Weapon.spawn(target, WeaponType.BatteringRam);
			}
		}
    }
	
	@EventHandler
	private void cancelRide(PlayerInteractEntityEvent e){
		//TODO: Minecart/Weapon Type differentiation.
		if(e.getRightClicked().getType() == EntityType.MINECART){
			e.setCancelled(true);
		}
		//TODO: Move the climb slope code to here. Take it out of collision for controllability.
		//TODO: Detect downhill slope and set tracks.
	}
}
