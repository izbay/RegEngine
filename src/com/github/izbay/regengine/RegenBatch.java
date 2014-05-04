
package com.github.izbay.regengine;

import java.util.*; // I get sick of managing these individually

import net.minecraft.util.com.google.common.collect.Sets;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import com.github.izbay.regengine.block.*;
import com.github.izbay.regengine.serialize.SerializedBlock;
import com.github.izbay.util.*;

/**
 * @author jdjs
 * An essential timing property we wish to hold invariant is 
 * 	$$ \exists B_1, B_2 \in activeBatches . (vectors B_1) \cap (vectors B_2) \ne \emptyset \Rightarrow (t_s(B_1) < t_s(B_2) \rightarrow t_f(B_1) > t_f(B_2)) \land t_f(B_1) \ne t_f(B_2) $$
 * $t_s()$ and $t_f()$ refer to batch queue and restoration times, or 'start' and 'finish', respectively.
 * Two batches $B_1$ & $B_2$ 'overlap' if $(vectors B_1) \cap (vectors B_2)$ is nonempty, meaning the regions in space they contain intersect.  I hesitated to specify the intersection of $B_1$ and $B_2$ *themselves*, since this could introduce ambiguity about block equivalence.
 * The $t_f(B_1) \ne t_f(B_2)$ clause addresses our not knowing whether the execution order of two tasks scheduled for the same tick can be predicted.  Since the second task to be executed will dominate, we need this certainty.  
 * 
 * 
 */
public class RegenBatch implements RegenBatchIface 
{
	public enum Status { PENDING_ALTERATION, ALTERED_BUT_NOT_QUEUED, PENDING_RESTORATION, DONE, CANCELLED };
	public enum Type { DESTRUCTION, CREATION, BACKUP };

	protected final World world;
	protected long queueTime = 0;
	protected final long delay;
	public final DependingBlockSet blocks;
	public final Material newMaterial;
	public final Plugin plugin;
	public final Type batchType;
	public Set<RestorationWarnings> warner = new LinkedHashSet<RestorationWarnings>();
	//public Map<Location, SerializedBlock> blockMap;
	protected Status status;
	public final LinkedList<SerializedBlock> blockOrder = new LinkedList<SerializedBlock>();

	protected static final Map<World,LinkedHashSet<RegenBatch>> activeBatches = new LinkedHashMap<World,LinkedHashSet<RegenBatch>>();

	//	private BlockImage[] blocks; // Comment this out until I'm ready to use it

	public static Map<World,LinkedHashSet<RegenBatch>> activeBatches() { return activeBatches; }
	public static Set<RegenBatch> activeBatches(final World world) { return activeBatches.get(world); }

	/* (non-Javadoc)
	 * @see com.github.izbay.regengine.RegenBatchIface#getWorld()
	 */
	@Override
	public World world() { return world; }

	@Override
	public long delay() { return delay; }
	
	/**
	 * Returns zero if the batch hasn't been queued yet.  Normally this won't be seen.
	 * @return time at which batch was queued for regeneration.
	 */
	public long queueTime() { return queueTime; }

	public Status status() 
	{ 
		assert(activeBatches.containsKey(world));
		assert(status == Status.DONE || activeBatches.get(world).contains(this));
		return this.status; 
	}// status()

	/**
	 * Alias for isQueued().
	 * @return
	 */
	public boolean isRunning() { return isQueued(); }
	
	/**
	 * @return true if the batch restoration timer has been started.
	 */
	public boolean isQueued() { return status == Status.PENDING_RESTORATION; }

	/**
	 * @return true if the batch is "still supposed to be" run but has not been used to alter the World.
	 */
	public boolean isActive() { return status != Status.DONE && status != Status.CANCELLED; }

	public boolean isDone() { return status == Status.DONE; }

