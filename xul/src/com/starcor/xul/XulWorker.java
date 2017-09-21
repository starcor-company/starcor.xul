package com.starcor.xul;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.shapes.RoundRectShape;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.starcor.xul.Graphics.BitmapTools;
import com.starcor.xul.Graphics.XulAnimationDrawable;
import com.starcor.xul.Graphics.XulBitmapDrawable;
import com.starcor.xul.Graphics.XulDrawable;
import com.starcor.xul.Utils.XulBufferedInputStream;
import com.starcor.xul.Utils.XulCachedHashMap;
import com.starcor.xul.Utils.XulMemoryOutputStream;
import com.starcor.xul.Utils.XulSimpleStack;

import org.apache.http.conn.ConnectTimeoutException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

/**
 * Created by hy on 2014/5/11.
 */
public class XulWorker {
	private static final String TAG = XulWorker.class.getSimpleName();
	private static final String XUL_DOWNLOAD_WORKER = "XUL Download Worker";
	private static final String XUL_DRAWABLE_WORKER = "XUL Drawable Worker";
	private static final float[] EMPTY_ROUND_RECT_RADIUS = new float[8];

	public static int DOWNLOAD_RESPONSE_UNKNOWN_EXCEPTION = 1900;
	public static int DOWNLOAD_RESPONSE_CONNECT_TIMEOUT = 1901;
	public static int DOWNLOAD_RESPONSE_SOCKET_TIMEOUT = 1902;
	public static int DOWNLOAD_RESPONSE_INVALID_URL = 1903;

	public static int DEFAULT_CONNECT_TIMEOUT = 4 * 1000;
	public static int DEFAULT_READ_TIMEOUT = 8 * 1000;


	private static byte[] _FILENAME_;
	private static byte[] _CONTENT_DISPOSITION_;
	private static byte[] _CONTENT_TYPE_;
	private static byte[] _APPLICATION_OCTET_STREAM_;
	private static byte[] _CR_LF_;

