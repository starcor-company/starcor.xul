package com.starcor.xul.Render.Transform.Algorithm;

/**
 * Created by hy on 2015/5/26.
 */
public class BouncingTransform extends BasicTransformAlgorithmImpl {
	@Override
	public String name() {
		return "bouncing";
	}

	@Override
	public float transform(float[] params, float time, float duration, float fromVal, float toVal) {
		float percent = time / duration;
		if (percent > 1.0f) {
			percent = 1.0f;
		}
		double growSpeed = params[0];
		double frequency = params[1];
		double slope = params[2];
		double bounceStrength = params[3];

		double x = Math.pow(percent, 2.5 / slope);
		double PI = Math.PI;
		double y = Math.pow(x, 0.15 + 0.1 * growSpeed) + 0.3 * bounceStrength * Math.pow(x, 0.4) * Math.sin(8 * PI * Math.pow(x * frequency, 1.6)) / Math.tan(x * PI / 2);
		return (float) y;
	}
}
