package com.starcor.xul;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.starcor.xul.Factory.XulFactory;
import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Graphics.XulDrawable;
import com.starcor.xul.Prop.XulBinding;
import com.starcor.xul.Prop.XulFocus;
import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.Render.Drawer.IXulAnimation;
import com.starcor.xul.Render.XulSliderAreaRender;
import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.Utils.XulArrayList;
import com.starcor.xul.Utils.XulCachedHashMap;
import com.starcor.xul.Utils.XulRenderDrawableItem;
import com.starcor.xul.Utils.XulSimpleArray;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by hy on 2014/5/11.
 */
public class XulRenderContext {
	public static final int EVENT_PENDING_IMAGE_LOADED = 0x0001;  // all pending image has loaded, param1 is the round count

	static final String TAG = XulRenderContext.class.getSimpleName();

	ArrayList<XulSelect> _globalSelectors;
	XulPage _page;
	XulTaskCollector _taskCollector;
	XulSuspendTaskCollector _suspendTaskCollector;
	boolean _suspended = false;
	boolean _skipSyncData = false;      // all of changed view's layout have not be finished
	volatile int _pendingImageItemNum = 0;
	volatile int _pendingImageCollectingRound = 0;
	int _pendingImageFinishingRound = 0;

	XulCachedHashMap<XulView, XulView> _dirtyViews = new XulCachedHashMap<XulView, XulView>(64);

	int _scheduledLayoutFinishTasksProtectRange = 0;
	XulSimpleArray<Runnable> _scheduledLayoutFinishedTasks = new XulSimpleArray<Runnable>() {
		@Override
		protected Runnable[] allocArrayBuf(int size) {
			return new Runnable[size];
		}
	};

	class FocusTracker {
		Matrix _tmpDrawMatrix = new Matrix();
		RectF _focusRect = new RectF();

		XulView _focusTrackView;

		public FocusTracker(XulView focusTrackerView) {
			_focusTrackView = focusTrackerView;
		}

		public void postDrawFocus(XulViewRender xulViewRender, XulDC dc, Rect rect, int xBase, int yBase) {
			Canvas canvas = dc.getCanvas();
			canvas.getMatrix(_tmpDrawMatrix);

			XulView view = xulViewRender.getView();
			int xDelta = 0;
			int yDelta = 0;

			XulArea parent = view.getParent();
			while (parent != null) {
				XulViewRender render = parent.getRender();
				if (render instanceof XulSliderAreaRender) {
					XulSliderAreaRender sliderRender = (XulSliderAreaRender) render;
					if (sliderRender.isVertical()) {
						yDelta += sliderRender.getScrollDelta();
					} else {
						xDelta += sliderRender.getScrollDelta();
					}
				}
				parent = parent.getParent();
			}

			RectF updateRect = xulViewRender.getUpdateRect();
			updateRect.offset(xBase, yBase);
			_tmpDrawMatrix.mapRect(updateRect);

			if (Math.abs(_focusRect.left - updateRect.left) < 0.1 &&
				Math.abs(_focusRect.top - updateRect.top) < 0.1 &&
				Math.abs(_focusRect.right - updateRect.right) < 0.1 &&
				Math.abs(_focusRect.bottom - updateRect.bottom) < 0.1) {
				return;
			}
			XulUtils.copyRect(updateRect, _focusRect);

			if (!_focusTrackView.isVisible()) {
				_focusTrackView.setStyle(XulPropNameCache.TagId.DISPLAY, "block");
			}

			_focusTrackView.setAttr(XulPropNameCache.TagId.X, String.valueOf((int) ((_focusRect.left + xDelta) / getXScalar())));
			_focusTrackView.setAttr(XulPropNameCache.TagId.Y, String.valueOf((int) ((_focusRect.top + yDelta) / getXScalar())));
			_focusTrackView.setAttr(XulPropNameCache.TagId.WIDTH, String.valueOf((int) (_focusRect.width() / getXScalar())));
			_focusTrackView.setAttr(XulPropNameCache.TagId.HEIGHT, String.valueOf((int) (_focusRect.height() / getXScalar())));
			XulViewRender render = _focusTrackView.getRender();
			render.refreshSizingMovingAnimation();
			_focusTrackView.resetRender();
		}

		public void preDrawFocus(XulViewRender xulViewRender, XulDC dc, Rect rect, int xBase, int yBase) {

		}
	}

	FocusTracker _focusTracker;

	public void preDrawFocus(XulViewRender xulViewRender, XulDC dc, Rect rect, int xBase, int yBase) {
		if (_focusTracker == null) {
			return;
		}
		_focusTracker.preDrawFocus(xulViewRender, dc, rect, xBase, yBase);

	}

	public void postDrawFocus(XulViewRender xulViewRender, XulDC dc, Rect rect, int xBase, int yBase) {
		if (_focusTracker == null) {
			return;
		}
		_focusTracker.postDrawFocus(xulViewRender, dc, rect, xBase, yBase);
	}

	private static class RenderViewList extends XulSimpleArray<XulViewRender> {

		public RenderViewList(int sz) {
			super(sz);
		}

