package com.starcor.xulapp.utils;

/**
 * Created by hy on 2016/1/26.
 */
public abstract class CancellableRunnable implements Runnable, XulCancelable {
	private volatile boolean _isCancelled = false;

	@Override
	public void cancel() {
		_isCancelled = true;
	}

	@Override
	public void run() {
		if (_isCancelled) {
			return;
		}
		doRun();
	}

	protected abstract void doRun();
}
