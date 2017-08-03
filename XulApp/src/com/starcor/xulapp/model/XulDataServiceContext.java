package com.starcor.xulapp.model;

import com.starcor.xul.XulDataNode;
import com.starcor.xulapp.XulApplication;
import com.starcor.xulapp.utils.XulLog;
import com.starcor.xulapp.utils.XulSystemUtil;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by hy on 2015/9/25.
 */
public class XulDataServiceContext {
	private static final String TAG = XulDataServiceContext.class.getSimpleName();
	private volatile boolean _isDestroyed = false;
	private WeakHashMap<XulDataOperation, Object> _pendingOps;
	private XulDataService _dataService;

	public XulDataServiceContext(XulDataService dataService) {
		_dataService = dataService;
	}

	public synchronized void attach(XulDataOperation op) {
		if (_pendingOps == null || _isDestroyed) {
			_pendingOps = new WeakHashMap<XulDataOperation, Object>();
		}
		_pendingOps.put(op, null);
	}

	public synchronized void detach(XulDataOperation op) {
		if (_pendingOps == null || _isDestroyed) {
			return;
		}
		_pendingOps.remove(op);
	}

	synchronized void destroy() {
		_isDestroyed = true;
		if (_pendingOps != null) {
			for (Map.Entry<XulDataOperation, Object> entry : _pendingOps.entrySet()) {
				XulDataOperation pendingOp = entry.getKey();
				if (pendingOp != null) {
					pendingOp.cancel();
				}
			}
			_pendingOps.clear();
		}
	}

	public boolean isDestroyed() {
		return _isDestroyed;
	}

	public synchronized void deliverResult(final XulDataCallback callback, final XulDataService.Clause clause, final XulDataNode root) {
		if (_isDestroyed || callback == null) {
			printDropLog("result", _isDestroyed);
			return;
		}
		detach(clause.dataOperation());
		if (XulSystemUtil.isMainThread()) {
			callback.onResult(clause, clause.getError(), root);
		} else {
			XulApplication.getAppInstance().postToMainLooper(new Runnable() {
				@Override
				public void run() {
					if (_isDestroyed) {
						printDropLog("result", _isDestroyed);
						return;
					}
					callback.onResult(clause, clause.getError(), root);
				}
			});
		}
	}

	public synchronized void deliverError(final XulDataCallback callback, final XulDataService.Clause clause) {
		if (_isDestroyed || callback == null) {
			printDropLog("error", _isDestroyed);
			return;
		}
		detach(clause.dataOperation());
		if (XulSystemUtil.isMainThread()) {
			callback.onError(clause, clause.getError());
		} else {
			XulApplication.getAppInstance().postToMainLooper(new Runnable() {
				@Override
				public void run() {
					if (_isDestroyed) {
						printDropLog("error", _isDestroyed);
						return;
					}
					callback.onError(clause, clause.getError());
				}
			});
		}
	}

	private static void printDropLog(String infoType, boolean isDestroyed) {
		XulLog.d(TAG, "drop %s(%s)", infoType, isDestroyed ? "context destroyed" : "NULL callback");
	}

	public Object getUserData() {
		return _dataService.getUserData();
	}
}
