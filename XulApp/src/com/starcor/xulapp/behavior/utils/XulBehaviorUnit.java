package com.starcor.xulapp.behavior.utils;

import android.graphics.Rect;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import com.starcor.xul.XulManager;
import com.starcor.xul.XulRenderContext;
import com.starcor.xulapp.behavior.XulUiBehavior;
import com.starcor.xulapp.debug.XulDebugMonitor;
import com.starcor.xulapp.debug.XulDebugServer;

/**
 * Created by skycnlr on 2018/9/6.
 */
public class XulBehaviorUnit implements IBehaviorOperation {
    public static final int STATE_NULL = 0;
    public static final int STATE_CREATE = 1;
    public static final int STATE_START = 2;
    public static final int STATE_RESUME = 3;
    public static final int STATE_PAUSE = 4;
    public static final int STATE_STOP = 5;
    public static final int STATE_DESTROY = 6;
    public static final int STATE_RESTART = 7;

    public static final int STATE_HIDE = 100;
    public static final int STATE_SHOW = 101;

    public static Rect defaultRect = new Rect(0, 0, XulManager.getPageWidth(), XulManager.getPageHeight());

    protected XulDebugMonitor _dbgMonitor;
    public Rect updateRect = defaultRect;
    private Rect tmpRect;
    private View rootView;

    public XulUiBehavior uiBehavior;
    public XulRenderContext renderContext;

    public String id;
    public boolean responseKeyEvent = true;
    public int behaviorState = STATE_NULL;
    public boolean visible = true;
    public LaunchMode launchMode;

    public enum LaunchMode {
        FLAG_BEHAVIOR_ATTACH_STACK, FLAG_BEHAVIOR_NEW_STACK;
        public static LaunchMode FromString(String name) {
            for (LaunchMode act : LaunchMode.values()) {
                if (act.name().equals(name)) {
                    return act;
                }
            }
            return FLAG_BEHAVIOR_ATTACH_STACK;
        }
    }

    public static XulBehaviorUnit create(XulUiBehavior behavior, XulRenderContext context) {
        return new XulBehaviorUnit(behavior, context);
    }

    public XulBehaviorUnit(XulUiBehavior behavior, XulRenderContext context) {
        if (behavior != null) {
            uiBehavior = behavior;
            id = uiBehavior.xulGetCurPageId();
        }
        _dbgMonitor = XulDebugServer.getMonitor();
        renderContext = context;
    }

    @Override
    public boolean close() {
        changeToState(STATE_DESTROY);
        return true;
    }

    @Override
    public boolean show() {
        changeToState(STATE_SHOW);
        invalidate();
        return false;
    }

    @Override
    public boolean hide() {
        changeToState(STATE_HIDE);
        invalidate();
        return false;
    }

    @Override
    public boolean changeBounds(Rect rect) {
        if (rect != null) {
            updateRect = rect;
        }
        if (rootView != null) {
            ViewGroup.LayoutParams params = rootView.getLayoutParams();
            if (params != null) {
                params.width = rect.width();
                params.height = rect.height();
                if (params instanceof FrameLayout.LayoutParams) {
                    ((FrameLayout.LayoutParams) params).leftMargin = rect.left;
                    ((FrameLayout.LayoutParams) params).topMargin = rect.top;
                } else if (params instanceof RelativeLayout.LayoutParams) {
                    ((RelativeLayout.LayoutParams) params).leftMargin = rect.left;
                    ((RelativeLayout.LayoutParams) params).topMargin = rect.top;
                } else {
                    int paddingRight = XulManager.getPageWidth() - rect.right;
                    int paddingBottom = XulManager.getPageHeight() - rect.bottom;
                    rootView.setPadding(rect.left, rect.top, paddingRight, paddingBottom);
                }
                rootView.setLayoutParams(params);
            }
        }
        invalidate();
        return true;
    }

    @Override
    public Rect getBounds() {
        if (tmpRect == null) {
            tmpRect =  new Rect();
        }
        tmpRect.left = updateRect.left ;
        tmpRect.top = updateRect.top;
        tmpRect.right = updateRect.right;
        tmpRect.bottom = updateRect.bottom;
        return tmpRect;
    }

    @Override
    public boolean enableKeyEvent(boolean enable) {
        responseKeyEvent = enable;
        return true;
    }

    @Override
    public boolean setBackgroundColor(int color) {
        if (rootView != null) {
            rootView.setBackgroundColor(color);
            return true;
        }
        return false;
    }

    public boolean xulOnDispatchTouchEvent(MotionEvent event) {
        if (!visible || uiBehavior == null || !responseKeyEvent) {
            return false;
        }
        if (uiBehavior.xulOnDispatchTouchEvent(event)) {
            return true;
        }
        return false;
    }

