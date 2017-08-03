package com.starcor.xulapp.model.utils;

/**
 * Created by hy on 2015/11/17.
 */
public class XulPullCollectionException extends Exception {
	int _code;
	public XulPullCollectionException(int code, String detailMessage) {
		super(detailMessage);
		_code = code;
	}

	public XulPullCollectionException(String detailMessage) {
		this(-1, detailMessage);
	}

	public int getCode() {
		return _code;
	}
}
