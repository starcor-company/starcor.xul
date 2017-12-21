package com.starcor.xulapp.model;

import com.starcor.xul.XulDataNode;
import com.starcor.xulapp.debug.IXulDebugCommandHandler;
import com.starcor.xulapp.debug.IXulDebuggableObject;
import com.starcor.xulapp.debug.XulDebugMonitor;
import com.starcor.xulapp.debug.XulDebugServer;
import com.starcor.xulapp.http.XulHttpServer;
import com.starcor.xulapp.utils.XulLog;

import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by hy on 2015/9/15.
 */
public class XulDataService {

	public static final int CODE_SUCCESS = 0;
	public static final int CODE_FAILED = -1;
	public static final int CODE_EXCEPTION = -2;
	public static final int CODE_NO_PROVIDER = -2;
	public static final int CODE_UNSUPPORTED = -3;
	public static final int CODE_DATA_EXCEPTION = -4;
	public static final int CODE_REMOTE_SERVICE_UNAVAILABLE = -5;
	public static final int XVERB_QUERY = 0x0001;
	public static final int XVERB_DELETE = 0x0002;
	public static final int XVERB_UPDATE = 0x0004;
	public static final int XVERB_INSERT = 0x0008;
	public static final int XVERB_INVOKE = 0x0010;
	public static final int XVERB_MASK = 0xFFFF;
	public static final int XVERB_MODE_PUSH = 0x00000;
	public static final int XVERB_MODE_PULL = 0x10000;
	public static final DataComparator XDC_EQUAL = new DataComparator(DataComparator.EQUAL) {
		@Override
		public boolean test(String v1, String v2) {
			return v1 == v2 || (v1 == null ? v2.equals(v1) : v1.equals(v2));
		}
	};
	public static final DataComparator XDC_GREAT = new DataComparator(DataComparator.GREAT);
	public static final DataComparator XDC_GREAT_EQUAL = new DataComparator(DataComparator.GREAT_EQ);
	public static final DataComparator XDC_LESS = new DataComparator(DataComparator.LESS);
	public static final DataComparator XDC_LESS_EQUAL = new DataComparator(DataComparator.LESS_EQ);
	public static final DataComparator XDC_ANY_OF = new DataComparator(DataComparator.ANY_OF);
	public static final XulDataOperation XOP_NEXT_OPERATOR = new XulDataOperation();
	private static final String TAG = XulDataService.class.getSimpleName();
	private static XulDebugMonitor dbgMonitor;
	private static HashMap<String, DataProviderInfo> _providerMap = new HashMap<String, DataProviderInfo>();
	private static XulDataServiceFactory _dataServiceFactory;
	private XulDataServiceContext _dataServiceContext;

	private Object _userData;

	protected XulDataService() {
	}

	public static XulDataService obtainDataService() {
		if (_dataServiceFactory == null) {
			return obtainLocalDataService();
		}
		return _dataServiceFactory.createXulDataService();
	}

	public static XulDataService obtainLocalDataService() {
		return new XulDataService();
	}

	public static void setDataServiceFactory(XulDataServiceFactory dataServiceFactory) {
		_dataServiceFactory = dataServiceFactory;
	}

	public static void registerDataProvider(String target, int verbMask, XulDataProvider provider) {
		registerDebugHelper();

		final DataProviderInfo info = new DataProviderInfo();
		info.verbMask = verbMask;
		info.target = target;
		info.provider = provider;
		info.nextProvider = _providerMap.get(target);
		_providerMap.put(target, info);
	}

	private static void debugDoQuery(String dataSource, final XulHttpServer.XulHttpServerResponse response, LinkedHashMap<String, String> queries) {
		debugDoQuery(dataSource, response, queries, false);
	}

