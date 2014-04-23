package com.github.izbay.regengine;

import org.bukkit.World;

public interface RegenBatchIface 
{

	/**
	 * @return the world in which this restoration unit operates
	 */
	public abstract World world();
	
	public abstract long delay();

	/**
	 * @return the tick at which restoration will proceed.  If the batch has not been queued (unusual), returns zero.
	 */
	public abstract long getRestorationTime();

	/**
	 * Deletes restoration unit.  NB: not very externally useful at the moment--it can't be used during the restoration wait, which is when it is most likely to be needed.
	 */
	public abstract void cancel();

}// RegenBatchIface
