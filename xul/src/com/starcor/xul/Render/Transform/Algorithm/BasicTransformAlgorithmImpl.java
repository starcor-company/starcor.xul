package com.starcor.xul.Render.Transform.Algorithm;

/**
 * Created by hy on 2015/6/30.
 */
public abstract class BasicTransformAlgorithmImpl implements ITransformAlgorithm {
	@Override
	public UpdateResult update(float[] params, long begin, long duration, long progress, float curVal, float srcVal, float oldDestVal, float newDestVal) {
		return commonUpdateAnimation(begin, duration, progress, curVal, srcVal, oldDestVal, newDestVal);
	}

	public static UpdateResult updateResult = new UpdateResult();

	public static UpdateResult commonUpdateAnimation(long begin, long duration, long progress, float curVal, float srcVal, float oldDestVal, float newDestVal) {
		boolean isSameDirection = (oldDestVal - curVal) * (newDestVal - curVal) >= 0;
		updateResult.newDest = newDestVal;
		updateResult.newSrc = curVal;
		if (isSameDirection) {
			updateResult.newBegin = begin + progress;
		} else {
			updateResult.newBegin = begin + 2 * progress - duration;
		}
		return updateResult;
	}
}
