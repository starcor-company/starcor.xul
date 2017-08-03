package com.starcor.xulapp.http;

/**
 * Created by hy on 2015/11/6.
 */
public abstract class XulHttpFilter {

	public abstract String name();

	public int doRequest(XulHttpStack.XulHttpCtx ctx, XulHttpRequest request) {
		return 0;
	}

	public int handleResponse(XulHttpStack.XulHttpCtx ctx, XulHttpResponse response) {
		return 0;
	}

}