    public boolean xulOnBackPressed() {
        if (!visible || uiBehavior == null || !responseKeyEvent) {
            return false;
        }
        if (uiBehavior.xulOnBackPressed()) {
            return true;
        }
        return false;
    }

    public boolean xulOnDispatchKeyEvent(KeyEvent event) {
        if (!visible || uiBehavior == null || !responseKeyEvent) {
            return false;
        }
        if (uiBehavior.xulOnDispatchKeyEvent(event)) {
            return true;
        }
        return false;
    }

    public void remove() {
        View view = rootView;
        if (view == null) {
            return;
        }
        ViewParent viewParent = view.getParent();
        if (viewParent instanceof ViewGroup) {
            final ViewGroup parent = (ViewGroup) viewParent;
            parent.removeView(rootView);
            rootView = null;
        }
    }

    private void invalidate() {
        if (rootView == null) {
            return;
        }
        rootView.requestLayout();
    }

    public void setRootView(View v) {
        rootView = v;
    }

    public void setLaunchMode(LaunchMode v) {
        launchMode  = v;
    }

    public LaunchMode getLaunchMode() {
        return launchMode;
    }

    public int changeToState(int state) {
        if (uiBehavior == null) {
            return -1;
        }
        Log.d("XulBehaviorUnit", "changeToState:" + state +"," + behaviorState);
        switch (state) {
            case STATE_SHOW:
                if (!visible) {
                    visible = true;
                }
                if (rootView != null) {
                    rootView.setVisibility(View.VISIBLE);
                }
                innerChangeToState(STATE_RESUME);
                break;
            case STATE_HIDE:
                if (visible) {
                    visible = false;
                }
                if (rootView != null) {
                    rootView.setVisibility(View.INVISIBLE);
                }
                innerChangeToState(STATE_STOP);
                break;
            default:
                innerChangeToState(state);
                return -1;
        }
        return state;
    }
    private int innerChangeToState(int state) {
        if (uiBehavior == null) {
            return -1;
        }
        Log.d("XulBehaviorUnit", "innerChangeToState:" + state +"," + behaviorState);
        if (state == behaviorState) {
            return state;
        }
        switch (state) {
            case STATE_CREATE:
                if (behaviorState == STATE_NULL) {
                    if (_dbgMonitor != null) {
                        _dbgMonitor.onPageCreate(uiBehavior);
                    }
                    uiBehavior.xulOnStart();
                    behaviorState = STATE_START;
                    return behaviorState;
                }
                return  -1;
            case STATE_DESTROY:
                if (behaviorState == STATE_RESUME) {
                    uiBehavior.xulOnPause();
                    uiBehavior.xulOnStop();
                }
                if (behaviorState == STATE_PAUSE) {
                    uiBehavior.xulOnStop();
                }
                uiBehavior.xulOnDestroy();
                XulRenderContext xulRenderContext = renderContext;
                if (xulRenderContext != null) {
                    xulRenderContext.destroy();
                }
                if (_dbgMonitor != null) {
                    _dbgMonitor.onPageDestroy(uiBehavior);
                }
                behaviorState = state;
                return behaviorState;
            case STATE_PAUSE:
                uiBehavior.xulOnPause();
                if (_dbgMonitor != null) {
                    _dbgMonitor.onPagePaused(uiBehavior);
                }
                behaviorState = state;
                return behaviorState;
            case STATE_STOP:
                if (behaviorState == STATE_RESUME) {
                    uiBehavior.xulOnPause();
                }
                uiBehavior.xulOnStop();
                if (_dbgMonitor != null) {
                    _dbgMonitor.onPageStopped(uiBehavior);
                }
                behaviorState = state;
                return behaviorState;
            case STATE_RESTART:
                if (!visible) {
                    return -1;
                }
                uiBehavior.xulOnRestart();
                behaviorState = state;
                return behaviorState;
            case STATE_RESUME:
                if (!visible) {
                    return -1;
                }
                if (behaviorState == STATE_RESTART) {
                    uiBehavior.xulOnStart();
                }
                if (behaviorState == STATE_STOP) {
                    uiBehavior.xulOnRestart();
                    uiBehavior.xulOnStart();
                }
                uiBehavior.xulOnResume();
                if (_dbgMonitor != null) {
                    _dbgMonitor.onPageResumed(uiBehavior);
                }
                behaviorState = state;
                return behaviorState;
            case STATE_START:
                if (!visible) {
                    return -1;
                }
                uiBehavior.xulOnStart();
                behaviorState = state;
                return behaviorState;
            default:
                return -1;
        }
    }

}