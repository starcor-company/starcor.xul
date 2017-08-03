package com.starcor.xulapp.model;

import com.starcor.xul.XulDataNode;

import java.util.ArrayList;

/**
 * Created by hy on 2015/10/8.
 */
public class XulStickyDataCallback extends XulDataCallback {
	static class TargetCallback {
		XulDataServiceContext ctx;
		XulDataService.Clause clause;
		XulDataCallback callback;

		public TargetCallback(XulDataServiceContext ctx, XulDataService.Clause clause, XulDataCallback callback) {
			this.ctx = ctx;
			this.clause = clause;
			this.callback = callback;
		}
	}

	ArrayList<TargetCallback> _cbs;
	int _state = 0; // 0 for uninitialized, 1 for success, others for failed
	int _code;
	String _msg;
	XulDataNode _data;

	@Override
	public void onResult(XulDataService.Clause clause, int code, XulDataNode data) {
		handlePendingCallbacks(1, clause, data);
	}

	@Override
	public void onError(XulDataService.Clause clause, int code) {
		handlePendingCallbacks(2, clause, null);
	}

	private void handlePendingCallbacks(int state, XulDataService.Clause clause, XulDataNode data) {
		if (_state != 0) {
			return;
		}
		_state = state;
		_code = clause.getError();
		_msg = clause.getMessage();
		_data = data;
		if (_cbs == null) {
			return;
		}
		for (TargetCallback cb : _cbs) {
			addCallback(cb.ctx, cb.clause, cb.callback);
		}
		_cbs.clear();
	}

	/**
	 * add pending result callback
	 * if the sticky callback has been already triggered, the newly added callback will be invoked synchronously
	 * @param ctx
	 * @param clause
	 * @param callback
	 * @return the number of pending callbacks
	 */
	public int addCallback(XulDataServiceContext ctx, XulDataService.Clause clause, XulDataCallback callback) {
		if (_state == 0) {
			if (_cbs == null) {
				_cbs = new ArrayList<TargetCallback>();
			}
			_cbs.add(new TargetCallback(ctx, clause, callback));
			return _cbs.size();
		}
		clause.setError(_code, _msg);
		if (_state == 1) {
			ctx.deliverResult(callback, clause, _data);
		} else {
			ctx.deliverError(callback, clause);
		}
		return 0;
	}
}
