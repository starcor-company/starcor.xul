package com.starcor.xulapp.debug;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.starcor.xul.*;
import com.starcor.xul.Prop.XulAction;
import com.starcor.xul.Prop.XulAttr;
import com.starcor.xul.Prop.XulData;
import com.starcor.xul.Prop.XulFocus;
import com.starcor.xul.Prop.XulProp;
import com.starcor.xul.Prop.XulStyle;
import com.starcor.xul.PropMap.IXulPropIterator;
import com.starcor.xul.Render.XulLayerRender;
import com.starcor.xul.Render.XulMassiveRender;
import com.starcor.xul.Render.XulSliderAreaRender;
import com.starcor.xul.Render.XulViewRender;
import com.starcor.xulapp.http.XulHttpServer;
import com.starcor.xulapp.utils.XulLog;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by hy on 2015/12/1.
 */
public class XulDebugMonitor {

	private static final List<String>[] focusOrderMap = new List[]{
		Arrays.asList("dynamic", "priority", "nearby"),
		Arrays.asList("dynamic", "nearby", "priority"),
		Arrays.asList("nearby", "dynamic", "priority"),
		Arrays.asList("nearby", "priority", "dynamic"),
		Arrays.asList("priority", "dynamic", "nearby"),
		Arrays.asList("priority", "nearby", "dynamic")
	};

	private static final String[] xulPropStateMap = new String[]{
		"normal", "focused", "disabled", "-", "visible", "-", "-", "-", "invisible"
	};
	private static final String TAG = XulDebugMonitor.class.getSimpleName();
	WeakHashMap<Object, PageInfo> _pages = new WeakHashMap<Object, PageInfo>();
	LinkedHashMap<Integer, PageInfo> _pagesById = new LinkedHashMap<Integer, PageInfo>();
	Map<Integer, WeakReference<XulView>> _viewMap = new TreeMap<Integer, WeakReference<XulView>>();
	Map<Integer, IXulDebuggableObject> _userObjectMap = new TreeMap<Integer, IXulDebuggableObject>();
	private volatile Bitmap _snapshotBmp;
	private volatile SimpleDateFormat _dateTimeFormat;

	private volatile Paint _debugInfoPaint;
	private volatile WeakReference<XulView> _debugInfoTarget;

	private static String propStateFromId(int state) {
		if (state < 0) {
			return "inline";
		}
		return xulPropStateMap[state];
	}

	public synchronized void onPageCreate(Object page) {
		PageInfo pageInfo = new PageInfo(page, page.hashCode());
		_pages.put(page, pageInfo);
		_pagesById.put(pageInfo.id, pageInfo);
	}

	public synchronized void onPageDestroy(Object page) {
		PageInfo pageInfo = _pages.remove(page);
		if (pageInfo == null) {
			return;
		}
		_pagesById.remove(pageInfo.id);
		pageInfo.status = "destroyed";
	}

	public synchronized void onPageStopped(Object page) {
		PageInfo pageInfo = _pages.get(page);
		if (pageInfo == null) {
			return;
		}
		pageInfo.status = "stopped";
	}

	public synchronized void onPageResumed(Object page) {
		PageInfo pageInfo = _pages.get(page);
		if (pageInfo == null) {
			return;
		}
		pageInfo.onResume();
	}

	public synchronized void onPagePaused(Object page) {
		PageInfo pageInfo = _pages.get(page);
		if (pageInfo == null) {
			return;
		}
		pageInfo.status = "paused";
	}

	public synchronized void onPageRefreshed(Object page, long drawingDuration) {
		PageInfo pageInfo = _pages.get(page);
		if (pageInfo == null) {
			return;
		}
		pageInfo.onPageRefreshed(drawingDuration);
	}

	public synchronized void onPageRenderIsReady(Object page) {
		PageInfo pageInfo = _pages.get(page);
		if (pageInfo == null) {
			return;
		}
		pageInfo.onPageRenderIsReady();
	}

	public void drawDebugInfo(XulRenderContext renderContext, Canvas canvas) {
		if (_debugInfoTarget == null) {
			return;
		}
		XulView xulView = _debugInfoTarget.get();
		if (xulView == null) {
			return;
		}

		RectF updateRc = xulView.getUpdateRc();
		XulViewRender render = xulView.getRender();

		if (render == null) {
			return;
		}

		if (render.getRenderContext() != renderContext) {
			return;
		}

		Rect padding = render.getPadding();
		float viewXScalar = render.getViewXScalar();
		float viewYScalar = render.getViewYScalar();

		while (xulView != null) {
			final XulViewRender parentRender = xulView.getRender();
			if (parentRender instanceof XulLayerRender) {
				XulLayerRender layerRender = (XulLayerRender) parentRender;
				viewXScalar *= parentRender.getViewXScalar();
				viewYScalar *= parentRender.getViewYScalar();
				updateRc.offset(layerRender.getDrawingOffsetX(), layerRender.getDrawingOffsetY());
			}
			xulView = xulView.getParent();
		}

		canvas.save(Canvas.CLIP_SAVE_FLAG);
		canvas.clipRect(updateRc);
		canvas.drawColor(0x70FFFFFF);
		canvas.restore();
		canvas.drawRect(updateRc, _debugInfoPaint);

		updateRc.left += padding.left * viewXScalar;
		updateRc.top += padding.top * viewYScalar;
		updateRc.right -= padding.right * viewXScalar;
		updateRc.bottom -= padding.bottom * viewYScalar;

		if (updateRc.bottom > updateRc.top && updateRc.right > updateRc.left) {
			canvas.save(Canvas.CLIP_SAVE_FLAG);
			canvas.clipRect(updateRc);
			canvas.drawColor(0x700000FF);
			canvas.restore();
		}
	}

