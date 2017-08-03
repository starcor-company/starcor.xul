package com.starcor.xulapp.model;

/**
 * Created by hy on 2015/9/25.
 */
class XulPendingDataCallback extends XulDataCallback {

	public boolean scheduleExec(XulDataOperation operation, XulDataCallback dataCallback) {
		return false;
	}
}
