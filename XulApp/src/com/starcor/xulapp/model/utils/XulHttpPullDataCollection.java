package com.starcor.xulapp.model.utils;


import com.starcor.xul.XulDataNode;
import com.starcor.xulapp.http.XulHttpRequest;
import com.starcor.xulapp.http.XulHttpResponse;
import com.starcor.xulapp.http.XulHttpStack;
import com.starcor.xulapp.model.XulClauseInfo;
import com.starcor.xulapp.model.XulDataCallback;
import com.starcor.xulapp.model.XulDataException;
import com.starcor.xulapp.model.XulDataService;
import com.starcor.xulapp.model.XulDataServiceContext;
import com.starcor.xulapp.model.XulPullDataCollection;
import com.starcor.xulapp.utils.XulLog;

import java.io.InputStream;

/**
 * Created by hy on 2015/11/17.
 */
public abstract class XulHttpPullDataCollection extends XulPullDataCollection {
	private static final String TAG = XulHttpPullDataCollection.class.getSimpleName();
	protected volatile int _curPageIdx = 0;
	protected volatile int _curPageSize = 16;
	volatile boolean _finished = false;
	XulDataServiceContext _ctx;
	XulClauseInfo _clauseInfo;
	volatile XulHttpStack.XulHttpTask _httpTask;
	XulHttpStack.XulHttpResponseHandler _httpHandler = new XulHttpStack.XulHttpResponseHandler() {
		@Override
		public int onResult(XulHttpStack.XulHttpTask task, XulHttpRequest request, XulHttpResponse response) {
			if (_httpTask != task) {
				return 0;
			}
			onHttpResult(_ctx, _clauseInfo, request, response);
			return 0;
		}
	};

	protected volatile XulDataCallback _callback;

	public XulHttpPullDataCollection(XulDataServiceContext ctx, XulClauseInfo clauseInfo, int curPageIdx, int curPageSize) {
		_curPageIdx = curPageIdx;
		_curPageSize = curPageSize;
		_ctx = ctx;
		_clauseInfo = clauseInfo;
	}

	public XulHttpPullDataCollection(XulDataServiceContext ctx, XulClauseInfo clauseInfo) {
		this(ctx, clauseInfo, 0, 16);
	}

	protected void onHttpResult(XulDataServiceContext ctx, XulClauseInfo clauseInfo, XulHttpRequest request, XulHttpResponse response) {
		XulDataService.Clause clause = this._clauseInfo.getClause();
		if (response.code == 200 && response.data != null) {
			try {
				XulDataNode result = buildResult(response.data);
				synchronized (this) {
					++_curPageIdx;
					_httpTask = null;
				}
				deliverResult(ctx, clause, result);
				return;
			} catch (XulPullCollectionException e) {
				clause.setError(e.getCode(), e.getMessage());
				XulLog.e(TAG, e);
			} catch (Exception e) {
				clause.setError(-1, e.getMessage());
				XulLog.e(TAG, e);
			}
		} else {
			clause.setError(-1, String.format("HTTP Error(%d):%s", response.code, response.message));
		}
		synchronized (this) {
			_httpTask = null;
		}
		ctx.deliverError(_callback, clause);
	}

	protected void deliverResult(XulDataServiceContext ctx, XulDataService.Clause clause, XulDataNode result) {ctx.deliverResult(_callback, clause, result);}

	protected abstract XulDataNode buildResult(InputStream data) throws XulPullCollectionException;

	protected abstract XulHttpStack.XulHttpTask createHttpTask(int curPageIdx, int curPageSize);

	public synchronized void pullData(final XulDataCallback callback) {
		if (_httpTask != null) {
			return;
		}
		_ctx.attach(this);
		_callback = callback;
		_httpTask = createHttpTask(_curPageIdx, _curPageSize);
		_httpTask.get(_httpHandler);
	}

	public synchronized void finishPullData() {
		_finished = true;
	}

	@Override
	public boolean exec(XulDataCallback callback) throws XulDataException {
		pullData(callback);
		return true;
	}

	@Override
	public boolean pull(XulDataCallback dataCallback) {
		synchronized (this) {
			if (_finished) {
				return false;
			}
		}
		pullData(dataCallback);
		return true;
	}

	@Override
	public boolean cancel() {
		synchronized (this) {
			XulHttpStack.XulHttpTask apiTask = _httpTask;
			_httpTask = null;
			_ctx.detach(this);
			if (apiTask != null) {
				apiTask.cancel();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean reset() {
		return reset(0);
	}

	@Override
	public boolean reset(int pageIdx) {
		cancel();
		synchronized (this) {
			_curPageIdx = pageIdx;
			_finished = false;
		}
		return true;
	}

	@Override
	public int currentPage() {
		return _curPageIdx;
	}

	@Override
	public int pageSize() {
		return _curPageSize;
	}

	@Override
	public boolean isFinished() {
		return _finished;
	}
}