	private static void debugDoQuery(String dataSource, final XulHttpServer.XulHttpServerResponse response, LinkedHashMap<String, String> queries, boolean pullMode) {
		final XulDataService xulDataService = obtainDataService();
		try {
			final QueryClause dataQuery = xulDataService.query(dataSource);

			if (queries != null) {
				for (Map.Entry<String, String> entry : queries.entrySet()) {
					dataQuery.where(entry.getKey()).is(entry.getValue());
				}
			}

			debugExecQuery(response, dataQuery, pullMode);
		} catch (Exception e) {
			e.printStackTrace(new PrintStream(response.getBodyStream()));
			response.addHeader("Content-Type", "text/plain")
				.send();
		}
	}

	private static void debugDoInvoke(String dataSource, String func, final XulHttpServer.XulHttpServerResponse response, LinkedHashMap<String, String> queries) {
		final XulDataService xulDataService = obtainDataService();
		try {
			final InvokeClause dataQuery = xulDataService.invoke(dataSource, func);

			if (queries != null) {
				for (Map.Entry<String, String> entry : queries.entrySet()) {
					String key = entry.getKey();
					if (key.startsWith("set-")) {
						dataQuery.set(key.substring(4), entry.getValue());
					} else if (key.equals("-add-value")) {
						dataQuery.value(entry.getValue());
					} else {
						dataQuery.where(key).is(entry.getValue());
					}
				}
			}

			debugExecQuery(response, dataQuery, false);
		} catch (Exception e) {
			e.printStackTrace(new PrintStream(response.getBodyStream()));
			response.addHeader("Content-Type", "text/plain")
				.send();
		}
	}

	private static void debugDoInsert(String dataSource, final XulHttpServer.XulHttpServerResponse response, LinkedHashMap<String, String> queries) {
		final XulDataService xulDataService = obtainDataService();
		try {
			final InsertClause dataQuery = xulDataService.insert(dataSource);

			if (queries != null) {
				for (Map.Entry<String, String> entry : queries.entrySet()) {
					String key = entry.getKey();
					if (key.startsWith("set-")) {
						dataQuery.set(key.substring(4), entry.getValue());
					} else if (key.equals("-add-value")) {
						dataQuery.value(entry.getValue());
					} else {
						dataQuery.where(key).is(entry.getValue());
					}
				}
			}

			debugExecQuery(response, dataQuery, false);
		} catch (Exception e) {
			e.printStackTrace(new PrintStream(response.getBodyStream()));
			response.addHeader("Content-Type", "text/plain")
				.send();
		}
	}

	private static void debugDoUpdate(String dataSource, final XulHttpServer.XulHttpServerResponse response, LinkedHashMap<String, String> queries) {
		final XulDataService xulDataService = obtainDataService();
		try {
			final UpdateClause dataQuery = xulDataService.update(dataSource);

			if (queries != null) {
				for (Map.Entry<String, String> entry : queries.entrySet()) {
					String key = entry.getKey();
					if (key.startsWith("set-")) {
						dataQuery.set(key.substring(4), entry.getValue());
					} else {
						dataQuery.where(key).is(entry.getValue());
					}
				}
			}

			debugExecQuery(response, dataQuery, false);
		} catch (Exception e) {
			e.printStackTrace(new PrintStream(response.getBodyStream()));
			response.addHeader("Content-Type", "text/plain")
				.send();
		}
	}

	private static void debugDoDelete(String dataSource, final XulHttpServer.XulHttpServerResponse response, LinkedHashMap<String, String> queries) {
		final XulDataService xulDataService = obtainDataService();
		try {
			final DeleteClause dataQuery = xulDataService.delete(dataSource);

			if (queries != null) {
				for (Map.Entry<String, String> entry : queries.entrySet()) {
					String key = entry.getKey();
					dataQuery.where(key).is(entry.getValue());
				}
			}

			debugExecQuery(response, dataQuery, false);
		} catch (Exception e) {
			e.printStackTrace(new PrintStream(response.getBodyStream()));
			response.addHeader("Content-Type", "text/plain")
				.send();
		}
	}