	/* (non-Javadoc)
	 * @see com.github.izbay.regengine.RegenBatchIface#getRestorationTime()
	 */
	@Override
	public long getRestorationTime() 
	{ return (queueTime() == 0) ? 0 : delay + queueTime; }


	/**
	 * Factory method.  Intended as the 
	 * @param blockVectors
	 * @param world
	 * @param targetTime
	 * @return
	 */
	public static RegenBatch destroying(final Plugin plugin, final World world, final Iterable<Vector> blockVectors, final long delay)
	{	return new RegenBatch(plugin, world, blockVectors, delay); }
	public static RegenBatch destroying(final Plugin plugin, final World world, final Vector[] blockVectors, final long delay)
	{	return destroying(plugin, world, Arrays.asList(blockVectors), delay); }
	
	
	/**
	 * Simple backup-and-restore facility.
	 * @param plugin
	 * @param world
	 * @param blockVectors
	 * @return
	 */
	public static RegenBatch storing(final Plugin plugin, final World world, final Iterable<Vector> blockVectors)
	{
		return new BackupRegenBatch(plugin, world, blockVectors);
	}// storing()
	
	public static RegenBatch altering(final Plugin plugin, final World w, final Iterable<Vector> blockVectors, final Material newMat, final long delay)
	{
		if(BlockTypes.isDestroyed(newMat))
		{	return( destroying(plugin, w, blockVectors, delay) ); }// if
		else
		{	return new RegenBatch(plugin, w, blockVectors, delay); }// else
	}// altering()
	/**
	 * Synonym for RegenBatch.destroying().
	 * @param plugin
	 * @param world
	 * @param blockVectors
	 * @param delay
	 * @return
	 */
	public static RegenBatch altering(final Plugin plugin, final World world, final Iterable<Vector> blockVectors, final long delay)
	{	return destroying(plugin, world, blockVectors, delay); }
	
	/**
	 * Primary "creation" batch constructor.  Use factory method RegenBatch.altering() client-side.
	 * @param plugin
	 * @param world
	 * @param blockVectors
	 * @param newMat
	 * @param delay
	 */

	/*
	private RegenBatch(final Plugin plugin, final World world, final Iterable<Vector> blockVectors, final Material newMat, final long delay)
	{
		super();
		
		this.plugin = plugin;
		this.world = world;
		this.newMaterial = newMat;
		this.delay = delay;
		this.batchType = (BlockTypes.isDestroyed(newMat) ? Type.DESTRUCTION : Type.CREATION);
		
		RegenBatch.ensureInActiveSet(this);
		
		// FIXME: 
	}// ctor
	*/

 // "Backup" constructor.  Called by subclass BackupRegenBatch.
	protected RegenBatch(final Plugin plugin, final World world, final Iterable<Vector> blockVectors)
	{
		super();
		this.batchType = Type.BACKUP;
		this.plugin = plugin;
		this.world = world;
		this.newMaterial = Material.AIR; // 
		this.delay = 0;
				
	// RegenBatch.ensureInBackupSet(this);
		final DependingBlockSet sIn = new DependingBlockSet();
		for(Vector v : blockVectors)
		{
			final Block b = Util.getBlockAt(v,world);
				sIn.add(DependingBlock.from(b, Action.RESTORE)); 
		}// for

		this.blocks = sIn.doFullDependencySearch(); // The RESTORE designation means that this will be, effectively, a reverse dependency search.
//		this.status = Status.PENDING_ALTERATION;
		this.status = Status.PENDING_RESTORATION;
	}// backup ctor
	
