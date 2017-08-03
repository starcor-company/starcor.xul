package com.starcor.xul.Render.Transform.Algorithm;

/**
 * Created by hy on 2015/5/26.
 */
public class SinTransform extends BasicTransformAlgorithmImpl {
	@Override
	public String name() {
		return "sin";
	}

	@Override
	public float transform(float[] params, float time, float duration, float fromVal, float toVal) {
		float percent = time / duration;
		if (percent > 1.0f) {
			percent = 1.0f;
		}
		double slope = params[0];
		double x = Math.pow(percent, 2.0 / slope);
		double PI = Math.PI;

		double y = (Math.sin((x - 0.5) * PI) + 1) / 2;
		return (float) y;
	}
}
