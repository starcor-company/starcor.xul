package com.starcor.xulapp.model;

/**
 * Created by hy on 2015/9/25.
 */
public class XulPullDataCollection extends XulDataOperation {

	public boolean pull(XulDataCallback dataCallback) {
		return false;
	}

	public boolean reset() {
		return false;
	}

	public boolean reset(int pageIdx) {
		return false;
	}

	public int currentPage() {
		return -1;
	}

	public int pageSize() {
		return 0;
	}

	public boolean isFinished() {
		return false;
	}

}
