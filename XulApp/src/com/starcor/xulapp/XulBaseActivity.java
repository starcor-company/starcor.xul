package com.starcor.xulapp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.starcor.xul.IXulExternalView;
import com.starcor.xul.XulManager;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulView;
import com.starcor.xul.XulWorker;
import com.starcor.xulapp.behavior.XulBehaviorManager;
import com.starcor.xulapp.behavior.XulUiBehavior;
import com.starcor.xulapp.debug.XulDebugMonitor;
import com.starcor.xulapp.debug.XulDebugServer;
import com.starcor.xulapp.utils.SystemUiHider;

import java.io.InputStream;

public class XulBaseActivity extends Activity implements XulPresenter {
	public static final int AUTO_HIDE_DELAY_MILLIS = 10;

	public static final String XPARAM_PAGE_ID = "xul_page_id";
	public static final String XPARAM_PAGE_BEHAVIOR = "xul_page_behavior";
	public static final String XPARAM_LAYOUT_FILE = "xul_layout_file";
	public static final String XPARAM_BEHAVIOR_PARAMS = "xul_behavior_params";

	public static final int XCUSTOM_OBJ_PRESENTER = 1;

	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	private SystemUiHider _systemUiHider;
	protected final XulDebugMonitor _dbgMonitor;

	private static class XulFrameLayout extends FrameLayout {

		public XulFrameLayout(Context context) {
			super(context);
			initBackground();
		}

		public XulFrameLayout(Context context, AttributeSet attrs) {
			super(context, attrs);
			initBackground();
		}

		public XulFrameLayout(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
			initBackground();
		}

		private void initBackground() {
			setBackgroundDrawable(new Drawable() {
				@Override
				public void draw(Canvas canvas) {

				}

				@Override
				public void setAlpha(int alpha) {

				}

				@Override
				public void setColorFilter(ColorFilter cf) {

				}

				@Override
				public int getOpacity() {
					return 0;
				}
			});
		}

		public boolean defaultDispatchKeyEvent(KeyEvent event) {
			return super.dispatchKeyEvent(event);
		}

		public boolean defaultDispatchTouchEvent(MotionEvent event) {
			return super.dispatchTouchEvent(event);
		}
	}

	public XulBaseActivity() {
		super();
		_dbgMonitor = XulDebugServer.getMonitor();
	}

