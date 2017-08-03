package com.starcor.xul.Render.Transform.Algorithm;

/**
 * Created by hy on 2015/5/26.
 */
public class LinearTransform extends BasicTransformAlgorithmImpl {
	@Override
	public String name() {
		return "linear";
	}

	@Override
	public float transform(float[] params, float time, float duration, float fromVal, float toVal) {
		float percent = time / duration;
		if (percent > 1.0f) {
			percent = 1.0f;
		}
		return percent;
	}
}
