package com.starcor.xulapp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
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
import android.widget.RelativeLayout;
import com.starcor.xul.*;
import com.starcor.xulapp.behavior.XulBehaviorManager;
import com.starcor.xulapp.behavior.XulUiBehavior;
import com.starcor.xulapp.behavior.utils.*;
import com.starcor.xulapp.debug.XulDebugMonitor;
import com.starcor.xulapp.debug.XulDebugServer;
import com.starcor.xulapp.utils.SystemUiHider;

import java.io.InputStream;

public class MultiBehaviorActivity extends Activity implements IBehaviorContact {
    public static final int AUTO_HIDE_DELAY_MILLIS = 10;

    public static final String XPARAM_PAGE_ID = "xul_page_id";
    public static final String XPARAM_PAGE_BEHAVIOR = "xul_page_behavior";
    public static final String XPARAM_LAYOUT_FILE = "xul_layout_file";
    public static final String XPARAM_BEHAVIOR_PARAMS = "xul_behavior_params";

    public static final int XCUSTOM_OBJ_PRESENTER = 1;
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    private SystemUiHider _systemUiHider;
    protected boolean _useSystemUiHider = true;

    private String _intentPageId;
    private String _curPageId;
    private String _intentLayoutFile;
    private String _intentBehavior;
    private String _currentBehavior;

    private View _xulRootView;
    private XulFrameLayout _xulFrameLayout;
    private XulRenderContext _xulRenderContext;
    protected XulUiBehavior _xulBehavior;

    private BehaviorProcessor behaviorProcessor;

    protected final XulDebugMonitor _dbgMonitor;
    private XulBehaviorUnit xulBehaviorUnit;


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
                    return PixelFormat.UNKNOWN;
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

    public MultiBehaviorActivity() {
        super();
        _dbgMonitor = XulDebugServer.getMonitor();
    }

    @Override
    protected void onDestroy() {
        xulOnDestroy();
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        xulOnNewIntent(intent);
    }

    public float getXScale(Rect _renderRect) {
        if (_renderRect.width() > 0) {
            return ((float) _renderRect.width() / XulManager.getPageWidth());
        }
        return 1.0f;
    }

