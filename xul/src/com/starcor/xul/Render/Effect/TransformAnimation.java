package com.starcor.xul.Render.Effect;

import com.starcor.xul.Render.Drawer.IXulAnimation;
import com.starcor.xul.Render.Transform.ITransformer;
import com.starcor.xul.Render.XulViewRender;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by hy on 2015/5/29.
 */
public class TransformAnimation implements IXulAnimation {
	boolean _running = false;
	long _begin;
	long _duration;
	long _progress;
	ITransformer _aniTransformer;
	XulViewRender _render;

	public static abstract class TransformValues {
		protected float _srcVal;
		protected float _destVal;
		protected float _val;

		public abstract void storeSrc();

		public abstract void storeDest();

		public abstract void restoreValue();

		public boolean identicalValue() {
			return _srcVal == _destVal;
		}

		public boolean updateValue(float percent) {
			float delta = _destVal - _srcVal;
			_val = _srcVal;
			if (delta == 0) {
				return false;
			}
			_val += delta * percent;
			return true;
		}

		public boolean customTransform() {
			return false;
		}
	}

	private ArrayList<TransformValues> _values;

	public TransformAnimation(XulViewRender render) {
		this(render, null);
	}

	public TransformAnimation(XulViewRender render, ITransformer aniTransformer) {
		this._aniTransformer = aniTransformer;
		this._render = render;
	}

	public void restoreSrcValue() {
		updateValues(_begin);
		restoreValue();
	}

	public void setTransformer(ITransformer aniTransformer) {
		this._aniTransformer = aniTransformer;
	}

	public ITransformer getTransformer() {
		return _aniTransformer;
	}

	public void addTransformValues(TransformValues... values) {
		if (values == null || values.length <= 0) {
			return;
		}
		if (_values == null) {
			_values = new ArrayList<TransformValues>();
		}
		_values.addAll(Arrays.asList(values));
	}

	@Override
	public boolean updateAnimation(long timestamp) {
		if (!_running) {
			return false;
		}
		if (_values == null) {
			_render.onAnimationFinished(false);
			return false;
		}
		boolean ret = updateValues(timestamp);
		restoreValue();
		_render.markDirtyView();
		_running = !ret;
		if (!_running) {
			_render.onAnimationFinished(true);
		}
		return !ret;
	}

	public boolean updateValues(long timestamp) {
		long t = timestamp - _begin;
		if (t < 0) {
			t = 0;
		}
		if (t > _duration || _values == null || _values.isEmpty()) {
			t = _duration;
		}
		_progress = t;
		float percent;
		if (_aniTransformer != null) {
			percent = _aniTransformer.transform(t, _duration, 0, 0);
		} else {
			percent = (float) t / _duration;
		}
		boolean isEnd = percent >= 1.0f;

		if (_values != null) {
			boolean anyChanged = false;
			for (int i = 0, valuesSize = _values.size(); i < valuesSize; i++) {
				TransformValues value = _values.get(i);
				if (value.customTransform()) {
					anyChanged = value.updateValue(percent) || anyChanged;
				} else {
					value.updateValue(percent);
				}
			}
			isEnd = isEnd && !anyChanged;
		}
		return isEnd;
	}

	public boolean identicalValues() {
		if (_values == null) {
			return true;
		}
		for (int i = 0, valuesSize = _values.size(); i < valuesSize; i++) {
			TransformValues value = _values.get(i);
			if (!value.identicalValue()) {
				return false;
			}
		}
		return true;
	}

	public boolean isRunning() {
		return _running;
	}

	public void startAnimation(long duration, boolean rollBack) {
		if (!_running || !rollBack) {
			_running = true;
			setDuration(duration);
			storeSrc();
		} else {
			long beginDelta = (long) (duration * (1.0 - (double) _progress / _duration));
			if (beginDelta < duration) {
				setDuration(duration - beginDelta);
			} else {
				setDuration(1);
			}
			storeSrc();
		}
	}

	public void storeSrc() {
		if (_values == null) {
			return;
		}
		for (int i = 0, valuesSize = _values.size(); i < valuesSize; i++) {
			TransformValues value = _values.get(i);
			value.storeSrc();
		}
	}

	public void storeDest() {
		if (_values == null) {
			return;
		}
		for (int i = 0, valuesSize = _values.size(); i < valuesSize; i++) {
			TransformValues value = _values.get(i);
			value.storeDest();
		}
	}

	public void restoreValue() {
		if (_values == null) {
			return;
		}
		for (int i = 0, valuesSize = _values.size(); i < valuesSize; i++) {
			TransformValues value = _values.get(i);
			value.restoreValue();
		}
	}

	public void setDuration(long duration) {
		_duration = duration;
		_begin = _render.animationTimestamp();
	}

}