	private static void debugExecQuery(final XulHttpServer.XulHttpServerResponse response, Clause dataQuery, boolean pullMode) {
		final XulDataCallback dataCallback = new XulDataCallback() {
			@Override
			public void onResult(Clause clause, int code, XulDataNode data) {
				try {
					XmlSerializer writer = XmlPullParserFactory.newInstance().newSerializer();
					writer.setOutput(response.getBodyStream(), "utf-8");
					response.addHeader("Content-Type", "text/xml");
					writer.startDocument("utf-8", true);
					XulDataNode.dumpXulDataNode(data, writer);
					writer.endDocument();
					writer.flush();
					response.send();
				} catch (Exception e) {
					e.printStackTrace(new PrintStream(response.getBodyStream()));
					response.addHeader("Content-Type", "text/plain")
						.send();
				}
			}

			@Override
			public void onError(Clause clause, int code) {
				response.setStatus(500)
					.addHeader("DS-Error-Code", String.valueOf(code))
					.addHeader("DS-Error-Info", clause.getMessage())
					.send();
			}
		};

		if (pullMode) {
			((QueryClause) dataQuery).pull(dataCallback);
		} else {
			dataQuery.exec(dataCallback);
		}
	}

	private static void registerDebugHelper() {
		if (dbgMonitor != null) {
			return;
		}
		dbgMonitor = XulDebugServer.getMonitor();
		if (dbgMonitor == null) {
			return;
		}
		dbgMonitor.registerDebuggableObject(new IXulDebuggableObject() {
			@Override
			public String name() {
				return "DataService";
			}

			@Override
			public boolean isValid() {
				return true;
			}

			@Override
			public boolean runInMainThread() {
				return false;
			}

			@Override
			public boolean buildBriefInfo(XulHttpServer.XulHttpServerRequest request, XmlSerializer infoWriter) {
				final int size = _providerMap.size();
				try {
					infoWriter.attribute(null, "providerNum", String.valueOf(size));
				} catch (IOException e) {
					XulLog.e(TAG, e);
				}
				return true;
			}

			@Override
			public boolean buildDetailInfo(XulHttpServer.XulHttpServerRequest request, XmlSerializer infoWriter) {
				dumpDataProviders(infoWriter);
				return true;
			}

			@Override
			public XulHttpServer.XulHttpServerResponse execCommand(String command, XulHttpServer.XulHttpServerRequest request, XulHttpServer.XulHttpServerHandler serverHandler) {
				final String[] commands = command.split("/");
				if (commands.length == 2) {
					final String cmd = commands[0];
					final String dataTarget = commands[1];
					if ("query".equals(cmd)) {
						debugDoQuery(dataTarget, serverHandler.getResponse(request), request.queries);
						return XulDebugServer.PENDING_RESPONSE;
					}

					if ("pull".equals(cmd)) {
						debugDoQuery(dataTarget, serverHandler.getResponse(request), request.queries, true);
						return XulDebugServer.PENDING_RESPONSE;
					}

					if ("insert".equals(cmd)) {
						debugDoInsert(dataTarget, serverHandler.getResponse(request), request.queries);
						return XulDebugServer.PENDING_RESPONSE;
					}

					if ("delete".equals(cmd)) {
						debugDoDelete(dataTarget, serverHandler.getResponse(request), request.queries);
						return XulDebugServer.PENDING_RESPONSE;
					}

					if ("update".equals(cmd)) {
						debugDoUpdate(dataTarget, serverHandler.getResponse(request), request.queries);
						return XulDebugServer.PENDING_RESPONSE;
					}
				} else if (commands.length == 3) {
					final String cmd = commands[0];
					final String dataTarget = commands[1];
					final String function = commands[2];
					if ("invoke".equals(cmd)) {
						debugDoInvoke(dataTarget, function, serverHandler.getResponse(request), request.queries);
						return XulDebugServer.PENDING_RESPONSE;
					}
				}

				return null;
			}

			protected void dumpDataProviders(XmlSerializer infoWriter) {
				final int size = _providerMap.size();
				try {
					infoWriter.attribute(null, "providerNum", String.valueOf(size));
				} catch (IOException e) {
					XulLog.e(TAG, e);
				}

				for (DataProviderInfo provider : _providerMap.values()) {
					try {
						infoWriter.startTag(null, "provider");
						infoWriter.attribute(null, "name", provider.target);

						DataProviderInfo curProvider = provider;
						infoWriter.startTag(null, "ds");

						while (curProvider != null) {
							final boolean pullMode = (curProvider.verbMask & XVERB_MODE_PULL) == XVERB_MODE_PULL;
							final boolean supportQuery = (curProvider.verbMask & XVERB_QUERY) == XVERB_QUERY;
							final boolean supportDelete = (curProvider.verbMask & XVERB_DELETE) == XVERB_DELETE;
							final boolean supportInsert = (curProvider.verbMask & XVERB_INSERT) == XVERB_INSERT;
							final boolean supportUpdate = (curProvider.verbMask & XVERB_UPDATE) == XVERB_UPDATE;
							final boolean supportInvoke = (curProvider.verbMask & XVERB_INVOKE) == XVERB_INVOKE;

							StringBuilder modeMask = new StringBuilder();
							if (pullMode) {
								modeMask.append("Pull");
							} else {
								modeMask.append("Push");
							}

							if (supportQuery) {
								modeMask.append("|Query");
							}

							if (supportDelete) {
								modeMask.append("|Delete");
							}

							if (supportInsert) {
								modeMask.append("|Insert");
							}

							if (supportUpdate) {
								modeMask.append("|Update");
							}

							if (supportInvoke) {
								modeMask.append("|Invoke");
							}

							infoWriter.attribute(null, "providerClass", curProvider.provider.getClass().getSimpleName());

							infoWriter.attribute(null, "mode", modeMask.toString());
							curProvider = curProvider.nextProvider;
						}

						infoWriter.endTag(null, "ds");

						infoWriter.endTag(null, "provider");
					} catch (IOException e) {
						XulLog.e(TAG, e);
					}
				}
			}
		});

		XulDebugServer.registerCommandHandler(new IXulDebugCommandHandler() {

			XulHttpServer.XulHttpServerHandler _serverHandler;

			@Override
			public XulHttpServer.XulHttpServerResponse execCommand(String url, XulHttpServer.XulHttpServerHandler serverHandler, XulHttpServer.XulHttpServerRequest request) {
				_serverHandler = serverHandler;
				if (url.startsWith("/api/query-data/")) {
					queryData(request, url.substring(16), false);
					return XulDebugServer.PENDING_RESPONSE;
				}

				if (url.startsWith("/api/pull-data/")) {
					queryData(request, url.substring(15), true);
					return XulDebugServer.PENDING_RESPONSE;
				}
				if (url.startsWith("/api/invoke-data/")) {
					String[] tmp = url.substring(17).split("/");
					invokeData(request, tmp[0], tmp[1]);
					return XulDebugServer.PENDING_RESPONSE;
				}
				return null;
			}

			private void queryData(final XulHttpServer.XulHttpServerRequest request, final String dataSource, boolean pullMode) {
				final XulHttpServer.XulHttpServerResponse response = _serverHandler.getResponse(request);
				debugDoQuery(dataSource, response, request.queries, pullMode);
			}

			private void invokeData(final XulHttpServer.XulHttpServerRequest request, final String dataSource, final String func) {
				final XulHttpServer.XulHttpServerResponse response = _serverHandler.getResponse(request);
				debugDoInvoke(dataSource, func, response, request.queries);
			}
		});
	}