    public float getYScale(Rect _renderRect) {
        if (_renderRect.height() > 0) {
            return ((float) _renderRect.height() / XulManager.getPageHeight());
        }
        return 1.0f;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _xulFrameLayout = new XulFrameLayout(this) {
            Rect _updateRc;
            @Override
            protected void dispatchDraw(Canvas canvas) {
                if (xulBehaviorUnit == null || !xulBehaviorUnit.visible) {
                    super.dispatchDraw(canvas);
                    return;
                }
                if (xulBehaviorUnit != null) {
                    _updateRc = xulBehaviorUnit.updateRect;
                }
                if (_updateRc == null || _updateRc.isEmpty()) {
                    _updateRc.set(0, 0, XulManager.getPageWidth(), XulManager.getPageHeight());
                }
                preDraw(canvas, _updateRc);
                if (_xulRenderContext != null && _xulRenderContext.beginDraw(canvas, _updateRc)) {
                    _xulRenderContext.endDraw();
                }
                endDraw(canvas);
                super.dispatchDraw(canvas);
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
            @Override
            public boolean dispatchGenericMotionEvent(MotionEvent event) {
                if (xulOnDispatchTouchEvent(event)) {
                    return true;
                }
                return super.dispatchGenericMotionEvent(event);
            }
        };

        Intent intent = getIntent();
        _intentPageId = getPageId(intent);
        _intentLayoutFile = getLayoutFile(intent);
        _intentBehavior = getBehaviorName(intent);

        xulPreCreate();
        xulOnInitXulBehavior(_intentBehavior);
        xulOnLoadLayoutFile(_intentLayoutFile);
        xulCreatePage(_intentPageId);
        xulOnRenderCreated();
        xulPageCreated();

        if (_xulBehavior == null) {
            return;
        }
        _xulRootView = _xulBehavior.initRenderContextView(_xulFrameLayout);
        xulBehaviorUnit = new XulBehaviorUnit(_xulBehavior, _xulRenderContext);
        xulBehaviorUnit.setRootView(_xulRootView);
        xulBehaviorUnit.setLaunchMode(XulBehaviorUnit.LaunchMode.FLAG_BEHAVIOR_ATTACH_STACK);

        behaviorProcessor = getBehaviorProcessor();
        behaviorProcessor.add(xulBehaviorUnit);
        xulOnInitXulRender();
        setContentView(_xulRootView);

        if (_dbgMonitor != null) {
            _dbgMonitor.onPageCreate(_xulBehavior);
        }

        final ViewGroup.LayoutParams layoutParams = _xulFrameLayout.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;

        _xulFrameLayout.setFocusable(true);
        _xulFrameLayout.setFocusableInTouchMode(true);

        if (_useSystemUiHider) {
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
    }

    protected void xulPreCreate() {
    }


    private String getLayoutFile(Intent intent) {
        return intent == null ? "" : intent.getStringExtra(XPARAM_LAYOUT_FILE);
    }

    private String getPageId(Intent intent) {
        return intent == null ? "" : intent.getStringExtra(XPARAM_PAGE_ID);
    }

    private String getBehaviorName(Intent intent) {
        return intent == null ? "" :intent.getStringExtra(XPARAM_PAGE_BEHAVIOR);
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
        xulOnResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        xulOnPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
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
            if (_systemUiHider != null) {
                _systemUiHider.hide();
            }
        }
    };

    private void delayedHide(int delayMillis) {
        _commonHandler.removeCallbacks(_hideRunnable);
        _commonHandler.postDelayed(_hideRunnable, delayMillis);
    }


    protected void xulCreatePage(final String pageId) {
        _curPageId = pageId;
        XulRenderContext.IXulRenderHandler xulRenderHandler = getRenderHandler(_xulBehavior);
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

    public void xulLoadLayoutFile(String layoutFile) {
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
        _xulBehavior = XulBehaviorManager.obtainBehavior(behavior, new XulPresenter() {
            @Override
            public Context xulGetContext() {
                return MultiBehaviorActivity.this;
            }

            @Override
            public String xulGetIntentPageId() {
                return _curPageId;
            }

            @Override
            public String xulGetCurPageId() {
                return _curPageId;
            }

            @Override
            public String xulGetIntentLayoutFile() {
                return _currentBehavior;
            }

            @Override
            public String xulGetCurLayoutFile() {
                return _intentLayoutFile;
            }

            @Override
            public String xulGetCurBehaviorName() {
                return _currentBehavior;
            }

            @Override
            public XulRenderContext xulGetRenderContext() {
                return _xulRenderContext;
            }

            @Override
            public FrameLayout xulGetRenderContextView() {
                return _xulFrameLayout;
            }

            @Override
            public void xulLoadLayoutFile(String layoutFile) {
                if (TextUtils.isEmpty(layoutFile)) {
                    return;
                }
                if (XulManager.isXulLoaded(layoutFile)) {
                    return;
                }
                XulApplication.getAppInstance().xulLoadLayouts(layoutFile);
            }

            @Override
            public boolean xulDefaultDispatchKeyEvent(KeyEvent event) {
                return _xulFrameLayout.defaultDispatchKeyEvent(event);
            }

            @Override
            public boolean xulDefaultDispatchTouchEvent(MotionEvent event) {
                return _xulFrameLayout.defaultDispatchTouchEvent(event);
            }

            @Override
            public Bundle xulGetBehaviorParams() {
                return getIntent().getBundleExtra(XPARAM_BEHAVIOR_PARAMS);
            }

            @Override
            public void xulDestroy() {
                finish();
            }

            @Override
            public boolean xulIsAlive() {
                return !isFinishing();
            }
        });
    }

    protected void xulOnInitXulRender() {
        if (_xulRenderContext != null) {
            _xulRenderContext.initXulRender();
        }
    }

    protected void xulOnDestroy() {
        if (behaviorProcessor != null && behaviorProcessor.xulOnDestroy()) {
            return;
        }
    }

    protected void xulOnRestart() {
        if (behaviorProcessor != null && behaviorProcessor.xulOnRestart()) {
            return;
        }
    }

    protected void xulOnStart() {
        if (behaviorProcessor != null && behaviorProcessor.xulOnStart()) {
            return;
        }
    }

    protected void xulOnResume() {
        if (behaviorProcessor != null && behaviorProcessor.xulOnResume()) {
            return;
        }
    }

    protected void xulOnStop() {
        if (behaviorProcessor != null && behaviorProcessor.xulOnStop()) {
            return;
        }
    }

    protected void xulOnPause() {
        if (behaviorProcessor != null) {
            behaviorProcessor.xulOnPause();
        }
    }

    protected boolean xulOnBackPressed() {
        if (behaviorProcessor != null && behaviorProcessor.xulOnBackPressed()) {
            _xulRootView.invalidate();
            return true;
        }
        final XulUiBehavior xulBehavior = _xulBehavior;
        if (xulBehavior != null) {
            return xulBehavior.xulOnBackPressed();
        }
        return false;
    }

    protected void xulOnNewIntent(Intent intent) {
        if (behaviorProcessor != null) {
            behaviorProcessor.xulOnNewIntent(intent);
        }
    }

    protected void xulOnSaveInstanceState(Bundle outState) {
        if (behaviorProcessor != null) {
            behaviorProcessor.xulOnSaveInstanceState(outState);
        }
    }

    protected void xulOnRestoreInstanceState(Bundle savedInstanceState) {
        if (behaviorProcessor != null) {
            behaviorProcessor.xulOnRestoreInstanceState(savedInstanceState);
        }
    }

    public boolean xulDefaultDispatchKeyEvent(KeyEvent event) {
        return _xulFrameLayout.defaultDispatchKeyEvent(event);
    }

    public boolean xulDefaultDispatchTouchEvent(MotionEvent event) {
        return _xulFrameLayout.defaultDispatchTouchEvent(event);
    }

    public boolean xulOnDispatchTouchEvent(MotionEvent event) {
        if (behaviorProcessor != null && behaviorProcessor.dispatchTouchEvent(event)) {
            return true;
        }
        return false;
    }

    public boolean xulOnDispatchKeyEvent(KeyEvent event) {
        if (behaviorProcessor != null && behaviorProcessor.dispatchKeyEvent(event)) {
            return true;
        }
        return false;
    }

    public void xulDoAction(XulView view, String action, String type, String command, Object userdata) {
        if (_xulBehavior != null) {
            _xulBehavior.xulDoAction(view, action, type, command, userdata);
            return;
        }
    }

    public IXulExternalView xulCreateExternalView(FrameLayout rootView, String cls, int x, int y, int width, int
            height, XulView view) {
        return null;
    }
    /************************************************************************************************************************/

    public XulRenderContext.IXulRenderHandler2 getRenderHandler(final XulUiBehavior _xulBehavior) {
        return new XulRenderContext.IXulRenderHandler2() {
            @Override
            public Object getCustomObject(int key) {
                switch (key) {
                    case XCUSTOM_OBJ_PRESENTER:
                        return MultiBehaviorActivity.this;
                }
                return null;
            }

            @Override
            public void invalidate(Rect rect) {
                if (_xulBehavior != null &&_xulBehavior.xulGetRenderContextView() != null) {
                    _xulBehavior.xulGetRenderContextView().invalidate();
                }
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
                if (_xulBehavior != null) {
                    _xulBehavior.xulDoAction(view, action, type, command, userdata);
                }
            }

            @Override
            public IXulExternalView createExternalView(String cls, int x, int y, int width, int height, XulView view) {
                if (_xulBehavior != null) {
                    IXulExternalView externalView = xulCreateExternalView(_xulBehavior.xulGetRenderContextView(),
                            cls, x, y, width, height, view);
                    if (externalView != null) {
                        return externalView;
                    }
                    return _xulBehavior.xulCreateExternalView(cls, x, y, width, height, view);
                }
                return null;
            }

            @Override
            public String resolve(XulWorker.DownloadItem item, String path) {
                if (_xulBehavior != null) {
                    return _xulBehavior.xulResolvePath(item, path);
                }
                return null;
            }

            @Override
            public InputStream getAssets(XulWorker.DownloadItem item, String path) {
                if (_xulBehavior != null) {
                    return _xulBehavior.xulGetAssets(item, path);
                }
                return null;
            }

            @Override
            public InputStream getAppData(XulWorker.DownloadItem item, String path) {
                if (_xulBehavior != null) {
                    return _xulBehavior.xulGetAppData(item, path);
                }
                return null;
            }

            @Override
            public InputStream getSdcardData(XulWorker.DownloadItem item, String path) {
                if (_xulBehavior != null) {
                    return _xulBehavior.xulGetSdcardData(item, path);
                }
                return null;
            }

            @Override
            public void onRenderIsReady() {
                if (_xulBehavior != null) {
                    _xulBehavior.xulOnRenderIsReady();
                }
                if (_dbgMonitor != null) {
                    _dbgMonitor.onPageRenderIsReady(_xulBehavior);
                }
            }

            @Override
            public void onRenderEvent(int eventId, int param1, int param2, Object msg) {
                if (_xulBehavior != null) {
                    _xulBehavior.xulOnRenderEvent(eventId, param1, param2, msg);
                }
            }
        };
    }


    @Override
    public boolean xulOpenBehavior(Intent intent){
        /*创建behavior*/
        XulBehaviorPresenter presenter = new XulBehaviorPresenter(intent);
        XulUiBehavior xulBehavior = XulBehaviorManager.obtainBehavior(getBehaviorName(intent), presenter);
        presenter.setUiBehavior(xulBehavior);
        if (xulBehavior == null) {
            try {
                throw new Exception("obtainBehavior " + getBehaviorName(intent) +" failed.");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        /*加载布局UI文件*/
        xulLoadLayoutFile(getLayoutFile(intent));

        /*创建XulRender*/
        XulRenderContext xulRenderContext = XulManager.createXulRender(getPageId(intent),
                getRenderHandler(xulBehavior), 0, 0, false, false, true);
        presenter.setRenderContext(xulRenderContext);
        if (xulRenderContext == null) {
            try {
                throw new Exception("createXulRender " + getPageId(intent) +" failed.");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        /*创建behavior单元控制对象*/
        XulBehaviorUnit behaviorUnit = XulBehaviorUnit.create(xulBehavior, xulRenderContext);

        /*初始化behavior参数信息*/
        Bundle bundle = presenter.xulGetBehaviorParams();

        String behaviorId = "";
        String launcherMode = XulBehaviorUnit.LaunchMode.FLAG_BEHAVIOR_ATTACH_STACK.name();
        int x = 0;
        int y = 0;
        int w = XulManager.getPageWidth();
        int h = XulManager.getPageHeight();
        boolean enableKey = true;
        if (bundle != null) {
            behaviorId = bundle.getString(BehaviorParams.XPARAM_ID_FLAG);
            x = XulUtils.tryParseInt(bundle.getString(BehaviorParams.XPARAM_X_FLAG), 0);
            y = XulUtils.tryParseInt(bundle.getString(BehaviorParams.XPARAM_Y_FLAG), 0);
            w = XulUtils.tryParseInt(bundle.getString(BehaviorParams.XPARAM_W_FLAG), XulManager.getPageWidth());
            h = XulUtils.tryParseInt(bundle.getString(BehaviorParams.XPARAM_H_FLAG), XulManager.getPageHeight());
            launcherMode = bundle.getString(BehaviorParams.XPARAM_BEHAVIOR_LAUNCH_MODE);
            enableKey = "1".equals(bundle.getString(BehaviorParams.XPARAM_ENABLE_KEY_FLAG));
        }

        if (!TextUtils.isEmpty(behaviorId)) {
            behaviorUnit.id = behaviorId;
        }

        /*设置view位置和添加到跟根布局*/
        Rect rect = new Rect();
        rect.left = x;
        rect.top = y;
        rect.right = x + w;
        rect.bottom = y + h;
        if (rect != null && !rect.isEmpty()) {
            behaviorUnit.updateRect = rect;
        }
        FrameLayout renderView = getRenderView(behaviorUnit);
        if (renderView == null) {
            return false;
        }
        presenter.setRenderContextView(renderView);
        xulBehavior.xulOnRenderCreated();

        View rootView = xulBehavior.initRenderContextView(renderView);
        if (rootView == null) {
            return false;
        }
        int color = XulUtils.tryParseInt(bundle.getString(BehaviorParams.XPARAM_COLOR_FLAG), -1);
        if (color != -1) {
            rootView.setBackgroundColor(color);
        }
        /*默认不可见*/
        if (bundle != null) {
            if ("0".equals(bundle.getString(BehaviorParams.XPARAM_VISIBLE_FLAG))) {
                rootView.setVisibility(View.INVISIBLE);
            }
        }
        if (_xulRootView instanceof FrameLayout) {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(rect.width(), rect.height());
            layoutParams.leftMargin = rect.left;
            layoutParams.topMargin = rect.top;
            ((ViewGroup) _xulRootView).addView(rootView, layoutParams);
        } else if (_xulRootView instanceof RelativeLayout){
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(rect.width(), rect.height());
            layoutParams.leftMargin = rect.left;
            layoutParams.topMargin = rect.top;
            ((ViewGroup) _xulRootView).addView(rootView, layoutParams);
        } else {
            int paddingRight = XulManager.getPageWidth() - rect.right;
            int paddingBottom = XulManager.getPageHeight() - rect.bottom;
            rootView.setPadding(rect.left, rect.top, paddingRight, paddingBottom);
            ((ViewGroup) _xulRootView).addView(rootView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        behaviorUnit.setRootView(rootView);
        /*设置加载模式*/
        behaviorUnit.setLaunchMode(XulBehaviorUnit.LaunchMode.FromString(launcherMode));
        if (behaviorProcessor != null) {
            behaviorProcessor.add(behaviorUnit);
        }
        behaviorUnit.responseKeyEvent = enableKey;
        /*开始绘制render*/
        xulRenderContext.initXulRender();
        return true;
    }

    private XulFrameLayout getRenderView(final XulBehaviorUnit xulBehaviorUnit) {
        if (xulBehaviorUnit != null) {
            XulFrameLayout renderView = new XulFrameLayout(this) {
                Rect updateRc = new Rect();
                Rect rc;
                XulRenderContext xulRenderContext;

                @Override
                public void dispatchDraw(Canvas canvas) {
                    if (xulBehaviorUnit == null || !xulBehaviorUnit.visible) {
                        super.dispatchDraw(canvas);
                        return;
                    }
                    if (xulRenderContext == null) {
                        xulRenderContext = xulBehaviorUnit.renderContext;
                    }
                    rc = xulBehaviorUnit.updateRect;
                    if (rc == null || rc.isEmpty()) {
                        updateRc.set(0, 0, XulManager.getPageWidth(), XulManager.getPageHeight());
                    } else {
                        updateRc.set(0, 0, rc.width(), rc.height());
                    }

                    float x = ((float) updateRc.width() / XulManager.getPageWidth());
                    float y = ((float) updateRc.height() / XulManager.getPageHeight());
                    canvas.scale(x, y);
                    if (xulRenderContext != null && xulRenderContext.beginDraw(canvas, updateRc)) {
                        super.dispatchDraw(canvas);
                        xulRenderContext.endDraw();
                    } else {
                        super.dispatchDraw(canvas);
                    }
                }
                @Override
                public boolean dispatchKeyEvent(KeyEvent event) {
                    if (xulBehaviorUnit != MultiBehaviorActivity.this.xulBehaviorUnit) {
                        return false;
                    }
                    if (xulOnDispatchKeyEvent(event)) {
                        return true;
                    }
                    return xulDefaultDispatchKeyEvent(event);
                }

                @Override
                public boolean dispatchTouchEvent(MotionEvent event) {
                    if (xulBehaviorUnit != MultiBehaviorActivity.this.xulBehaviorUnit) {
                        return false;
                    }
                    if (xulOnDispatchTouchEvent(event)) {
                        return true;
                    }
                    return xulDefaultDispatchTouchEvent(event);
                }
                @Override
                public boolean dispatchGenericMotionEvent(MotionEvent event) {
                    if (xulBehaviorUnit != MultiBehaviorActivity.this.xulBehaviorUnit) {
                        return false;
                    }
                    if (xulOnDispatchTouchEvent(event)) {
                        return true;
                    }
                    return super.dispatchGenericMotionEvent(event);
                }
            };

            ViewGroup.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            renderView.setLayoutParams(params);
            return renderView;
        }
        return null;
    }

    private void preDraw(Canvas canvas, Rect rect) {
        if (rect == null) {
            return;
        }
        float xScale = getXScale(rect);
        float YScale = getYScale(rect);
        if ((int)xScale != 1 || (int)YScale != 1) {
            canvas.scale(getXScale(rect), getYScale(rect));
        }
    }

    private void endDraw(Canvas canvas) {
        canvas.restore();
    }

    @Override
    public boolean xulCloseBehavior(String id) {
        if (behaviorProcessor != null) {
            behaviorProcessor.closeBehavior(id);
            return false;
        }
        return true;
    }

    @Override
    public IBehaviorOperation getXulBehaviorOperation(XulUiBehavior behavior) {
        if (behaviorProcessor != null) {
            return behaviorProcessor.getXulBehaviorOperation(behavior);
        }
        return null;
    }

    @Override
    public IBehaviorOperation getXulBehaviorOperation(String id) {
        if (behaviorProcessor != null) {
            return behaviorProcessor.getXulBehaviorOperation(id);
        }
        return null;
    }

    private BehaviorProcessor getBehaviorProcessor() {
        if (behaviorProcessor == null) {
            behaviorProcessor = new BehaviorProcessor();
        }
        return behaviorProcessor;
    }

    private void xulPageCreated() {
    }



    private class XulBehaviorPresenter implements XulPresenter{
        XulRenderContext renderContext;
        XulUiBehavior behavior;
        Intent intent;
        FrameLayout layout;

        public XulBehaviorPresenter(Intent intent) {
            this.intent = intent;
        }

        public void setRenderContext(XulRenderContext renderContext) {
            this.renderContext = renderContext;
        }
        public void setUiBehavior(XulUiBehavior uiBehavior) {
            this.behavior = uiBehavior;
        }
        public void setRenderContextView(FrameLayout layout) {
            this.layout = layout;
        }

        @Override
        public Context xulGetContext() {
            return MultiBehaviorActivity.this;
        }

        @Override
        public String xulGetIntentPageId() {
            return intent.getStringExtra(XPARAM_PAGE_ID);
        }

        @Override
        public String xulGetCurPageId() {
            return intent.getStringExtra(XPARAM_PAGE_ID);
        }

        @Override
        public String xulGetIntentLayoutFile() {
            return intent.getStringExtra(XPARAM_LAYOUT_FILE);
        }

        @Override
        public String xulGetCurLayoutFile() {
            return intent.getStringExtra(XPARAM_LAYOUT_FILE);
        }

        @Override
        public String xulGetCurBehaviorName() {
            return intent.getStringExtra(XPARAM_PAGE_BEHAVIOR);
        }

        @Override
        public XulRenderContext xulGetRenderContext() {
            return renderContext;
        }

        @Override
        public FrameLayout xulGetRenderContextView() {
            return layout;
        }

        @Override
        public void xulLoadLayoutFile(String layoutFile) {
            if (TextUtils.isEmpty(layoutFile)) {
                return;
            }
            if (XulManager.isXulLoaded(layoutFile)) {
                return;
            }
            XulApplication.getAppInstance().xulLoadLayouts(layoutFile);
        }

        @Override
        public boolean xulDefaultDispatchKeyEvent(KeyEvent event) {
            return false;
        }

        @Override
        public boolean xulDefaultDispatchTouchEvent(MotionEvent event) {
            return false;
        }

        @Override
        public Bundle xulGetBehaviorParams() {
            if (intent != null) {
                return intent.getBundleExtra(XPARAM_BEHAVIOR_PARAMS);
            }
            return null;
        }

        @Override
        public void xulDestroy() {
            MultiBehaviorActivity.this.xulOnDestroy();
        }

        @Override
        public boolean xulIsAlive() {
            return !MultiBehaviorActivity.this.isFinishing();
        }
    }

}
