package com.starcor.xulapp.model;

import com.starcor.xulapp.model.IXulRemoteDataCallback;

/**
 * Created by hy on 2016/11/15.
 */

interface IXulRemoteDataOperation {
	boolean exec(IXulRemoteDataCallback callback);
	boolean cancel();
	boolean pull(IXulRemoteDataCallback callback);
	boolean reset();
	boolean resetEx(int pageIdx);
	int currentPage();
	int pageSize();
	boolean isFinished();
	boolean isPullOperation();
}
