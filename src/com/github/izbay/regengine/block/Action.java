package com.github.izbay.regengine.block;

public enum Action 
{	DESTROY,			// A block is "destroyed" (changed to AIR), and a dependencies check will be performed in view of this fact.
	RESTORE,			// The dependency check will be done to ensure that the block can be, and is, replaced at restoration time.  This indicates a 'soft' dependency--blocks might or might not actually need replacement.
	RESTORE_AFTER_LOSS;	// Like DESTROY except that destruction is not explicitly performed by REGENgine.  It is, however, assumed that, while not immediately, the block will effectively be 'lost', or destroyed, between the time its batch is queued and when it is restored.  This is a form of 'hard' dependency mainly for describing the effect of gravity on sand and the like.

	public boolean isHardDependency()
	{ return (this == DESTROY || this == RESTORE_AFTER_LOSS); }
}// Action
