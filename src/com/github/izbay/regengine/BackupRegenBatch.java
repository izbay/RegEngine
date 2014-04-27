package com.github.izbay.regengine;

import java.util.*;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class BackupRegenBatch extends RegenBatch 
{

 // "Backup" constructor.  Invoked by RegenBatch.storing().
	// TODO: protected.
	public BackupRegenBatch(final Plugin plugin, final World world, final Iterable<Vector> blockVectors)
	{
		super(plugin, world, blockVectors);
	}// backup ctor

	@Override
	public RegenBatch restore()
	{
		RegEnginePlugin.getInstance().doWithDisabledPhysics(
				new Runnable() { public void run()
					{
						// TODO: Multiple loops, if necessary, to ensure proper replacement.
						// TODO: Pop-to-item the blocks being replaced.
						for(Iterator<SerializedBlock> i = blockOrder.iterator(); i.hasNext(); )
						{
							final SerializedBlock block = i.next();
							block.place();
						}// for
					}// λ
				});
		return this;
	}// restore()
}// BackupRegenBatch

