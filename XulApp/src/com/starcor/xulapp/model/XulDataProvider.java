package com.starcor.xulapp.model;

/**
 * Created by hy on 2015/9/25.
 */
public class XulDataProvider {

	final XulDataOperation dispatchClause(XulDataServiceContext ctx, XulClauseInfo clauseInfo) throws XulDataException {
		if ((clauseInfo.verb & XulDataService.XVERB_MODE_PULL) == XulDataService.XVERB_MODE_PULL) {
			return this.execPullClause(ctx, clauseInfo);
		}
		return this.execClause(ctx, clauseInfo);
	}

	public XulDataOperation execClause(XulDataServiceContext ctx, XulClauseInfo clauseInfo) throws XulDataException {
		return null;
	}

	public XulDataOperation execPullClause(XulDataServiceContext ctx, XulClauseInfo clauseInfo) throws XulDataException {
		return null;
	}
}
