package com.starcor.xul.Render.Drawer;

/**
 * Created by hy on 2014/11/12.
 */
public interface IXulAnimation {
	// 返回false表示动画已经结束，不用理处理更新动作
	boolean updateAnimation(long timestamp);
}
