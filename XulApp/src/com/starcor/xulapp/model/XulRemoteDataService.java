package com.starcor.xulapp.model;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.starcor.xul.XulDataNode;
import com.starcor.xulapp.XulApplication;
import com.starcor.xulapp.utils.XulLog;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.WeakHashMap;

/**
 * Created by hy on 2016/11/15.
 */
public class XulRemoteDataService extends XulDataService {
	static LinkedList<Runnable> _pendingServiceCall;

	private static final String TAG = XulRemoteDataService.class.getSimpleName();

	static volatile IXulRemoteDataService _globalRemoteDataService;
	static ServiceConnection _conn = new ServiceConnection() {
		int _failedCount = 0;

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			XulLog.d(TAG, "Remote data service connected! " + _failedCount);
			_globalRemoteDataService = IXulRemoteDataService.Stub.asInterface(service);
			LinkedList<Runnable> pendingServiceCall = _pendingServiceCall;
			if (pendingServiceCall == null) {
				XulLog.e(TAG, "Pending Service Calls queue is null!");
				return;
			}
			synchronized (pendingServiceCall) {
				while (!pendingServiceCall.isEmpty()) {
					Runnable pendingCall = pendingServiceCall.pop();
					try {
						XulApplication.getAppInstance().postToMainLooper(pendingCall);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			XulLog.d(TAG, "Remote data service connection lost! " + _failedCount);
			++_failedCount;
			_globalRemoteDataService = null;
			if (_failedCount > 20) {
				XulLog.e(TAG, "Failed to connect remote data service!!!");

				_cleanUpPendingRequests();
				return;
			}
			XulApplication.getAppInstance().postDelayToMainLooper(new Runnable() {
				@Override
				public void run() {
					XulLog.d(TAG, "Reconnecting remote data service..." + _failedCount);
					initRemoteDataService(_serviceAction, _servicePackage);
				}
			}, 50);
		}
	};

	private static void _cleanUpPendingRequests() {
		LinkedList<Runnable> pendingServiceCall = _pendingServiceCall;
		_pendingServiceCall = null;
		if (pendingServiceCall == null) {
			XulLog.e(TAG, "Pending Service Calls queue is null!");
			return;
		}
		synchronized (pendingServiceCall) {
			while (!pendingServiceCall.isEmpty()) {
				Runnable pendingCall = pendingServiceCall.pop();
				try {
					XulApplication.getAppInstance().postToMainLooper(pendingCall);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	static String _serviceAction;
	static String _servicePackage;
	static WeakHashMap<XulDataCallback, WeakReference<XulRemoteDataCallbackProxy>> _callbackProxyCache = new WeakHashMap<>();
	IXulRemoteDataService _remoteDataService;
	private static XulDataServiceFactory _dataServiceFactory;

	private static XulDataServiceFactory getDataServiceFactory() {
		if (_dataServiceFactory != null) {
			return _dataServiceFactory;
		}
		_dataServiceFactory = new XulDataServiceFactory() {
			@Override
			XulDataService createXulDataService() {
				XulLog.d(TAG, "Create remote data service... GLOBAL RDS:" + _globalRemoteDataService);
				return new XulRemoteDataService();
			}
		};
		return _dataServiceFactory;
	}

	@Deprecated
	public static void initRemoteDataService(String serviceAction) {
		initRemoteDataService(serviceAction, null);
	}

	public static boolean initRemoteDataService(String serviceAction, String servicePackage) {
		_serviceAction = serviceAction;
		_servicePackage = servicePackage;
		Intent serviceIntent = new Intent(serviceAction);
		if (!TextUtils.isEmpty(servicePackage)) {
			serviceIntent.setPackage(servicePackage);
		}
		XulDataService.setDataServiceFactory(getDataServiceFactory());
		boolean success = XulApplication.getAppContext().bindService(serviceIntent, _conn, Service.BIND_AUTO_CREATE);
		if (success) {
			_pendingServiceCall = new LinkedList<Runnable>();
		}
		return success;
	}

	public XulRemoteDataService() {
		if (_globalRemoteDataService == null) {
			return;
		}
		try {
			_remoteDataService = _globalRemoteDataService.makeClone();
		} catch (Exception e) {
			XulLog.e(TAG, e);
		}
	}

	private synchronized static XulRemoteDataCallbackProxy getRemoteCallbackProxy(XulDataServiceContext ctx, XulClauseInfo clauseInfo, XulDataCallback dataCallback) {
		WeakReference<XulRemoteDataCallbackProxy> remoteDataCallbackProxyRef = _callbackProxyCache.get(dataCallback);
		XulRemoteDataCallbackProxy remoteDataCallbackProxy = null;
		if (remoteDataCallbackProxyRef != null) {
			remoteDataCallbackProxy = remoteDataCallbackProxyRef.get();
		}
		if (remoteDataCallbackProxy == null) {
			remoteDataCallbackProxy = new XulRemoteDataCallbackProxy(ctx, clauseInfo, dataCallback);
			_callbackProxyCache.put(dataCallback, new WeakReference<XulRemoteDataCallbackProxy>(remoteDataCallbackProxy));
		} else {
			remoteDataCallbackProxy._ctx = ctx;
			remoteDataCallbackProxy._clauseInfo = clauseInfo;
			remoteDataCallbackProxy._dataOperation = null;
			remoteDataCallbackProxy._dataCallback = dataCallback;
		}
		return remoteDataCallbackProxy;
	}

	private IXulRemoteDataService getRemoteDataService() {
		if (checkRemoteService()) {
			return _remoteDataService;
		}
		if (_globalRemoteDataService != null) {
			try {
				_remoteDataService = _globalRemoteDataService.makeClone();
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
		}
		if (checkRemoteService()) {
			return _remoteDataService;
		}
		return null;
	}

	private boolean checkRemoteService() {
		if (_remoteDataService == null) {
			return false;
		}
		IBinder iBinder = _remoteDataService.asBinder();
		if (iBinder == null) {
			return false;
		}
		if (!iBinder.isBinderAlive()) {
			return false;
		}
		return true;
	}

	@Override
	protected XulDataOperation execClause(final XulDataServiceContext ctx, final XulClauseInfo clauseInfo, final XulDataCallback dataCallback) {
		IXulRemoteDataService remoteDataService = getRemoteDataService();
		if (remoteDataService == null) {
			LinkedList<Runnable> pendingServiceCall = _pendingServiceCall;
			if (pendingServiceCall == null) {
				Clause clause = clauseInfo.getClause();
				clause.setError(CODE_REMOTE_SERVICE_UNAVAILABLE, "remote service unavailable!!");
				ctx.deliverError(dataCallback, clause);
				return null;
			}
			synchronized (pendingServiceCall) {
				if (_globalRemoteDataService != null) {
					remoteDataService = getRemoteDataService();
				} else {
					pendingServiceCall.push(new Runnable() {
						@Override
						public void run() {
							execClause(ctx, clauseInfo, dataCallback);
						}
					});
					return null;
				}
			}
		}
		try {
			XulRemoteDataCallbackProxy callback = getRemoteCallbackProxy(ctx, clauseInfo, dataCallback);
			IXulRemoteDataOperation xulRemoteDataOperation = remoteDataService.invokeRemoteService(clauseInfo, callback);
			if (clauseInfo.clause.getError() != XulDataService.CODE_NO_PROVIDER) {
				return callback.getDataOperation(xulRemoteDataOperation);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return super.execClause(ctx, clauseInfo, dataCallback);
	}

	static class XulRemoteDataCallbackProxy extends IXulRemoteDataCallback.Stub {
		XulDataServiceContext _ctx;
		XulDataCallback _dataCallback;
		XulDataOperation _dataOperation;
		XulClauseInfo _clauseInfo;

		public XulRemoteDataCallbackProxy(XulDataServiceContext ctx, XulClauseInfo clauseInfo, XulDataCallback callback) {
			_ctx = ctx;
			_dataCallback = callback;
			_clauseInfo = clauseInfo;
		}

		@Override
		public void setError(int error) {
			_clauseInfo.clause.setError(error);
		}

		@Override
		public void setErrorEx(int error, String msg) {
			_clauseInfo.clause.setError(error, msg);
		}

		@Override
		public void onResult(IXulRemoteDataOperation op, int code, XulDataNode data) throws RemoteException {
			_clauseInfo.dataOperation = getDataOperation(op);
			_ctx.deliverResult(_dataCallback, _clauseInfo.clause, data);
		}

		@Override
		public void onError(IXulRemoteDataOperation op, int code) throws RemoteException {
			if (code == XulDataService.CODE_NO_PROVIDER) {
				return;
			}
			_clauseInfo.dataOperation = getDataOperation(op);
			_ctx.deliverError(_dataCallback, _clauseInfo.clause);
		}

		public XulDataOperation getDataOperation(IXulRemoteDataOperation xulRemoteDataOperation) {
			if (xulRemoteDataOperation == null) {
				return null;
			}
			try {
				if (xulRemoteDataOperation.isPullOperation()) {
					if (!(_dataOperation instanceof XulRemotePullOperationProxy)) {
						_dataOperation = new XulRemotePullOperationProxy(_ctx, _clauseInfo, xulRemoteDataOperation);
					}
					XulRemotePullOperationProxy proxy = (XulRemotePullOperationProxy) _dataOperation;
					proxy._remoteDataOp = xulRemoteDataOperation;
					proxy._clauseInfo = _clauseInfo;
					proxy._ctx = _ctx;
					return proxy;
				}
				if (!(_dataOperation instanceof XulRemoteOperationProxy)) {
					_dataOperation = new XulRemoteOperationProxy(_ctx, _clauseInfo, xulRemoteDataOperation);
				}
				XulRemoteOperationProxy proxy = (XulRemoteOperationProxy) _dataOperation;
				proxy._remoteDataOp = xulRemoteDataOperation;
				proxy._clauseInfo = _clauseInfo;
				proxy._ctx = _ctx;
				return proxy;
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
			return null;
		}
	}

	static class XulRemotePullOperationProxy extends XulPullDataCollection {
		XulDataServiceContext _ctx;
		IXulRemoteDataOperation _remoteDataOp;
		XulClauseInfo _clauseInfo;

		public XulRemotePullOperationProxy(XulDataServiceContext ctx, XulClauseInfo clauseInfo, IXulRemoteDataOperation dataOp) {
			_ctx = ctx;
			_remoteDataOp = dataOp;
			_clauseInfo = clauseInfo;
		}

		@Override
		public boolean pull(XulDataCallback dataCallback) {
			try {
				return _remoteDataOp.pull(getRemoteCallbackProxy(_ctx, _clauseInfo, dataCallback));
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
			return false;
		}

		@Override
		public boolean reset() {
			try {
				return _remoteDataOp.reset();
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
			return false;
		}

		@Override
		public boolean reset(int pageIdx) {
			try {
				return _remoteDataOp.resetEx(pageIdx);
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
			return false;
		}

		@Override
		public int currentPage() {
			try {
				return _remoteDataOp.currentPage();
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
			return 0;
		}

		@Override
		public int pageSize() {
			try {
				return _remoteDataOp.pageSize();
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
			return 0;
		}

		@Override
		public boolean isFinished() {
			try {
				return _remoteDataOp.isFinished();
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
			return false;
		}

		@Override
		public boolean exec(XulDataCallback callback) throws XulDataException {
			try {
				return _remoteDataOp.exec(getRemoteCallbackProxy(_ctx, _clauseInfo, callback));
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
			return false;
		}

		@Override
		public boolean cancel() {
			try {
				return _remoteDataOp.cancel();
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
			return false;
		}
	}

	static class XulRemoteOperationProxy extends XulDataOperation {
		XulDataServiceContext _ctx;
		IXulRemoteDataOperation _remoteDataOp;
		XulClauseInfo _clauseInfo;

		public XulRemoteOperationProxy(XulDataServiceContext ctx, XulClauseInfo clauseInfo, IXulRemoteDataOperation dataOp) {
			_ctx = ctx;
			_remoteDataOp = dataOp;
			_clauseInfo = clauseInfo;
		}

		@Override
		public boolean exec(XulDataCallback callback) throws XulDataException {
			try {
				return _remoteDataOp.exec(getRemoteCallbackProxy(_ctx, _clauseInfo, callback));
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
			return false;
		}

		@Override
		public boolean cancel() {
			try {
				return _remoteDataOp.cancel();
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
			return false;
		}
	}

}