	/**
	 * Primary destruction constructor; for internal use.  Externally the factory methods are preffered.
	 * @param plugin
	 * @param blockVectors
	 * @param world
	 * @param delay
	 */
	private RegenBatch(final Plugin plugin, final World world, final Iterable<Vector> blockVectors, final long delay) 
	{
		super();
		
		//		this.blocks = blocks;
		this.batchType = Type.DESTRUCTION;
		this.plugin = plugin;
		this.world = world;
		this.newMaterial = Material.AIR;
		this.delay = delay;
		
		RegenBatch.ensureInActiveSet(this);
		
		final DependingBlockSet sIn = new DependingBlockSet();
		for(Vector v : blockVectors)
		{ 
			final Block b = Util.getBlockAt(v,world);
			if(b.getType() != newMaterial)
			{ sIn.add(DependingBlock.from(b, Action.DESTROY)); }// if
		}// for

		this.blocks = sIn.doFullDependencySearch();
		this.status = Status.PENDING_ALTERATION;
	}// ctor



	/* (non-Javadoc)
	 * @see com.github.izbay.regengine.RegenBatchIface#cancel()
	 */
	@Override
	public void cancel()
	{
		switch(this.status)
		{
		case PENDING_ALTERATION:
		case ALTERED_BUT_NOT_QUEUED:
			this.status = Status.CANCELLED;
			RegenBatch.removeFromActiveSet(this);
			break;
		case PENDING_RESTORATION:
			// TODO:
			throw new Error("Cancelling mid-regeneration not yet implemented!");
		case DONE:
		case CANCELLED:
			assert(!this.isActive());
			assert(!this.isRunning());
			assert(!activeBatches.get(world).contains(this));
			break;
		default:
			throw new Error("Fell through RegenBatch.cancel(); shouldn't happen.");
		}// switch
	}// cancel


	/**
	 * Wrapper that activates ('alters') and immediately schedules restoration of blocks.
	 * @return
	 */
	public RegenBatch alterAndRestore()
	{
		batchAlter();
		assert(status == Status.ALTERED_BUT_NOT_QUEUED);
		queueBatchRestoration();
		assert(this.isRunning());
		return this;
	}// destroyAndRestore()

	/**
	 * For internal use.  Does *not* queue for regeneration.  To cause that as well, use .alterAndRestore().
	 * @return
	 */
	public RegenBatch batchAlter()
	{
		if (status != Status.PENDING_ALTERATION)
		{	throw new Error("Called RegenBatch.batchAlter() on an already-altered Batch.  (I think.)"); }// if
		else if(!blocks.isEmpty())
		{
			//this.blockMap = new LinkedHashMap<Location,SerializedBlock>();
			//this.blockOrder = new ArrayDeque<SerializedBlock>();
			for(DependingBlock d : this.blocks)
			{
				assert(d.getType() != this.newMaterial);
				//final Location normal = Util.normalizeLocation(d.getLocation());
				final Block b = Util.getBlockAt(d.getLocation()); 
				this.blockOrder.add(new SerializedBlock(b));
				//				this.blockMap.put(normal, new SerializedBlock(b));
			}// for
			//			assert(this.blockMap.size() == this.blocks.size());
			assert(this.blockOrder.size() == this.blocks.size());

			// Mutating sort:
			Collections.sort(this.blockOrder);

			// Removal loop:
			//		for(Map.Entry<Location,SerializedBlock> entry : this.blockMap.entrySet())
			for(Iterator<SerializedBlock> i = this.blockOrder.descendingIterator(); i.hasNext(); )
				//		for(Map.Entry<Location,SerializedBlock> entry : blockMap.entrySet())
			{
				final SerializedBlock sb = i.next();
//			for(SerializedBlock sb : this.blockOrder)
//			{
				final Block block = Util.getBlockAt(sb.getVector(),this.world);
				final BlockState state = block.getState();
				if(state instanceof Chest){
					((Chest)state).getBlockInventory().clear();
				} else if(state instanceof InventoryHolder){
					((InventoryHolder)state).getInventory().clear();
				}

				// We only back up, we don't remove, blocks marked 'Action.RESTORE' or the like.
				if(this.blocks.get(sb.getVector()).action() == Action.DESTROY)
				{	block.setType(newMaterial);	}// if
			}// for

		}// if

		status = Status.ALTERED_BUT_NOT_QUEUED;
		return this;
	}// batchAlter()
	
