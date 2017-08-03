package com.starcor.xul.Render.Effect;

import com.starcor.xul.Render.Drawer.IXulAnimation;
import com.starcor.xul.Render.Transform.Algorithm.BasicTransformAlgorithmImpl;
import com.starcor.xul.Render.Transform.Algorithm.ITransformAlgorithm;
import com.starcor.xul.Render.Transform.ITransformer;
import com.starcor.xul.Render.XulViewRender;

/**
 * Created by hy on 2015/5/29.
 */
public abstract class SimpleTransformAnimation implements IXulAnimation {
	boolean _running = false;
	long _begin;
	protected long _duration;
	long _progress;
	ITransformer _aniTransformer;
	XulViewRender _render;

	protected float _srcVal;
	protected float _destVal;
	protected float _val;

	public SimpleTransformAnimation(XulViewRender render) {
		this(render, null);
	}

	public SimpleTransformAnimation(XulViewRender render, ITransformer aniTransformer) {
		this._aniTransformer = aniTransformer;
		this._render = render;
	}

	public void restoreSrcValue() {
		updateValue(_begin);
		restoreValue();
	}

	public void setTransformer(ITransformer aniTransformer) {
		this._aniTransformer = aniTransformer;
	}

	public ITransformer getTransformer() {
		return _aniTransformer;
	}

	@Override
	public boolean updateAnimation(long timestamp) {
		if (!_running) {
			return false;
		}
		boolean ret = updateValue(timestamp);
		restoreValue();
		_render.markDirtyView();
		_running = !ret;
		if (!_running) {
			onAnimationStop();
		}
		return _running;
	}

	public void onAnimationStop() {
		_render.onAnimationFinished(true);
	}

	/**
	 * update animation information
	 * @param timestamp
	 * @return true indicates end of animation
	 */
	public boolean updateValue(long timestamp) {
		long t = timestamp - _begin;
		if (t < 0) {
			t = 0;
		}
		_progress = t;
		float percent;
		if (_aniTransformer != null) {
			final float scalar = getScalar();
			percent = _aniTransformer.transform(t, _duration, _srcVal / scalar, _destVal / scalar);
		} else {
			percent = (float) t / _duration;
		}
		if (percent >= 1.0f) {
			percent = 1.0f;
		}
		boolean isEnd = percent >= 1.0f;

		float delta = _destVal - _srcVal;
		_val = _srcVal;
		_val += delta * percent;
		return isEnd;
	}

	public boolean isRunning() {
		return _running;
	}

	public void prepareAnimation(int duration) {
		if (!_running) {
			_duration = duration;
			storeSrc();
		}
	}

	public void startAnimation() {
		if (!_running) {
			storeDest();
			_begin = _render.animationTimestamp();
			_running = true;
		} else {
			float curVal = _val;
			float oldDest = _destVal;
			storeDest();
			float newDest = _destVal;
			if (Math.abs(newDest - oldDest) < 0.01f) {
				return;
			}
			final float scalar = getScalar();
			ITransformer transformer = _aniTransformer;
			ITransformAlgorithm.UpdateResult updateResult;
			if (transformer != null) {
				updateResult = transformer.updateAnimation(_begin, _duration, _progress, curVal / scalar, _srcVal / scalar, oldDest / scalar, newDest / scalar);
			} else {
				updateResult = BasicTransformAlgorithmImpl.commonUpdateAnimation(_begin, _duration, _progress, curVal / scalar, _srcVal / scalar, oldDest / scalar, newDest / scalar);
			}
			_begin = updateResult.newBegin;
			_srcVal = updateResult.newSrc * scalar;
			_destVal = updateResult.newDest * scalar;
		}
	}

	public abstract void storeSrc();

	public abstract void storeDest();

	public abstract void restoreValue();

	public float getScalar() {
		return 1.0f;
	}

	public void stopAnimation() {
		if (_running) {
			_running = false;
			onAnimationStop();
		}
	}
}
