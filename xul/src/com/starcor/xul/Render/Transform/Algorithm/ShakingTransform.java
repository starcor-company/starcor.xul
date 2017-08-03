package com.starcor.xul.Render.Transform.Algorithm;

/**
 * Created by hy on 2015/5/26.
 */
public class ShakingTransform extends BasicTransformAlgorithmImpl {
	@Override
	public String name() {
		return "shaking";
	}

	@Override
	public float transform(float[] params, float time, float duration, float fromVal, float toVal) {
		float percent = time / duration;
		if (percent > 1.0f) {
			percent = 1.0f;
		}
		double frequency = params[0];
		double offset = params[1];
		double delta = params[2];
		double strength = params[3];
		double grow = params[4];

		double PI = Math.PI;
		double x = percent;
		double g = 1 * grow + (1 - grow) * (Math.cos(x * 2 * PI + PI) + 1) / 2;
		double y = (Math.cos(x * frequency * 4 * PI + offset * PI) + delta) / 2 * strength * g;
		return (float) y;
	}
}