	/*
	public static 
	
	public static ensureInBackupSet(final RegenBatch bat)
	{
		if(!RegenBatch.backupBatches.containsKey(batch.world())) 
        {	RegenBatch.backupBatches.put(batch.world(), new LinkedHashSet<RegenBatch>()); }// if
		assert(RegenBatch.backupBatches.containsKey(batch.world()));

		assert(!RegenBatch.backupBatches.get(batch.world).contains(batch));
		assert(!RegenBatch.activeBatches.get(batch.world).contains(batch));
	}
	*/

	public static void ensureInActiveSet(final RegenBatch batch)
	{
		if(!RegenBatch.activeBatches.containsKey(batch.world())) RegenBatch.activeBatches.put(batch.world(), new LinkedHashSet<RegenBatch>());
		assert(RegenBatch.activeBatches.containsKey(batch.world()));

		assert(!RegenBatch.activeBatches.get(batch.world).contains(batch));
		RegenBatch.activeBatches.get(batch.world).add(batch);
		assert(!RegenBatch.activeBatches.get(batch.world).contains(batch));
	}// ensureInActiveSet()

	/**
	 * Public only to be considerate.  Only to be used internally.
	 * @param batch
	 */
	public static void removeFromActiveSet(final RegenBatch batch)
	{
		assert(activeBatches.containsKey(batch.world));
		assert(activeBatches.get(batch.world).contains(batch));
		activeBatches.get(batch.world).remove(batch);
		assert(!activeBatches.get(batch.world).contains(batch));
	}// removeFromActiveSet()

	/*protected void disablePhysics()
	{	RegEnginePlugin.getInstance().disablePhysics(); }

	protected void enablePhysics()
	{	RegEnginePlugin.getInstance().enablePhysics(); }
	*/

	protected RegenBatch restore()
	{
		if(status != Status.PENDING_RESTORATION) throw new Error("Can only call RegenBatch.restore() on a batch 'PENDING_RESTORATION'.");

		RegEnginePlugin.getInstance().doWithDisabledPhysics(
				new Runnable() { public void run()
					{
						// TODO: Multiple loops, if necessary, to ensure proper replacement.
						// TODO: Pop-to-item the blocks being replaced.
						for(Iterator<SerializedBlock> i = blockOrder.iterator(); i.hasNext(); )
							//		for(Map.Entry<Location,SerializedBlock> entry : blockMap.entrySet())
						{
							final SerializedBlock block = i.next();
							block.place();
						}// for
					}// λ
				});

		// Mark the batch as 'done', including removing it from the global map:
		this.status = Status.DONE;
		RegenBatch.removeFromActiveSet(this);
		// Close out the warning system (remove the reference to the existing set):
		this.warner = new HashSet<RestorationWarnings>();

		return this;
	}// restore()

	/**
	 * For internal use.  Called by batchAlterAndRestore().
	 * @return
	 */
	public RegenBatch queueBatchRestoration()
	{
		if(status != Status.ALTERED_BUT_NOT_QUEUED)
		{	throw new Error("Can only call RegenBatch.queueBatchRestoration() with an 'ALTERED' status.  Did you run .batchAlter() first?"); }// if

		// If it's supposed to occur "instantly", don't delay restoration atall.
		if(delay > 0)
		{
			this.queueTime = world.getFullTime();

			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { public void run() 
				{
					restore();
				}// λ
			}, this.delay);

			// Enable warnings if applicable:
			if(RegEnginePlugin.getInstance().sendRegenPlayerWarnings)
			{
				for(Map.Entry<BlockVector,DependingBlock> entry : this.blocks.blocks.entrySet())
				{
					// Start the warning system iff the block is of a material unhealthy to inhabit:
					if(BlockTypes.needsWarnPlayerOnRegeneration(entry.getValue().block.getType()))
					{
						final RestorationWarnings w = new RestorationWarnings(this.delay, Util.getLocation(entry.getKey(), world), this.plugin);
						w.start();
						warner.add(w);
					}// if
				}// for
			}// if

			this.status = Status.PENDING_RESTORATION;
			assert(this.isRunning());
		}// if
		else restore(); // immediately

