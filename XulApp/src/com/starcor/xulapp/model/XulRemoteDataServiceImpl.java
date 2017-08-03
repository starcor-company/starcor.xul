package com.starcor.xulapp.model;

import android.os.RemoteException;

import com.starcor.xul.XulDataNode;
import com.starcor.xulapp.utils.XulLog;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import static com.starcor.xulapp.model.XulDataService.*;

/**
 * Created by hy on 2016/11/15.
 */

public class XulRemoteDataServiceImpl extends IXulRemoteDataService.Stub {

	static WeakHashMap<IXulRemoteDataCallback, WeakReference<XulRemoteDataCallbackProxy>> callbackProxyCache = new WeakHashMap<>();
	XulDataService dataService = obtainDataService();

	private static XulRemoteDataCallbackProxy getCallbackProxy(IXulRemoteDataCallback callback) {
		WeakReference<XulRemoteDataCallbackProxy> callbackProxyReference = callbackProxyCache.get(callback);
		XulRemoteDataCallbackProxy callbackProxy = null;
		if (callbackProxyReference != null) {
			callbackProxy = callbackProxyReference.get();
		}
		if (callbackProxy == null) {
			callbackProxy = new XulRemoteDataCallbackProxy(callback);
			callbackProxyCache.put(callback, new WeakReference<XulRemoteDataCallbackProxy>(callbackProxy));
		}
		return callbackProxy;
	}

	@Override
	public IXulRemoteDataService makeClone() {
		return new XulRemoteDataServiceImpl();
	}

	@Override
	public void cancelClause() {
		dataService.cancelClause();
	}

	@Override
	public IXulRemoteDataOperation invokeRemoteService(XulClauseInfo clauseInfo, IXulRemoteDataCallback callback) throws RemoteException {
		XulRemoteDataCallbackProxy dataCallback = getCallbackProxy(callback);
		dataService.new Clause(clauseInfo);
		XulDataOperation xulDataOperation = dataService.execClause(dataService.getServiceContext(), clauseInfo, dataCallback);
		if (xulDataOperation == null) {
			return null;
		}
		return dataCallback.getDataOperation(xulDataOperation);
	}

	static class XulRemoteDataOperationImpl extends IXulRemoteDataOperation.Stub {
		XulDataOperation _dataOperation;

		public XulRemoteDataOperationImpl(XulDataOperation dataOperation) {
			this._dataOperation = dataOperation;
		}

		@Override
		public boolean exec(IXulRemoteDataCallback callback) {
			try {
				return _dataOperation.exec(getCallbackProxy(callback));
			} catch (XulDataException e) {
				e.printStackTrace();
			}
			return false;
		}

		@Override
		public boolean cancel() {
			return _dataOperation.cancel();
		}

		@Override
		public boolean pull(IXulRemoteDataCallback callback) {
			return ((XulPullDataCollection) _dataOperation).pull(getCallbackProxy(callback));
		}

		@Override
		public boolean reset() {
			return ((XulPullDataCollection) _dataOperation).reset();
		}

		@Override
		public boolean resetEx(int pageIdx) {
			return ((XulPullDataCollection) _dataOperation).reset(pageIdx);
		}

		@Override
		public int currentPage() {
			return ((XulPullDataCollection) _dataOperation).currentPage();
		}

		@Override
		public int pageSize() {
			return ((XulPullDataCollection) _dataOperation).pageSize();
		}

		@Override
		public boolean isFinished() {
			return ((XulPullDataCollection) _dataOperation).isFinished();
		}

		@Override
		public boolean isPullOperation() {
			return _dataOperation instanceof XulPullDataCollection;
		}

	}

	static class XulRemoteDataCallbackProxy extends XulDataCallback {
		private static final String TAG = XulRemoteDataCallbackProxy.class.getSimpleName();
		XulRemoteDataOperationImpl _remoteDataOperation;
		IXulRemoteDataCallback _callback;

		public XulRemoteDataCallbackProxy(IXulRemoteDataCallback callback) {
			_callback = callback;
		}

		@Override
		public void onResult(XulDataService.Clause clause, int code, XulDataNode data) {
			try {
				IXulRemoteDataCallback callback = _callback;
				callback.setErrorEx(clause.getError(), clause.getMessage());
				callback.onResult(getDataOperation(clause.dataOperation()), code, data);
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
		}

		@Override
		public void onError(XulDataService.Clause clause, int code) {
			try {
				IXulRemoteDataCallback callback = _callback;
				callback.setErrorEx(clause.getError(), clause.getMessage());
				callback.onError(getDataOperation(clause.dataOperation()), code);
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
		}

		public IXulRemoteDataOperation getDataOperation(XulDataOperation xulDataOperation) {
			if (_remoteDataOperation == null) {
				_remoteDataOperation = new XulRemoteDataOperationImpl(xulDataOperation);
			} else {
				_remoteDataOperation._dataOperation = xulDataOperation;
			}
			return _remoteDataOperation;
		}
	}

}
