package com.starcor.xulapp.http;

import com.starcor.xulapp.utils.XulLog;

/**
 * Created by hy on 2015/11/23.
 */
public class XulHttpStatisticFilter extends XulHttpFilter {
	private static final String TAG = XulHttpStatisticFilter.class.getSimpleName();

	@Override
	public String name() {
		return "HTTP Statistic";
	}

	@Override
	public int handleResponse(XulHttpStack.XulHttpCtx ctx, XulHttpResponse response) {
		long t0 = ctx.getRequestTime();
		XulHttpRequest initialRequest = ctx.getInitialRequest();
		StringBuilder logBuilder = new StringBuilder();
		logBuilder.append(String.format("----- %s -----\n", initialRequest.path));
		while (ctx.getNextCtx() != null) {
			XulHttpFilter filter = ctx.getFilter();
			ctx = ctx.getNextCtx();
			if (filter == this) {
				continue;
			}
			String name = filter.name();
			long t1 = ctx.getRequestTime();
			logBuilder.append(String.format("REQU : %.3f - %s\n", (t1 - t0) / 1000.0, name));
			t0 = t1;
		}

		while (ctx.getPrevCtx() != null) {
			XulHttpFilter filter = ctx.getFilter();
			ctx = ctx.getPrevCtx();
			String name = filter.name();
			long t1 = ctx.getResponseTime();
			logBuilder.append(String.format("RESP : %.3f - %s\n", (t1 - t0) / 1000.0, name));
			t0 = t1;
		}

		XulLog.d(TAG, logBuilder.toString());
		return 0;
	}
}