		return this;
	}// queueBatchRestoration()		

	
	public Set<BlockVector> intersection(final RegenBatch rhs)
	{ return this.blocks.intersection(rhs.blocks); }// intersection()
	
	public long getProjectedRestorationTime()
	{	return delay + this.world.getFullTime(); }
	

	public LinkedList<RegenBatch> getPossiblyConflictingBatches()
	{
		final LinkedList<RegenBatch> batches = new LinkedList<RegenBatch>();
		for(RegenBatch bat : RegenBatch.activeBatches(this.world))
		{
			if(this != bat 
				&& bat.isRunning() 
				&& this.getProjectedRestorationTime() >= bat.getProjectedRestorationTime())
			{	batches.add(bat); }
		}// for
		
		// Sort from high to low by finishing time:
		Collections.sort(batches, new Comparator<RegenBatch>() 
		{ 
			public int compare(final RegenBatch b1, final RegenBatch b2) 
			{ 
				// To achieve the reverse order, swap the comparison signs:
				if(b1.getProjectedRestorationTime() < b2.getProjectedRestorationTime())
					return 1; 
				else if (b1.getProjectedRestorationTime() == b2.getProjectedRestorationTime())
					return 0;
				else return -1;
			}// compare()
		});// sort
		
		return batches;
	}// getPossiblyConflictingBatches()
	
	
	public Map<RegenBatch, Set<BlockVector>> groupByConflicts(final Set<RegenBatch> set)
	{
		final LinkedList<RegenBatch> remainingBatches = getPossiblyConflictingBatches();
		Set<BlockVector> remainingBlocks = new LinkedHashSet<BlockVector>(this.blocks.blocks.keySet());
		final Map<RegenBatch,Set<BlockVector>> acc = new LinkedHashMap<RegenBatch, Set<BlockVector>>();
		while(true)
		{
			if(remainingBlocks.isEmpty()) 
			{	
				assert(!acc.isEmpty());
				return acc;
			}// if
			else if(remainingBatches.isEmpty()) 
			{
				assert(!acc.containsKey(this));
				acc.put(this, remainingBlocks);
				return acc;
			}// elif
			else // continue loop
			{
				final RegenBatch nextBat = remainingBatches.pop();
				final Set<BlockVector> isec = Sets.intersection(remainingBlocks, nextBat.blocks.blocks.keySet());
				remainingBlocks = Sets.difference(remainingBlocks, isec);
				if(!isec.isEmpty())
				{	acc.put(nextBat, isec); }// if
			}// else
			
		}// while true
	}// groupByConflicts()

	/**
	 * @param key
	 * @return
	 * @see com.github.izbay.regengine.block.DependingBlockSet#get(org.bukkit.util.BlockVector)
	 */
	public DependingBlock get(final BlockVector key) {
		return blocks.get(key);
	}
	/**
	 * @return
	 * @see com.github.izbay.regengine.block.DependingBlockSet#isEmpty()
	 */
	public boolean isEmpty() {
		return blocks.isEmpty();
	}
	/**
	 * @return
	 * @see com.github.izbay.regengine.block.DependingBlockSet#size()
	 */
	public int size() {
		return blocks.size();
	}
	/**
	 * @param rhs
	 * @return
	 * @see com.github.izbay.regengine.block.DependingBlockSet#intersection(com.github.izbay.regengine.block.DependingBlockSet)
	 */
	public Set<BlockVector> intersection(final DependingBlockSet rhs) {
		return blocks.intersection(rhs);
	}
	
	
}// RegenBatch
