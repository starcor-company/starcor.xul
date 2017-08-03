package com.starcor.xulapp.http;

import android.text.TextUtils;

import com.starcor.xul.Utils.XulCircleQueue;
import com.starcor.xul.XulUtils;
import com.starcor.xulapp.utils.XulLog;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by hy on 2015/11/6.
 */
public class XulHttpStack {

	private static final String TAG = XulHttpStack.class.getSimpleName();

	public static class XulHttpTask {

		private XulHttpRequest _request = new XulHttpRequest();
		private XulHttpResponseHandler _handler;
		private volatile boolean _isCancelled = false;
		private Object _userData;

		public XulHttpTask addQuery(String key, String val) {
			_request.addQueryString(key, val);
			return this;
		}

		public XulHttpTask addQuery(String key, int val) {
			_request.addQueryString(key, String.valueOf(val));
			return this;
		}

		public XulHttpTask addHeader(String key, String val) {
			_request.addHeaderParam(key, val);
			return this;
		}

		public XulHttpTask addForm(String key, String val) {
			_request.addFormParam(key, val);
			return this;
		}

		public XulHttpTask addForm(String key, int val) {
			_request.addFormParam(key, String.valueOf(val));
			return this;
		}

		public XulHttpTask setBody(byte[] data) {
			_request.body = data;
			return this;
		}

		public XulHttpTask setConnectTimeout(int ms) {
			_request.connectTimeout = ms;
			return this;
		}

		public XulHttpTask setReadTimeout(int ms) {
			_request.readTimeout = ms;
			return this;
		}

		public void cancel() {
			_isCancelled = true;
		}

		public XulHttpTask get(XulHttpResponseHandler handler) {
			this._handler = handler;
			scheduleHttpTask(this);
			return this;
		}

		public XulHttpTask post(XulHttpResponseHandler handler) {
			this._handler = handler;
			_request.method = "post";
			scheduleHttpTask(this);
			return this;
		}

		public XulHttpTask setHost(String host) {
			this._request.host = host;
			return this;
		}

		public XulHttpTask setPort(int port) {
			this._request.port = port;
			return this;
		}

		public XulHttpTask setPath(String path) {
			this._request.path = path;
			return this;
		}

		public XulHttpTask setSchema(String schema) {
			this._request.schema = schema;
			return this;
		}

		public Object getUserData() {
			return _userData;
		}

		public void setUserData(Object data) {
			_userData = data;
		}
	}

	public interface XulHttpResponseHandler {
		int onResult(XulHttpTask task, XulHttpRequest request, XulHttpResponse response);
	}

	public static class XulHttpCtx {
		private XulHttpTask _task;
		private XulHttpFilter _filter;
		private XulHttpCtx _prevCtx;
		private XulHttpCtx _nextCtx;
		private XulHttpRequest _request;
		private XulHttpResponse _response;
		private long _reqTime = 0;
		private long _respTime = 0;
		private Object _userData;

		public XulHttpCtx(XulHttpTask task) {
			_task = task;
			_request = task._request;
		}

		public XulHttpCtx(XulHttpTask task, XulHttpRequest request) {
			_task = task;
			_request = request;
		}

		public XulHttpTask getTask() {
			return _task;
		}

		public XulHttpRequest getInitialRequest() {
			return _task._request;
		}

		public XulHttpRequest getRequest() {
			return this._request;
		}

		public void replaceRequest(XulHttpRequest req) {
			this._request = req;
		}

		public void postResponse(XulHttpResponse resp) {
			this._response = resp;
			scheduleHttpResponse(this);
		}

		public XulHttpCtx createNextContext() {
			final XulHttpCtx newCtx = new XulHttpCtx(_task, this._request);
			this._nextCtx = newCtx;
			newCtx._prevCtx = this;
			return newCtx;
		}

		private int doRequest(XulHttpFilter filter) {
			_reqTime = XulUtils.timestamp_us();
			_filter = filter;
			return filter.doRequest(this, _request);
		}

		private int handleResponse(XulHttpResponse response) {
			_respTime = XulUtils.timestamp_us();
			_response = response;
			return _filter.handleResponse(this, response);
		}

		public long getRequestTime() {
			return _reqTime;
		}

		public long getResponseTime() {
			return _respTime;
		}

		public XulHttpCtx getNextCtx() {
			return _nextCtx;
		}

		public XulHttpCtx getPrevCtx() {
			return _prevCtx;
		}

		public XulHttpFilter getFilter() {
			return _filter;
		}

		public Object getUserData() {
			return _userData;
		}

		public void setUserData(Object data) {
			_userData = data;
		}
	}

	static ArrayList<XulHttpFilter> _filterList = new ArrayList<XulHttpFilter>();

	public static XulHttpTask newTask(String url) {
		XulHttpTask xulHttpTask = new XulHttpTask();
		parseUrl(xulHttpTask, url);
		return xulHttpTask;
	}

	private static void parseUrl(XulHttpTask xulHttpTask, String url) {
		try {
			URL reqUrl = new URL(url);
			xulHttpTask.setSchema(reqUrl.getProtocol())
					.setHost(reqUrl.getHost())
					.setPort(reqUrl.getPort())
					.setPath(reqUrl.getPath());
			String queryStr = reqUrl.getQuery();
			if (!TextUtils.isEmpty(queryStr)) {
				String[] params = queryStr.split("&");
				for (String param : params) {
					String[] pair = param.split("=");
					encodeParams(pair);
					if (pair.length == 2) {
						xulHttpTask.addQuery(pair[0], pair[1]);
					} else if (pair.length == 1) {
						xulHttpTask.addQuery(pair[0], "");
					} // else 无效参数
				}
			}
		} catch (MalformedURLException e) {
			xulHttpTask.setPath(url);
		}
	}