	protected XulDataOperation execClause(XulDataServiceContext ctx, XulClauseInfo clauseInfo, XulDataCallback dataCallback) {
		DataProviderInfo dataProviderInfo = _providerMap.get(clauseInfo.target);
		final Clause clause = clauseInfo.getClause();
		try {
			for (; dataProviderInfo != null; dataProviderInfo = dataProviderInfo.nextProvider) {
				final int verb = clauseInfo.verb & XVERB_MASK;
				if ((dataProviderInfo.verbMask & verb) == verb) {
					XulDataOperation operation = dataProviderInfo.provider.dispatchClause(ctx, clauseInfo);
					if (operation == null) {
						notifyClauseError(dataCallback, clause, CODE_UNSUPPORTED, "unsupported operation");
						return null;
					}
					if (operation == XOP_NEXT_OPERATOR) {
						continue;
					}
					clauseInfo.dataOperation = operation;
					boolean result;
					if (dataCallback instanceof XulPendingDataCallback) {
						final XulPendingDataCallback pendingDataCallback = (XulPendingDataCallback) dataCallback;
						result = pendingDataCallback.scheduleExec(operation, dataCallback);
					} else {
						result = operation.exec(dataCallback);
					}
					if (result) {
						return operation;
					}
					notifyClauseError(dataCallback, clause, CODE_FAILED, "execute clause failed");
					return null;
				}
			}
		} catch (XulDataException e) {
			notifyClauseError(dataCallback, clause, CODE_DATA_EXCEPTION, e.getMessage());
			XulLog.e(TAG, e);
			return null;
		} catch (Exception e) {
			notifyClauseError(dataCallback, clause, CODE_EXCEPTION, e.getMessage());
			XulLog.e(TAG, e);
			return null;
		}
		notifyClauseError(dataCallback, clause, CODE_NO_PROVIDER, "No such data provider");
		return null;
	}

