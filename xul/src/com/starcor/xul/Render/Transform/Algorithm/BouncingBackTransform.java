package com.starcor.xul.Render.Transform.Algorithm;

/**
 * Created by hy on 2015/5/26.
 */
public class BouncingBackTransform extends BasicTransformAlgorithmImpl {
	@Override
	public String name() {
		return "bouncing-back";
	}

	@Override
	public float transform(float[] params, float time, float duration, float fromVal, float toVal) {
		float percent = time / duration;
		if (percent >= 1.0f) {
			return -1000;
		}
		double slope = params[0];
		double d2 = params[1];
		double strength = params[2];
		double x = Math.pow(percent, 2.0 / slope);
		double PI = Math.PI;

		double y = (Math.sin((x - 0.5) * PI) + 1) / 2;
		y += Math.sin(Math.pow(percent, 10 / d2) * PI) * strength * 0.5;
		return (float) y;
	}
}
