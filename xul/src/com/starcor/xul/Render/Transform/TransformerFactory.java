package com.starcor.xul.Render.Transform;

import com.starcor.xul.Render.Transform.Algorithm.*;
import com.starcor.xul.Utils.XulCachedHashMap;

/**
 * Created by hy on 2015/5/26.
 */
public class TransformerFactory {
	public static final String ALGORITHM_LINEAR = "linear";
	public static final String ALGORITHM_SIN = "sin";
	public static final String ALGORITHM_POW = "pow";
	public static final String ALGORITHM_BOUNCING = "bouncing";
	public static final String ALGORITHM_SHAKING = "shaking";
	public static final String ALGORITHM_CONSTANT = "constant";

	static Class<? extends ITransformAlgorithm>[] _algorithmMap = new Class[]{
		BouncingTransform.class,
		LinearTransform.class,
		PowTransform.class,
		ShakingTransform.class,
		SinTransform.class,
		ConstantTransform.class,
		BouncingBackTransform.class,
	};

	static XulCachedHashMap<String, ITransformAlgorithm> _algorithms = new XulCachedHashMap<String, ITransformAlgorithm>();

	static {
		for (Class<? extends ITransformAlgorithm> algorithmClass : _algorithmMap) {
			try {
				final ITransformAlgorithm algorithm = algorithmClass.newInstance();
				_algorithms.put(algorithm.name(), algorithm);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	private static class TransformerImpl implements ITransformer {
		@Override
		public boolean switchAlgorithm(String algorithm) {
			final ITransformAlgorithm newAlgorithm = _algorithms.get(algorithm);
			if (newAlgorithm == null) {
				return false;
			}
			_algo = newAlgorithm;
			_isEnd = false;
			return true;
		}

		@Override
		public boolean switchParams(float[] params) {
			_params = params;
			_isEnd = false;
			return true;
		}

		@Override
		public float transform(float time, float duration, float fromVal, float toVal) {
			float outValue = _algo.transform(_params, time, duration, fromVal, toVal);
			if (outValue <= -1000) {
				_isEnd = true;
				outValue = 1;
			}
			// Log.d("Transformer", String.format("%.3f => %.3f:end(%s):%s", time / duration, outValue, _isEnd, this));
			return outValue;
		}

		@Override
		public ITransformAlgorithm.UpdateResult updateAnimation(long begin, long duration, long progress, float curVal, float srcVal, float oldDestVal, float newDestVal) {
			return _algo.update(_params, begin, duration, progress, curVal, srcVal, oldDestVal, newDestVal);
		}

		@Override
		public boolean isEnd() {
			if (_isEnd) {
				_isEnd = false;
				return true;
			}
			return false;
		}

		public TransformerImpl(ITransformAlgorithm algorithm, float[] params) {
			this._algo = algorithm;
			this._params = params;
			_isEnd = false;
		}

		ITransformAlgorithm _algo;
		boolean _isEnd;
		float[] _params;
	}

	public static ITransformer createTransformer(String algorithm, float[] params) {
		final ITransformAlgorithm transformAlgorithm = _algorithms.get(algorithm);
		if (transformAlgorithm == null) {
			return null;
		}
		return new TransformerImpl(transformAlgorithm, params);
	}

	public static boolean addUserTransformer(ITransformAlgorithm algorithm) {
		_algorithms.put(algorithm.name(), algorithm);
		return true;
	}
}