	protected void notifyClauseError(XulDataCallback dataCallback, Clause clause, int code, String msg) {
		clause.setError(code, msg);
		if (dataCallback != null) {
			dataCallback.onError(clause, code);
		} else {
			XulLog.e(TAG, "clause error", code, msg);
		}
	}

	public QueryClause query(String target) {
		final QueryClause clause = new QueryClause(target);
		return clause;
	}

	public DeleteClause delete(String target) {
		return new DeleteClause(target);
	}

	public InsertClause insert(String target) {
		return new InsertClause(target);
	}

	public UpdateClause update(String target) {
		return new UpdateClause(target);
	}

	public InvokeClause invoke(String target, String func) {
		return new InvokeClause(target, func);
	}

	public void cancelClause() {
		XulDataServiceContext dataServiceContext = _dataServiceContext;
		if (dataServiceContext != null) {
			_dataServiceContext = null;
			dataServiceContext.destroy();
		}
	}

	protected XulDataServiceContext getServiceContext() {
		if (_dataServiceContext == null) {
			_dataServiceContext = new XulDataServiceContext(this);
		}
		return _dataServiceContext;
	}

	public static class DataComparator {
		static final int EQUAL = 0x0000;
		static final int GREAT = 0x0001;
		static final int LESS = 0x0002;
		static final int GREAT_EQ = 0x0003;
		static final int LESS_EQ = 0x0004;
		static final int ANY_OF = 0x0005;
		final int _comparator;

		public DataComparator(int comparator) {
			_comparator = comparator;
		}

		public boolean test(String v1, String v2) {
			return false;
		}

		public boolean test(String[] v1, String v2) {
			return false;
		}

		public boolean test(String v1, String[] v2) {
			return false;
		}

		public boolean test(String[] v1, String[] v2) {
			return false;
		}
	}

	public static DataComparator getDataComparator(int t) {
		switch (t) {
		case DataComparator.EQUAL:
			return XDC_EQUAL;
		case DataComparator.GREAT:
			return XDC_GREAT;
		case DataComparator.LESS:
			return XDC_LESS;
		case DataComparator.GREAT_EQ:
			return XDC_GREAT_EQUAL;
		case DataComparator.LESS_EQ:
			return XDC_LESS_EQUAL;
		case DataComparator.ANY_OF:
			return XDC_ANY_OF;
		}
		return new DataComparator(t);
	}

	protected static class DataProviderInfo {
		String target;
		int verbMask;
		XulDataProvider provider;
		DataProviderInfo nextProvider;
	}

	public class Clause {
		protected XulClauseInfo _clauseInfo = new XulClauseInfo();
		private int _error = 0;
		private String _message = "";