	static {
		try {
			_FILENAME_ = "; filename=\"".getBytes("UTF-8");
			_CONTENT_DISPOSITION_ = "Content-Disposition: form-data; name=\"".getBytes("UTF-8");
			_CONTENT_TYPE_ = "Content-Type: ".getBytes("UTF-8");
			_APPLICATION_OCTET_STREAM_ = "application/octet-stream".getBytes("UTF-8");
			_CR_LF_ = "\r\n".getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}


	public static XulDrawable loadDrawableFromCache(String url) {
		XulDrawable bitmap = null;
		// synchronized (_cachedDrawable) {
		// 	bitmap = _cachedDrawable.get(url);
		// }
		if (bitmap == null) {
			synchronized (_weakCachedDrawable) {
				bitmap = _weakCachedDrawable.get(url);
				if (bitmap != null && bitmap.isRecycled()) {
					_weakCachedDrawable.remove(url);
					return null;
				}
			}
			// if (bitmap != null) {
			// 	_cachedDrawable.put(url, bitmap);
			// }
		}
		return bitmap;
	}

	public static boolean isDrawableCached(String url) {
		// synchronized (_cachedDrawable) {
		// 	return _cachedDrawable.containsKey(url);
		// }
		synchronized (_weakCachedDrawable) {
			return _weakCachedDrawable.containsKey(url);
		}
	}

	public static void removeDrawableCache(String url) {
		// synchronized (_cachedDrawable) {
		// 	_cachedDrawable.remove(url);
		// }
	}

	// 添加的对象生命周期默认为5秒，超时后，添加的对象可能失效
	public static void addDrawableToCache(String url, XulDrawable drawable) {
		addDrawableToCache(url, drawable, 5000);
	}

	public static void addDrawableToCache(String url, XulDrawable drawable, int lifeTimeMS) {
		_addDrawableToCache(url, drawable);
		synchronized (_autoCleanList) {
			_autoCleanList.add(Pair.create(lifeTimeMS + XulUtils.timestamp(), (Object) drawable));
		}
	}

	private static void _cleanAutoCleanList(long timestamp) {
		synchronized (_autoCleanList) {
			for (int i = 0; i < _autoCleanList.size(); ++i) {
				Pair<Long, Object> longObjectPair = _autoCleanList.get(i);
				if (timestamp > longObjectPair.first) {
					_autoCleanList.remove(i);
					++i;
				}
			}
		}
	}

	// 添加的对象生命周期和holder对象相同
	public static void addDrawableToCache(String url, XulDrawable drawable, Object holder) {
		_addDrawableToCache(url, drawable);
		_weakRefObjects.put(holder, drawable);
	}

	private static void _addDrawableToCache(String url, XulDrawable bmp) {
		if (bmp == null || !bmp.cacheable()) {
			return;
		}
		synchronized (_weakCachedDrawable) {
			if (bmp == null) {
				_weakCachedDrawable.remove(url);
			} else {
				_weakCachedDrawable.put(url, bmp);
			}
		}
	}

	public static void clearDrawableCachePermanently() {
		synchronized (_weakCachedDrawable) {
			_weakCachedDrawable.clear();
		}
	}

	public static void removeDrawableCachePermanently(String url) {
		if ("*".equals(url)) {
			clearDrawableCachePermanently();
			return;
		}

		XulDrawable bmp2 = null;

		synchronized (_weakCachedDrawable) {
			bmp2 = _weakCachedDrawable.remove(url);
		}

		if (bmp2 != null) {
			if (bmp2 instanceof XulBitmapDrawable) {
				BitmapTools.recycleBitmap(XulBitmapDrawable.detachBitmap((XulBitmapDrawable) bmp2));
				bmp2.recycle();
			}
		}
	}

	public interface IXulWorkerHandler {
		InputStream getAssets(String path);

		InputStream getAppData(String path);

		String resolvePath(DownloadItem downloadItem, String path);

		InputStream loadCachedData(String path);

		boolean storeCachedData(String path, InputStream stream);

		String calCacheKey(String url);

		boolean preloadImage(DrawableItem drawableItem, Rect rcOut);

		Bitmap preprocessImage(DrawableItem drawableItem, Bitmap bitmap);
	}

	private static IXulWorkerHandler _handler;

	public static void setHandler(IXulWorkerHandler handler) {
		_handler = handler;
	}

	private static String _internalResolvePath(DownloadItem downloadItem, String path) {
		try {
			String resolvedPath = downloadItem.__ownerDownloadHandler.resolve(downloadItem, path);
			if (resolvedPath != null) {
				return resolvedPath;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if (_handler == null) {
				return null;
			}
			return _handler.resolvePath(downloadItem, path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static boolean waitPendingStream(InputStream stream) {
		if (stream == null) {
			return false;
		}
		if (stream instanceof XulPendingInputStream) {
			// 如果是可挂起流，则挂起等待
			XulPendingInputStream pendingInputStream = (XulPendingInputStream) stream;
			pendingInputStream.checkPending();
			return pendingInputStream.isReady();
		}
		return true;
	}

	private static InputStream _internalGetAssets(DownloadItem downloadItem, String path) {
		InputStream stream = null;
		try {
			stream = downloadItem.__ownerDownloadHandler.getAssets(downloadItem, path);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (stream == null && _handler != null) {
			// 如果通过下载项关联的handler无法获取相关信息则通过全局handler处理
			try {
				return _handler.getAssets(path);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (waitPendingStream(stream)) {
			return stream;
		}
		return null;
	}

	private static InputStream _internalGetAppData(DownloadItem downloadItem, String path) {
		InputStream stream = null;
		try {
			stream = downloadItem.__ownerDownloadHandler.getAppData(downloadItem, path);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (stream == null && _handler != null) {
			try {
				stream = _handler.getAppData(path);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (waitPendingStream(stream)) {
			return stream;
		}
		return null;
	}

	private static String _internalCalCacheKey(DownloadItem downloadItem, String url) {
		if (_handler == null) {
			return null;
		}
		try {
			return _handler.calCacheKey(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static InputStream _internalLoadCachedData(String path) {
		if (_handler == null) {
			return null;
		}
		try {
			return _handler.loadCachedData(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static boolean _internalStoreCachedData(String path, InputStream stream) {
		if (_handler == null) {
			return false;
		}
		try {
			return _handler.storeCachedData(path, stream);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private static Bitmap _internalPreprocessImage(DrawableItem drawableItem, Bitmap bitmap) {
		if (_handler == null) {
			return null;
		}
		try {
			return _handler.preprocessImage(drawableItem, bitmap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static boolean _internalPreloadImage(DrawableItem drawableItem, Rect rcOut) {
		if (_handler == null) {
			return false;
		}
		try {
			return _handler.preloadImage(drawableItem, rcOut);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	////////////////////////////////////////////////////////////////
	public static class DownloadItem {
		int __retryCounter = 0;
		IXulWorkItemSource __ownerDownloadHandler = null;
		String __cacheKey = null;
		String __resolvedPath = "";

		public String url;
		// 是否直接从源读取文件
		public boolean isDirect = false;
		// 内容是否可缓存文件
		public boolean noFileCache = false;

		public String getInternalResolvedPath() {
			return __resolvedPath;
		}

		public String getInternalCacheKey() {
			return __cacheKey;
		}
	}

	public interface IXulWorkItemSource {
		DownloadItem getDownloadItem();

		void onDownload(DownloadItem item, InputStream data);

		DrawableItem getDrawableItem();

		void onDrawableLoaded(DrawableItem item, XulDrawable bmp);

		String resolve(DownloadItem item, String path);

		InputStream getAssets(DownloadItem item, String path);

		InputStream getAppData(DownloadItem item, String path);
	}

	private static final ArrayList<WeakReference<IXulWorkItemSource>> _downloader = new ArrayList<WeakReference<IXulWorkItemSource>>();

	public static void registerDownloader(IXulWorkItemSource downloader) {
		synchronized (_downloader) {
			_downloader.add(0, new WeakReference<IXulWorkItemSource>(downloader));
		}
		_notifyDownloadWorker();
	}

	public static void unregisterDownloader(IXulWorkItemSource downloader) {
		synchronized (_downloader) {
			for (int i = 0, downloaderSize = _downloader.size(); i < downloaderSize; i++) {
				WeakReference<IXulWorkItemSource> weakDownloader = _downloader.get(i);
				if (weakDownloader.get() == downloader) {
					_downloader.remove(i);
					break;
				}
			}
		}
	}

	private static class _syncDownloadItem extends DownloadItem {
		IXulWorkItemSource syncSource = new IXulWorkItemSource() {
			@Override
			public DownloadItem getDownloadItem() {
				return null;
			}

			@Override
			public void onDownload(DownloadItem item, InputStream data) {
				((_syncDownloadItem) item)._result = data;
			}

			@Override
			public DrawableItem getDrawableItem() {
				return null;
			}

			@Override
			public void onDrawableLoaded(DrawableItem item, XulDrawable bmp) {
			}

			@Override
			public String resolve(DownloadItem item, String path) {
				if (_nextSource != null) {
					return _nextSource.resolve(item, path);
				}
				return null;
			}

			@Override
			public InputStream getAssets(DownloadItem item, String path) {
				if (_nextSource != null) {
					return _nextSource.getAssets(item, path);
				}
				return null;
			}

			@Override
			public InputStream getAppData(DownloadItem item, String path) {
				if (_nextSource != null) {
					return _nextSource.getAppData(item, path);
				}
				return null;
			}
		};
		private InputStream _result;
		private IXulWorkItemSource _nextSource;

		_syncDownloadItem(String xulPath, IXulWorkItemSource nextSource) {
			this.url = xulPath;
			this.__ownerDownloadHandler = syncSource;
			this._nextSource = nextSource;
		}
	}

	public static InputStream loadData(String xulPath, IXulWorkItemSource workItemSource, boolean noCache, String... extHeaders) {
		_syncDownloadItem item = new _syncDownloadItem(xulPath, workItemSource);
		item.noFileCache = noCache;
		item.isDirect = noCache;
		_doDownload(new _downloadContext(), item, new XulDownloadParams(false, null, extHeaders));
		return item._result;
	}

	public static InputStream loadData(String xulPath, IXulWorkItemSource workItemSource, String... extHeaders) {
		return loadData(xulPath, workItemSource, false, extHeaders);
	}

	public static InputStream loadData(String xulPath, boolean noCache, String... extHeaders) {
		return loadData(xulPath, null, noCache, extHeaders);
	}

	public static InputStream loadData(String xulPath, String... extHeaders) {
		return loadData(xulPath, false, extHeaders);
	}

	public static InputStream postData(String xulPath, IXulWorkItemSource workItemSource, boolean noCache, byte[] postData, String... extHeaders) {
		_syncDownloadItem item = new _syncDownloadItem(xulPath, workItemSource);
		item.noFileCache = noCache;
		item.isDirect = noCache;
		_doDownload(new _downloadContext(), item, new XulDownloadParams(true, postData, extHeaders));
		return item._result;
	}

	public static InputStream postData(String xulPath, IXulWorkItemSource workItemSource, byte[] postData, String... extHeaders) {
		return postData(xulPath, workItemSource, false, postData, extHeaders);
	}

	public static InputStream postData(String xulPath, boolean noCache, byte[] postData, String... extHeaders) {
		return postData(xulPath, null, noCache, postData, extHeaders);
	}

	public static InputStream postData(String xulPath, byte[] postData, String... extHeaders) {
		return postData(xulPath, false, postData, extHeaders);
	}

	public static InputStream postForm(String xulPath, XulFormEntity... formEntities) {
		_syncDownloadItem item = new _syncDownloadItem(xulPath, null);
		item.noFileCache = true;
		item.isDirect = true;
		XulDownloadParams params = new XulDownloadParams(formEntities);
		_doDownload(new _downloadContext(), item, params);
		return item._result;
	}

	public static InputStream loadData(String xulPath, boolean noCache, XulDownloadParams params) {
		_syncDownloadItem item = new _syncDownloadItem(xulPath, null);
		item.noFileCache = noCache;
		item.isDirect = noCache;
		_doDownload(new _downloadContext(), item, params);
		return item._result;
	}

	////////////////////////////////////////////////////////////////
	public static class DrawableItem extends DownloadItem {
		IXulWorkItemSource __ownerDrawableHandler = null;
		InputStream __dataStream = null;

		public int width = 0;   // 加载宽度，0为自动（根据图片原始尺寸比例缩放）
		public int height = 0;  // 加载高度，0为自动（根据图片原始尺寸比例缩放）
		public float scalarX = 1.0f; // screen scalar-x
		public float scalarY = 1.0f; // screen scalar-y
		public boolean reusable = false;
		public Bitmap.Config pixFmt = XulManager.DEF_PIXEL_FMT;

		public void setRoundRect(float radiusX, float radiusY) {
			_roundRectRadius = new float[2];
			_roundRectRadius[0] = radiusX;
			_roundRectRadius[1] = radiusY;
		}

		public void setRoundRect(float radiusLT, float radiusRT, float radiusRB, float radiusLB) {
			setRoundRect(radiusLT, radiusLT,
				radiusRT, radiusRT,
				radiusRB, radiusRB,
				radiusLB, radiusLB);
		}

		public void setRoundRect(float radiusLTX, float radiusLTY,
		                         float radiusRTX, float radiusRTY,
		                         float radiusRBX, float radiusRBY,
		                         float radiusLBX, float radiusLBY
		) {

			_roundRectRadius = new float[8];
			_roundRectRadius[0] = radiusLTX;
			_roundRectRadius[1] = radiusLTY;
			_roundRectRadius[2] = radiusRTX;
			_roundRectRadius[3] = radiusRTY;
			_roundRectRadius[4] = radiusRBX;
			_roundRectRadius[5] = radiusRBY;
			_roundRectRadius[6] = radiusLBX;
			_roundRectRadius[7] = radiusLBY;
		}

		public void setRoundRect(float[] roundRadius) {
			_roundRectRadius = roundRadius;
		}


		float[] _roundRectRadius;
		public float shadowSize = 0;
		public int shadowColor = 0xFF000000;
		public int target_width = 0;    // 显示目标宽度（仅图片处理圆角时使用）
		public int target_height = 0;   // 显示目标高度（仅图片处理圆角时使用）
	}

	// 挂起的任务
	private static final ArrayList<DownloadItem> _pendingDownloadTask = new ArrayList<DownloadItem>();
	private static final ArrayList<DrawableItem> _pendingDrawableTask = new ArrayList<DrawableItem>();

	// 执行计划列表
	private static final XulCachedHashMap<String, ArrayList<DownloadItem>> _scheduledDownloadTask = new XulCachedHashMap<String, ArrayList<DownloadItem>>();
	private static final XulCachedHashMap<String, ArrayList<DrawableItem>> _scheduledDrawableTask = new XulCachedHashMap<String, ArrayList<DrawableItem>>();
	// 挂起的执行计划，图片解码失败时要挂起当前图片的所有执行计划
	private static final XulCachedHashMap<String, ArrayList<DrawableItem>> _pendingScheduledImagedTask = new XulCachedHashMap<String, ArrayList<DrawableItem>>();

	// 图片缓冲
	// private static HashMap<String, XulDrawable> _cachedDrawable = new HashMap<String, XulDrawable>();
	private static class WeakDrawableCache {
		XulCachedHashMap<String, WeakReference<XulDrawable>> _cache = new XulCachedHashMap<String, WeakReference<XulDrawable>>();

		public XulDrawable get(String key) {
			WeakReference<XulDrawable> xulDrawableWeakReference = _cache.get(key);
			if (xulDrawableWeakReference == null) {
				return null;
			}
			XulDrawable xulDrawable = xulDrawableWeakReference.get();
			return xulDrawable;
		}

		private WeakReference<XulDrawable> getWeakReference(XulDrawable xulDrawable) {
			return new WeakReference<XulDrawable>(xulDrawable);
		}

		public void put(String key, XulDrawable val) {
			_cache.put(key, getWeakReference(val));
		}

		public XulDrawable remove(String key) {
			WeakReference<XulDrawable> xulDrawableWeakReference = _cache.remove(key);
			if (xulDrawableWeakReference == null) {
				return null;
			}
			XulDrawable xulDrawable = xulDrawableWeakReference.get();
			xulDrawableWeakReference.clear();
			return xulDrawable;
		}

		public void clear() {
			_cache.clear();
		}

		public boolean containsKey(String key) {
			return _cache.containsKey(key);
		}
	}

	private static final WeakDrawableCache _weakCachedDrawable = new WeakDrawableCache();

	// 添加到加载计划中
	// 返回true表示相同任务已经在计划中，任务将由其它线程处理
	// 返回false表示任务未处理，必须立即处理
	private static boolean _addToDownloadSchedule(DownloadItem item) {
		synchronized (_scheduledDownloadTask) {
			ArrayList<DownloadItem> downloadItems = _scheduledDownloadTask.get(item.url);
			if (downloadItems == null) {
				downloadItems = new ArrayList<DownloadItem>();
				_scheduledDownloadTask.put(item.url, downloadItems);
			}
			boolean pending = !downloadItems.isEmpty();
			downloadItems.add(item);
			return pending;
		}
	}

	private static void _finishSchedule(DownloadItem item, InputStream stream) {
		ArrayList<DownloadItem> downloadItems;
		synchronized (_scheduledDownloadTask) {
			downloadItems = _scheduledDownloadTask.remove(item.url);
		}
		for (DownloadItem downloadItem : downloadItems) {
			try {
				downloadItem.__ownerDownloadHandler.onDownload(downloadItem, stream);
			} catch (Exception e) {
				e.printStackTrace();
			}
			downloadItem.__ownerDownloadHandler = null;
		}
	}

	private static boolean _addToImageSchedule(DrawableItem item) {
		if (item.reusable) {
			return false;
		}
		synchronized (_scheduledDrawableTask) {
			String imageKey = item.url;
			ArrayList<DrawableItem> drawableItems = _scheduledDrawableTask.get(imageKey);
			boolean pending = false;
			if (drawableItems == null) {
				drawableItems = _pendingScheduledImagedTask.remove(imageKey);
				if (drawableItems == null) {
					drawableItems = new ArrayList<DrawableItem>();
				}
				_scheduledDrawableTask.put(imageKey, drawableItems);
			} else {
				pending = !drawableItems.isEmpty();
			}
			drawableItems.add(item);
			return pending;
		}
	}

	private static void _removeAndHangupDrawableSchedule(DrawableItem item) {
		synchronized (_scheduledDrawableTask) {
			String imageKey = item.url;
			ArrayList<DrawableItem> drawableItems = _scheduledDrawableTask.remove(imageKey);
			if (drawableItems == null) {
				return;
			}
			drawableItems.remove(item);
			_pendingScheduledImagedTask.put(imageKey, drawableItems);
		}
	}

	private static void _finishSchedule(DrawableItem item, XulDrawable bmp) {
		if (item.reusable) {
			if (item.__dataStream != null) {
				try {
					item.__dataStream.close();
					item.__dataStream = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				item.__ownerDrawableHandler.onDrawableLoaded(item, bmp);
			} catch (Exception e) {
				e.printStackTrace();
			}
			item.__ownerDownloadHandler = null;
			item.__ownerDrawableHandler = null;
			return;
		}

		ArrayList<DrawableItem> drawableItems;
		synchronized (_scheduledDrawableTask) {
			drawableItems = _scheduledDrawableTask.remove(item.url);
		}
		_addDrawableToCache(item.url, bmp);
		if (item.__dataStream != null) {
			try {
				item.__dataStream.close();
				item.__dataStream = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for (DrawableItem drawableItem : drawableItems) {
			try {
				drawableItem.__ownerDrawableHandler.onDrawableLoaded(drawableItem, bmp);
			} catch (Exception e) {
				e.printStackTrace();
			}
			drawableItem.__ownerDownloadHandler = null;
			drawableItem.__ownerDrawableHandler = null;
		}
	}
	////////////////////////////////////////////////////////////////

	static volatile Thread[] _downloadWorkers = new Thread[8];
	static volatile Thread[] _drawableWorkers = new Thread[2];

	static final Object _drawableWorkerWaitObj = new Object();
	static final Object _downloadWorkerWaitObj = new Object();

	private static final LinkedList<Pair<Long, Object>> _autoCleanList = new LinkedList<Pair<Long, Object>>();
	private static WeakHashMap<Object, Object> _weakRefObjects = new WeakHashMap<Object, Object>();

	private static volatile long _suspendTime = 0;
	private static volatile long _suspendBegin = 0;    // 暂停开始时间
	private static final int MAX_WORKER_SUSPEND_DURATION = 100;     // 最大连续暂停时间

	public static void suspendDrawableWorker(int ms) {
		long timestamp = XulUtils.timestamp();
		long suspendTime = timestamp + ms;
		if (suspendTime > _suspendTime) {
			if (_suspendTime == 0) {
				_suspendBegin = timestamp;
			}
			_suspendTime = suspendTime;
		}
	}

	public static void resumeDrawableWorkers() {
		if (_suspendTime == 0) {
			return;
		}
		_suspendTime = 0;
		_suspendBegin = 0;
		if (!_pendingDrawableTask.isEmpty()) {
			synchronized (_pendingDrawableTask) {
				if (!_pendingDrawableTask.isEmpty()) {
					_notifyDrawableWorker();
				}
			}
		}
	}

	private static boolean isDrawableWorkersSuspended() {
		long timestamp = XulUtils.timestamp();
		if (_suspendTime > 0 && timestamp >= _suspendTime) {
			_suspendTime = 0;
			_suspendBegin = 0;
			return false;
		}
		if (timestamp - _suspendBegin >= MAX_WORKER_SUSPEND_DURATION) {
			_suspendBegin = timestamp;
			return false;
		}
		return _suspendTime > 0;
	}

	static IXulWorkItemSource _internalDrawableDownloadHandler = new IXulWorkItemSource() {
		@Override
		public DownloadItem getDownloadItem() {
			return null;
		}

		@Override
		public void onDownload(DownloadItem item, InputStream data) {
			assert item instanceof DrawableItem;
			DrawableItem imgItem = (DrawableItem) item;
			imgItem.__dataStream = data;
			_addPendingDrawableItem(imgItem);
		}

		@Override
		public DrawableItem getDrawableItem() {
			return null;
		}

		@Override
		public void onDrawableLoaded(DrawableItem item, XulDrawable bmp) {
		}

		@Override
		public String resolve(DownloadItem item, String path) {
			assert item instanceof DrawableItem;
			DrawableItem imgItem = (DrawableItem) item;
			return imgItem.__ownerDrawableHandler.resolve(item, path);
		}

		@Override
		public InputStream getAssets(DownloadItem item, String path) {
			assert item instanceof DrawableItem;
			DrawableItem imgItem = (DrawableItem) item;
			return imgItem.__ownerDrawableHandler.getAssets(item, path);
		}

		@Override
		public InputStream getAppData(DownloadItem item, String path) {
			assert item instanceof DrawableItem;
			DrawableItem imgItem = (DrawableItem) item;
			return imgItem.__ownerDrawableHandler.getAppData(item, path);
		}
	};

	static void _threadWait(int ms) {
		try {
			String name = Thread.currentThread().getName();
			if (XUL_DOWNLOAD_WORKER.equals(name)) {
				synchronized (_downloadWorkerWaitObj) {
					_downloadWorkerWaitObj.wait(ms);
				}
			}
			if (XUL_DRAWABLE_WORKER.equals(name)) {
				synchronized (_drawableWorkerWaitObj) {
					_drawableWorkerWaitObj.wait(ms);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void _notifyDrawableWorker() {
		try {
			synchronized (_drawableWorkerWaitObj) {
				_drawableWorkerWaitObj.notify();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void _notifyAllDrawableWorker() {
		try {
			synchronized (_drawableWorkerWaitObj) {
				_drawableWorkerWaitObj.notifyAll();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void _notifyDownloadWorker() {
		try {
			synchronized (_downloadWorkerWaitObj) {
				_downloadWorkerWaitObj.notify();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void _notifyAllDownloadWorker() {
		try {
			synchronized (_downloadWorkerWaitObj) {
				_downloadWorkerWaitObj.notifyAll();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void _addPendingDrawableItem(DrawableItem item) {
		synchronized (_pendingDrawableTask) {
			_pendingDrawableTask.add(item);
		}
		_notifyDrawableWorker();
	}

	private static void _addPendingDownloadTask(DownloadItem item) {
		synchronized (_pendingDownloadTask) {
			_pendingDownloadTask.add(item);
		}
	}

	static DownloadItem _getPendingDownloadItem() {
		do {
			try {
				synchronized (_pendingDownloadTask) {
					if (!_pendingDownloadTask.isEmpty()) {
						return _pendingDownloadTask.remove(0);
					}
				}
				synchronized (_pendingDrawableTask) {
					if (_pendingDrawableTask.size() > 5) {
						return null;
					}
				}

				for (int i = 0; ; i++) {
					IXulWorkItemSource xulDrawableLoader;
					synchronized (_downloader) {
						if (i >= _downloader.size()) {
							break;
						}
						WeakReference<IXulWorkItemSource> weakDownloader = _downloader.get(i);
						xulDrawableLoader = weakDownloader.get();
						if (xulDrawableLoader == null) {
							_downloader.remove(i);
							--i;
							continue;
						}
					}
					DrawableItem drawableItem = xulDrawableLoader.getDrawableItem();
					if (drawableItem != null) {
						if (TextUtils.isEmpty(drawableItem.__cacheKey)) {
							drawableItem.__cacheKey = _internalCalCacheKey(drawableItem, drawableItem.url);
						}
						drawableItem.__ownerDownloadHandler = _internalDrawableDownloadHandler;
						drawableItem.__ownerDrawableHandler = xulDrawableLoader;

						if (drawableItem.reusable) {
							return drawableItem;
						}

						XulDrawable bitmap = loadDrawableFromCache(drawableItem.url);
						if (bitmap != null) {
							try {
								drawableItem.__ownerDrawableHandler.onDrawableLoaded(drawableItem, bitmap);
							} catch (Exception e) {
								e.printStackTrace();
							}
							drawableItem.__ownerDrawableHandler = null;
							drawableItem.__ownerDownloadHandler = null;
							drawableItem.__dataStream = null;
							continue;
						}
						return drawableItem;
					}
				}

				for (int i = 0; ; i++) {
					IXulWorkItemSource xulDownloader;
					synchronized (_downloader) {
						if (i >= _downloader.size()) {
							break;
						}

						WeakReference<IXulWorkItemSource> weakDownloader = _downloader.get(i);
						xulDownloader = weakDownloader.get();
						if (xulDownloader == null) {
							_downloader.remove(i);
							--i;
							continue;
						}
					}
					DownloadItem downloadItem = xulDownloader.getDownloadItem();
					if (downloadItem != null) {
						downloadItem.__ownerDownloadHandler = xulDownloader;
						return downloadItem;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} while (false);
		return null;
	}

	static AtomicInteger _workerCounter = new AtomicInteger(0);
	private static SSLContext _sslCtx = null;
	static acceptAllHostnameVerifier _hostVerifier = null;

	static class _downloadContext {
		Pattern assetsPat = Pattern.compile("^file:///\\.assets/(.+)$");
		Pattern appDataPat = Pattern.compile("^file:///\\.app/(.+)$");
		byte downloadBuffer[] = new byte[1024];
	}

	static void _downloadWorkerRun() {
		_downloadContext ctx = new _downloadContext();

		while (_downloadWorkers != null) {
			boolean mainWorker = _workerCounter.compareAndSet(0, 1);
			DownloadItem downloadItem = _getPendingDownloadItem();
			if (mainWorker) {
				long timestamp = XulUtils.timestamp();
				_cleanAutoCleanList(timestamp);
				BitmapTools.cleanRecycledBitmaps(timestamp);
				// Log.d(TAG, "MainWorker " + Thread.currentThread().getName() + " - " + Thread.currentThread().getId());
			}
			if (downloadItem == null) {
				if (mainWorker) {
					_threadWait(50);
					_workerCounter.set(0);
				} else {
					_threadWait(Integer.MAX_VALUE);
				}
				continue;
			} else if (mainWorker) {
				_workerCounter.set(0);
				_notifyDownloadWorker();
			}

			_doDownload(ctx, downloadItem);
		}
	}

	public static class XulDownloadOutputBuffer extends XulMemoryOutputStream {
		public XulDownloadOutputBuffer(int initialCapacity) {
			super(initialCapacity);
		}

		@Override
		public void onClose() {
			recycleDownloadBuffer(this);
		}
	}

	private static XulSimpleStack<XulDownloadOutputBuffer> _downloadBufferList = new XulSimpleStack<XulDownloadOutputBuffer>(16);

	synchronized private static void recycleDownloadBuffer(XulDownloadOutputBuffer buf) {
		_downloadBufferList.push(buf);
	}

	synchronized public static XulDownloadOutputBuffer obtainDownloadBuffer(int initialCapacity) {
		XulDownloadOutputBuffer buf = _downloadBufferList.pop();
		if (buf == null) {
			return new XulDownloadOutputBuffer(initialCapacity);
		}
		buf.reset(initialCapacity);
		return buf;
	}

	private static void _doDownload(_downloadContext ctx, DownloadItem downloadItem) {
		_doDownload(ctx, downloadItem, null);
	}

	private static void _doDownload(_downloadContext ctx, DownloadItem downloadItem, XulDownloadParams params) {
		Pattern assetsPat = ctx.assetsPat;
		Pattern appDataPat = ctx.appDataPat;
		byte downloadBuffer[] = ctx.downloadBuffer;

		String downloadUrl = downloadItem.url;
		String cacheKey = downloadItem.__cacheKey;
		boolean post = false;
		byte[] postData = null;
		XulFormEntity[] formEntities = null;
		String[] extHeaders = null;
		int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
		int readTimeout = DEFAULT_READ_TIMEOUT;
		if (params != null) {
			post = params.post;
			postData = params.postBody;
			formEntities = params.formEntities;
			extHeaders = params.extHeaders;
			if (params.connectTimeout > 500) {
				connectTimeout = params.connectTimeout;
			}
			if (params.readTimeout > 500) {
				readTimeout = params.readTimeout;
			}
		}

		if (TextUtils.isEmpty(downloadUrl)) {
			return;
		}

		if (downloadUrl.startsWith("@color:")) {
			_notifyDownloadResult(downloadItem, null);
			return;
		}

		String resolvedPath;
		while (!TextUtils.isEmpty(resolvedPath = _internalResolvePath(downloadItem, downloadUrl))) {
			downloadUrl = resolvedPath;
		}

		if (!downloadUrl.equals(downloadItem.__resolvedPath)) {
			downloadItem.__resolvedPath = downloadUrl;
			cacheKey = null;
		}

		if (TextUtils.isEmpty(cacheKey)) {
			downloadItem.__cacheKey = cacheKey = _internalCalCacheKey(downloadItem, downloadUrl);
			if (XulManager.DEBUG_XUL_WORKER) {
				Log.d(TAG, "cal cache key:" + cacheKey + " / " + downloadUrl);
			}
		}

		if (TextUtils.isEmpty(cacheKey)) {
			if (XulManager.DEBUG_XUL_WORKER) {
				Log.d(TAG, "invalid cache key:" + cacheKey + " / " + downloadUrl);
			}
			cacheKey = downloadUrl;
		}

		Matcher matcher = assetsPat.matcher(downloadUrl);
		if (matcher.matches()) {
			String filePath = matcher.group(1);
			if (XulManager.DEBUG_XUL_WORKER) {
				Log.d(TAG, "load assets data:" + filePath);
			}
			String assetsCacheKey = "assets-" + XulUtils.calMD5(filePath);
			InputStream inputStream = null;
			XulUtils.ticketMarker tm = null;
			if (XulManager.PERFORMANCE_BENCH) {
				tm = new XulUtils.ticketMarker("load assets ", true);
				tm.mark();
			}
			if (!downloadItem.isDirect) {
				inputStream = _internalLoadCachedData(assetsCacheKey);
				if (tm != null) {
					tm.mark("cache[" + inputStream + "]");
				}
			}
			if (inputStream == null) {
				int queryStringPartPos = filePath.indexOf('?');
				int fragmentPartPos = filePath.indexOf('#');
				if (queryStringPartPos > 0) {
					filePath = filePath.substring(0, queryStringPartPos);
				} else if (fragmentPartPos > 0) {
					filePath = filePath.substring(0, fragmentPartPos);
				}
				inputStream = _internalGetAssets(downloadItem, filePath);
				if (inputStream != null && !downloadItem.noFileCache) {
					if (tm != null) {
						tm.mark("assets[" + inputStream + "]");
					}
					_internalStoreCachedData(assetsCacheKey, inputStream);
					if (tm != null) {
						tm.mark("load[" + inputStream + "]");
					}
					try {
						inputStream.reset();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			if (tm != null) {
				tm.mark("finish[" + inputStream + "]");
				Log.d(TAG, tm.toString());
			}
			_notifyDownloadResult(downloadItem, inputStream);
			return;
		}

		matcher = appDataPat.matcher(downloadUrl);
		if (matcher.matches()) {
			String filePath = matcher.group(1);
			if (XulManager.DEBUG_XUL_WORKER) {
				Log.d(TAG, "load app data:" + filePath);
			}
			InputStream inputStream = _internalGetAppData(downloadItem, filePath);
			_notifyDownloadResult(downloadItem, inputStream);
			return;
		}

		String itemKey = cacheKey;
		String localCacheKey = XulUtils.calMD5(itemKey);

		if (!downloadItem.isDirect) {
			InputStream cachedStream = _internalLoadCachedData(localCacheKey);
			if (XulManager.DEBUG_XUL_WORKER) {
				Log.d(TAG, "load cache:" + cachedStream + " - " + localCacheKey + " / " + itemKey);
			}
			if (cachedStream != null) {
				// 从本地缓存中读取图片
				_notifyDownloadResult(downloadItem, cachedStream);
				return;
			}
		}

		// 下载数据
		HttpURLConnection conn = null;

		try {
			URL url = new URL(downloadUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(readTimeout);
			conn.setConnectTimeout(connectTimeout);
			conn.setRequestProperty("Accept-Encoding", "gzip");
			if (extHeaders != null) {
				for (int i = 0, extHeadersLength = extHeaders.length; i + 1 < extHeadersLength; i += 2) {
					String key = extHeaders[i + 0];
					String val = extHeaders[i + 1];
					conn.setRequestProperty(key, val);
				}
			}
			if (post) {
				conn.setRequestMethod("POST");
				if (postData != null) {
					conn.setDoOutput(true);
					conn.getOutputStream().write(postData);
				} else if (formEntities != null) {
					Random random = new Random();
					byte[] randBytes = new byte[64];
					random.nextBytes(randBytes);
					String boundaryStr = XulUtils.calMD5(randBytes);
					conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundaryStr);
					conn.setDoOutput(true);


					OutputStream os = conn.getOutputStream();
					byte[] boundaryHead = ("--" + boundaryStr).getBytes("UTF-8");

					for (int i = 0, formEntitiesLength = formEntities.length; i < formEntitiesLength; i++) {
						XulFormEntity formEntity = formEntities[i];
						os.write(boundaryHead);
						os.write(_CR_LF_);
						formEntity.writeBlock(os);
						os.write(_CR_LF_);
					}

					// tail
					os.write(boundaryHead);
					os.write('-');
					os.write('-');
				}
			}

			//信任https站点
			if (conn instanceof HttpsURLConnection) {
				HttpsURLConnection conns = (HttpsURLConnection) conn;
				conns.setHostnameVerifier(_hostVerifier);
				if (_sslCtx != null) {
					conns.setSSLSocketFactory(_sslCtx.getSocketFactory());
				}
			}

			int imageSize = conn.getHeaderFieldInt("Content-Length", -1);
			String encoding = conn.getHeaderField("Content-Encoding");

			InputStream is = null;
			try {
				is = conn.getInputStream();
			} catch (IOException e) {
				if (XulManager.DEBUG_XUL_WORKER) {
					Log.e(TAG, "Invalid http data stream ! " + conn.getResponseCode());
				}
				e.printStackTrace();
			}

			if (params != null) {
				params.responseCode = conn.getResponseCode();
				params.responseMsg = conn.getResponseMessage();
				params.responseHeaders = conn.getHeaderFields();
			}

			if (is != null) {
				XulDownloadOutputBuffer byteArrayOutputStream;
				if ("gzip".equalsIgnoreCase(encoding)) {
					is = new GZIPInputStream(is);
					byteArrayOutputStream = obtainDownloadBuffer(32768);
				} else {
					byteArrayOutputStream = obtainDownloadBuffer(Math.max(imageSize > 0 ? imageSize : 8192, 1024));
				}

				while (true) {
					int len = is.read(downloadBuffer);
					if (len > 0) {
						byteArrayOutputStream.write(downloadBuffer, 0, len);
						continue;
					}
					if (len <= 0) {
						is.close();
						is = byteArrayOutputStream.toInputStream();
						break;
					}
				}

				if (!downloadItem.noFileCache) {
					if (XulManager.DEBUG_XUL_WORKER) {
						Log.d(TAG, "store cache:" + localCacheKey + " / " + itemKey);
					}
					_internalStoreCachedData(localCacheKey, is);
					is.reset();
				}
			}

			_notifyDownloadResult(downloadItem, is);

			try {
				conn.disconnect();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		} catch (ConnectTimeoutException e) {
			String msg = "Connect TimeOut: " + downloadUrl;
			Log.e(TAG, msg);
			if (params != null) {
				params.responseCode = DOWNLOAD_RESPONSE_CONNECT_TIMEOUT;
				params.responseMsg = msg;
			}
		} catch (MalformedURLException e) {
			String msg = "Invalid url: " + downloadUrl;
			Log.e(TAG, msg);
			if (params != null) {
				params.responseCode = DOWNLOAD_RESPONSE_INVALID_URL;
				params.responseMsg = msg;
			}
		} catch (SocketTimeoutException e) {
			String msg = "Socket TimeOut: " + downloadUrl;
			Log.e(TAG, msg);
			if (params != null) {
				params.responseCode = DOWNLOAD_RESPONSE_SOCKET_TIMEOUT;
				params.responseMsg = msg;
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (params != null) {
				params.responseCode = DOWNLOAD_RESPONSE_UNKNOWN_EXCEPTION;
				params.responseMsg = e.getMessage();
			}
		}

		if (conn != null) {
			try {
				conn.disconnect();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		_notifyDownloadResult(downloadItem, null);
	}

	private static class acceptAllHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}

	}

	/**
	 * 证书验证类
	 */
	public static class acceptAllTrustManager implements javax.net.ssl.TrustManager, javax.net.ssl.X509TrustManager {
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public boolean isServerTrusted(java.security.cert.X509Certificate[] certs) {
			return true;
		}

		public boolean isClientTrusted(java.security.cert.X509Certificate[] certs) {
			return true;
		}

		public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
			throws java.security.cert.CertificateException {
			return;
		}

		public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
			throws java.security.cert.CertificateException {
			return;
		}
	}

	static void _notifyDownloadResult(DownloadItem downloadItem, InputStream inputStream) {
		try {
			downloadItem.__ownerDownloadHandler.onDownload(downloadItem, inputStream);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static DrawableItem _getPendingDrawableItem() {
		synchronized (_pendingDrawableTask) {
			if (_pendingDrawableTask.isEmpty()) {
				return null;
			}
			return _pendingDrawableTask.remove(0);
		}
	}

	static boolean _isGIF(Pattern gifPattern, String path, InputStream inputStream) {
		Matcher matcher = gifPattern.matcher(path);
		if (matcher != null && matcher.matches()) {
			return true;
		}

		if (inputStream != null && inputStream.markSupported()) {
			try {
				inputStream.mark(64);
				if (inputStream.read() == 'G' &&
					inputStream.read() == 'I' &&
					inputStream.read() == 'F' &&
					inputStream.read() == '8' &&
					inputStream.read() == '9' &&
					inputStream.read() == 'a'
					) {
					inputStream.reset();
					return true;
				}
				inputStream.reset();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	static void _drawableWorkerWaitSuspend() {
		while (isDrawableWorkersSuspended()) {
			_threadWait(10);
		}
	}

	static void _drawableWorkerRun() {

		Pattern ninePatchPattern = Pattern.compile("^.+\\.9(\\.png)?$", Pattern.CASE_INSENSITIVE);
		Pattern animationPkgPattern = Pattern.compile("^.+\\.ani(\\.zip)?$", Pattern.CASE_INSENSITIVE);

		Paint paintSolid = new Paint();
		paintSolid.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.HINTING_ON | Paint.FILTER_BITMAP_FLAG);
		paintSolid.setAntiAlias(true);
		paintSolid.setColor(Color.WHITE);

		Paint paintSrcIn = new Paint();
		paintSrcIn.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.HINTING_ON | Paint.FILTER_BITMAP_FLAG);
		paintSrcIn.setAntiAlias(true);
		paintSrcIn.setColor(Color.WHITE);
		paintSrcIn.setStyle(Paint.Style.FILL);
		paintSrcIn.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

		Paint paintClear = new Paint();
		paintClear.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.HINTING_ON | Paint.FILTER_BITMAP_FLAG);
		paintClear.setAntiAlias(true);
		paintClear.setColor(Color.WHITE);
		paintClear.setStyle(Paint.Style.FILL);
		paintClear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

		Paint paintShadow = new Paint();
		paintShadow.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.HINTING_ON | Paint.FILTER_BITMAP_FLAG);
		paintShadow.setAntiAlias(true);
		paintShadow.setColor(Color.BLACK);
		paintShadow.setStyle(Paint.Style.FILL);
		paintShadow.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));

		Paint paintDstOut = new Paint();
		paintDstOut.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.HINTING_ON | Paint.FILTER_BITMAP_FLAG);
		paintDstOut.setAntiAlias(true);
		paintDstOut.setColor(Color.BLACK);
		paintDstOut.setStyle(Paint.Style.FILL);
		paintDstOut.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

		XulBufferedInputStream bufferedInputStream = null;

		Canvas canvas = new Canvas();

		float[] tmpRoundRadius2 = new float[2];
		float[] tmpRoundRadius8 = new float[8];

		Rect rcTmp = new Rect();

		while (_drawableWorkers != null) {
			DrawableItem drawableItem = _getPendingDrawableItem();
			if (drawableItem == null) {
				_threadWait(Integer.MAX_VALUE);
				continue;
			}

			if (_addToImageSchedule(drawableItem)) {
				// 已经在计划中，当前Worker可以处理其它任务
				continue;
			}

			String imageKey = drawableItem.url;

			if (!TextUtils.isEmpty(imageKey) && imageKey.startsWith("@color:")) {
				int width = drawableItem.width;
				int height = drawableItem.height;
				if (width == 0) {
					if (height == 0) {
						width = height = 1;
					} else {
						width = height;
					}
				} else if (height == 0) {
					height = width;
				}
				float[] roundRectRadius = drawableItem._roundRectRadius;
				if (roundRectRadius == null) {
					roundRectRadius = EMPTY_ROUND_RECT_RADIUS;
				}
				String[] vals = imageKey.substring(7).split(",");
				int color = (int) XulUtils.tryParseHex(vals[0], Color.BLACK);
				float xScalar = drawableItem.scalarX;
				float yScalar = drawableItem.scalarY;

				if (vals.length == 3) {
					roundRectRadius = new float[]{
						XulUtils.tryParseFloat(vals[1], roundRectRadius[0]) * xScalar,
						XulUtils.tryParseFloat(vals[2], roundRectRadius[1]) * yScalar
					};
				} else if (vals.length == 5) {
					roundRectRadius = new float[]{
						XulUtils.tryParseFloat(vals[1], roundRectRadius[0]) * xScalar,
						XulUtils.tryParseFloat(vals[1], roundRectRadius[1]) * yScalar,
						XulUtils.tryParseFloat(vals[2], roundRectRadius[2]) * xScalar,
						XulUtils.tryParseFloat(vals[2], roundRectRadius[3]) * yScalar,
						XulUtils.tryParseFloat(vals[3], roundRectRadius[4]) * xScalar,
						XulUtils.tryParseFloat(vals[3], roundRectRadius[5]) * yScalar,
						XulUtils.tryParseFloat(vals[4], roundRectRadius[6]) * xScalar,
						XulUtils.tryParseFloat(vals[4], roundRectRadius[7]) * yScalar,
					};

				} else if (vals.length == 9) {
					roundRectRadius = new float[]{
						XulUtils.tryParseFloat(vals[1], roundRectRadius[0]) * xScalar,
						XulUtils.tryParseFloat(vals[2], roundRectRadius[1]) * yScalar,
						XulUtils.tryParseFloat(vals[3], roundRectRadius[2]) * xScalar,
						XulUtils.tryParseFloat(vals[4], roundRectRadius[3]) * yScalar,
						XulUtils.tryParseFloat(vals[5], roundRectRadius[4]) * xScalar,
						XulUtils.tryParseFloat(vals[6], roundRectRadius[5]) * yScalar,
						XulUtils.tryParseFloat(vals[7], roundRectRadius[6]) * xScalar,
						XulUtils.tryParseFloat(vals[8], roundRectRadius[7]) * yScalar,
					};
				}
				XulDrawable drawable = XulDrawable.fromColor(color, width, height, roundRectRadius, drawableItem.url, imageKey);
				_finishSchedule(drawableItem, drawable);
				continue;
			}
			_drawableWorkerWaitSuspend();
			XulDrawable cachedBmp = loadDrawableFromCache(imageKey);
			if (cachedBmp != null) {
				if (drawableItem.reusable) {
					cachedBmp = cachedBmp.makeClone();
				}
				if (cachedBmp != null) {
					_finishSchedule(drawableItem, cachedBmp);
					continue;
				}
			}

			InputStream inputStream = drawableItem.__dataStream;

			if (inputStream != null) {
				if (bufferedInputStream == null) {
					inputStream = bufferedInputStream = new XulBufferedInputStream(inputStream, 64 * 1024);
				} else {
					try {
						bufferedInputStream.resetInputStream(inputStream);
						inputStream = bufferedInputStream;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			_drawableWorkerWaitSuspend();
			{
				Matcher matcher = animationPkgPattern.matcher(drawableItem.__resolvedPath);
				if (matcher != null && matcher.matches()) {
					XulDrawable drawable = XulAnimationDrawable.buildAnimation(inputStream, drawableItem.url, imageKey);
					_finishSchedule(drawableItem, drawable);
					continue;
				}
			}
			{
				// load nine-patch drawable
				Matcher matcher = ninePatchPattern.matcher(drawableItem.__resolvedPath);
				if (matcher != null && matcher.matches()) {
					Bitmap bitmap = null;
					if (inputStream != null) {
						try {
							bitmap = BitmapTools.decodeStream(inputStream, XulManager.DEF_PIXEL_FMT, 0, 0);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					XulDrawable drawable = XulDrawable.fromNinePitchBitmap(bitmap, drawableItem.url, imageKey);
					_finishSchedule(drawableItem, drawable);
					continue;
				}
			}
			_drawableWorkerWaitSuspend();

			// prepare bitmap
			Bitmap bitmap = null;
			if (inputStream != null) {
				try {
					if (_internalPreloadImage(drawableItem, rcTmp)) {
						bitmap = BitmapTools.decodeStream(inputStream, drawableItem.pixFmt, XulUtils.calRectWidth(rcTmp), XulUtils.calRectHeight(rcTmp), drawableItem.target_width, drawableItem.target_height);
					} else {
						bitmap = BitmapTools.decodeStream(inputStream, drawableItem.pixFmt, drawableItem.width, drawableItem.height, drawableItem.target_width, drawableItem.target_height);
					}
					_drawableWorkerWaitSuspend();
					Bitmap processedBmp = _internalPreprocessImage(drawableItem, bitmap);
					if (processedBmp != null && processedBmp != bitmap) {
						BitmapTools.recycleBitmap(bitmap);
						bitmap = processedBmp;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			_drawableWorkerWaitSuspend();
			if (bitmap == null) {
				if (XulManager.DEBUG_XUL_WORKER) {
					Log.e(TAG, "decode image failed!!! - " + drawableItem.url);
				}

				if (drawableItem.__retryCounter < 1) {
					// 重试时直接下载
					_removeAndHangupDrawableSchedule(drawableItem);
					drawableItem.__retryCounter++;
					drawableItem.isDirect = true;
					_addPendingDownloadTask(drawableItem);
					_notifyDownloadWorker();
					continue;
				}
			} else if (drawableItem._roundRectRadius != null) {
				float[] roundRadius = drawableItem._roundRectRadius;

				float shadowSize = drawableItem.shadowSize;
				if (drawableItem.height == 0 && drawableItem.width == 0) {
					float scalarX = 1.0f;
					float scalarY = 1.0f;
					if (drawableItem.target_height != 0) {
						if (drawableItem.target_width != 0) {
							scalarX = ((float) bitmap.getWidth()) / drawableItem.target_width;
							scalarY = ((float) bitmap.getHeight()) / drawableItem.target_height;
						} else {
							scalarX = scalarY = ((float) bitmap.getHeight()) / drawableItem.target_height;
						}
					} else if (drawableItem.target_width != 0) {
						scalarX = scalarY = ((float) bitmap.getWidth()) / drawableItem.target_width;
					}

					float[] scaledRoundRadius;
					if (roundRadius.length == 2) {
						scaledRoundRadius = tmpRoundRadius2;
					} else {
						scaledRoundRadius = tmpRoundRadius8;
					}

					for (int i = 0, roundRadiusLength = roundRadius.length; i < roundRadiusLength; i += 2) {
						float x = roundRadius[i];
						float y = roundRadius[i + 1];
						scaledRoundRadius[i] = x * scalarX;
						scaledRoundRadius[i + 1] = y * scalarY;
					}
					roundRadius = scaledRoundRadius;
					shadowSize *= Math.max(scalarX, scalarY);
				}

				try {
					Bitmap oldBmp = bitmap;
					if (shadowSize > 0.5) {
						bitmap = toRoundCornerShadowBitmap(canvas, paintSolid, paintSrcIn, paintDstOut, paintShadow, bitmap, roundRadius, shadowSize, drawableItem.shadowColor);
					} else {
						bitmap = toRoundCornerBitmap(canvas, paintClear, bitmap, roundRadius);
					}
					if (bitmap != oldBmp) {
						BitmapTools.recycleBitmap(oldBmp);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			_finishSchedule(drawableItem, XulDrawable.fromBitmap(bitmap, drawableItem.url, imageKey));
		}

	}

	private static Bitmap toRoundCornerMutableBitmap(Canvas canvas, Paint paintClear, Bitmap srcBitmap, float[] roundRadius) {
		canvas.setBitmap(srcBitmap);
		RectF inset = new RectF(1, 1, 1, 1);
		if (roundRadius.length == 2) {
			float[] tmpRoundRadius = new float[8];
			for (int i = 0; i < 4; ++i) {
				tmpRoundRadius[i * 2 + 0] = roundRadius[0];
				tmpRoundRadius[i * 2 + 1] = roundRadius[1];
				roundRadius = tmpRoundRadius;
			}
		}
		canvas.save();
		canvas.translate(-1, -1);
		RoundRectShape roundRectShape = new RoundRectShape(null, inset, roundRadius);
		roundRectShape.resize(srcBitmap.getWidth() + 2, srcBitmap.getHeight() + 2);
		roundRectShape.draw(canvas, paintClear);
		canvas.restore();
		canvas.setBitmap(null);
		return srcBitmap;
	}

	private static Bitmap toRoundCornerBitmap(Canvas canvas, Paint paintClear, Bitmap srcBitmap, float[] roundRadius) {
		if (srcBitmap.isMutable() && srcBitmap.hasAlpha()) {
			return toRoundCornerMutableBitmap(canvas, paintClear, srcBitmap, roundRadius);
		}
		Bitmap output;
		if (false) {
			output = BitmapTools.createBitmapFromRecycledBitmaps(srcBitmap.getWidth(), srcBitmap.getHeight(), XulManager.DEF_PIXEL_FMT);
		} else {
			output = BitmapTools.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), XulManager.DEF_PIXEL_FMT);
		}
		canvas.setBitmap(output);
		Rect rect = new Rect(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight());
		canvas.drawBitmap(srcBitmap, rect, rect, null);
		return toRoundCornerMutableBitmap(canvas, paintClear, output, roundRadius);
	}

	private static Bitmap toRoundCornerShadowBitmap(Canvas canvas, Paint paintSolid, Paint paintSrcIn, Paint paintDstOut, Paint paintShadow, Bitmap srcBitmap, float[] roundRadius, float shadowSize, int shadowColor) {
		int borderSize = XulUtils.roundToInt(shadowSize) * 2;
		Bitmap output;
		if (false) {
			output = BitmapTools.createBitmapFromRecycledBitmaps(srcBitmap.getWidth(), srcBitmap.getHeight(), XulManager.DEF_PIXEL_FMT);
		} else {
			output = BitmapTools.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), XulManager.DEF_PIXEL_FMT);
		}
		canvas.setBitmap(output);
		Rect rectSrc = new Rect(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight());
		Rect rectDst = new Rect(rectSrc);
		XulUtils.offsetRect(rectDst, borderSize / 2, borderSize / 2);
		RectF rectF = new RectF(rectDst);
		if (roundRadius.length == 2) {
			float rY = roundRadius[1];
			float rX = roundRadius[0];
			canvas.drawRoundRect(rectF, rX, rY, paintSolid);
			canvas.drawBitmap(srcBitmap, rectSrc, rectDst, paintSrcIn);

			XulUtils.saveCanvasLayer(canvas, 0, 0, output.getWidth(), output.getHeight(), paintSolid, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
			paintShadow.setShadowLayer(shadowSize, 0, 0, shadowColor);
			canvas.drawRoundRect(rectF, rX, rY, paintShadow);
			canvas.drawRoundRect(rectF, rX, rY, paintDstOut);
			XulUtils.restoreCanvas(canvas);
		} else {
			RoundRectShape roundRectShape = new RoundRectShape(roundRadius, null, null);
			roundRectShape.resize(rectF.width(), rectF.height());

			XulUtils.saveCanvas(canvas);
			canvas.translate(rectF.left, rectF.top);
			roundRectShape.draw(canvas, paintSolid);
			canvas.translate(-rectF.left, -rectF.top);

			canvas.drawBitmap(srcBitmap, rectSrc, rectDst, paintSrcIn);
			XulUtils.saveCanvasLayer(canvas, 0, 0, output.getWidth(), output.getHeight(), paintSolid, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
			paintShadow.setShadowLayer(shadowSize, 0, 0, shadowColor);

			canvas.translate(rectF.left, rectF.top);
			roundRectShape.draw(canvas, paintShadow);
			roundRectShape.draw(canvas, paintDstOut);

			XulUtils.restoreCanvas(canvas);
			XulUtils.restoreCanvas(canvas);
		}

		canvas.setBitmap(null);
		return output;
	}

	static {
		ThreadGroup xulThreadGroup = new ThreadGroup("XUL");
		for (int i = 0; i < _downloadWorkers.length; i++) {
			_downloadWorkers[i] = new Thread(xulThreadGroup, XUL_DOWNLOAD_WORKER) {
				@Override
				public void run() {
					_downloadWorkerRun();
				}
			};
			// _downloadWorkers[i].setPriority(Thread.NORM_PRIORITY);
			_downloadWorkers[i].start();
		}

		for (int i = 0; i < _drawableWorkers.length; i++) {
			_drawableWorkers[i] = new Thread(xulThreadGroup, XUL_DRAWABLE_WORKER) {
				@Override
				public void run() {
					_drawableWorkerRun();
				}
			};
			_drawableWorkers[i].setPriority(Thread.NORM_PRIORITY - 2);
			_drawableWorkers[i].start();
		}

		try {
			_sslCtx = SSLContext.getInstance("SSL");
			_sslCtx.init(null, new TrustManager[]{new acceptAllTrustManager()}, new SecureRandom());
		} catch (Exception e) {
			e.printStackTrace();
		}

		_hostVerifier = new acceptAllHostnameVerifier();
	}

	public static class XulDownloadParams {
		public boolean post = false;
		public int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
		public int readTimeout = DEFAULT_READ_TIMEOUT;
		public byte[] postBody;
		public String[] extHeaders;
		public int responseCode;
		public String responseMsg;
		public Map<String, List<String>> responseHeaders;
		public XulFormEntity[] formEntities;

		public XulDownloadParams(boolean post, byte[] postBody, String[] extHeaders) {
			this.post = post;
			this.postBody = postBody;
			this.extHeaders = extHeaders;
		}

		public XulDownloadParams(XulFormEntity[] formEntities) {
			this.post = true;
			this.formEntities = formEntities;
		}
	}


	public static abstract class XulFormEntity {
		public abstract int calLength();

		public abstract int writeBlock(OutputStream os);

		protected static byte[] getBytes(String v) {
			try {
				return v.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	public static class XulFormField extends XulFormEntity {
		public byte[] name;
		public byte[] value;

		public XulFormField(String name, String value) {
			this.name = getBytes(name);
			this.value = getBytes(value);
		}

		@Override
		public int calLength() {
			// _CONTENT_DISPOSITION_.length + name + "\"\r\n"
			int length = _CONTENT_DISPOSITION_.length + name.length + 3;
			length += _CR_LF_.length;    // \r\n
			if (value != null) {
				length += value.length;
			}
			return length;
		}

		@Override
		public int writeBlock(OutputStream os) {
			try {
				os.write(_CONTENT_DISPOSITION_);
				os.write(name);
				os.write('\"');
				os.write(_CR_LF_);
				os.write(_CR_LF_);
				if (value != null) {
					os.write(value);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return 0;
		}
	}

	public static class XulFormFile extends XulFormEntity {
		public byte[] name;
		public File file;
		public byte[] contentType;

		private byte[] _fileName;
		private FileInputStream _fileInputStream;

		public XulFormFile(String name, File file) {
			this(getBytes(name), file, _APPLICATION_OCTET_STREAM_);
		}

		public XulFormFile(byte[] name, File file, byte[] contentType) {
			this.name = name;
			this.file = file;
			this.contentType = contentType;

			_fileName = getBytes(file.getName());

			if (!file.exists()) {
				return;
			}

			try {
				_fileInputStream = new FileInputStream(file);
			} catch (FileNotFoundException e) {
			}
		}

		public XulFormFile(String name, File file, String contentType) {
			this(getBytes(name), file, getBytes(contentType));
		}

		@Override
		public int calLength() {
			// _CONTENT_DISPOSITION_.length + name + "\""
			int length = _CONTENT_DISPOSITION_.length + name.length + 1;
			if (_fileName != null) {
				// _FILENAME_.length + _fileName + "\""
				length += _FILENAME_.length + _fileName.length + 1;
			}
			length += _CR_LF_.length;    // \r\n

			if (contentType != null) {
				// _CONTENT_TYPE_.length + contentType + "\r\n"
				length += _CONTENT_TYPE_.length + contentType.length + _CR_LF_.length;
			}

			length += _CR_LF_.length;    // \r\n

			if (_fileInputStream != null) {
				try {
					length += _fileInputStream.available();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return length;
		}

		@Override
		public int writeBlock(OutputStream os) {
			try {
				os.write(_CONTENT_DISPOSITION_);
				os.write(name);
				os.write('\"');

				if (_fileName != null) {
					os.write(_FILENAME_);
					os.write(_fileName);
					os.write('\"');
				}

				if (contentType != null) {
					os.write(_CR_LF_);
					os.write(_CONTENT_TYPE_);
					os.write(contentType);
				}

				os.write(_CR_LF_);
				os.write(_CR_LF_);
				if (_fileInputStream != null) {
					byte[] buf = new byte[1024];
					for (int len = _fileInputStream.read(buf); len > 0; len = _fileInputStream.read(buf)) {
						os.write(buf, 0, len);
					}
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return 0;
		}
	}

}
