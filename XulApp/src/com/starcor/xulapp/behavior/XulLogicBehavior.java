package com.starcor.xulapp.behavior;

/**
 * Created by hy on 2015/9/15.
 */
public class XulLogicBehavior implements XulBehavior {

	@Override
	public int getBehaviorType() {
		return XulBehavior.BEHAVIOR_TYPE_LOGIC;
	}

	public boolean exec(String command, String[] params) {
		return false;
	}

	// public boolean queryData( query_params, dataCallback ) {}
	// public boolean queryDataAsync( query_params, dataCallback ) {}
	// public PullDataCollection pullData( query_params ) {}

	public boolean registerEventListener() {
		return false;
	}
}
