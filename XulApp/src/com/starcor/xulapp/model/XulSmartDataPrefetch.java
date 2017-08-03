package com.starcor.xulapp.model;

import com.starcor.xul.Utils.XulSimpleArray;
import com.starcor.xul.XulUtils;
import com.starcor.xulapp.XulApplication;

import java.util.ArrayList;

/**
 * Created by hy on 2016/3/16.
 */
public class XulSmartDataPrefetch {
	public static final int PREFETCH_DELAY = 210;
	XulDataService _dataService;
	ArrayList<XulDataPrefetchClient> _prefetchClients;
	XulSimpleArray<PendingPrefetchOperation> _pendingOperations;

	private class PendingPrefetchOperation implements Runnable {
		long _timestamp;
		int _eventId;
		Object _eventInfo;
		XulDataPrefetchClient _acceptor;

		public PendingPrefetchOperation(XulDataPrefetchClient acceptor, int eventId, Object eventInfo) {
			_timestamp = XulUtils.timestamp();
			_eventId = eventId;
			_eventInfo = eventInfo;
			_acceptor = acceptor;
		}

		@Override
		public void run() {
			try {
				_acceptor.doPrefetch(_dataService, _eventId, _eventInfo);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void postPrefetchOperation(XulDataPrefetchClient client, int eventId, Object eventInfo) {
		int pendingOperationNum = _pendingOperations.size();
		PendingPrefetchOperation[] operations = _pendingOperations.getArray();

		int writePos = 0;
		for (int i = 0; i < pendingOperationNum; i++) {
			PendingPrefetchOperation op = operations[i];
			if (op._acceptor == client && op._eventId == eventId) {
				continue;
			}

			if (writePos != i) {
				operations[writePos] = op;
			}
			++writePos;
		}
		_pendingOperations.resize(writePos);
		_pendingOperations.add(new PendingPrefetchOperation(client, eventId, eventInfo));
	}

	public XulSmartDataPrefetch() {
		_dataService = XulDataService.obtainDataService();
		_prefetchClients = new ArrayList<XulDataPrefetchClient>();
		_pendingOperations = new XulSimpleArray<PendingPrefetchOperation>(128) {
			@Override
			protected PendingPrefetchOperation[] allocArrayBuf(int size) {
				return new PendingPrefetchOperation[size];
			}
		};

		final XulApplication appInstance = XulApplication.getAppInstance();
		appInstance.postDelayToMainLooper(new Runnable() {
			@Override
			public void run() {
				handlePrefetchOperation();
				appInstance.postDelayToMainLooper(this, 10);
			}
		}, 10);
	}

	private void handlePrefetchOperation() {
		long curTime = XulUtils.timestamp();

		int pendingOperationNum = _pendingOperations.size();
		PendingPrefetchOperation[] operations = _pendingOperations.getArray();

		int writePos = 0;
		for (int i = 0; i < pendingOperationNum; i++) {
			PendingPrefetchOperation op = operations[i];
			if (curTime - op._timestamp >= PREFETCH_DELAY) {
				consumePrefetchOperation(op);
				continue;
			}
			if (writePos != i) {
				operations[writePos] = op;
			}
			++writePos;
		}
		_pendingOperations.resize(writePos);
	}

	private void consumePrefetchOperation(PendingPrefetchOperation op) {
		XulApplication.getAppInstance().postToMainLooper(op);
	}

	public void registerPrefetchClient(XulDataPrefetchClient client) {
		if (!_prefetchClients.contains(client)) {
			_prefetchClients.add(client);
		}
	}

	public void onPrefetchEvent(int eventId, Object eventInfo) {
		for (XulDataPrefetchClient client : _prefetchClients) {
			if (client.isPrefetchEvent(eventId, eventInfo)) {
				postPrefetchOperation(client, eventId, eventInfo);
			}
		}
	}

	public interface XulDataPrefetchClient {
		boolean isPrefetchEvent(int eventId, Object info);

		boolean doPrefetch(XulDataService dataService, int _eventId, Object info);
	}
}
