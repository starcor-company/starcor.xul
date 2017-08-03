package com.starcor.xul.Render.Transform.Algorithm;

/**
 * Created by hy on 2015/5/26.
 */
public interface ITransformAlgorithm {
	class UpdateResult {
		public long newBegin;
		public float newSrc;
		public float newDest;
	}
	String name();
	float transform(float[] params, float time, float duration, float fromVal, float toVal);
	UpdateResult update(float[] params, long begin, long duration, long progress, float curVal, float srcVal, float oldDestVal, float newDestVal);
}