	public synchronized boolean dumpPageList(final XulHttpServer.XulHttpServerRequest request, final XulHttpServer.XulHttpServerResponse response) {
		XmlSerializer xmlWriter;
		try {
			XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
			xmlWriter = xmlPullParserFactory.newSerializer();
			xmlWriter.setOutput(response.getBodyStream(), "utf-8");
		} catch (Exception e) {
			XulLog.e(TAG, e);
			return false;
		}

		try {
			xmlWriter.startDocument("utf-8", Boolean.TRUE);
			xmlWriter.startTag(null, "pages");
			for (Map.Entry<Integer, PageInfo> page : _pagesById.entrySet()) {
				PageInfo value = page.getValue();
				Object pageObj = value.page.get();
				if (pageObj == null) {
					continue;
				}

				xmlWriter.startTag(null, "page");
				xmlWriter.attribute(null, "id", Integer.toString(value.id));

				XulDebugAdapter.writePageSpecifiedAttribute(pageObj, xmlWriter);

				xmlWriter.attribute(null, "status", value.status);


				if (value.firstResumedTime >= value.createTime) {
					xmlWriter.attribute(null, "resumeTime", Long.toString(value.firstResumedTime - value.createTime));
				}

				if (value.renderIsReadyTime >= value.createTime) {
					xmlWriter.attribute(null, "readyTime", Long.toString(value.renderIsReadyTime - value.createTime));
				}

				if (value.refreshCount > 0) {
					xmlWriter.attribute(null, "drawing",
						String.format(
							"frames:%d, avg:%.2f, min:%.2f, max:%.2f",
							value.refreshCount,
							(((double) value.totalDrawingDuration) / value.refreshCount / 1000.0),
							((double) value.minDrawingDuration / 1000.0),
							((double) value.maxDrawingDuration / 1000.0)
						)
					);
				}
				xmlWriter.endTag(null, "page");
			}
			xmlWriter.endTag(null, "pages");
			xmlWriter.endDocument();
			xmlWriter.flush();
			return true;
		} catch (IOException e) {
			XulLog.e(TAG, e);
		}
		return false;
	}