		Clause(XulClauseInfo clauseInfo) {
			_clauseInfo = clauseInfo;
			_clauseInfo.clause = this;
		}

		public Clause() {
			_clauseInfo = new XulClauseInfo();
			_clauseInfo.clause = this;
		}

		public void setError(int error, String msg) {
			_error = error;
			_message = msg;
		}

		public int getError() {
			return _error;
		}

		public void setError(int error) {
			_error = error;
			_message = "";
		}

		public String getMessage() {
			return _message;
		}

		public boolean exec(XulDataCallback dataCallback) {
			return execClause(getServiceContext(), _clauseInfo, dataCallback) != null;
		}

		void addCondition(String key, DataComparator comparator, String value) {
			_clauseInfo.addCondition(key, comparator, value);
		}

		void addCondition(String key, DataComparator comparator, String[] values) {
			_clauseInfo.addCondition(key, comparator, values);
		}

		public XulDataOperation dataOperation() {
			return _clauseInfo.dataOperation;
		}
	}

	public class QueryConditionClause {
		private QueryClause _clause;
		private String _key;

		QueryConditionClause(QueryClause queryClause, String key) {
			_clause = queryClause;
			_key = key;
		}

		public QueryClause is(int value) {
			_clause.addCondition(_key, XDC_EQUAL, String.valueOf(value));
			return _clause;
		}

		public QueryClause is(String value) {
			_clause.addCondition(_key, XDC_EQUAL, value);
			return _clause;
		}

		public QueryClause is(String... values) {
			_clause.addCondition(_key, XDC_EQUAL, values);
			return _clause;
		}

		public QueryClause is(DataComparator comparator, String value) {
			_clause.addCondition(_key, comparator, value);
			return _clause;
		}

		public QueryClause is(DataComparator comparator, String... values) {
			_clause.addCondition(_key, comparator, values);
			return _clause;
		}
	}

	public class QueryClause extends Clause {
		QueryClause(String target) {
			_clauseInfo.target = target;
			_clauseInfo.verb = XVERB_QUERY;
		}

		public QueryConditionClause where(String key) {
			return new QueryConditionClause(this, key);
		}

		public XulPullDataCollection pull(XulDataCallback dataCallback) {
			_clauseInfo.verb |= XVERB_MODE_PULL;
			final XulDataOperation operation = execClause(getServiceContext(), _clauseInfo, dataCallback);

			return (operation instanceof XulPullDataCollection) ? (XulPullDataCollection) operation : null;
		}
	}

	public class DeleteConditionClause {
		private DeleteClause _clause;
		private String _key;

		DeleteConditionClause(DeleteClause deleteClause, String key) {
			this._clause = deleteClause;
			this._key = key;
		}

		public DeleteClause is(String value) {
			_clause.addCondition(_key, XDC_EQUAL, value);
			return _clause;
		}

		public DeleteClause is(String... values) {
			_clause.addCondition(_key, XDC_EQUAL, values);
			return _clause;
		}

		public DeleteClause is(DataComparator comparator, String value) {
			_clause.addCondition(_key, comparator, value);
			return _clause;
		}

		public DeleteClause is(DataComparator comparator, String... values) {
			_clause.addCondition(_key, comparator, values);
			return _clause;
		}
	}

	public class DeleteClause extends Clause {
		DeleteClause(String target) {
			_clauseInfo.target = target;
			_clauseInfo.verb = XVERB_DELETE;
		}

		public DeleteConditionClause where(String key) {
			return new DeleteConditionClause(this, key);
		}
	}

	public class InsertClause extends Clause {
		InsertClause(String target) {
			_clauseInfo.target = target;
			_clauseInfo.verb = XVERB_INSERT;
		}

		public InsertConditionClause where(String key) {
			return new InsertConditionClause(this, key);
		}

		public InsertClause set(String key, String... values) {
			_clauseInfo.addDataItem(key, values);
			return this;
		}

		public InsertClause set(String key, String value) {
			_clauseInfo.addDataItem(key, value);
			return this;
		}

