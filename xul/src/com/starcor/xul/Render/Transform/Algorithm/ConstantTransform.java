package com.starcor.xul.Render.Transform.Algorithm;

/**
 * Created by hy on 2015/5/26.
 */
public class ConstantTransform extends BasicTransformAlgorithmImpl {
	@Override
	public String name() {
		return "constant";
	}

	@Override
	public float transform(float[] params, float time, float duration, float fromVal, float toVal) {
		float speed = params[0];
		float startDuration = params[1];
		float endDuration = params[2];
		if (speed <= 0) {
			return 1.0f;
		}

		float v0 = speed / duration;    // px/ms

		float totalRange = Math.abs(toVal - fromVal);

		if (totalRange <= 0.01f) {
			return 1.0f;
		}

		float startRange = startDuration * v0 / 2;
		float endRange = endDuration * v0 / 2;
		float cRange = totalRange - startRange - endRange;
		if (cRange <= 0) {
			cRange = 0;
			startRange = totalRange * startRange / (startDuration + endDuration);
			endRange = totalRange - startRange;
		}

		float t = cRange / v0;

		float pos;
		if (time <= startDuration) {
			if (startDuration > 0) {
				pos = (float) (Math.pow(time / startDuration, 2) * startRange);
			} else {
				pos = 0;
			}
		} else if (time > startDuration + t) {
			if (endDuration > 0) {
				float v = (time - startDuration - t) / endDuration;
				if (v > 1.0f) {
					v = 0;
				} else {
					v = 1.0f - v;
				}
				pos = totalRange - v * v * endRange;
			} else {
				pos = totalRange;
			}
		} else if (t > 0) {
			pos = startRange + (time - startDuration) / t * cRange;
		} else {
			pos = startRange + cRange;
		}

		if (pos >= totalRange) {
			pos = totalRange;
		}

		return pos / totalRange;
	}

	@Override
	public UpdateResult update(float[] params, long begin, long duration, long progress, float curVal, float srcVal, float oldDestVal, float newDestVal) {
		boolean isSameDirection = (oldDestVal - curVal) * (newDestVal - curVal) >= 0;
		float startDuration = params[1];
		updateResult.newDest = newDestVal;
		updateResult.newSrc = curVal;
		if (isSameDirection) {
			if (startDuration > 0) {
				if (progress >= startDuration) {
					updateResult.newBegin = (long) (begin + progress - startDuration);
					float delta = transform(params, startDuration, duration, srcVal, newDestVal) * (newDestVal - srcVal);
					updateResult.newSrc = curVal - delta;
				} else {
					updateResult.newBegin = begin;
					updateResult.newSrc = srcVal;
				}
			} else {
				updateResult.newBegin = begin + progress;
			}
		} else {
			updateResult.newBegin = begin + 2 * progress - duration;
		}
		return updateResult;
	}
}
