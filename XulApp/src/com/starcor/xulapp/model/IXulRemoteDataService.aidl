package com.starcor.xulapp.model;

/**
 * Created by hy on 2016/11/15.
 */

import com.starcor.xulapp.model.IXulRemoteDataCallback;
import com.starcor.xulapp.model.IXulRemoteDataOperation;
import com.starcor.xulapp.model.XulClauseInfo;

interface IXulRemoteDataService {
	IXulRemoteDataService makeClone();
	void cancelClause();
	IXulRemoteDataOperation invokeRemoteService(in XulClauseInfo clauseInfo, in IXulRemoteDataCallback callback);
}
