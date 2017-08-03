package com.starcor.xulapp.model;

/**
 * Created by hy on 2016/11/15.
 */

import com.starcor.xul.XulDataNode;
import com.starcor.xulapp.model.IXulRemoteDataOperation;

interface IXulRemoteDataCallback {
	void setError(int error);
	void setErrorEx(int error, in String msg);
	void onResult(IXulRemoteDataOperation op, int code, in XulDataNode data);
	void onError(IXulRemoteDataOperation op, int code);
}
