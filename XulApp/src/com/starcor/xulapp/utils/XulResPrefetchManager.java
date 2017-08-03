package com.starcor.xulapp.utils;

import com.starcor.xul.Graphics.XulDrawable;
import com.starcor.xul.Utils.XulSimpleArray;
import com.starcor.xul.XulManager;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulWorker;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by hy on 2015/6/26.
 */
public class XulResPrefetchManager implements XulWorker.IXulWorkItemSource {

	static final int _DOWNLOAD_ITEM_LIMIT = 16;
	static final int _DRAWABLE_ITEM_LIMIT = 16;
	static final int _MAX_CONCURRENCY = 2;
	static final int _DEFAULT_DRAWABLE_OBJECT_LIFE_MS = 1000;
	static XulResPrefetchManager _inst;
	AtomicInteger _runningItems = new AtomicInteger(0);
	_XulDownloadItemList _downloadItems = new _XulDownloadItemList(_DOWNLOAD_ITEM_LIMIT);
	_XulDrawableItemList _drawableItems = new _XulDrawableItemList(_DRAWABLE_ITEM_LIMIT);
	ArrayList<XulDrawable> _persistDrawableItems = new ArrayList<XulDrawable>();

	public synchronized static void init() {
		if (_inst == null) {
			_inst = new XulResPrefetchManager();
			XulWorker.registerDownloader(_inst);
		}
	}

	public synchronized static void prefetch(String xulPath) {
		if (_inst._downloadItems.size() >= _DOWNLOAD_ITEM_LIMIT) {
			_inst._downloadItems.remove(0);
		}
		XulWorker.DownloadItem item = new XulWorker.DownloadItem();
		item.url = xulPath;
		_inst._downloadItems.add(item);
	}

	public static void prefetchImage(String xulPath) {
		prefetchImage(xulPath, 0, 0);
	}

	public static void prefetchImage(String xulPath, int width, int height) {
		prefetchImage(xulPath, width, height, 0, 0);
	}

	public static void prefetchImage(String xulPath, int width, int height, int lifeMS) {
		prefetchImage(xulPath, width, height, 0, 0, lifeMS);
	}

	public static void prefetchImage(String xulPath, int width, int height, float radiusX, float radiusY) {
		prefetchImage(xulPath, width, height, 0, 0, _DEFAULT_DRAWABLE_OBJECT_LIFE_MS);
	}

	public synchronized static void prefetchImage(String xulPath, int width, int height, float radiusX, float radiusY, int lifeMS) {
		_XulDrawableItemList drawableItems = _inst._drawableItems;
		int drawableItemCount = drawableItems.size();
		if (lifeMS > 0) {
			for (int i = 0; i < drawableItemCount && drawableItemCount >= _DRAWABLE_ITEM_LIMIT; i++) {
				if (drawableItems.get(i)._lifeMS > 0) {
					drawableItems.remove(i);
					--drawableItemCount;
				}
			}
		}
		_XulDrawableItem item = new _XulDrawableItem();
		item.url = xulPath;
		float globalXScalar = XulManager.getGlobalXScalar();
		float globalYScalar = XulManager.getGlobalYScalar();
		item.width = XulUtils.roundToInt(width * globalXScalar);
		item.height = XulUtils.roundToInt(height * globalYScalar);
		item.scalarX = globalXScalar;
		item.scalarY = globalYScalar;
		item._lifeMS = lifeMS;
		if (radiusX > 0.1f && radiusY > 0.1f) {
			item.setRoundRect(radiusX, radiusY);
		}
		drawableItems.add(item);
	}

	private synchronized XulWorker.DownloadItem _getDownloadItem() {
		if (_downloadItems.isEmpty()) {
			return null;
		}
		if (_runningItems.incrementAndGet() > _MAX_CONCURRENCY) {
			_runningItems.decrementAndGet();
			return null;
		}
		return _downloadItems.pop();
	}

	private synchronized XulWorker.DrawableItem _getDrawableItem() {
		if (_drawableItems.isEmpty()) {
			return null;
		}
		if (_runningItems.incrementAndGet() > _MAX_CONCURRENCY) {
			_runningItems.decrementAndGet();
			return null;
		}
		return _drawableItems.pop();
	}

	@Override
	public XulWorker.DownloadItem getDownloadItem() {
		return _getDownloadItem();
	}

	@Override
	public void onDownload(XulWorker.DownloadItem item, InputStream data) {
		_runningItems.decrementAndGet();
	}

	@Override
	public XulWorker.DrawableItem getDrawableItem() {
		return _getDrawableItem();
	}

	@Override
	public void onDrawableLoaded(XulWorker.DrawableItem item, XulDrawable bmp) {
		_runningItems.decrementAndGet();
		// make drawable object alive for 1 second
		int lifeMS = ((_XulDrawableItem) item)._lifeMS;
		if (lifeMS > 0) {
			XulWorker.addDrawableToCache(item.url, bmp, lifeMS);
		} else if (lifeMS < 0) {
			synchronized (_persistDrawableItems) {
				_persistDrawableItems.add(bmp);
			}
		}
	}

	@Override
	public String resolve(XulWorker.DownloadItem item, String path) {
		return null;
	}

	@Override
	public InputStream getAssets(XulWorker.DownloadItem item, String path) {
		return null;
	}

	@Override
	public InputStream getAppData(XulWorker.DownloadItem item, String path) {
		return null;
	}

	private static class _XulDownloadItemList extends XulSimpleArray<XulWorker.DownloadItem> {

		public _XulDownloadItemList(int size) {
			super(size);
		}

		@Override
		protected XulWorker.DownloadItem[] allocArrayBuf(int size) {
			return new XulWorker.DownloadItem[size];
		}
	}

	private static class _XulDrawableItem extends XulWorker.DrawableItem {
		int _lifeMS;
	}

	private static class _XulDrawableItemList extends XulSimpleArray<_XulDrawableItem> {

		public _XulDrawableItemList(int size) {
			super(size);
		}

		@Override
		protected _XulDrawableItem[] allocArrayBuf(int size) {
			return new _XulDrawableItem[size];
		}
	}
}
