package com.starcor.xulapp.behavior;

/**
 * Created by hy on 2015/9/7.
 */
public interface XulBehavior {
	int BEHAVIOR_TYPE_UI = 0x0001;
	int BEHAVIOR_TYPE_APP = 0x0002;
	int BEHAVIOR_TYPE_LOGIC = 0x0002;

	int getBehaviorType();
}
