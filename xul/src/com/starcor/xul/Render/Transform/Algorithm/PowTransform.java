package com.starcor.xul.Render.Transform.Algorithm;

/**
 * Created by hy on 2015/5/26.
 */
public class PowTransform extends BasicTransformAlgorithmImpl {
	@Override
	public String name() {
		return "pow";
	}

	@Override
	public float transform(float[] params, float time, float duration, float fromVal, float toVal) {
		float percent = time / duration;
		if (percent > 1.0f) {
			percent = 1.0f;
		}
		double factor = params[0] * 2.0;
		return (float) Math.pow(percent, factor);
	}
}
