package com.starcor.xulapp.http;

import android.util.Log;

import com.starcor.xul.XulWorker;
import com.starcor.xulapp.utils.XulLog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by hy on 2015/11/6.
 */
public class XulHttpClientFilter extends XulHttpFilter {
	private static final String TAG = XulHttpClientFilter.class.getSimpleName();

	@Override
	public String name() {
		return "http client";
	}

	@Override
	public int doRequest(XulHttpStack.XulHttpCtx ctx, XulHttpRequest request) {
		addNewTask(ctx);
		return -1;
	}

	@Override
	public int handleResponse(XulHttpStack.XulHttpCtx ctx, XulHttpResponse response) {
		return 0;
	}


	private static Queue<XulHttpStack.XulHttpCtx> _requestQueue = new LinkedList<XulHttpStack.XulHttpCtx>();

	private static Object _waitableObject = new Object();

	private static void doHttpRequest() {
		while (true) {
			if (_requestQueue == null) {
				return;
			}
			XulHttpStack.XulHttpCtx httpCtx;
			synchronized (_requestQueue) {
				httpCtx = _requestQueue.poll();
			}
			if (httpCtx == null) {
				waitForTask(50);
				continue;
			}

			final XulHttpResponse xulHttpResponse = new XulHttpResponse();
			XulWorker.XulDownloadParams params = null;
			try {
				final XulHttpRequest request = httpCtx.getRequest();
				xulHttpResponse.request = request;
				final String url = request.toString();
				Log.i(TAG, "taskId = " + httpCtx.getInitialRequest().path + ", url = " + url);

				boolean isPost = "post".equals(request.method);
				String[] headers = null;
				ArrayList<String> headerParam = request.getHeaderParam();
				byte[] body = null;

				if (isPost) {
					LinkedHashMap<String, String> form = request.form;
					body = request.body;
					if (headerParam != null) {
						headers = new String[(form != null && body == null ? 2 : 0) + headerParam.size()];
						headerParam.toArray(headers);
					}

					if (body != null) {
					} else if (form != null) {
						String formParams = request.getFormParams();
						if (headers == null) {
							headers = new String[2];
						}
						headers[headers.length - 2] = "Content-Type";
						headers[headers.length - 1] = "application/x-www-form-urlencoded";
						body = formParams.getBytes("utf-8");
					}
				} else {
					if (headerParam != null) {
						headers = new String[headerParam.size()];
						headerParam.toArray(headers);
					}
				}
				params = new XulWorker.XulDownloadParams(isPost, body, headers);
				xulHttpResponse.data = XulWorker.loadData(url, true, params);
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}

			if (params != null) {
				xulHttpResponse.code = params.responseCode;
				xulHttpResponse.message = params.responseMsg;
				xulHttpResponse.headers = params.responseHeaders;
			} else if (xulHttpResponse.data == null) {
				xulHttpResponse.code = 404;
				xulHttpResponse.message = "网络连接超时";
			} else {
				xulHttpResponse.code = 200;
				xulHttpResponse.message = "OK";
			}

			httpCtx.postResponse(xulHttpResponse);
		}
	}

	private static void waitForTask(int ms) {
		synchronized (_waitableObject) {
			try {
				_waitableObject.wait(ms);
			} catch (InterruptedException e) {
				XulLog.e(TAG, e);
			}
		}
	}

	private static void addNewTask(XulHttpStack.XulHttpCtx httpCtx) {
		if (_requestQueue == null) {
			return;
		}

		synchronized (_requestQueue) {
			_requestQueue.add(httpCtx);
		}

		synchronized (_waitableObject) {
			_waitableObject.notify();
		}
	}

	private static Thread[] _workers = new Thread[3];

	static {
		for (int i = 0, workersLength = _workers.length; i < workersLength; i++) {
			Thread worker = _workers[i] = new Thread() {
				@Override
				public void run() {
					doHttpRequest();
				}
			};
			worker.setName(String.format("XulHttpClient Worker-%d", i));
			worker.start();
		}
	}
}