	@Override
	protected void onDestroy() {
		if (_dbgMonitor != null) {
			_dbgMonitor.onPageDestroy(this);
		}

		xulOnDestroy();
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		xulOnNewIntent(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (_dbgMonitor != null) {
			_dbgMonitor.onPageCreate(this);
		}

		_xulFrameLayout = new XulFrameLayout(this) {

			Rect _updateRc;

			@Override
			protected void dispatchDraw(Canvas canvas) {
				if (_updateRc == null) {
					_updateRc = new Rect();
				}
				this.getDrawingRect(_updateRc);
				if (_updateRc.isEmpty()) {
					_updateRc.set(0, 0, XulManager.getPageWidth(), XulManager.getPageHeight());
				}
				XulDebugMonitor dbgMonitor = _dbgMonitor;
				if (dbgMonitor != null) {
					long beginTime = XulUtils.timestamp_us();
					if (_xulRenderContext != null && _xulRenderContext.beginDraw(canvas, _updateRc)) {
						super.dispatchDraw(canvas);
						_xulRenderContext.endDraw();
						dbgMonitor.drawDebugInfo(_xulRenderContext, canvas);
						long endTime = XulUtils.timestamp_us();
						dbgMonitor.onPageRefreshed(XulBaseActivity.this, endTime - beginTime);
						return;
					}
					super.dispatchDraw(canvas);
					dbgMonitor.drawDebugInfo(_xulRenderContext, canvas);
				} else {
					if (_xulRenderContext != null && _xulRenderContext.beginDraw(canvas, _updateRc)) {
						super.dispatchDraw(canvas);
						_xulRenderContext.endDraw();
						return;
					}
					super.dispatchDraw(canvas);
				}
			}

			@Override
			public boolean dispatchKeyEvent(KeyEvent event) {
				if (xulOnDispatchKeyEvent(event)) {
					return true;
				}
				return xulDefaultDispatchKeyEvent(event);
			}

			@Override
			public boolean dispatchTouchEvent(MotionEvent event) {
				if (xulOnDispatchTouchEvent(event)) {
					return true;
				}
				return xulDefaultDispatchTouchEvent(event);
			}
		};

		Intent intent = getIntent();
		_intentPageId = intent.getStringExtra(XPARAM_PAGE_ID);
		_intentLayoutFile = intent.getStringExtra(XPARAM_LAYOUT_FILE);
		_intentBehavior = intent.getStringExtra(XPARAM_PAGE_BEHAVIOR);

		xulPreCreate();
		xulOnInitXulBehavior(_intentBehavior);
		xulOnLoadLayoutFile(_intentLayoutFile);
		xulCreatePage(_intentPageId);
		xulOnRenderCreated();
		xulOnInitXulRender();

		_xulRootView = null;
		if (_xulBehavior != null) {
			_xulRootView = _xulBehavior.initRenderContextView(_xulFrameLayout);
		}
		if (_xulRootView == null) {
			_xulRootView = _xulFrameLayout;
		}
		setContentView(_xulRootView);

		final ViewGroup.LayoutParams layoutParams = _xulFrameLayout.getLayoutParams();
		layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
		layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;

		_xulFrameLayout.setFocusable(true);
		_xulFrameLayout.setFocusableInTouchMode(true);

		_systemUiHider = SystemUiHider.getInstance(this, _xulFrameLayout, HIDER_FLAGS);
		_systemUiHider.setup();
		_systemUiHider.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
			@Override
			@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
			public void onVisibilityChange(boolean visible) {
				if (visible) {
					delayedHide(AUTO_HIDE_DELAY_MILLIS);
				}
			}
		});
	}

	protected void xulPreCreate() {
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		delayedHide(10);
	}

	@Override
	protected void onRestart() {
        xulOnRestart();
		super.onRestart();
	}

	@Override
	protected void onStart() {
        xulOnStart();
		super.onStart();
	}

	@Override
	protected void onResume() {
		if (_dbgMonitor != null) {
			_dbgMonitor.onPageResumed(this);
		}

		xulOnResume();
		super.onResume();
	}

	@Override
	protected void onPause() {
		if (_dbgMonitor != null) {
			_dbgMonitor.onPagePaused(this);
		}

		xulOnPause();
		super.onPause();
	}

	@Override
	protected void onStop() {
		if (_dbgMonitor != null) {
			_dbgMonitor.onPageStopped(this);
		}

		xulOnStop();
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		if (xulOnBackPressed()) {
			return;
		}
		super.onBackPressed();
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        xulOnSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        xulOnRestoreInstanceState(savedInstanceState);
        super.onRestoreInstanceState(savedInstanceState);
    }

    protected Handler _commonHandler = new Handler();
	Runnable _hideRunnable = new Runnable() {
		@Override
		public void run() {
			_systemUiHider.hide();
		}
	};

	private void delayedHide(int delayMillis) {
		_commonHandler.removeCallbacks(_hideRunnable);
		_commonHandler.postDelayed(_hideRunnable, delayMillis);
	}

	/// XUL APIs -------------------------
	private String _intentPageId;   // page id from start intent
	private String _curPageId;      // page id actually used
	private String _intentLayoutFile;
	private String _curLayoutFile;
	private String _intentBehavior = null;
	private String _currentBehavior;

	private View _xulRootView;
	private XulFrameLayout _xulFrameLayout;
	private XulRenderContext _xulRenderContext;
	protected XulUiBehavior _xulBehavior;

	protected void xulCreatePage(final String pageId) {
		_curPageId = pageId;
		XulRenderContext.IXulRenderHandler xulRenderHandler = new XulRenderContext.IXulRenderHandler2() {
			@Override
			public Object getCustomObject(int key) {
				switch (key) {
				case XCUSTOM_OBJ_PRESENTER:
					return XulBaseActivity.this;
				}
				return null;
			}

			@Override
			public void invalidate(Rect rect) {
				_xulFrameLayout.invalidate();
			}

			@Override
			public void uiRun(Runnable runnable) {
				_commonHandler.post(runnable);
			}

			@Override
			public void uiRun(Runnable runnable, int delayMS) {
				_commonHandler.postDelayed(runnable, delayMS);
			}

			@Override
			public void onDoAction(XulView view, String action, String type, String command, Object userdata) {
				xulDoAction(view, action, type, command, userdata);
			}

			@Override
			public IXulExternalView createExternalView(String cls, int x, int y, int width, int height, XulView view) {
				return xulCreateExternalView(cls, x, y, width, height, view);
			}

			@Override
			public String resolve(XulWorker.DownloadItem item, String path) {
				return xulResolvePath(item, path);
			}

			@Override
			public InputStream getAssets(XulWorker.DownloadItem item, String path) {
				return xulGetAssets(item, path);
			}

			@Override
			public InputStream getAppData(XulWorker.DownloadItem item, String path) {
				return xulGetAppData(item, path);
			}

			@Override
			public void onRenderIsReady() {
				xulOnRenderIsReady();
			}

			@Override
			public void onRenderEvent(int eventId, int param1, int param2, Object msg) {
				xulOnRenderEvent(eventId, param1, param2, msg);
			}
		};
		_xulRenderContext = XulManager.createXulRender(pageId, xulRenderHandler, 0, 0, false, false, true);
	}

	protected void xulOnRenderCreated() {
		final XulUiBehavior xulBehavior = _xulBehavior;
		if (xulBehavior != null) {
			xulBehavior.xulOnRenderCreated();
		}
	}

	protected void xulOnLoadLayoutFile(String layoutFile) {
		final XulUiBehavior xulBehavior = _xulBehavior;
		if (xulBehavior != null) {
			xulBehavior.xulLoadLayoutFile(layoutFile);
			return;
		}
		xulLoadLayoutFile(layoutFile);
	}

	@Override
	public void xulLoadLayoutFile(String layoutFile) {
		_curLayoutFile = layoutFile;
		if (TextUtils.isEmpty(layoutFile)) {
			return;
		}
		if (XulManager.isXulLoaded(layoutFile)) {
			return;
		}
		XulApplication.getAppInstance().xulLoadLayouts(layoutFile);
	}

	protected void xulOnInitXulBehavior(String behavior) {
		_currentBehavior = behavior;
		_xulBehavior = XulBehaviorManager.obtainBehavior(behavior, this);
	}

	protected void xulOnInitXulRender() {
		if (_xulRenderContext != null) {
			_xulRenderContext.initXulRender();
		}
	}

	protected void xulOnDestroy() {
		final XulUiBehavior xulBehavior = _xulBehavior;
		_xulBehavior = null;
		if (xulBehavior != null) {
			xulBehavior.xulOnDestroy();
		}

		XulRenderContext xulRenderContext = _xulRenderContext;
		_xulRenderContext = null;
		if (xulRenderContext != null) {
			xulRenderContext.destroy();
		}
	}

    protected void xulOnRestart() {
        final XulUiBehavior xulBehavior = _xulBehavior;
        if (xulBehavior != null) {
            xulBehavior.xulOnRestart();
        }
    }

    protected void xulOnStart() {
        final XulUiBehavior xulBehavior = _xulBehavior;
        if (xulBehavior != null) {
            xulBehavior.xulOnStart();
        }

        final XulRenderContext xulRenderContext = _xulRenderContext;
        if (xulRenderContext != null) {
            xulRenderContext.suspend(false);
        }
    }

	protected void xulOnResume() {
		final XulUiBehavior xulBehavior = _xulBehavior;
		if (xulBehavior != null) {
			xulBehavior.xulOnResume();
		}

		final XulRenderContext xulRenderContext = _xulRenderContext;
		if (xulRenderContext != null) {
			xulRenderContext.suspend(false);
		}
	}

	protected void xulOnStop() {
		final XulUiBehavior xulBehavior = _xulBehavior;
		if (xulBehavior != null) {
			xulBehavior.xulOnStop();
		}

		final XulRenderContext xulRenderContext = _xulRenderContext;
		if (xulRenderContext != null) {
			xulRenderContext.suspend(true);
		}
	}

	protected void xulOnPause() {
		final XulUiBehavior xulBehavior = _xulBehavior;
		if (xulBehavior != null) {
			xulBehavior.xulOnPause();
		}

		final XulRenderContext xulRenderContext = _xulRenderContext;
		if (xulRenderContext != null) {
			xulRenderContext.suspend(true);
		}
	}

	protected boolean xulOnBackPressed() {
		final XulUiBehavior xulBehavior = _xulBehavior;
		if (xulBehavior != null) {
			return xulBehavior.xulOnBackPressed();
		}
		return false;
	}

	protected void xulOnNewIntent(Intent intent) {
		final XulUiBehavior xulBehavior = _xulBehavior;
		if (xulBehavior != null) {
			xulBehavior.xulOnNewIntent(intent);
		}
	}

    protected void xulOnSaveInstanceState(Bundle outState) {
        final XulUiBehavior xulBehavior = _xulBehavior;
        if (xulBehavior != null) {
            xulBehavior.xulOnSaveInstanceState(outState);
        }
    }

    protected void xulOnRestoreInstanceState(Bundle savedInstanceState) {
        final XulUiBehavior xulBehavior = _xulBehavior;
        if (xulBehavior != null) {
            xulBehavior.xulOnRestoreInstanceState(savedInstanceState);
        }
    }

	@Override
	public boolean xulDefaultDispatchKeyEvent(KeyEvent event) {
		return _xulFrameLayout.defaultDispatchKeyEvent(event);
	}

	@Override
	public boolean xulDefaultDispatchTouchEvent(MotionEvent event) {
		return _xulFrameLayout.defaultDispatchTouchEvent(event);
	}

	public boolean xulOnDispatchTouchEvent(MotionEvent event) {
		if (_xulBehavior != null) {
			return _xulBehavior.xulOnDispatchTouchEvent(event);
		}

		if (_xulRenderContext != null && _xulRenderContext.onMotionEvent(event)) {
			return true;
		}
		return false;
	}

	public boolean xulOnDispatchKeyEvent(KeyEvent event) {
		if (_xulBehavior != null) {
			return _xulBehavior.xulOnDispatchKeyEvent(event);
		}

		if (_xulRenderContext != null && _xulRenderContext.onKeyEvent(event)) {
			return true;
		}
		return false;
	}

	@Override
	public String xulGetIntentPageId() {
		return _intentPageId;
	}

	@Override
	public String xulGetCurPageId() {
		return _curPageId;
	}

	@Override
	public String xulGetIntentLayoutFile() {
		return _intentLayoutFile;
	}

	@Override
	public String xulGetCurLayoutFile() {
		return _curLayoutFile;
	}

	@Override
	public String xulGetCurBehaviorName() {
		return _currentBehavior;
	}

	protected XulUiBehavior getXulBehavior(){
		return _xulBehavior;
	}

	protected void setXulCurrentContent(String pageId, String pageFile, String currentBehavior,XulUiBehavior xulBehavior, XulRenderContext currentRenderContext){
		if (pageId != null){
			this._curPageId = pageId;
		}
		if (pageFile != null){
			this._curLayoutFile = pageFile;
		}if (currentBehavior != null){
			this._currentBehavior = currentBehavior;
		}if (xulBehavior != null){
			this._xulBehavior = xulBehavior;
		}if (currentRenderContext != null){
			this._xulRenderContext = currentRenderContext;
		}
	}

	@Override
	public XulRenderContext xulGetRenderContext() {
		return _xulRenderContext;
	}

	@Override
	public FrameLayout xulGetRenderContextView() {
		return _xulFrameLayout;
	}


	public View xulGetRootView() {
		return _xulRootView;
	}

	@Override
	public void xulDestroy() {
		finish();
	}

	@Override
	public boolean xulIsAlive() {
		return !isFinishing();
	}

	@Override
	public Bundle xulGetBehaviorParams() {
		return getIntent().getBundleExtra(XPARAM_BEHAVIOR_PARAMS);
	}

	@Override
	public Context xulGetContext() {
		return this;
	}

	public IXulExternalView xulCreateExternalView(String cls, int x, int y, int width, int height, XulView view) {
		if (_xulBehavior != null) {
			return _xulBehavior.xulCreateExternalView(cls, x, y, width, height, view);
		}
		return null;
	}

	public String xulResolvePath(XulWorker.DownloadItem item, String path) {
		if (_xulBehavior != null) {
			return _xulBehavior.xulResolvePath(item, path);
		}
		return null;
	}

	public InputStream xulGetAssets(XulWorker.DownloadItem item, String path) {
		if (_xulBehavior != null) {
			return _xulBehavior.xulGetAssets(item, path);
		}
		return null;
	}

	public InputStream xulGetAppData(XulWorker.DownloadItem item, String path) {
		if (_xulBehavior != null) {
			return _xulBehavior.xulGetAppData(item, path);
		}
		return null;
	}

	protected void xulOnRenderIsReady() {
		if (_dbgMonitor != null) {
			_dbgMonitor.onPageRenderIsReady(this);
		}

		if (_xulBehavior != null) {
			_xulBehavior.xulOnRenderIsReady();
			return;
		}
	}

	protected void xulOnRenderEvent(int eventId, int param1, int param2, Object msg) {
		if (_xulBehavior != null) {
			_xulBehavior.xulOnRenderEvent(eventId, param1, param2, msg);
			return;
		}
	}

	public void xulDoAction(XulView view, String action, String type, String command, Object userdata) {
		if (_xulBehavior != null) {
			_xulBehavior.xulDoAction(view, action, type, command, userdata);
			return;
		}
	}

}
