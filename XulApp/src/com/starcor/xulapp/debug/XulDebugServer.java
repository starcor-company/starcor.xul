package com.starcor.xulapp.debug;

import android.content.Intent;
import android.text.TextUtils;

import com.starcor.xul.XulUtils;
import com.starcor.xul.XulWorker;
import com.starcor.xulapp.http.XulHttpRequest;
import com.starcor.xulapp.http.XulHttpServer;
import com.starcor.xulapp.utils.XulLog;
import com.starcor.xulapp.utils.XulSystemUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by hy on 2015/11/30.
 */
public class XulDebugServer extends XulHttpServer {

	public static XulHttpServerResponse PENDING_RESPONSE = new XulHttpServerResponse(null);
	private static XulHttpServer _debugServer;
	private static XulDebugMonitor _monitor;
	private static ArrayList<IXulDebugCommandHandler> _userHandlers;

	private XulDebugServer(int servPort) {
		super(servPort);
	}

	public static void startUp() {
		startUp(55550);
	}

	public static void startUp(int servPort) {
		if (_debugServer != null) {
			return;
		}
		_debugServer = new XulDebugServer(servPort);
		_monitor = new XulDebugMonitor();
		XulDebugUtils.registerBitmapCacheDebugger();
	}

	public static XulDebugMonitor getMonitor() {
		return _monitor;
	}

	public static synchronized boolean registerCommandHandler(IXulDebugCommandHandler debugCommandHandler) {
		if (_userHandlers == null) {
			_userHandlers = new ArrayList<IXulDebugCommandHandler>();
		}

		if (_userHandlers.contains(debugCommandHandler)) {
			return false;
		}
		_userHandlers.add(debugCommandHandler);
		return true;
	}

	@Override
	protected XulHttpServerHandler createHandler(XulHttpServer server, SocketChannel socketChannel) {
		return new XulDebugApiHandler(server, socketChannel);
	}

	private static class XulDebugApiHandler extends XulHttpServerHandler {

		private static final String TAG = XulDebugApiHandler.class.getSimpleName();
		private volatile SimpleDateFormat _dateTimeFormat;

		public XulDebugApiHandler(XulHttpServer server, SocketChannel socketChannel) {
			super(server, socketChannel);
		}

