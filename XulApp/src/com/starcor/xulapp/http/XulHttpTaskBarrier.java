package com.starcor.xulapp.http;

import com.starcor.xulapp.utils.XulLog;

import java.util.HashSet;

/**
 * Created by hy on 2015/11/11.
 */
public class XulHttpTaskBarrier {
	private static final String TAG = XulHttpTaskBarrier.class.getSimpleName();
	public static int COND_OK = -1;
	public static int COND_FAILED = -2;

	public interface BarrierHandler {
	}

	public static class BasicBarrierHandler implements BarrierHandler {
		XulHttpTaskBarrier _barrier;

		public void notifyResult(boolean success) {
			_barrier.notifyResult(this, success);
		}
	}

	private class BarrierResponseHandler implements XulHttpStack.XulHttpResponseHandler, BarrierHandler {
		XulHttpStack.XulHttpResponseHandler _handler;
		int _condition;

		public BarrierResponseHandler(int condition, XulHttpStack.XulHttpResponseHandler handler) {
			_condition = condition;
			_handler = handler;
		}

		@Override
		public int onResult(XulHttpStack.XulHttpTask task, XulHttpRequest request, XulHttpResponse response) {
			final int ret = _handler.onResult(task, request, response);
			if (_condition == COND_OK && response.code == 200) {
				notifyResult(this, true);
			} else if (_condition == COND_FAILED && response.code != 200) {
				notifyResult(this, true);
			} else if (_condition >= 0 && response.code == _condition) {
				notifyResult(this, true);
			} else {
				notifyResult(this, false);
			}
			return ret;
		}
	}

	private volatile int _errorCount = 0;

	private void notifyResult(BarrierHandler handler, boolean success) {
		synchronized (_barrierHandler) {
			_barrierHandler.remove(handler);
		}

		if (!success && _errorCount++ == 0 && _onError != null) {
			scheduleRunnable(_onError);
		}

		if (_barrierHandler.isEmpty()) {
			if (_errorCount == 0) {
				scheduleRunnable(_onOk);
			}
			scheduleRunnable(_onFinish);
		}
	}

	private void scheduleRunnable(Runnable runnable) {
		if (runnable == null) {
			return;
		}
		try {
			runnable.run();
		} catch (Exception e) {
			XulLog.e(TAG, e);
		}
	}

	private HashSet<BarrierHandler> _barrierHandler = new HashSet<BarrierHandler>();

	public XulHttpStack.XulHttpResponseHandler wrap(int condition, XulHttpStack.XulHttpResponseHandler handler) {
		final BarrierResponseHandler newHandler = new BarrierResponseHandler(condition, handler);
		synchronized (_barrierHandler) {
			_barrierHandler.add(newHandler);
		}
		return newHandler;
	}

	public XulHttpStack.XulHttpResponseHandler wrap(XulHttpStack.XulHttpResponseHandler handler) {
		return wrap(COND_OK, handler);
	}

	public BasicBarrierHandler wrap(BasicBarrierHandler handler) {
		handler._barrier = this;
		synchronized (_barrierHandler) {
			_barrierHandler.add(handler);
		}
		return handler;
	}

	Runnable _onFinish;
	Runnable _onError;
	Runnable _onOk;

	public void onFinish(Runnable runnable) {
		_onFinish = runnable;
	}

	public void onError(Runnable runnable) {
		_onError = runnable;
	}

	public void onOk(Runnable runnable) {
		_onOk = runnable;
	}

	public void resetErrorCountter() {
		_errorCount = 0;
	}
}
