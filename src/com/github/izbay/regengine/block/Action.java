package com.github.izbay.regengine.block;

public enum Action 
{ DESTROY, RESTORE, RESTORE_AFTER_LOSS;

	public boolean isHardDependency()
	{ return (this == DESTROY || this == RESTORE_AFTER_LOSS); }
}// Action