		@Override
		public XulHttpServerResponse getResponse(XulHttpServerRequest httpRequest) {
			XulHttpServerResponse response = super.getResponse(httpRequest);
			synchronized (this) {
				if (_dateTimeFormat == null) {
					_dateTimeFormat = new SimpleDateFormat("ccc, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
					_dateTimeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
				}
				response.addHeader("Date", _dateTimeFormat.format(new Date()))
					.addHeader("Server", "Xul Debug Server/1.0");
			}
			return response;
		}

		@Override
		protected void handleHttpRequest(XulHttpServerRequest request) throws IOException {
			String path = request.path;
			if (path != null && path.startsWith("/api/")) {
				XulHttpServerResponse response = null;
				if ("/api/list-pages".equals(path)) {
					response = listPages(request);
				} else if (path.startsWith("/api/take-snapshot/")) {
					response = getPageSnapShot(request, XulUtils.tryParseInt(path.substring(19)));
				} else if (path.startsWith("/api/get-layout/")) {
					response = dumpPageContent(request, XulUtils.tryParseInt(path.substring(16)));
				} else if (path.startsWith("/api/get-item/")) {
					response = dumpItemContent(request, XulUtils.tryParseInt(path.substring(14)));
				} else if (path.startsWith("/api/add-class/")) {
					response = addItemClass(request, path.substring(15).split("/"));
				} else if (path.startsWith("/api/remove-class/")) {
					response = removeItemClass(request, path.substring(18).split("/"));
				} else if (path.startsWith("/api/set-style/")) {
					response = setItemStyle(request, path.substring(15).split("/"));
				} else if (path.startsWith("/api/set-data/")) {
					// response = setItemData(request, path.substring(14).split("/"));
				} else if (path.startsWith("/api/set-attr/")) {
					response = setItemAttr(request, path.substring(14).split("/"));
				} else if (path.startsWith("/api/request-focus/")) {
					response = requestItemFocus(request, XulUtils.tryParseInt(path.substring(19)));
				} else if (path.startsWith("/api/push-states/")) {
					response = pushPageState(request, path.substring(17).split("/"));
				} else if (path.startsWith("/api/pop-states/")) {
					response = popPageState(request, XulUtils.tryParseInt(path.substring(16)));
				} else if (path.startsWith("/api/drop-states/")) {
					response = dropPageState(request, XulUtils.tryParseInt(path.substring(17)));
				} else if (path.startsWith("/api/get-assets/")) {
					response = getAssets(request, path.substring(16));
				} else if (path.startsWith("/api/send-key/")) {
					response = sendKeyEvent(request, path.substring(14).split("/"));
				} else if (path.startsWith("/api/send-motion/")) {
					response = sendMotionEvent(request, path.substring(17).split("/"));
				} else if (path.equals("/api/list-user-objects")) {
					response = listUserObjects(request);
				} else if (path.startsWith("/api/get-user-object/")) {
					String objectId = path.substring(21);
					int objectIdEndPos = objectId.indexOf('/');
					String param = "";
					if (objectIdEndPos > 0) {
						param = objectId.substring(objectIdEndPos + 1);
						objectId = objectId.substring(0, objectIdEndPos);
					}
					if (TextUtils.isEmpty(param)) {
						response = getUserObject(request, XulUtils.tryParseInt(objectId));
					} else {
						response = execUserObjectCommand(request, XulUtils.tryParseInt(objectId), param);
					}
				} else if (path.startsWith("/api/close-page/")) {
					response = closePage(request, XulUtils.tryParseInt(path.substring(16)));
				} else if (path.equals("/api/start-activity")) {
					response = startActivity(request, null, Intent.ACTION_MAIN, null);
				} else if (path.startsWith("/api/start-activity/")) {
					response = startActivity(request, path.substring(20).split("/"));
				} else if (path.startsWith("/api/highlight-item/")) {
					response = highlightItem(request, XulUtils.tryParseInt(path.substring(20)));
				} else if (path.startsWith("/api/fire-event/")) {
					response = fireEvent(request, path.substring(16).split("/"));
				} else if (path.equals("/api/get-trace")) {
					response = responseCommandOutput(request, "cat /data/anr/traces.txt");
				} else if (path.equals("/api/get-logcat")) {
					response = responseCommandOutput(request, "logcat -v time");
				} else if (path.equals("/api/dump-heap")) {
					response = getResponse(request);
					final XulHttpRequest xulHttpRequest = request.makeCloneNoQueryString();
					xulHttpRequest.path += "/" + XulDebugAdapter.getPackageName() + ".hprof";
					response.setStatus(302)
						.addHeader("Location", xulHttpRequest.toString());
				} else if (path.startsWith("/api/dump-heap/")) {
					File debugTempPath = XulSystemUtil.getDiskCacheDir(XulDebugAdapter.getAppContext(), "debug");
					final File tempFile = File.createTempFile("dump-", ".hprof", debugTempPath);
					android.os.Debug.dumpHprofData(tempFile.getAbsolutePath());
					responseFile(request, tempFile, true);
					return;
				} else {
					response = unsupportedCommand(request);
				}

				if (response == PENDING_RESPONSE) {
					return;
				}

				if (response != null) {
					response.send();
					return;
				}
			}
			super.handleHttpRequest(request);
		}

		private XulHttpServerResponse fireEvent(XulHttpServerRequest request, String[] params) {
			if (params.length < 2) {
				return null;
			}
			int itemId = XulUtils.tryParseInt(params[0]);
			String action = params[1];
			XulHttpServerResponse response = getResponse(request);
			String[] extParams = params.length > 2 ? Arrays.copyOfRange(params, 2, params.length) : null;
			if (_monitor.fireEvent(itemId, action, extParams, request, response)) {
				response.addHeader("Content-Type", "text/xml")
					.writeBody("<result status=\"OK\"/>");
			} else {
				response.setStatus(404)
					.cleanBody();
			}
			return response;
		}

		private void responseFile(XulHttpServerRequest request, final File fileName, boolean autoDelete) throws IOException {
			if (fileName == null || !fileName.exists() || !fileName.canRead()) {
				super.handleHttpRequest(request);
				return;
			}

			final FileInputStream fileInputStream = new FileInputStream(fileName);
			final XulHttpServerResponse response = getResponse(request);
			response.addHeader("Content-Type", "application/oct-stream")
				.writeStream(fileInputStream);

			if (autoDelete) {
				response.setCleanUp(new Runnable() {
					@Override
					public void run() {
						fileName.delete();
					}
				});
			}
			response.send();
		}

		private XulHttpServerResponse responseCommandOutput(XulHttpServerRequest request, String command) {
			XulHttpServerResponse response;
			response = getResponse(request)
				.addHeader("Content-Type", "text/plain");
			try {
				final Process exec = Runtime.getRuntime().exec(command);
				final InputStream inputStream = exec.getInputStream();
				response.writeStream(inputStream)
					.addHeader("Transfer-Encoding", "chunked")
					.setCleanUp(new Runnable() {
						@Override
						public void run() {
							exec.destroy();
						}
					});
			} catch (Exception e) {
				e.printStackTrace(new PrintStream(response.getBodyStream()));
			}
			return response;
		}

		private XulHttpServerResponse highlightItem(XulHttpServerRequest request, int itemId) {
			XulHttpServerResponse response = getResponse(request);
			if (_monitor.highlightItem(itemId, request, response)) {
				response.addHeader("Content-Type", "text/xml")
					.writeBody("<result status=\"OK\"/>");
			} else {
				response.setStatus(404)
					.cleanBody();
			}
			return response;
		}

		private XulHttpServerResponse closePage(XulHttpServerRequest request, int pageId) {
			XulHttpServerResponse response = getResponse(request);
			if (_monitor.closePage(pageId, request, response)) {
				response.addHeader("Content-Type", "text/xml")
					.writeBody("<result status=\"OK\"/>");
			} else {
				response.setStatus(404)
					.cleanBody();
			}
			return response;
		}

		private XulHttpServerResponse startActivity(XulHttpServerRequest request, String[] params) {
			if (params == null) {
				return null;
			}
			String activity = params.length > 0 ? params[0].trim() : null;
			String action = params.length > 1 ? params[1].trim() : Intent.ACTION_MAIN;
			String category = params.length > 2 ? params[2].trim() : null;

			return startActivity(request, activity, action, category);
		}

		private XulHttpServerResponse startActivity(XulHttpServerRequest request, String activity, String action, String category) {
			try {
				Intent intent = new Intent(action);
				String packageName = XulDebugAdapter.getPackageName();
				if (!TextUtils.isEmpty(activity)) {
					String[] activityInfo = activity.split(",");
					if (activityInfo.length == 1) {
					} else if (activityInfo.length == 2) {
						packageName = activityInfo[0];
						activity = activityInfo[1];
					}
					if (activity.startsWith(".")) {
						activity = packageName + activity;
					}
					intent.setClass(XulDebugAdapter.getAppContext(), this.getClass().getClassLoader().loadClass(activity));
				}
				if (!TextUtils.isEmpty(packageName)) {
					intent.setPackage(packageName);
				}

				if (!TextUtils.isEmpty(category)) {
					intent.addCategory(category);
				}
				if (request.queries != null) {
					for (Map.Entry<String, String> queryInfo : request.queries.entrySet()) {
						intent.putExtra(queryInfo.getKey(), queryInfo.getValue());
					}
				}
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				XulDebugAdapter.startActivity(intent);

				XulHttpServerResponse response = getResponse(request);
				return response.addHeader("Content-Type", "text/xml")
					.writeBody("<result status=\"OK\"/>");
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
			return null;
		}

		private XulHttpServerResponse getUserObject(XulHttpServerRequest request, int objectId) {
			XulHttpServerResponse response = getResponse(request);
			if (_monitor.getUserObject(objectId, request, response)) {
				response.addHeader("Content-Type", "text/xml");
			} else {
				response.setStatus(404)
					.cleanBody();
			}
			return response;
		}

		private XulHttpServerResponse execUserObjectCommand(XulHttpServerRequest request, int objectId, String command) {
			return _monitor.execUserObjectCommand(objectId, command, request, this);
		}

		private XulHttpServerResponse listUserObjects(XulHttpServerRequest request) {
			XulHttpServerResponse response = getResponse(request);
			if (_monitor.listUserObjects(request, response)) {
				response.addHeader("Content-Type", "text/xml");
			} else {
				response.setStatus(404)
					.cleanBody();
			}
			return response;
		}

		private XulHttpServerResponse sendKeyEvent(XulHttpServerRequest request, String[] events) {
			XulHttpServerResponse response = getResponse(request);
			if (_monitor.sendKeyEvents(events, request, response)) {
				response.addHeader("Content-Type", "text/xml")
					.writeBody("<result status=\"OK\"/>");
			} else {
				response.setStatus(404);
			}
			return response;
		}

		private XulHttpServerResponse sendMotionEvent(XulHttpServerRequest request, String[] events) {
			return null;
		}

		private XulHttpServerResponse getAssets(XulHttpServerRequest request, String assetsPath) {
			try {
				assetsPath = URLDecoder.decode(assetsPath, "utf-8");
			} catch (UnsupportedEncodingException e) {
				XulLog.e(TAG, e);
				return null;
			}
			InputStream inputStream = null;
			try {
				inputStream = XulWorker.loadData(assetsPath);
				if (inputStream == null) {
					return null;
				}
				return getResponse(request)
					.addHeader("Content-Type", "application/octet- stream")
					.writeBody(inputStream);
			} catch (Exception e) {
				XulLog.e(TAG, e);
				return null;
			} finally {
				try {
					if (inputStream != null) {
						inputStream.close();
					}
				} catch (IOException e) {
					XulLog.e(TAG, e);
				}
			}
		}

		private XulHttpServerResponse dropPageState(XulHttpServerRequest request, int pageId) {
			XulHttpServerResponse response = getResponse(request);
			if (_monitor.popPageState(pageId, true, request, response)) {
				response.addHeader("Content-Type", "text/xml");
				response.writeBody("<result status=\"OK\"/>");
			} else {
				response.setStatus(404);
			}
			return response;
		}

		private XulHttpServerResponse popPageState(XulHttpServerRequest request, int pageId) {
			XulHttpServerResponse response = getResponse(request);
			if (_monitor.popPageState(pageId, false, request, response)) {
				response.addHeader("Content-Type", "text/xml");
				response.writeBody("<result status=\"OK\"/>");
			} else {
				response.setStatus(404);
			}
			return response;
		}

		private XulHttpServerResponse pushPageState(XulHttpServerRequest request, String[] params) {
			XulHttpServerResponse response = getResponse(request);
			if (_monitor.pushPageState(params, request, response)) {
				response.addHeader("Content-Type", "text/xml");
				response.writeBody("<result status=\"OK\"/>");
			} else {
				response.setStatus(404);
			}
			return response;
		}

		// private XulHttpServerResponse setItemData(XulHttpServerRequest request, String[] params) {
		// 	if (params.length < 2) {
		// 		return null;
		// 	}
		// 	XulHttpServerResponse response = getResponse(request);
		// 	String name = params[1];
		// 	String value = params.length > 2 ? params[2] : null;
		// 	if (_monitor.setItemData(
		// 		XulUtils.tryParseInt(params[0]), name, value, request, response)) {
		// 		response.addHeader("Content-Type", "text/xml");
		// 	} else {
		// 		response.setStatus(404);
		// 	}
		// 	return response;
		// }

		private XulHttpServerResponse requestItemFocus(XulHttpServerRequest request, int itemId) {
			XulHttpServerResponse response = getResponse(request);
			if (_monitor.requestItemFocus(itemId, request, response)) {
				response.addHeader("Content-Type", "text/xml");
				response.writeBody("<result status=\"OK\"/>");
			} else {
				response.setStatus(404);
			}
			return response;
		}

		private XulHttpServerResponse setItemAttr(XulHttpServerRequest request, String[] params) {
			if (params.length != 2 && params.length != 3) {
				return null;
			}
			XulHttpServerResponse response = getResponse(request);
			try {
				String name = URLDecoder.decode(params[1], "utf-8");
				String value = params.length > 2 ? URLDecoder.decode(params[2], "utf-8") : null;
				if (_monitor.setItemAttr(XulUtils.tryParseInt(params[0]), name, value, request, response)) {
					response.addHeader("Content-Type", "text/xml");
					response.writeBody("<result status=\"OK\"/>");
				} else {
					response.setStatus(404);
				}
			} catch (Exception e) {
				response.setStatus(501);
				e.printStackTrace(new PrintStream(response.getBodyStream()));
			}
			return response;
		}

		private XulHttpServerResponse setItemStyle(XulHttpServerRequest request, String[] params) {
			if (params.length != 2 && params.length != 3) {
				return null;
			}
			XulHttpServerResponse response = getResponse(request);
			try {
				String name = URLDecoder.decode(params[1], "utf-8");
				String value = params.length > 2 ? URLDecoder.decode(params[2], "utf-8") : null;
				if (_monitor.setItemStyle(XulUtils.tryParseInt(params[0]), name, value, request, response)) {
					response.addHeader("Content-Type", "text/xml");
					response.writeBody("<result status=\"OK\"/>");
				} else {
					response.setStatus(404);
				}
			} catch (Exception e) {
				response.setStatus(501);
				e.printStackTrace(new PrintStream(response.getBodyStream()));
			}
			return response;
		}

		private XulHttpServerResponse removeItemClass(XulHttpServerRequest request, String[] params) {
			if (params.length < 2) {
				return null;
			}
			XulHttpServerResponse response = getResponse(request);
			if (_monitor.removeItemClass(XulUtils.tryParseInt(params[0]), Arrays.copyOfRange(params, 1, params.length), request, response)) {
				response.addHeader("Content-Type", "text/xml");
				response.writeBody("<result status=\"OK\"/>");
			} else {
				response.setStatus(404);
			}
			return response;
		}

		private XulHttpServerResponse addItemClass(XulHttpServerRequest request, String[] params) {
			if (params.length < 2) {
				return null;
			}
			XulHttpServerResponse response = getResponse(request);
			if (_monitor.addItemClass(XulUtils.tryParseInt(params[0]), Arrays.copyOfRange(params, 1, params.length), request, response)) {
				response.addHeader("Content-Type", "text/xml");
				response.writeBody("<result status=\"OK\"/>");
			} else {
				response.setStatus(404);
			}
			return response;
		}

		private XulHttpServerResponse getPageSnapShot(XulHttpServerRequest request, int pageId) throws IOException {
			XulHttpServerResponse response = getResponse(request);
			if (_monitor.getPageSnapshot(pageId, request, response)) {
				response.addHeader("Content-Type", "image/jpeg");
			} else {
				response.setStatus(404);
			}
			return response;
		}

		private XulHttpServerResponse dumpPageContent(XulHttpServerRequest request, int pageId) throws IOException {
			XulHttpServerResponse response = getResponse(request);
			if (_monitor.getPageLayout(pageId, request, response)) {
				response.addHeader("Content-Type", "text/xml");
			} else {
				response.setStatus(404)
					.cleanBody();
			}
			return response;
		}


		private XulHttpServerResponse dumpItemContent(XulHttpServerRequest request, int itemId) {
			XulHttpServerResponse response = getResponse(request);
			if (_monitor.getItemContent(itemId, request, response)) {
				response.addHeader("Content-Type", "text/xml");
			} else {
				response.setStatus(404)
					.cleanBody();
			}
			return response;
		}

		private XulHttpServerResponse unsupportedCommand(XulHttpServerRequest request) {
			ArrayList<IXulDebugCommandHandler> userHandlers = _userHandlers;
			if (userHandlers != null) {
				for (int i = 0, userHandlersSize = userHandlers.size(); i < userHandlersSize; i++) {
					IXulDebugCommandHandler handler = userHandlers.get(i);
					try {
						XulHttpServerResponse response = handler.execCommand(request.path, this, request);
						if (response != null) {
							return response;
						}
					} catch (Exception e) {
						XulLog.e(TAG, e);
					}
				}
			}
			return getResponse(request)
				.setStatus(501)
				.setMessage("Debug API Not implemented");
		}

		private XulHttpServerResponse listPages(XulHttpServerRequest request) {
			XulHttpServerResponse response = getResponse(request);
			if (_monitor.dumpPageList(request, response)) {
				response.addHeader("Content-Type", "text/xml");
			} else {
				response.setStatus(404)
					.cleanBody();
			}
			return response;
		}
	}
}
