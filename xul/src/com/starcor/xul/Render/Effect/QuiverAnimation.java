package com.starcor.xul.Render.Effect;

import android.util.Log;
import com.starcor.xul.Render.Drawer.IXulAnimation;
import com.starcor.xul.Render.Transform.ITransformer;
import com.starcor.xul.Render.Transform.TransformerFactory;
import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.XulManager;

/**
 * Created by hy on 2015/6/2.
 */
public abstract class QuiverAnimation implements IXulAnimation {
	public final static String DEFAULT_MODE = "pow";
	public final static float[] DEFAULT_PARAMS = new float[]{0.15f, 0f, 0f, 0f, 0f, 0f};
	private final XulViewRender _render;
	ITransformer _transformer;
	long _duration = 120;
	long _begin;
	float _xStrength;
	float _yStrength;
	int _repeat = 1;
	float _repeatStrength = 1;

	public QuiverAnimation(XulViewRender render, float xStrength, float yStrength, int repeat, float repeatStrength) {
		this._render = render;
		this._xStrength = xStrength;
		this._yStrength = yStrength;
		this._repeat = Math.max(repeat, 1);
		this._repeatStrength = Math.max(repeatStrength, 0.01f);
		_begin = _render.animationTimestamp();
		_transformer = TransformerFactory.createTransformer(DEFAULT_MODE, DEFAULT_PARAMS);
	}

	public void switchMode(String mode, float[] params) {
		_transformer.switchAlgorithm(mode);
		_transformer.switchParams(params);
	}

	public void updateDuration(int duration) {
		_duration = duration;
	}

	@Override
	public boolean updateAnimation(long timestamp) {
		long l = timestamp - _begin;
		long duration = _duration / _repeat;
		float percent = (float) (l) / duration;
		float pval;
		float roundScalar = percent / _repeat;
		percent = (float) (4.0f * (percent - Math.floor(percent)));
		if (percent <= 1.0f) {
			// phase 1
			pval = _transformer.transform(percent * duration, duration, 0, 0);
		} else if (percent <= 2.0f) {
			pval = _transformer.transform((2.0f - percent) * duration, duration, 0, 0);
		} else if (percent <= 3.0f) {
			pval = -_transformer.transform((percent - 2.0f) * duration, duration, 0, 0);
		} else {
			pval = -_transformer.transform((4.0f - percent) * duration, duration, 0, 0);
		}
		pval = pval * (1.0f + roundScalar * (_repeatStrength - 1.0f));
		if (XulManager.DEBUG) {
			Log.d("quiver", String.format("percent:%.3f, pval:%.3f, roundScalar:%.3f, repeatStrength:%.3f\n", percent, pval, roundScalar, _repeatStrength));
		}

		boolean ret = doQuiver(timestamp - _begin, _xStrength * pval, _yStrength * pval);
		if (!ret) {
			_render.onAnimationFinished(true);
		}
		return ret;
	}

	public abstract boolean doQuiver(long duration, float xDelta, float yDelta);
}