	public boolean getPageSnapshot(final int pageId, final XulHttpServer.XulHttpServerRequest request, final XulHttpServer.XulHttpServerResponse response) {
		synchronized (this) {
			if (_snapshotBmp == null) {
				_snapshotBmp = Bitmap.createBitmap(XulManager.getPageWidth(), XulManager.getPageHeight(), Bitmap.Config.ARGB_8888);
			}
		}
		XulPageOpRunnable pageOpRunnable = new XulPageOpRunnable() {
			String curPageId;
			Object pageObj;

			@Override
			boolean beginExec() {
				PageInfo pageInfo = _pageInfo;
				if (pageInfo == null) {
					return false;
				}
				pageObj = pageInfo.page.get();
				if (pageObj == null) {
					return false;
				}
				curPageId = XulDebugAdapter.getPageId(pageObj);
				String etag = XulUtils.calMD5(curPageId + pageInfo.id + pageInfo.refreshTime);
				String ifNoneMatch = request.getHeader("if-none-match");

				String ifModifiedSince = request.getQueryString("if-modified-since");
				if (_dateTimeFormat == null) {
					_dateTimeFormat = new SimpleDateFormat("ccc, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
					_dateTimeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
				}
				Date lastModifiedDate = new Date(pageInfo.getRefreshTime());
				String lastModified = _dateTimeFormat.format(lastModifiedDate);

				if (etag.equals(ifNoneMatch)) {
					response.setStatus(304)
						.addHeader("Last-Modified", lastModified)
						.addHeader("ETag", etag)
						.addHeader("Cache-Control", "private");

					setFinished();
					return true;
				}

				if (!TextUtils.isEmpty(ifModifiedSince)) {
					try {
						Date ifModifiedSinceDate = _dateTimeFormat.parse(ifModifiedSince);
						if (!lastModifiedDate.after(ifModifiedSinceDate)) {
							response.setStatus(304)
								.addHeader("Last-Modified", lastModified)
								.addHeader("ETag", etag)
								.addHeader("Cache-Control", "private");
							setFinished();
							return true;
						}
					} catch (ParseException e) {
					}
				}

				return true;
			}

			@Override
			protected void exec(PageInfo pageInfo, XulPage xulPage, XulView xulView) throws Exception {
				Canvas canvas = new Canvas(_snapshotBmp);
				XulDebugAdapter.drawPage(pageObj, canvas);
				canvas.setBitmap(null);
			}

			@Override
			boolean endExec() {
				int quality = XulUtils.tryParseInt(request.getQueryString("quality"), 55);
				if (_snapshotBmp.compress(Bitmap.CompressFormat.JPEG, quality, response.getBodyStream(200 * 1024))) {
					String etag = XulUtils.calMD5(curPageId + _pageInfo.id + _pageInfo.refreshTime);
					Date lastModifiedDate = new Date(_pageInfo.getRefreshTime());
					String lastModified = _dateTimeFormat.format(lastModifiedDate);

					response.addHeader("Last-Modified", lastModified)
						.addHeader("ETag", etag)
						.addHeader("Cache-Control", "private");
					return true;
				}
				return false;
			}
		};
		synchronized (_snapshotBmp) {
			return execUiOpAndWait(pageId, pageOpRunnable);
		}
	}

	public int xulViewToId(XulView view) {
		int id = view.hashCode();
		_viewMap.put(id, view.getWeakReference());
		return id;
	}

	public XulView xulViewFromId(int id) {
		WeakReference<XulView> ref = _viewMap.get(id);
		if (ref == null) {
			return null;
		}
		XulView xulView = ref.get();
		if (xulView == null) {
			_viewMap.remove(id);
		}
		return xulView;
	}

	public boolean getPageLayout(final int pageId, final XulHttpServer.XulHttpServerRequest request, final XulHttpServer.XulHttpServerResponse response) {
		return execUiOpAndWait(pageId, new XulPageOpRunnable() {
			@Override
			protected void exec(PageInfo pageInfo, XulPage xulPage, XulView xulView) throws Exception {
				if (pageInfo == null) {
					return;
				}

				Object pageObj = pageInfo.page.get();
				if (pageObj == null) {
					return;
				}

				XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
				XmlSerializer writer = xmlPullParserFactory.newSerializer();
				writer.setOutput(response.getBodyStream(), "utf-8");

				writer.startDocument("utf-8", Boolean.TRUE);

				writer.startTag(null, "page");
				writer.attribute(null, "id", String.valueOf(pageInfo.id));

				XulDebugAdapter.writePageSpecifiedAttribute(pageObj, writer);

				writer.attribute(null, "status", pageInfo.status);
				writer.attribute(null, "refreshTime", String.valueOf(pageInfo.getRefreshTime()));

				dumpLayout(xulPage.getLayout(), writer, request.queries);

				writer.endTag(null, "page");
				writer.endDocument();
				writer.flush();
			}
		});
	}

	public boolean getItemContent(final int itemId, final XulHttpServer.XulHttpServerRequest request, final XulHttpServer.XulHttpServerResponse response) {
		return execUiOpAndWait(itemId, new UiOpRunnable() {
			@Override
			boolean beginExec() {
				return !(_xulPage == null && _xulView == null);
			}

			@Override
			protected void exec(PageInfo pageInfo, XulPage xulPage, XulView xulView) throws Exception {
				if (xulView == null) {
					xulView = xulPage.getLayout();
				}

				XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
				XmlSerializer writer = xmlPullParserFactory.newSerializer();
				writer.setOutput(response.getBodyStream(), "utf-8");

				writer.startDocument("utf-8", Boolean.TRUE);
				dumpItem(xulView, writer, request.queries);

				writer.endDocument();
				writer.flush();
			}
		});
	}

	protected boolean execUiOpAndWait(int itemId, UiOpRunnable runnable) {
		synchronized (this) {
			final XulView xulView = xulViewFromId(itemId);
			if (xulView == null) {
				PageInfo pageInfo = _pagesById.get(itemId);
				if (pageInfo == null) {
					return false;
				}
				Object pageObj = pageInfo.page.get();
				runnable.setCurrentPageInfo(pageInfo);

				XulRenderContext xulRenderContext = XulDebugAdapter.getPageRenderContext(pageObj);
				if (xulRenderContext != null) {
					XulPage page = xulRenderContext.getPage();
					runnable.setCurrentPage(page);
				}
			} else {
				runnable.setCurrentView(xulView);
			}
			if (!runnable.beginExec()) {
				return false;
			}
			if (runnable.start()) {
				return true;
			}
		}
		return runnable.waitFinish();
	}

	public boolean addItemClass(int itemId, final String[] classNames, XulHttpServer.XulHttpServerRequest request, XulHttpServer.XulHttpServerResponse response) {
		return execUiOpAndWait(itemId, new XulViewOpRunnable() {
			@Override
			protected void exec(PageInfo _pageInfo, XulPage xulPage, XulView xulView) throws Exception {
				for (String name : classNames) {
					if (xulView.addClass(name)) {
						xulView.resetRender();
					}
				}
			}
		});
	}

	public boolean removeItemClass(int itemId, final String[] classNames, XulHttpServer.XulHttpServerRequest request, XulHttpServer.XulHttpServerResponse response) {
		return execUiOpAndWait(itemId, new XulViewOpRunnable() {
			@Override
			protected void exec(PageInfo _pageInfo, XulPage xulPage, XulView xulView) throws Exception {
				for (String name : classNames) {
					if (xulView.removeClass(name)) {
						xulView.resetRender();
					}
				}
			}
		});
	}

	public boolean setItemStyle(int itemId, final String name, final String value, XulHttpServer.XulHttpServerRequest request, XulHttpServer.XulHttpServerResponse response) {
		return execUiOpAndWait(itemId, new XulViewOpRunnable() {
			@Override
			protected void exec(PageInfo _pageInfo, XulPage xulPage, XulView xulView) throws Exception {
				xulView.setStyle(name, value);
				xulView.resetRender();
			}
		});
	}

	public boolean setItemAttr(int itemId, final String name, final String value, XulHttpServer.XulHttpServerRequest request, XulHttpServer.XulHttpServerResponse response) {
		return execUiOpAndWait(itemId, new XulViewOpRunnable() {
			@Override
			protected void exec(PageInfo _pageInfo, XulPage xulPage, XulView xulView) throws Exception {
				xulView.setAttr(name, value);
				xulView.resetRender();
			}
		});
	}

	public boolean requestItemFocus(int itemId, XulHttpServer.XulHttpServerRequest request, XulHttpServer.XulHttpServerResponse response) {
		return execUiOpAndWait(itemId, new XulPageOpRunnable() {
			@Override
			protected void exec(PageInfo _pageInfo, XulPage xulPage, XulView xulView) throws Exception {
				XulLayout layout = xulPage.getLayout();
				if (layout == null) {
					return;
				}
				layout.requestFocus(xulView);
			}
		});
	}

	private void dumpLayout(XulLayout layout, XmlSerializer writer, LinkedHashMap<String, String> queries) throws Exception {
		if (layout == null) {
			return;
		}
		XmlContentDumper contentDumper = new XmlContentDumper(writer);

		if (queries != null) {
			String noProp = queries.get("skip-prop");
			String withPosition = queries.get("with-position");
			String withBindingData = queries.get("with-binding-data");

			if ("true".equals(noProp)) {
				contentDumper.setNoProp(true);
			}

			if ("true".equals(withPosition)) {
				contentDumper.setNoPosition(false);
			} else {
				contentDumper.setNoPosition(true);
			}

			if ("true".equals(withBindingData)) {
				contentDumper.setNoBindingData(false);
			} else {
				contentDumper.setNoBindingData(true);
			}
		} else {
			contentDumper.setNoPosition(true);
			contentDumper.setNoBindingData(true);
		}

		contentDumper.dumpXulLayout(layout);
	}

	private void dumpItem(XulView view, XmlSerializer writer, LinkedHashMap<String, String> queries) throws Exception {
		if (view == null) {
			return;
		}
		XmlContentDumper contentDumper = new XmlContentDumper(writer);

		if (queries != null) {
			String withChildren = queries.get("with-children");
			String noProp = queries.get("skip-prop");
			String withPosition = queries.get("with-position");
			String withBindingData = queries.get("with-binding-data");
			if ("true".equals(noProp)) {
				contentDumper.setNoProp(true);
			}

			if ("true".equals(withChildren)) {
				contentDumper.setNoChildren(false);
			} else {
				contentDumper.setNoChildren(true);
			}

			if ("true".equals(withPosition)) {
				contentDumper.setNoPosition(false);
			} else {
				contentDumper.setNoPosition(true);
			}

			if ("true".equals(withBindingData)) {
				contentDumper.setNoBindingData(false);
			} else {
				contentDumper.setNoBindingData(true);
			}
		} else {
			contentDumper.setNoChildren(true);
			contentDumper.setNoPosition(true);
			contentDumper.setNoBindingData(true);
		}

		contentDumper.dumpXulView(view);
	}

	public boolean popPageState(int pageId, final boolean discard, XulHttpServer.XulHttpServerRequest request, XulHttpServer.XulHttpServerResponse response) {
		return execUiOpAndWait(pageId, new XulPageOpRunnable() {
			@Override
			protected void exec(PageInfo pageInfo, XulPage xulPage, XulView xulView) throws Exception {
				xulPage.popStates(discard);
			}
		});
	}

	public boolean pushPageState(final String[] params, XulHttpServer.XulHttpServerRequest request, XulHttpServer.XulHttpServerResponse response) {
		return execUiOpAndWait(XulUtils.tryParseInt(params[0]), new XulPageOpRunnable() {
			@Override
			protected void exec(PageInfo pageInfo, XulPage xulPage, XulView xulView) throws Exception {
				Object[] parsedParams = new Object[params.length];

				for (int i = 0, paramsLength = params.length; i < paramsLength; i++) {
					String param = params[i];
					if (TextUtils.isDigitsOnly(param)) {
						XulView view = xulViewFromId(XulUtils.tryParseInt(param));
						parsedParams[i] = view == null ? param : view;
					} else {
						parsedParams[i] = URLDecoder.decode(param, "utf-8");
					}
				}
				xulPage.pushStates(parsedParams);
			}
		});
	}

	public boolean sendKeyEvents(final String[] events, XulHttpServer.XulHttpServerRequest request, XulHttpServer.XulHttpServerResponse response) {
		int topPage = 0;
		for (Map.Entry<Integer, PageInfo> entry : _pagesById.entrySet()) {
			PageInfo pageInfo = entry.getValue();
			if (!"resumed".equals(pageInfo.status)) {
				continue;
			}
			topPage = entry.getKey();
		}
		return execUiOpAndWait(topPage, new XulPageOpRunnable() {
			@Override
			protected void exec(PageInfo pageInfo, XulPage xulPage, XulView xulView) throws Exception {
				Object pageObj = pageInfo.page.get();
				for (String event : events) {
					String[] keys = event.split(",");
					int action = KeyEvent.ACTION_MULTIPLE;
					if ("down".equals(keys[0])) {
						action = KeyEvent.ACTION_DOWN;
					} else if ("up".equals(keys[0])) {
						action = KeyEvent.ACTION_UP;
					} else if ("click".equals(keys[0])) {
						action = KeyEvent.ACTION_MULTIPLE;
					}
					for (int i = 1, keysLength = keys.length; i < keysLength; i++) {
						String key = keys[i];
						int keyCode;
						if ("left".equals(key)) {
							keyCode = KeyEvent.KEYCODE_DPAD_LEFT;
						} else if ("up".equals(key)) {
							keyCode = KeyEvent.KEYCODE_DPAD_UP;
						} else if ("down".equals(key)) {
							keyCode = KeyEvent.KEYCODE_DPAD_DOWN;
						} else if ("right".equals(key)) {
							keyCode = KeyEvent.KEYCODE_DPAD_RIGHT;
						} else if ("enter".equals(key)) {
							keyCode = KeyEvent.KEYCODE_ENTER;
						} else if ("ok".equals(key)) {
							keyCode = KeyEvent.KEYCODE_DPAD_CENTER;
						} else if ("back".equals(key)) {
							keyCode = KeyEvent.KEYCODE_BACK;
						} else if ("menu".equals(key)) {
							keyCode = KeyEvent.KEYCODE_MENU;
						} else {
							keyCode = KeyEvent.keyCodeFromString(key);
						}
						if (XulDebugAdapter.isPageFinished(pageObj)) {
							return;
						}
						if (KeyEvent.ACTION_MULTIPLE == action) {
							XulDebugAdapter.dispatchKeyEvent(pageObj, new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
							XulDebugAdapter.dispatchKeyEvent(pageObj, new KeyEvent(KeyEvent.ACTION_UP, keyCode));
						} else {
							XulDebugAdapter.dispatchKeyEvent(pageObj, new KeyEvent(action, keyCode));
						}
					}
				}
			}
		});
	}

	public synchronized boolean listUserObjects(XulHttpServer.XulHttpServerRequest request, XulHttpServer.XulHttpServerResponse response) {
		try {
			XmlSerializer writer = XmlPullParserFactory.newInstance().newSerializer();
			writer.setOutput(response.getBodyStream(), "utf-8");
			writer.startDocument("utf-8", Boolean.TRUE);
			writer.startTag(null, "objects");
			Iterator<Map.Entry<Integer, IXulDebuggableObject>> iterator = _userObjectMap.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<Integer, IXulDebuggableObject> entry = iterator.next();
				IXulDebuggableObject object = entry.getValue();
				if (!object.isValid()) {
					iterator.remove();
					continue;
				}
				writer.startTag(null, "object");
				writer.attribute(null, "id", String.valueOf(entry.getKey()));
				writer.attribute(null, "name", object.name());
				try {
					object.buildBriefInfo(request, writer);
				} catch (Exception e) {
					XulLog.e(TAG, e);
				}

				writer.endTag(null, "object");
			}
			writer.endTag(null, "objects");
			writer.endDocument();
			writer.flush();
			return true;
		} catch (XmlPullParserException e) {
			XulLog.e(TAG, e);
		} catch (IOException e) {
			XulLog.e(TAG, e);
		}
		return false;
	}

	public synchronized boolean registerDebuggableObject(IXulDebuggableObject obj) {
		_userObjectMap.put(obj.hashCode(), obj);
		return true;
	}

	public boolean getUserObject(int objectId, final XulHttpServer.XulHttpServerRequest request, final XulHttpServer.XulHttpServerResponse response) {
		try {
			final IXulDebuggableObject object;
			synchronized (this) {
				object = _userObjectMap.get(objectId);
				if (object == null || !object.isValid()) {
					_userObjectMap.remove(objectId);
					return false;
				}
			}

			final XmlSerializer writer = XmlPullParserFactory.newInstance().newSerializer();
			writer.setOutput(response.getBodyStream(), "utf-8");
			writer.startDocument("utf-8", Boolean.TRUE);
			writer.startTag(null, "object");

			writer.startTag(null, "object");
			writer.attribute(null, "id", String.valueOf(objectId));
			writer.attribute(null, "name", object.name());

			if (object.runInMainThread()) {
				final Semaphore sem = new Semaphore(0);
				XulDebugAdapter.postToMainLooper(new Runnable() {
					@Override
					public void run() {
						try {
							object.buildDetailInfo(request, writer);
						} catch (Exception e) {
							XulLog.e(TAG, e);
						}
						sem.release();
					}
				});
				sem.tryAcquire(20, TimeUnit.SECONDS);
			} else {
				object.buildDetailInfo(request, writer);
			}
			writer.endDocument();
			writer.flush();
			return true;
		} catch (Exception e) {
			XulLog.e(TAG, e);
		}
		return false;
	}

	public XulHttpServer.XulHttpServerResponse execUserObjectCommand(int objectId, final String command, final XulHttpServer.XulHttpServerRequest request, final XulHttpServer.XulHttpServerHandler serverHandler) {
		try {
			final IXulDebuggableObject object;
			final XulHttpServer.XulHttpServerResponse[] response = new XulHttpServer.XulHttpServerResponse[1];
			synchronized (this) {
				object = _userObjectMap.get(objectId);
				if (object == null || !object.isValid()) {
					_userObjectMap.remove(objectId);
					return null;
				}
			}

			if (object.runInMainThread()) {
				final Semaphore sem = new Semaphore(0);
				XulDebugAdapter.postToMainLooper(new Runnable() {
					@Override
					public void run() {
						try {
							response[0] = object.execCommand(command, request, serverHandler);
						} catch (Exception e) {
							XulLog.e(TAG, e);
						}
						sem.release();
					}
				});
				sem.tryAcquire(20, TimeUnit.SECONDS);
			} else {
				response[0] = object.execCommand(command, request, serverHandler);
			}
			return response[0];
		} catch (Exception e) {
			XulLog.e(TAG, e);
		}
		return null;
	}

	public boolean closePage(int pageId, XulHttpServer.XulHttpServerRequest request, XulHttpServer.XulHttpServerResponse response) {
		return execUiOpAndWait(pageId, new UiOpRunnable() {
			@Override
			boolean beginExec() {
				return _pageInfo != null;
			}

			@Override
			protected void exec(PageInfo pageInfo, XulPage xulPage, XulView xulView) throws Exception {
				Object pageObj = pageInfo.page.get();
				if (pageObj != null) {
					XulDebugAdapter.finishPage(pageObj);
				}
			}
		});
	}

	public synchronized boolean highlightItem(int itemId, XulHttpServer.XulHttpServerRequest request, XulHttpServer.XulHttpServerResponse response) {
		if (itemId == 0) {
			try {
				XulView xulView = _debugInfoTarget.get();
				_debugInfoTarget = null;
				XulViewRender render = xulView.getRender();
				final XulRenderContext renderContext = render.getRenderContext();
				renderContext.uiRun(new Runnable() {
					@Override
					public void run() {
						renderContext.invalidate(null);
					}
				});
			} catch (Exception e) {
			}
			return true;
		}
		XulView xulView = xulViewFromId(itemId);
		XulRenderContext renderContext = null;
		try {
			if (xulView == null) {
				final Object pageObj = _pagesById.get(itemId).page.get();
				final XulRenderContext xulRenderContext = XulDebugAdapter.getPageRenderContext(pageObj);
				xulView = xulRenderContext.getLayout();
			}
			renderContext = xulView.getRender().getRenderContext();
		} catch (Exception e) {
		}
		if (xulView == null || renderContext == null) {
			return false;
		}

		if (_debugInfoPaint == null) {
			_debugInfoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			_debugInfoPaint.setColor(Color.GREEN);
			_debugInfoPaint.setAlpha(200);
			_debugInfoPaint.setStrokeWidth(1.5f);
			_debugInfoPaint.setStyle(Paint.Style.STROKE);

		}
		_debugInfoTarget = xulView.getWeakReference();

		final XulRenderContext finalRenderContext = renderContext;
		renderContext.uiRun(new Runnable() {
			@Override
			public void run() {
				finalRenderContext.invalidate(null);
			}
		});
		return true;
	}

	public boolean fireEvent(int itemId, final String action, final String[] extParams, XulHttpServer.XulHttpServerRequest request, XulHttpServer.XulHttpServerResponse response) {
		return execUiOpAndWait(itemId, new UiOpRunnable() {
			@Override
			boolean beginExec() {
				return !(_pageInfo == null && _xulView == null);
			}

			@Override
			protected void exec(PageInfo pageInfo, XulPage xulPage, XulView xulView) throws Exception {
				if (xulView == null) {
					xulView = xulPage;
				}
				if (extParams != null && extParams.length > 0) {
					xulPage.invokeActionNoPopupWithArgs(xulView, action, (Object[]) extParams);
				} else {
					xulPage.invokeActionNoPopup(xulView, action);
				}
			}
		});
	}

	private static abstract class UiOpRunnable implements Runnable {
		PageInfo _pageInfo;
		XulPage _xulPage;
		XulView _xulView;
		Semaphore _sem;
		volatile boolean _result = false;

		void setCurrentPageInfo(PageInfo pageInfo) {
			_pageInfo = pageInfo;
		}

		void setCurrentPage(XulPage xulPage) {
			_xulPage = xulPage;
		}

		void setCurrentView(XulView xulView) {
			_xulView = xulView;
			if (xulView != null) {
				_xulPage = xulView.getOwnerPage();
			}
		}

		void setFinished() {
			_result = true;
		}

		boolean beginExec() {
			return true;
		}

		@Override
		public void run() {
			try {
				exec(_pageInfo, _xulPage, _xulView);
				_result = true;
			} catch (Exception e) {
				XulLog.e(TAG, e);
				_result = false;
			}
			_sem.release();
		}

		public boolean waitFinish() {
			return _internalWaitFinish() && endExec();
		}

		boolean endExec() {
			return true;
		}

		protected boolean _internalWaitFinish() {
			try {
				return _sem.tryAcquire(20, TimeUnit.SECONDS) && _result;
			} catch (InterruptedException e) {
			}
			return false;
		}

		protected abstract void exec(PageInfo pageInfo, XulPage xulPage, XulView xulView) throws Exception;

		boolean start() {
			if (_result) {
				return true;
			}
			_sem = new Semaphore(0);
			XulDebugAdapter.postToMainLooper(this);
			return false;
		}
	}

	private static abstract class XulViewOpRunnable extends UiOpRunnable {
		@Override
		boolean beginExec() {
			return _xulView != null;
		}

	}

	private static abstract class XulPageOpRunnable extends UiOpRunnable {
		@Override
		boolean beginExec() {
			return _xulPage != null;
		}
	}

	private class XmlContentDumper extends XulArea.XulAreaIterator {
		final XmlSerializer writer;
		ArrayList<String> focusMode = new ArrayList<String>();
		private boolean _noProp = false;
		private boolean _noChildren = false;
		private boolean _noBindingData = true;
		private boolean _noPosition = true;

		XmlContentDumper(XmlSerializer writer) {
			this.writer = writer;
		}

		public void dumpXulLayout(XulLayout layout) {
			try {
				_writeItemHead("layout", layout);
				layout.eachChild(this);
				_writeItemTail("layout");
			} catch (IOException e) {
				XulLog.e(TAG, e);
			}
		}

		public void dumpXulView(XulView view) {
			if (view instanceof XulArea) {
				onXulArea(0, (XulArea) view);
			} else if (view instanceof XulItem) {
				onXulItem(0, (XulItem) view);
			}
		}

		@Override
		public boolean onXulArea(int pos, XulArea area) {
			try {
				_writeItemHead("area", area);
				if (!_noChildren) {
					area.eachChild(this);
				}
				_writeItemTail("area");
			} catch (IOException e) {
				XulLog.e(TAG, e);
			}
			return true;
		}

		private void _writeItemTail(String name) throws IOException {
			writer.endTag(null, name);
		}

		private void _writeItemHead(String name, XulView view) throws IOException {
			writer.startTag(null, name);

			int viewId = xulViewToId(view);
			writer.attribute(null, "id", String.valueOf(viewId));

			String id = view.getId();
			if (!TextUtils.isEmpty(id)) {
				writer.attribute(null, "xulId", id);
			}
			String type = view.getType();
			if (!TextUtils.isEmpty(type)) {
				writer.attribute(null, "type", type);
			}

			String[] allClass = view.getAllClass();
			if (allClass != null) {
				writer.attribute(null, "class", XulUtils.join(",", allClass));
			}
			String binding = view.getBinding();
			if (!TextUtils.isEmpty(binding)) {
				writer.attribute(null, "binding", binding);
			}

			if (view.isFocused()) {
				writer.attribute(null, "focused", "true");
			}

			if (!view.isEnabled()) {
				writer.attribute(null, "disabled", "true");
			}

			if (view instanceof XulArea) {
				XulArea area = (XulArea) view;
				XulView dynamicFocus = area.getDynamicFocus();
				if (dynamicFocus != null && !dynamicFocus.isDiscarded()) {
					writer.attribute(null, "dynamicFocus", String.valueOf(xulViewToId(dynamicFocus)));
				}
			} else if (view instanceof XulLayout) {
				final XulPage ownerPage = view.getOwnerPage();
				final int designPageHeight = ownerPage.getDesignPageHeight();
				final int designPageWidth = ownerPage.getDesignPageWidth();
				final int pageHeight = ownerPage.getPageHeight();
				final int pageWidth = ownerPage.getPageWidth();
				writer.attribute(null, "design", String.format("%dx%d", designPageWidth, designPageHeight));
				writer.attribute(null, "device", String.format("%dx%d", pageWidth, pageHeight));
			}

			if ("massive".equals(type)) {
				XulMassiveRender massiveRender = (XulMassiveRender) view.getRender();
				final int itemNum = massiveRender.getDataItemNum();
				writer.attribute(null, "dataItemNum", String.valueOf(itemNum));
			} else if ("slider".equals(type)) {
				XulSliderAreaRender sliderRender = (XulSliderAreaRender) view.getRender();
				final boolean vertical = sliderRender.isVertical();
				final int scrollPos = sliderRender.getScrollPos();
				final int scrollRange = sliderRender.getScrollRange();
				final int scrollDelta = sliderRender.getScrollDelta();
				writer.attribute(null, "scroll", String.format("%s %d/%d delta:%d", (vertical ? "V" : "H"), scrollPos, scrollRange, scrollDelta));
			}

			if (!_noPosition) {
				XulViewRender render = view.getRender();
				if (render != null && render.getDrawingRect() != null) {
					RectF rc = render.getUpdateRect();
					writer.attribute(null, "position", String.format("(%.2f,%.2f)-(%.2f,%.2f)", rc.left, rc.top, rc.right, rc.bottom));
				}
			}

			if (!_noBindingData) {
				ArrayList<XulDataNode> bindingData = view.getBindingData();
				if (bindingData != null && !bindingData.isEmpty()) {
					writer.startTag(null, "BINDING_DATA");
					writer.attribute(null, "size", String.valueOf(bindingData.size()));
					for (int i = 0, bindingDataSize = bindingData.size(); i < bindingDataSize; i++) {
						XulDataNode data = bindingData.get(i);
						XulDataNode.dumpXulDataNode(data, writer);
					}
					writer.endTag(null, "BINDING_DATA");
				}
			}

			if (!_noProp) {
				view.eachProp(new IXulPropIterator<XulProp>() {
					@Override
					public void onProp(XulProp prop, int state) {
						try {
							if (prop instanceof XulAttr) {
								_writeItemHead("attr", prop, state);
								_writeItemTail("attr");
							} else if (prop instanceof XulStyle) {
								_writeItemHead("style", prop, state);
								_writeItemTail("style");
							} else if (prop instanceof XulData) {
								_writeItemHead("data", prop, state);
								_writeItemTail("data");
							} else if (prop instanceof XulAction) {
								_writeItemHead("action", (XulAction) prop, -1);
								_writeItemTail("action");
							} else if (prop instanceof XulFocus) {
								_writeItemHead("focus", (XulFocus) prop, -1);
								_writeItemTail("focus");
							}
						} catch (IOException e) {
							XulLog.e(TAG, e);
						}
					}
				});
			}
		}

		private void _writeItemHead(String tagName, XulProp attr, int state) throws IOException {
			writer.startTag(null, tagName);
			String name = attr.getName();
			if (!TextUtils.isEmpty(name)) {
				writer.attribute(null, "name", name);
			}

			String binding = attr.getBinding();
			if (!TextUtils.isEmpty(binding)) {
				writer.attribute(null, "binding", binding);
			}

			int priority = attr.getPriority();
			writer.attribute(null, "P", Integer.toHexString(priority));

			writer.attribute(null, "S", propStateFromId(state));

			String value = attr.getStringValue();
			if (!TextUtils.isEmpty(value)) {
				writer.text(value);
			}
		}

		private void _writeItemHead(String tagName, XulAction action, int state) throws IOException {
			writer.startTag(null, tagName);
			String name = action.getName();
			if (!TextUtils.isEmpty(name)) {
				writer.attribute(null, "action", name);
			}

			String type = action.getType();
			if (!TextUtils.isEmpty(type)) {
				writer.attribute(null, "type", type);
			}

			String binding = action.getBinding();
			if (!TextUtils.isEmpty(binding)) {
				writer.attribute(null, "binding", binding);
			}

			int priority = action.getPriority();
			writer.attribute(null, "P", Integer.toHexString(priority));

			writer.attribute(null, "S", propStateFromId(state));

			String value = action.getStringValue();
			if (!TextUtils.isEmpty(value)) {
				writer.text(value);
			}
		}

		private void _writeItemHead(String tagName, XulFocus focus, int state) throws IOException {
			writer.startTag(null, tagName);

			int mode = focus.getFocusMode();
			focusMode.clear();
			int order = (mode & XulFocus.MODE_SEARCH_ORDER_MASK) / 0x1000;
			focusMode.addAll(focusOrderMap[order]);

			if (!focus.hasModeBits(XulFocus.MODE_DYNAMIC)) {
				focusMode.remove("dynamic");
			}
			if (!focus.hasModeBits(XulFocus.MODE_NEARBY)) {
				focusMode.remove("nearby");
			}
			if (!focus.hasModeBits(XulFocus.MODE_PRIORITY)) {
				focusMode.remove("priority");
			}
			if (focus.hasModeBits(XulFocus.MODE_FOCUSABLE)) {
				focusMode.add("focusable");
			}
			if (focus.hasModeBits(XulFocus.MODE_NOFOCUS)) {
				focusMode.remove("focusable");
				focusMode.add("nofocus");
			}
			if (focus.hasModeBits(XulFocus.MODE_LOOP)) {
				focusMode.add("loop");
			}
			if (focus.hasModeBits(XulFocus.MODE_WRAP)) {
				focusMode.add("wrap");
			}

			if (!focusMode.isEmpty()) {
				writer.attribute(null, "mode", XulUtils.join(",", focusMode));
			}

			if (focus.isDefaultFocused()) {
				writer.attribute(null, "focused", "true");
			}

			int focusPriority = focus.getFocusPriority();
			if (focusPriority > 0) {
				writer.attribute(null, "priority", String.valueOf(focusPriority));
			}
		}

		@Override
		public boolean onXulItem(int pos, XulItem item) {
			try {
				_writeItemHead("item", item);
				_writeItemTail("item");
			} catch (IOException e) {
				XulLog.e(TAG, e);
			}
			return true;
		}

		public void setNoProp(boolean noProp) {
			_noProp = noProp;
		}

		public void setNoBindingData(boolean noBindingData) {
			_noBindingData = noBindingData;
		}

		public void setNoPosition(boolean noPosition) {
			_noPosition = noPosition;
		}

		public void setNoChildren(boolean noChildren) {
			_noChildren = noChildren;
		}
	}

	class PageInfo {
		int id;
		WeakReference<Object> page;
		String status;
		long createTime;
		long firstResumedTime;
		long renderIsReadyTime;
		long refreshTime;

		int refreshCount;
		long totalDrawingDuration;
		long maxDrawingDuration;
		long minDrawingDuration = Integer.MAX_VALUE;

		public PageInfo(Object page, int id) {
			this.page = new WeakReference<Object>(page);
			this.id = id;
			this.status = "create";
			this.createTime = XulUtils.timestamp();
		}

		public void onPageRefreshed(long drawingDuration) {
			++refreshCount;
			totalDrawingDuration += drawingDuration;
			if (drawingDuration > maxDrawingDuration) {
				maxDrawingDuration = drawingDuration;
			}
			if (drawingDuration < minDrawingDuration) {
				minDrawingDuration = drawingDuration;
			}
			refreshTime = XulUtils.timestamp();
		}

		public long getRefreshTime() {
			long deltaTime = XulUtils.timestamp() - this.refreshTime;
			return System.currentTimeMillis() - deltaTime;
		}

		public void onResume() {
			status = "resumed";
			if (firstResumedTime == 0) {
				firstResumedTime = XulUtils.timestamp();
			}
		}

		public void onPageRenderIsReady() {
			renderIsReadyTime = XulUtils.timestamp();
		}
	}

}