	private static void encodeParams(String[] params) {
		if (params == null) {
			return;
		}

		for (int i = 0; i < params.length; i++) {
			try {
				params[i] = URLEncoder.encode(params[i], "UTF-8");
			} catch (UnsupportedEncodingException e) {
				XulLog.w(TAG, "Encode url param failed! param=" + params[i], e);
			}
		}
	}

	public static XulHttpTask newTask(String host, String path) {
		XulHttpTask xulHttpTask = new XulHttpTask();
		xulHttpTask.setHost(host)
			.setPath(path);
		return xulHttpTask;
	}

	public static void initFilters(Class<?>... filterClasses) {
		if (!_filterList.isEmpty()) {
			return;
		}

		if (filterClasses.length < 1) {
			return;
		}

		if (!XulHttpClientFilter.class.isAssignableFrom(filterClasses[0])) {
			_filterList.add(new XulHttpClientFilter());
		}

		for (Class<?> filterClass : filterClasses) {
			XulHttpFilter filter = null;
			try {
				filter = (XulHttpFilter) filterClass.newInstance();
				_filterList.add(filter);
			} catch (InstantiationException e) {
				XulLog.e(TAG, e);
			} catch (IllegalAccessException e) {
				XulLog.e(TAG, e);
			}

			if (filter != null) {
				XulLog.i(TAG, "createFilter:%s", filter.name());
			} else {
				XulLog.i(TAG, "createFilter failed:%s", filterClass.getSimpleName());
			}
		}
	}

	private volatile static XulCircleQueue<XulHttpCtx> _responseQueue = new XulCircleQueue<XulHttpCtx>();
	private volatile static XulCircleQueue<XulHttpTask> _requestQueue = new XulCircleQueue<XulHttpTask>();

	private static void scheduleHttpResponse(XulHttpCtx ctx) {
		final XulCircleQueue<XulHttpCtx> responseQueue = _responseQueue;
		if (responseQueue == null) {
			return;
		}
		synchronized (responseQueue) {
			responseQueue.add(ctx);
		}
		synchronized (_workers) {
			_workers.notify();
		}
	}

	private static void scheduleHttpTask(XulHttpTask task) {
		final XulCircleQueue<XulHttpTask> requestQueue = _requestQueue;
		if (requestQueue == null) {
			return;
		}
		synchronized (requestQueue) {
			requestQueue.add(task);
		}
		synchronized (_workers) {
			_workers.notify();
		}
	}

	private static Thread[] _workers = new Thread[3];

	static {
		for (int i = 0, workersLength = _workers.length; i < workersLength; i++) {
			Thread t = _workers[i] = new Thread() {
				@Override
				public void run() {
					doWork();
				}
			};
			t.setName(String.format("HTTP Stack Worker - %d", i));
			t.start();
		}
	}

	private static void doWork() {
		boolean requestFirst = false;

		while (true) {
			requestFirst = !requestFirst;
			if (requestFirst) {
				if (handleHttpRequest()) {
					continue;
				}
				if (handleHttpResponse()) {
					continue;
				}
			} else {
				if (handleHttpResponse()) {
					continue;
				}
				if (handleHttpRequest()) {
					continue;
				}
			}
			synchronized (_workers) {
				try {
					_workers.wait(50);
				} catch (InterruptedException e) {
					XulLog.e(TAG, e);
				}
			}
		}
	}

	private static boolean handleHttpResponse() {
		final XulCircleQueue<XulHttpCtx> responseQueue = _responseQueue;
		if (responseQueue == null) {
			return false;
		}
		XulHttpCtx headCtx;
		synchronized (responseQueue) {
			if (responseQueue.isEmpty()) {
				return false;
			}
			headCtx = responseQueue.poll();
		}
		final XulHttpTask task = headCtx._task;
		if (task._isCancelled) {
			return true;
		}
		final XulHttpResponseHandler handler = task._handler;

		XulHttpCtx ctx = headCtx._prevCtx;
		XulHttpResponse response = headCtx._response;
		while (ctx != null) {
			if (task._isCancelled) {
				return true;
			}
			try {
				if (ctx.handleResponse(response) != 0) {
					response = null;
					break;
				}
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
			response = ctx._response;
			// move to next _handler
			ctx = ctx._prevCtx;
		}

		if (handler != null && response != null) {
			try {
				handler.onResult(task, task._request, response);
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
		}
		return true;
	}

	private static boolean handleHttpRequest() {
		final XulCircleQueue<XulHttpTask> requestQueue = _requestQueue;
		if (requestQueue == null) {
			return false;
		}
		XulHttpTask task;
		synchronized (requestQueue) {
			if (requestQueue.isEmpty()) {
				return false;
			}
			task = requestQueue.poll();
		}
		if (task._isCancelled) {
			return true;
		}
		XulHttpCtx headCtx = new XulHttpCtx(task);
		int i = _filterList.size() - 1;
		while (i >= 0) {
			if (task._isCancelled) {
				return true;
			}
			XulHttpFilter filter = _filterList.get(i);
			if (headCtx.doRequest(filter) != 0) {
				break;
			}
			if (--i < 0) {
				break;
			}
			headCtx = headCtx.createNextContext();
		}
		return true;
	}
}