		public InsertClause set(String key, int value) {
			_clauseInfo.addDataItem(key, value);
			return this;
		}

		public InsertClause value(Object... objs) {
			_clauseInfo.addDataItem(objs);
			return this;
		}
	}

	public class InsertConditionClause {
		private InsertClause _clause;
		private String _key;

		InsertConditionClause(InsertClause clause, String key) {
			this._clause = clause;
			this._key = key;
		}

		public InsertClause is(String value) {
			_clause.addCondition(_key, XDC_EQUAL, value);
			return _clause;
		}

		public InsertClause is(String... values) {
			_clause.addCondition(_key, XDC_EQUAL, values);
			return _clause;
		}

		public InsertClause is(DataComparator comparator, String value) {
			_clause.addCondition(_key, comparator, value);
			return _clause;
		}

		public InsertClause is(DataComparator comparator, String... values) {
			_clause.addCondition(_key, comparator, values);
			return _clause;
		}
	}

	public class UpdateClause extends Clause {
		UpdateClause(String target) {
			_clauseInfo.target = target;
			_clauseInfo.verb = XVERB_UPDATE;
		}

		public UpdateConditionClause where(String key) {
			return new UpdateConditionClause(this, key);
		}

		public UpdateClause set(String key, String value) {
			_clauseInfo.addDataItem(key, value);
			return this;
		}

		public UpdateClause set(String key, int value) {
			_clauseInfo.addDataItem(key, value);
			return this;
		}
	}

	public class UpdateConditionClause {
		private UpdateClause _clause;
		private String _key;

		UpdateConditionClause(UpdateClause clause, String key) {
			this._clause = clause;
			this._key = key;
		}

		public UpdateClause is(String value) {
			_clause.addCondition(_key, XDC_EQUAL, value);
			return _clause;
		}

		public UpdateClause is(String... values) {
			_clause.addCondition(_key, XDC_EQUAL, values);
			return _clause;
		}

		public UpdateClause is(DataComparator comparator, String value) {
			_clause.addCondition(_key, comparator, value);
			return _clause;
		}

		public UpdateClause is(DataComparator comparator, String... values) {
			_clause.addCondition(_key, comparator, values);
			return _clause;
		}
	}

	public class InvokeClause extends Clause {
		InvokeClause(String target, String func) {
			_clauseInfo.target = target;
			_clauseInfo.func = func;
			_clauseInfo.verb = XVERB_INVOKE;
		}

		public InvokeConditionClause where(String key) {
			return new InvokeConditionClause(this, key);
		}

		public InvokeClause set(String key, String... values) {
			_clauseInfo.addDataItem(key, values);
			return this;
		}

		public InvokeClause set(String key, String value) {
			_clauseInfo.addDataItem(key, value);
			return this;
		}

		public InvokeClause set(String key, int value) {
			_clauseInfo.addDataItem(key, value);
			return this;
		}

		public InvokeClause value(Object... objs) {
			_clauseInfo.addDataItem(objs);
			return this;
		}
	}

	public class InvokeConditionClause {
		private InvokeClause _clause;
		private String _key;

		InvokeConditionClause(InvokeClause clause, String key) {
			this._clause = clause;
			this._key = key;
		}

		public InvokeClause is(String value) {
			_clause.addCondition(_key, XDC_EQUAL, value);
			return _clause;
		}

		public InvokeClause is(String... values) {
			_clause.addCondition(_key, XDC_EQUAL, values);
			return _clause;
		}

		public InvokeClause is(DataComparator comparator, String value) {
			_clause.addCondition(_key, comparator, value);
			return _clause;
		}

		public InvokeClause is(DataComparator comparator, String... values) {
			_clause.addCondition(_key, comparator, values);
			return _clause;
		}
	}

	public void setUserData(Object userData) {
		_userData = userData;
	}

	public Object getUserData() {
		return _userData;
	}

	public static abstract class XulDataServiceFactory {
		abstract XulDataService createXulDataService();
	}
}