		@Override
		protected XulViewRender[] allocArrayBuf(int size) {
			return new XulViewRender[size];
		}
	}

	private static class AnimationList extends XulSimpleArray<IXulAnimation> {

		public AnimationList(int sz) {
			super(sz);
		}

		@Override
		protected IXulAnimation[] allocArrayBuf(int size) {
			return new IXulAnimation[size];
		}
	}

	RenderViewList _changedViews = new RenderViewList(512);

	AnimationList _animation = new AnimationList(64);

	XulPage.IActionCallback actionCallback = new XulPage.IActionCallback() {
		@Override
		public void doAction(XulView view, String action, String type, String command, Object userdata) {
			if (_handler == null) {
				return;
			}
			try {
				_handler.onDoAction(view, action, type, command, userdata);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	static private Paint _defStrokePaint;
	static private Paint _defTextPaint;
	static private Paint _defShadowTextPaint;
	static private Paint _defSolidPaint;
	static private Paint _defPicPaint;
	static private Paint _defAlphaPicPaint;
	static private Paint _defSolidShadowPaint;
	static private Typeface _defTypeFace;
	private boolean _readyToDraw = false;
	private XulDC _xulDC = new XulDC(this);

	public void setPageSize(int widthPixels, int heightPixels) {
		_page.setPageSize(widthPixels, heightPixels);
	}

	public int getPageWidth() {
		return _page.getPageWidth();
	}

	public int getPageHeight() {
		return _page.getPageHeight();
	}

	public IXulExternalView createExternalView(String cls, int x, int y, int width, int height, XulView view) {
		if (_handler == null) {
			return null;
		}
		return _handler.createExternalView(cls, x, y, width, height, view);
	}

	public boolean isReady() {
		return _readyToDraw;
	}

	public void refreshBinding(String bindingId) {
		if (_page == null) {
			return;
		}
		_page.refreshBinding(bindingId);
	}

	public void refreshBinding(String bindingId, String url) {
		if (_page == null) {
			return;
		}
		_page.refreshBinding(bindingId, url);
	}

	public void refreshBinding(String bindingId, XulDataNode dataNode) {
		if (_page == null) {
			return;
		}
		_page.refreshBinding(bindingId, dataNode);
		_page.applyBinding(getDefaultActionCallback());
	}

	public void updateHandler(IXulRenderHandler handler) {
		_handler = handler;
	}

	public XulPage getPage() {
		return _page;
	}

	long _animationTimestamp;

	public long animationTimestamp() {
		return _animationTimestamp;
	}

	public interface IXulRenderHandler {

		void invalidate(Rect rect);

		void uiRun(Runnable runnable);

		void uiRun(Runnable runnable, int delayMS);

		void onDoAction(XulView view, String action, String type, String command, Object userdata);

		IXulExternalView createExternalView(String cls, int x, int y, int width, int height, XulView view);

		String resolve(XulWorker.DownloadItem item, String path);

		InputStream getAssets(XulWorker.DownloadItem item, String path);

		InputStream getAppData(XulWorker.DownloadItem item, String path);

		InputStream getSdcardData(XulWorker.DownloadItem item, String path);

		void onRenderIsReady();

		void onRenderEvent(int eventId, int param1, int param2, Object msg);
	}

	public interface IXulRenderHandler2 extends IXulRenderHandler {
		Object getCustomObject(int key);
	}

	private IXulRenderHandler _handler;

	public Object getCustomObject(int key) {
		IXulRenderHandler handler = _handler;
		if (handler == null || !(handler instanceof IXulRenderHandler2)) {
			return null;
		}
		return ((IXulRenderHandler2) handler).getCustomObject(key);
	}

	public void invalidate(Rect rect) {
		if (_handler == null) {
			return;
		}
		_handler.invalidate(rect);
	}

	public void uiRun(Runnable runnable, int delayMS) {
		if (_handler == null) {
			Log.e(TAG, "uiRun(delay:" + delayMS + ") - handler is null!!");
			return;
		}
		_handler.uiRun(runnable, delayMS);
	}

	public void uiRun(Runnable runnable) {
		if (_handler == null) {
			try {
				runnable.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		_handler.uiRun(runnable);
	}

	private static class TypefaceInfo {
		String name;
		Typeface typeface;
		Paint paint;
	}

	private static XulCachedHashMap<String, TypefaceInfo> _font_map = new XulCachedHashMap<String, TypefaceInfo>();

	static public void addTypeFace(String name, Typeface typeface) {
		TypefaceInfo typefaceInfo = new TypefaceInfo();
		typefaceInfo.name = name;
		typefaceInfo.typeface = typeface;
		_font_map.put(name, typefaceInfo);
	}

	static public void setDefTypeFace(Typeface typeFace) {
		_defTypeFace = typeFace;
	}

	static public Typeface getDefTypeFace() {
		return _defTypeFace;
	}

	static public Paint getDefStrokePaint() {
		if (_defStrokePaint == null) {
			_defStrokePaint = new Paint();
			_defStrokePaint.setColor(Color.WHITE);
			_defStrokePaint.setStrokeWidth(2.0f);
			_defStrokePaint.setAntiAlias(true);
			_defStrokePaint.setStyle(Paint.Style.STROKE);
		}
		return _defStrokePaint;
	}

	static public Paint getDefSolidPaint() {
		if (_defSolidPaint == null) {
			_defSolidPaint = new Paint();
			_defSolidPaint.setAntiAlias(true);
			_defSolidPaint.setColor(Color.WHITE);
			_defSolidPaint.setStrokeWidth(1.0f);
			_defSolidPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		}
		return _defSolidPaint;
	}

	static public Paint getDefSolidShadowPaint() {
		if (_defSolidShadowPaint == null) {
			_defSolidShadowPaint = new Paint();
			_defSolidShadowPaint.setAntiAlias(true);
			_defSolidShadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		}
		return _defSolidShadowPaint;
	}

	static public Paint getDefTextPaint() {
		if (_defTextPaint == null) {
			_defTextPaint = new Paint();
			Typeface typeFace = getDefTypeFace();
			if (typeFace != null) {
				_defTextPaint.setTypeface(typeFace);
			}
			_defTextPaint.setAntiAlias(true);
			_defTextPaint.setSubpixelText(true);
			// _defTextPaint.setStyle(Paint.Style.FILL);
		}
		return _defTextPaint;
	}

	static public Paint getTextPaintByName(String name) {
		if (name == null) {
			return getDefTextPaint();
		}
		TypefaceInfo typefaceInfo = _font_map.get(name);
		if (typefaceInfo == null) {
			return getDefTextPaint();
		}
		Paint paint = typefaceInfo.paint;
		if (paint == null) {
			typefaceInfo.paint = paint = new Paint();
			paint.setTypeface(typefaceInfo.typeface);
			paint.setAntiAlias(true);
			paint.setSubpixelText(true);
		}
		return paint;
	}

	static public Paint getDefPicPaint() {
		if (_defPicPaint == null) {
			_defPicPaint = new Paint();
			_defPicPaint.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
			_defPicPaint.setColor(Color.BLACK);
			_defPicPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		}
		return _defPicPaint;
	}

	static public Paint getDefAlphaPicPaint() {
		if (_defAlphaPicPaint == null) {
			_defAlphaPicPaint = new Paint();
			_defAlphaPicPaint.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
			_defAlphaPicPaint.setColor(Color.BLACK);
			_defAlphaPicPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		}
		return _defAlphaPicPaint;
	}

	static public Paint getDefShadowTextPaint() {
		if (_defShadowTextPaint == null) {
			_defShadowTextPaint = new Paint();
			_defShadowTextPaint.setAntiAlias(true);
			_defShadowTextPaint.setSubpixelText(true);
			Typeface typeFace = getDefTypeFace();
			if (typeFace != null) {
				_defShadowTextPaint.setTypeface(typeFace);
			}
			_defShadowTextPaint.setShadowLayer(3, 0, 0, Color.BLACK);
		}
		return _defShadowTextPaint;
	}

	static public Paint getShadowTextPaintByName(String name) {
		if (name == null) {
			return getDefShadowTextPaint();
		}
		TypefaceInfo typefaceInfo = _font_map.get(name);
		if (typefaceInfo == null) {
			return getDefShadowTextPaint();
		}
		Paint paint = typefaceInfo.paint;
		if (paint == null) {
			typefaceInfo.paint = paint = new Paint();
			paint.setTypeface(typefaceInfo.typeface);
			paint.setAntiAlias(true);
			paint.setSubpixelText(true);
			paint.setShadowLayer(3, 0, 0, Color.BLACK);
		}
		return paint;
	}

	public XulPage.IActionCallback getDefaultActionCallback() {
		return actionCallback;
	}

	public void markDirtyView(XulView view) {
		_dirtyViews.put(view, view);
	}

	public void markDataChanged(XulViewRender view) {
		_skipSyncData = false;
		_changedViews.add(view);
	}

	public void addAnimation(IXulAnimation ani) {
		if (_animation.contains(ani)) {
			return;
		}
		_animation.add(ani);
	}

	public void removeAnimation(IXulAnimation ani) {
		_animation.remove(ani);
	}

	public void suspend(boolean isSuspend) {
		if (_suspended == isSuspend) {
			return;
		}
		if (_suspendTaskCollector != null) {
			_suspendTaskCollector.reset();
		}
		_suspended = isSuspend;
	}

	public boolean isSuspended() {
		return _suspended;
	}

	public boolean isDestroyed() {
		return _page == null;
	}

	private synchronized XulWorker.DownloadItem _getPendingItem() {
		if (_suspended) {
			if (_suspendTaskCollector == null) {
				_suspendTaskCollector = new XulSuspendTaskCollector();
			}
			_suspendTaskCollector.init(_page.getLayout());
			_suspendTaskCollector.doSuspendWork();
			return null;
		}
		if (_page == null) {
			return null;
		}
		return _page.getPendingItem();
	}

	private synchronized XulWorker.DrawableItem _getPendingImageItem() {
		if (_suspended) {
			return null;
		}
		if (_page == null || _page.getLayout() == null) {
			return null;
		}
		if (_taskCollector == null) {
			_taskCollector = new XulTaskCollector();
			_taskCollector.init(_page.getLayout());
		}
		XulWorker.DrawableItem drawableItem = _taskCollector.collectPendingDrawableItem();
		if (_taskCollector.isFinished()) {
			++_pendingImageCollectingRound;
		}
		if (drawableItem != null) {
			++_pendingImageItemNum;
		}
		return drawableItem;
	}

	private void _onPendingItemReady(final XulWorker.DownloadItem item, final InputStream data) {
		if (_page == null) {
			return;
		}
		uiRun(new Runnable() {
			@Override
			public void run() {
				XulPage page = _page;
				if (page == null) {
					// page already destroyed
					return;
				}

				boolean bindingFinished = page.isBindingFinished();
				if (page.setPendingItemData(item, data, actionCallback)) {
					if (page != _page) {
						Log.w(TAG, "Page has been destroyed during binding events! " + page.toString());
						return;
					}
					if (bindingFinished) {
						doLayout();
						syncData();
						invalidate(null);
					} else {
						_onBindingFinished();
					}
				}
			}
		});
	}

	private void _onPendingImageItemReady(final XulWorker.DrawableItem item, final XulDrawable bmp) {
		uiRun(new Runnable() {
			@Override
			public void run() {
				if (--_pendingImageItemNum == 0 && _pendingImageCollectingRound > 1) {
					++_pendingImageFinishingRound;
					IXulRenderHandler handler = _handler;
					if (handler != null) {
						handler.onRenderEvent(EVENT_PENDING_IMAGE_LOADED, _pendingImageFinishingRound, 0, null);
					}
				}
				if (item instanceof XulRenderDrawableItem) {
					((XulRenderDrawableItem) item).onImageReady(bmp);
				}
			}
		});
	}

	private boolean _applyBinding() {
		assert _page != null;
		if (!_page.applyBinding(actionCallback)) {
			return false;
		}
		// _page may be set to null during applyBinding()
		if (_page == null) {
			Log.w(TAG, "Page has been destroyed during binding events!");
			return true;
		}
		return _onBindingFinished();
	}

	private boolean _onBindingFinished() {
		XulPage page = _page;
		page.applySelectors();
		page.prepareRender(this);
		page.getLayout().initFocus();
		doLayout();
		syncData();

		if (!_readyToDraw) {
			uiRun(new Runnable() {
				@Override
				public void run() {
					XulPage page = _page;
					if (page == null) {
						Log.e(TAG, "page already destroyed before ready!");
						return;
					}
					XulPage.invokeActionNoPopup(page, "ready", actionCallback);
					if (_handler != null) {
						_handler.onRenderIsReady();
					}
					_readyToDraw = true;
				}
			});
		}
		// UI初始化成功，刷新界面
		invalidate(null);
		return true;
	}

	XulWorker.IXulWorkItemSource _downloader = new XulWorker.IXulWorkItemSource() {
		@Override
		public XulWorker.DownloadItem getDownloadItem() {
			return _getPendingItem();
		}

		@Override
		public void onDownload(XulWorker.DownloadItem item, InputStream data) {
			_onPendingItemReady(item, data);
		}

		@Override
		public XulWorker.DrawableItem getDrawableItem() {
			return _getPendingImageItem();
		}

		@Override
		public void onDrawableLoaded(XulWorker.DrawableItem item, XulDrawable bmp) {
			_onPendingImageItemReady(item, bmp);
		}

		@Override
		public String resolve(XulWorker.DownloadItem item, String path) {
			if (_handler != null) {
				return _handler.resolve(item, path);
			}
			return null;
		}

		@Override
		public InputStream getAssets(XulWorker.DownloadItem item, String path) {
			if (_handler != null) {
				return _handler.getAssets(item, path);
			}
			return null;
		}

		@Override
		public InputStream getAppData(XulWorker.DownloadItem item, String path) {
			if (_handler != null) {
				return _handler.getAppData(item, path);
			}
			return null;
		}

		@Override
		public InputStream getSdcardData(XulWorker.DownloadItem item, String path) {
			if (_handler != null) {
				return _handler.getSdcardData(item, path);
			}
			return null;
		}
	};

	public XulRenderContext(XulPage page, ArrayList<XulSelect> globalSelectors, ArrayList<XulBinding> globalBindings, IXulRenderHandler handler, int pageWidth, int pageHeight) {
		this(page, globalSelectors, globalBindings, handler, pageWidth, pageHeight, false, false);
	}

	public XulRenderContext(XulPage page, ArrayList<XulSelect> globalSelectors, ArrayList<XulBinding> globalBindings, IXulRenderHandler handler, int pageWidth, int pageHeight, final boolean suspend, boolean noGlobalBinding) {
		this(page, globalSelectors, globalBindings, handler, pageWidth, pageHeight, suspend, noGlobalBinding, false);
	}

	public XulRenderContext(XulPage page, ArrayList<XulSelect> globalSelectors, ArrayList<XulBinding> globalBindings, IXulRenderHandler handler, int pageWidth, int pageHeight, final boolean suspend, boolean noGlobalBinding, boolean doNotInit) {
		_globalSelectors = globalSelectors;
		_page = page.makeClone(this, pageWidth, pageHeight);
		_handler = handler;
		if (!noGlobalBinding) {
			_page.setGlobalBindings(globalBindings);
		}
		_suspended = suspend;
		if (doNotInit) {
			return;
		}
		initXulRender();
	}

	private boolean _initialized = false;

	public void initXulRender() {
		if (_initialized) {
			return;
		}
		_initialized = true;
		_page.preloadBinding(_downloader);

		XulPage.invokeActionNoPopup(_page, "load", actionCallback);

		if (!_applyBinding()) {
			_page.applySelectors();
			_page.prepareRender(this);
			_page.getLayout().initFocus();
			doLayout();
			syncData();
		}

		XulView focusTrackerView = getLayout().findItemById("@focus-tracker");
		if (focusTrackerView != null) {
			_focusTracker = new FocusTracker(focusTrackerView);
		}

		XulWorker.registerDownloader(_downloader);

		uiRun(new Runnable() {
			Rect _updateRect = new Rect();

			@Override
			public void run() {
				XulPage page = _page;
				if (page == null) {
					return;
				}
				if (_suspended) {
					uiRun(this, 50);
					return;
				}
				final float slowDown = 1f;
				long beginTime = XulUtils.timestamp();
				if (_readyToDraw) {
					final long timestamp = (long) (beginTime * slowDown);
					long newAniTimestamp = (timestamp + 4) & ~0x0007L;
					if (newAniTimestamp != _animationTimestamp) {
						_animationTimestamp = newAniTimestamp;
						if (!_animation.isEmpty()) {
							updateAnimation();
						}
					}

					if (!_dirtyViews.isEmpty()) {
						updateWholeView();
						// updateDirtyRect();
					}
					XulLayout layout = page.getLayout();
					if (layout != null) {
						XulViewRender render = layout.getRender();
						if (render.isLayoutChanged()) {
							updateWholeView();
						}
					}
				}
				long endTime = XulUtils.timestamp();
				long execDelay = endTime - beginTime;
				if (execDelay >= 16) {
					uiRun(this);
				} else {
					uiRun(this, (int) ((12 - execDelay) / slowDown));
				}
			}

			private void updateAnimation() {
				long timestamp = animationTimestamp();
				int curSize = _animation.size();
				for (int i = 0; i < curSize; ) {
					IXulAnimation ani = _animation.get(i);
					if (ani.updateAnimation(timestamp) == false) {
						_animation.remove(i);
						--curSize;
					} else {
						++i;
					}
				}
			}

			private void updateWholeView() {
				_dirtyViews.clear();
				if (!initDraw()) {
					return;
				}
				XulWorker.suspendDrawableWorker(16);
				invalidate(null);
			}

			private void updateDirtyRect() {
				_updateRect.set(99999, 99999, 0, 0);

				for (XulView view : _dirtyViews.values()) {
					RectF rc = view.getRender().getUpdateRect();
					if (_updateRect.left > rc.left) {
						_updateRect.left = XulUtils.roundToInt(rc.left);
					}
					if (_updateRect.top > rc.top) {
						_updateRect.top = XulUtils.roundToInt(rc.top);
					}
					if (_updateRect.right < rc.right) {
						_updateRect.right = XulUtils.roundToInt(rc.right);
					}
					if (_updateRect.bottom < rc.bottom) {
						_updateRect.bottom = XulUtils.roundToInt(rc.bottom);
					}
				}
				_dirtyViews.clear();
				invalidate(_updateRect);
			}
		}, 20);
	}

	public void resetUiTaskCollector() {
		XulTaskCollector taskCollector = _taskCollector;
		if (taskCollector == null) {
			return;
		}
		taskCollector.reset();
	}

	public void destroy() {
		_readyToDraw = false;
		XulWorker.unregisterDownloader(_downloader);
		if (_page != null) {
			_page.destroy();
			_page = null;
		}
		_handler = null;
	}

	public XulLayout getLayout() {
		return _page.getLayout();
	}

	public XulView findItemById(String id) {
		if (_page == null) {
			return null;
		}
		return _page.findItemById(id);
	}

	public XulArrayList<XulView> findItemsByClass(String... cls) {
		if (_page == null) {
			return null;
		}
		return _page.findItemsByClass(cls);
	}

	public XulView findCustomItemByExtView(IXulExternalView extView) {
		if (_page == null) {
			return null;
		}
		return _page.findCustomItemByExtView(extView);
	}

	public float getXScalar() {
		return _page.getXScalar();
	}

	public float getYScalar() {
		return _page.getYScalar();
	}

	XulUtils.ticketMarker _tsMarker;

	{
		if (XulManager.PERFORMANCE_BENCH) {
			_tsMarker = new XulUtils.ticketMarker("render duration ", false);
		}
	}

	public boolean beginDraw(Canvas canvas, Rect rect) {
		if (!initDraw()) {
			return false;
		}
		return beginDrawEx(canvas, rect);
	}

	public boolean beginDrawEx(Canvas canvas, Rect rect) {
		if (_tsMarker != null) {
			_tsMarker.mark("");
		}
		_xulDC.beginDraw(canvas);
		if (_tsMarker != null) {
			_tsMarker.mark("begin");
		}
		_page.draw(_xulDC, rect, 0, 0);
		if (_tsMarker != null) {
			_tsMarker.mark("page");
		}
		return true;
	}

	public boolean initDraw() {
		// if (!_readyToDraw) {
		// 	return false;
		// }
		if (_tsMarker != null) {
			_tsMarker.reset();
		}
		if (_page == null) {
			return false;
		}
		if (_tsMarker != null) {
			_tsMarker.mark();
		}
		if (!_dirtyViews.isEmpty()) {
			// Rect focusRc = _page.getLayout().getFocusRc();
			// Rect updateRc = new Rect(focusRc.right, focusRc.bottom, focusRc.left, focusRc.top);
			// for (XulView view : _dirtyViews) {
			// 	Rect viewRC = view.getFocusRc();
			// 	if (updateRc.bottom < viewRC.bottom) {
			// 		updateRc.bottom = viewRC.bottom;
			// 	}
			// 	if (updateRc.right < viewRC.right) {
			// 		updateRc.right = viewRC.right;
			// 	}
			// 	if (updateRc.left > viewRC.left) {
			// 		updateRc.left = viewRC.left;
			// 	}
			// 	if (updateRc.top > viewRC.top) {
			// 		updateRc.top = viewRC.top;
			// 	}
			// }
			// _dirtyViews.clear();
			// rect = updateRc;
		}

		// canvas.save();
		// canvas.clipRect(rect);
		doLayout();
		if (_tsMarker != null) {
			_tsMarker.mark("layout");
		}

		syncData();
		if (_tsMarker != null) {
			_tsMarker.mark("syncData");
		}

		XulLayout layout = _page.getLayout();
		if (layout != null) {
			XulViewRender render = layout.getRender();
			if (render != null && render.isLayoutChanged()) {
				internalDoLayout();
				if (render.isLayoutChanged()) {
					Log.e(TAG, "invalid layout state!!!!");
				}
				_page.markDirtyView();
			}
		}
		return true;
	}

	private void syncData() {
		for (int round = 0; !_skipSyncData && round < 3 && !_changedViews.isEmpty(); ++round) {
			int viewNum = _changedViews.size();
			int emptyNum = 0;
			for (int i = 0; i < viewNum; ++i) {
				XulViewRender changedView = _changedViews.get(i);
				if (changedView == null) {
					// FIXME: find the root cause
					emptyNum++;
					continue;
				}
				if (changedView.getDrawingRect() == null) {
					_changedViews.add(changedView);
					emptyNum++;
					continue;
				}
				try {
					changedView.doSyncData();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			_changedViews.remove(0, viewNum);
			if (emptyNum == viewNum) {
				// skip sync data operation, until layout or another view has been changed
				_skipSyncData = true;
				break;
			}
		}
	}

	public static void suspendDrawableWorker() {
		XulWorker.suspendDrawableWorker(80);
	}

	public void endDraw() {
		if (_tsMarker != null) {
			_tsMarker.mark("delay");
		}
		_xulDC.endDraw();
		if (_tsMarker != null) {
			_tsMarker.mark("end");
		}

		if (_tsMarker != null) {
			Log.d(TAG, _tsMarker.toString());
		}
	}

	// 对元素进行布局
	public void doLayout() {
		if (internalDoLayout()) {
			return;
		}

		int protectRangeBegin = _scheduledLayoutFinishTasksProtectRange;
		int protectRangeEnd = _scheduledLayoutFinishedTasks.size();
		if (protectRangeEnd > 0) {
			_scheduledLayoutFinishTasksProtectRange = protectRangeEnd;
			Runnable[] data = _scheduledLayoutFinishedTasks.getArray();
			for (int i = protectRangeBegin; i < protectRangeEnd; i++) {
				Runnable runnable = data[i];
				data[i] = null;
				try {
					runnable.run();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			_scheduledLayoutFinishedTasks.remove(protectRangeBegin, protectRangeEnd);
			_scheduledLayoutFinishTasksProtectRange = protectRangeBegin;
		}
	}

	public boolean internalDoLayout() {
		XulPage page = _page;
		if (page == null) {
			return true;
		}
		XulLayout layout = page.getLayout();
		if (layout == null) {
			return true;
		}
		XulViewRender render = layout.getRender();
		if (render == null) {
			return true;
		}
		if (render.isLayoutChanged()) {
			_skipSyncData = false;
		}
		page.doLayout(0, 0);
		return false;
	}

	public void scheduleLayoutFinishedTask(Runnable runnable) {
		if (_scheduledLayoutFinishedTasks.contains(runnable)) {
			return;
		}
		_scheduledLayoutFinishedTasks.add(runnable);
	}

	private ArrayList<WeakReference<XulView>> _findViewCache = new ArrayList<WeakReference<XulView>>();

	ArrayList<WeakReference<XulView>> findViewsByPoint(final int event, final PointF pt) {
		_findViewCache.clear();
		_page.getLayout().eachChild(new XulArea.XulAreaIterator() {

			@Override
			public boolean onXulArea(int pos, XulArea area) {
				if (area.hitTest(event, pt.x, pt.y)) {
					float x = pt.x, y = pt.y;
					area.hitTestTranslate(pt);
					_findViewCache.add(area.getWeakReference());
					area.eachChild(this);
					pt.set(x, y);
				}
				return super.onXulArea(pos, area);
			}

			@Override
			public boolean onXulItem(int pos, XulItem item) {
				if (item.hitTest(event, pt.x, pt.y)) {
					_findViewCache.add(item.getWeakReference());
				}
				return super.onXulItem(pos, item);
			}
		});
		return _findViewCache;
	}

	WeakReference<XulView> lastMotionDownEventView;
	private static PointF _motionEventsHitTestPt = new PointF();

	public boolean onMotionEvent(MotionEvent event) {
		XulLayout layout;
		if (_page == null || (layout = _page.getLayout()) == null) {
			lastMotionDownEventView = null;
			return false;
		}
		float x = event.getX();
		float y = event.getY();

		int action = event.getAction();
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_HOVER_MOVE:
		case MotionEvent.ACTION_MOVE:
			break;
		case MotionEvent.ACTION_SCROLL: {
			_motionEventsHitTestPt.set(x, y);
			ArrayList<WeakReference<XulView>> viewsByPoint = findViewsByPoint(XulManager.HIT_EVENT_SCROLL, _motionEventsHitTestPt);
			if (viewsByPoint.isEmpty()) {
				return false;
			}
			float hScroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
			float vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
			for (int i = viewsByPoint.size() - 1; i >= 0; --i) {
				XulView xulView = viewsByPoint.get(i).get();
				if (xulView != null && xulView.handleScrollEvent(hScroll, vScroll)) {
					viewsByPoint.clear();
					xulView.resetRender();
					return true;
				}
			}
		}
		break;
		case MotionEvent.ACTION_DOWN: {
			lastMotionDownEventView = null;
			_motionEventsHitTestPt.set(x, y);
			ArrayList<WeakReference<XulView>> viewsByPoint = findViewsByPoint(XulManager.HIT_EVENT_DOWN, _motionEventsHitTestPt);
			if (viewsByPoint.isEmpty()) {
				return false;
			}
			for (int i = 0, viewsByPointSize = viewsByPoint.size(); i < viewsByPointSize; i++) {
				XulView xulView = viewsByPoint.get(i).get();
				if (xulView != null && xulView.focusable()) {
					lastMotionDownEventView = xulView.getWeakReference();
					layout.requestFocus(xulView);
					viewsByPoint.clear();
					return true;
				}
			}
		}
		break;
		case MotionEvent.ACTION_UP: {
			_motionEventsHitTestPt.set(x, y);
			ArrayList<WeakReference<XulView>> viewsByPoint = findViewsByPoint(XulManager.HIT_EVENT_UP, _motionEventsHitTestPt);
			if (viewsByPoint.isEmpty() || lastMotionDownEventView == null) {
				lastMotionDownEventView = null;
				return false;
			}
			for (int i = 0, viewsByPointSize = viewsByPoint.size(); i < viewsByPointSize; i++) {
				XulView xulView = viewsByPoint.get(i).get();
				if (xulView != null && xulView.focusable()) {
					viewsByPoint.clear();
					XulView downEventView = lastMotionDownEventView.get();
					if (downEventView == xulView) {
						layout.doClick(actionCallback);
					}
					return true;
				}
			}
		}
		break;
		}

		return false;
	}

	int lastKeyAction;
	int lastKeyCode;
	boolean lastDownHandled;
	WeakReference<XulView> lastKeyEventView;

	public boolean onKeyEvent(KeyEvent event) {
		XulLayout layout;
		if (_page == null || (layout = _page.getLayout()) == null) {
			lastKeyAction = -1;
			lastKeyCode = -1;
			lastKeyEventView = null;
			lastDownHandled = false;
			return false;
		}
		int keyAction = event.getAction();
		int keyCode = event.getKeyCode();
		XulView focusView = layout.getFocus();
		boolean ret = false;

		if (keyAction == KeyEvent.ACTION_DOWN) {
			if (layout.onKeyEvent(event)) {
				lastKeyAction = keyAction;
				lastKeyCode = keyCode;
				lastKeyEventView = getWeakReference(focusView);
				ret = true;
			} else switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_LEFT:
				ret = layout.moveFocus(XulLayout.FocusDirection.MOVE_LEFT);
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				ret = layout.moveFocus(XulLayout.FocusDirection.MOVE_RIGHT);
				break;
			case KeyEvent.KEYCODE_DPAD_UP:
				ret = layout.moveFocus(XulLayout.FocusDirection.MOVE_UP);
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				ret = layout.moveFocus(XulLayout.FocusDirection.MOVE_DOWN);
				break;
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_ENTER:
				ret = layout.doClick(actionCallback);
				break;
			case KeyEvent.KEYCODE_BACK:
				ret = _page.popStates();
				if (ret) {
					XulPage.invokeActionNoPopup(_page, "statesRestored");
				}
				break;
			}
			lastDownHandled = ret;
		} else if (keyAction == KeyEvent.ACTION_UP) {
			XulView xulView = lastKeyEventView == null ? null : lastKeyEventView.get();
			if (xulView != focusView) {
				ret = lastDownHandled;
			} else if (layout.onKeyEvent(event)) {
				lastKeyAction = keyAction;
				lastKeyCode = keyCode;
				lastKeyEventView = getWeakReference(focusView);
				ret = true;
			} else switch (keyCode) {
			default:
				ret = lastDownHandled;
				break;
			}
			lastDownHandled = false;
		}

		lastKeyAction = keyAction;
		lastKeyCode = keyCode;
		lastKeyEventView = getWeakReference(focusView);

		if (ret) {
			suspendDrawableWorker();
		}
		return ret;
	}

	private WeakReference<XulView> getWeakReference(XulView focusView) {
		if (focusView == null) {
			return null;
		}
		return focusView.getWeakReference();
	}

	public XulCreator newXulCreator() {
		return new XulCreator();
	}

	public class XulCreator {
		private XulSimpleArray<XulView> _newNodes = new XulSimpleArray<XulView>(32) {
			@Override
			protected XulView[] allocArrayBuf(int size) {
				return new XulView[size];
			}
		};

		protected void addNewNode(XulArea area) {
			XulView[] nodes = _newNodes.getArray();
			for (int i = _newNodes.size() - 1; i >= 0; --i) {
				XulView node = nodes[i];
				if (node.isChildOf(area)) {
					_newNodes.remove(i);
				}
			}
			_newNodes.add(area);
		}

		protected void addNewNode(XulItem item) {
			XulView[] nodes = _newNodes.getArray();
			for (int i = _newNodes.size() - 1; i >= 0; --i) {
				XulView node = nodes[i];
				if (node instanceof XulItem) {
					continue;
				}
				if (item.isChildOf(node)) {
					return;
				}
			}
			_newNodes.add(item);
		}

		public XulArea createChildArea(XulArea parent, int pos, String id, String type, String binding) {
			XulArea area = new XulArea(parent, pos, type);
			area._id = id;
			area._binding = binding;
			addNewNode(area);
			return area;
		}

		public XulArea createChildArea(XulArea parent, int pos, String id, String type) {
			return createChildArea(parent, pos, id, type, null);
		}

		public XulArea createChildArea(XulArea parent, int pos, String type) {
			return createChildArea(parent, pos, null, type);
		}

		public XulArea createChildArea(XulArea parent, String id, String type, String binding) {
			return createChildArea(parent, -1, id, type, binding);
		}

		public XulArea createChildArea(XulArea parent, String id, String type) {
			return createChildArea(parent, -1, id, type, null);
		}

		public XulArea createChildArea(XulArea parent, String type) {
			return createChildArea(parent, -1, null, type);
		}

		public XulItem createChildItem(XulArea parent, int pos, String id, String type, String binding) {
			XulItem item = new XulItem(parent, pos);
			item._id = id;
			item._type = type;
			item._binding = binding;
			addNewNode(item);
			return item;
		}

		public XulItem createChildItem(XulArea parent, int pos, String id, String type) {
			return createChildItem(parent, pos, id, type, null);
		}

		public XulItem createChildItem(XulArea parent, int pos, String id) {
			return createChildItem(parent, pos, id, null, null);
		}

		public XulItem createChildItem(XulArea parent, String id, String type, String binding) {
			return createChildItem(parent, -1, id, type, binding);
		}

		public XulItem createChildItem(XulArea parent, String id, String type) {
			return createChildItem(parent, -1, id, type, null);
		}

		public XulItem createChildItem(XulArea parent, String type) {
			return createChildItem(parent, -1, null, type);
		}

		public XulFocus createFocusProp(XulView owner, final String mode, final String priority) {
			XulFocus._Builder builder = XulFocus._Builder.create(owner);
			builder.initialize("item", new XulFactory.Attributes() {
				@Override
				public String getValue(String name) {
					if ("mode".equals(name)) {
						return mode;
					}
					if ("priority".equals(name)) {
						return priority;
					}
					return null;
				}

				@Override
				public String getValue(int i) {
					return null;
				}

				@Override
				public int getLength() {
					return 0;
				}

				@Override
				public String getName(int i) {
					return null;
				}
			});
			return (XulFocus) builder.finalItem();
		}

		public XulFocus createFocusProp(XulView owner, String mode) {
			return createFocusProp(owner, mode, null);
		}

		public void finish() {
			XulView[] nodes = _newNodes.getArray();
			XulRenderContext ctx = XulRenderContext.this;
			for (int i = 0, size = _newNodes.size(); i < size; i++) {
				XulView node = nodes[i];
				node.prepareRender(ctx);
				if (node instanceof XulArea) {
					_page.addSelectorTargets((XulArea) node);
					_page.applySelectors((XulArea) node);
				} else {
					_page.addSelectorTarget((XulItem) node);
					_page.applySelectors((XulItem) node);
				}
			}
		}
	}
}
