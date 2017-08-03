package com.starcor.xulapp.model;

import com.starcor.xul.XulDataNode;

/**
 * Created by hy on 2015/9/25.
 */
public class XulDataCallback {

	public static XulDataCallback DUMMY_CALLBACK = new XulDataCallback();

	public void onResult(XulDataService.Clause clause, int code, XulDataNode data) {
	}

	public void onError(XulDataService.Clause clause, int code) {
	}

	public Object getUserData() {
		return null;
	}
}
