package com.starcor.xulapp.model.utils;

import com.starcor.xul.XulDataNode;
import com.starcor.xulapp.http.XulHttpRequest;
import com.starcor.xulapp.http.XulHttpResponse;
import com.starcor.xulapp.http.XulHttpStack;
import com.starcor.xulapp.model.XulClauseInfo;
import com.starcor.xulapp.model.XulDataCallback;
import com.starcor.xulapp.model.XulDataException;
import com.starcor.xulapp.model.XulDataOperation;
import com.starcor.xulapp.model.XulDataService;
import com.starcor.xulapp.model.XulDataServiceContext;
import com.starcor.xulapp.utils.XulLog;

import java.io.InputStream;

/**
 * Created by hy on 2016/1/27.
 */
public abstract class XulHttpDataOperation extends XulDataOperation {
	private static final String TAG = XulHttpPullDataCollection.class.getSimpleName();
	XulDataServiceContext _ctx;
	XulClauseInfo _clauseInfo;
	volatile XulHttpStack.XulHttpTask _apiTask;
	XulHttpStack.XulHttpResponseHandler _apiHandler = new XulHttpStack.XulHttpResponseHandler() {
		@Override
		public int onResult(XulHttpStack.XulHttpTask task, XulHttpRequest request, XulHttpResponse response) {
			if (_apiTask != task) {
				return 0;
			}
			_apiTask = null;
			onApiResult(_ctx, _clauseInfo, request, response);
			return 0;
		}
	};

	protected volatile XulDataCallback _callback;

	public XulHttpDataOperation(XulDataServiceContext ctx, XulClauseInfo clauseInfo) {
		_ctx = ctx;
		_clauseInfo = clauseInfo;
	}

	protected void onApiResult(XulDataServiceContext ctx, XulClauseInfo clauseInfo, XulHttpRequest request, XulHttpResponse response) {
		XulDataService.Clause clause = this._clauseInfo.getClause();
		if (response.code == 200 && response.data != null) {
			try {
				XulDataNode result = buildResult(response.data);
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
		ctx.deliverError(_callback, clause);
	}

	protected void deliverResult(XulDataServiceContext ctx, XulDataService.Clause clause, XulDataNode result) {ctx.deliverResult(_callback, clause, result);}

	protected abstract XulDataNode buildResult(InputStream data) throws XulPullCollectionException;

	protected abstract XulHttpStack.XulHttpTask createHttpTask();


	@Override
	public boolean exec(XulDataCallback callback) throws XulDataException {
		if (_apiTask != null) {
			return false;
		}
		_ctx.attach(this);
		_callback = callback;
		_apiTask = createHttpTask();
		_apiTask.get(_apiHandler);
		return true;
	}

	@Override
	public boolean cancel() {
		XulHttpStack.XulHttpTask apiTask = _apiTask;
		_ctx.attach(this);
		_apiTask = null;
		if (apiTask != null) {
			apiTask.cancel();
			return true;
		}
		return false;
	}
}
