package com.starcor.xuldemo.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.starcor.xul.IXulExternalView;
import com.starcor.xul.XulManager;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulView;
import com.starcor.xul.XulWorker;
import com.starcor.xuldemo.XulDemoApp;
import com.starcor.xuldemo.behavior.XulActivityBehavior;
import com.starcor.xuldemo.utils.log.LogUtil;

import junit.framework.Assert;

import java.io.InputStream;

/**
 * Created by ZFB on 2015/7/16 0016.
 */
public abstract class XulDemoBaseActivity extends Activity
	implements XulRenderContext.IXulRenderHandler {

	public static final String EXTRA_XUL_PAGE_ID = "xul-page-id";
	public static final String EXTRA_XUL_FILE_NAME = "xul-file-name";
	public static final String EXTRA_XUL_PAGE_BEHAVIOR = "xul-behavior";

	protected final String TAG = getClass().getSimpleName();

	protected String mXulFileName = null;
	protected String mPageId = null;
	protected String mXulPageBehavior = null;

	protected RelativeLayout mLayout;
	protected XulRenderContext mXulPageRender;
	private Handler mEventHandler = new Handler();
	private Object mRedrawWaitableObject = new Object();
	protected XulActivityBehavior mXulBehavior;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initLayout();

		if (!TextUtils.isEmpty(mXulPageBehavior)) {
			mXulBehavior = XulActivityBehavior.createBehavior(mXulPageBehavior);
		}

		if (mXulBehavior == null) {
			initXulRender();
		} else {
			mXulBehavior.initLayout(this, mLayout);
			initXulRender();
			mXulBehavior.initXulRender(mXulPageRender);
		}

		Window window = getWindow();
		window.addFlags(Window.FEATURE_NO_TITLE|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.hide();
		}
		mLayout.setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
				View.SYSTEM_UI_FLAG_FULLSCREEN |
				View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
				View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
				View.SYSTEM_UI_FLAG_LOW_PROFILE |
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
		);

		setContentView(mLayout);
	}

	private void initLayout() {
		mLayout = new RelativeLayout(this) {
			Rect drawingRc = new Rect();

			@Override
			protected void dispatchDraw(Canvas canvas) {
				getDrawingRect(drawingRc);
				if ((mXulPageRender != null) && mXulPageRender.beginDraw(canvas, drawingRc)) {
					super.dispatchDraw(canvas);
					mXulPageRender.endDraw();
				} else {
					super.dispatchDraw(canvas);
				}

				synchronized (mRedrawWaitableObject) {
					mRedrawWaitableObject.notifyAll();
				}
			}

			@Override
			protected void onFocusChanged(boolean gainFocus, int direction,
			                              Rect previouslyFocusedRect) {
				super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
				if (!gainFocus) {
					LogUtil.d(TAG, "focus lost!!!!");
					post(new Runnable() {
						@Override
						public void run() {
							LogUtil.d(TAG, "update focus!!!!");
							View view = findFocus();
							IXulExternalView xulView = null;
							if (view != null) {
								if (view instanceof IXulExternalView) {
									xulView = (IXulExternalView) view;
								} else {
									ViewParent vp = view.getParent();
									while (vp != null && !(vp instanceof IXulExternalView)) {
										vp = vp.getParent();
									}
									if (vp != null) {
										xulView = (IXulExternalView) vp;
									}
								}
							}
							if (xulView != null) {
								XulView customItemByExtView =
									mXulPageRender.findCustomItemByExtView(xulView);
								mXulPageRender.getLayout().requestFocus(customItemByExtView);
							}
						}
					});
				}
			}
		};
		mLayout.setFocusable(true);
		mLayout.setFocusableInTouchMode(true);
		mLayout.setLayoutParams(new RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	}

	private void initXulRender() {

		if (!TextUtils.isEmpty(mXulFileName) && !XulManager.isXulLoaded(mXulFileName)) {
			XulDemoApp.loadXulFile(this, mXulFileName);
		}

		Assert.assertFalse(TextUtils.isEmpty(mPageId));
		mXulPageRender = XulManager.createXulRender(mPageId, this);
		if (mXulPageRender == null) {
			LogUtil.e(TAG, "XulPage init failed!");
		}
		LogUtil.i(TAG, "XulPage init!");
	}

	public void waitUntilRedraw(int ms) {
		synchronized (mRedrawWaitableObject) {
			try {
				if (ms < 0) {
					mRedrawWaitableObject.wait();
				} else {
					mRedrawWaitableObject.wait(ms);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void waitUntilRedraw() {
		waitUntilRedraw(-1);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		LogUtil.i(TAG, "dispatchKeyEvent, KeyEvent=" + event);
		if ((mXulPageRender != null) && mXulPageRender.onKeyEvent(event)) {
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		LogUtil.i(TAG, "onGenericMotionEvent, MotionEvent=" + event);
		if ((mXulPageRender != null) && mXulPageRender.onMotionEvent(event)) {
			return true;
		}
		return super.onGenericMotionEvent(event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		LogUtil.i(TAG, "onTouchEvent, MotionEvent=" + event);
		if ((mXulPageRender != null) && mXulPageRender.onMotionEvent(event)) {
			return true;
		}
		return super.onTouchEvent(event);
	}

	@Override
	public void invalidate(Rect rect) {
		if (mLayout == null) {
			return;
		}
		mLayout.postInvalidate();
	}

	@Override
	public void uiRun(Runnable runnable) {
		mEventHandler.post(runnable);
	}

	@Override
	public void uiRun(Runnable runnable, int delayMS) {
		mEventHandler.postDelayed(runnable, delayMS);
	}

	@Override
	public void onDoAction(XulView view, String action, String type, String command,
	                       Object userdata) {
		if (mXulBehavior != null) {
			mXulBehavior.onDoAction(view, action, type, command, userdata);
		}
	}

	@Override
	public IXulExternalView createExternalView(String cls, int x, int y, int width, int height,
	                                           XulView view) {
		if (mXulBehavior == null) {
			return null;
		}
		return mXulBehavior.createExternalView(cls, x, y, width, height, view);
	}

	@Override
	public String resolve(XulWorker.DownloadItem item, String path) {
		if (mXulBehavior == null) {
			return null;
		}
		return mXulBehavior.resolve(item, path);
	}

	@Override
	public InputStream getAssets(XulWorker.DownloadItem item, String path) {
		if (mXulBehavior == null) {
			return null;
		}
		return mXulBehavior.getAssets(item, path);
	}

	@Override
	public InputStream getAppData(XulWorker.DownloadItem item, String path) {
		if (mXulBehavior == null) {
			return null;
		}
		return mXulBehavior.getAppData(item, path);
	}

	@Override
	public InputStream getSdcardData(XulWorker.DownloadItem item, String path) {
		return null;
	}

	@Override
	public void onRenderIsReady() {
		if (mXulBehavior != null) {
			mXulBehavior.onRenderIsReady();
		}
	}

	@Override
	public void onRenderEvent(int eventId, int param1, int param2, Object msg) {
		if (mXulBehavior != null) {
			mXulBehavior.onRenderEvent(eventId, param1, param2, msg);
		}
	}
}
