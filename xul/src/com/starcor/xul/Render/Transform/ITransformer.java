package com.starcor.xul.Render.Transform;

import com.starcor.xul.Render.Transform.Algorithm.ITransformAlgorithm;

/**
 * Created by hy on 2015/5/26.
 */
public interface ITransformer {
	boolean switchAlgorithm(String algorithm);
	boolean switchParams(float[] params);
	float transform(float time, float duration, float fromVal, float toVal);
	// return new begin time
	ITransformAlgorithm.UpdateResult updateAnimation(long begin, long duration, long progress, float curVal, float srcVal, float oldDestVal, float newDestVal);
}
