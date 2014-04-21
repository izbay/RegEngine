package com.github.izbay.regengine;

import org.bukkit.World;

public interface RegenBatchIface 
{

	/**
	 * @return the world in which this restoration unit operates
	 */
	public abstract World getWorld();

	/**
	 * @return the tick at which restoration should proceed
	 */
	public abstract long getRestorationTime();

	/**
	 * Dequeues restoration unit.
	 */
	public abstract void cancel();

}// RegenBatchIface
