package com.starcor.xulapp.third.lottie;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.airbnb.lottie.LottieAnimationView;
import com.starcor.xul.IXulExternalView;
import com.starcor.xul.XulPage;
import com.starcor.xul.XulView;

public class XulLottieView extends LottieAnimationView implements IXulExternalView {

    private static final String TAG = "XulLottieView";
    private XulView mXulView;

    public static final String ATTR_ANIMATION = "animation";
    public static final String ATTR_IMG_SET = "image_set";
    public static final String ATTR_AUTO_START = "auto_start";
    public static final String ATTR_LOOP = "loop";
    public static final String ATTR_AUTO_HIDE = "auto_hide";
    private static final String ACTION_END = "action_end";

    /**
     * 动画Json Assets路径
     */
    private String mAnimation;

    /**
     * 动画Image Assets路径
     */
    private String mImageSet;

    /**
     * 是否自动开始
     */
    private boolean mIsAutoStart;

    /**
     * 是否循环
     */
    private boolean mLoop;

    /**
     * 动画完成的时候自动隐藏
     */
    private boolean mIsAutoHide;

    private Animator.AnimatorListener mAnimationListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mIsAutoHide) {
                setVisibility(GONE);
            }
            XulPage.invokeAction(mXulView, ACTION_END);
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    public XulLottieView(Context context, XulView xulView) {
        super(context);
        this.mXulView = xulView;
        addAnimatorListener(mAnimationListener);
    }

    @Override
    public void extMoveTo(int x, int y, int width, int height) {
        Log.i(TAG, "extMoveTo." + x + ", " + y + ", w = " + width + ", h = " + height);
        if (getLayoutParams() instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();
            layoutParams.width = width;
            layoutParams.height = height;
            layoutParams.topMargin = y;
            layoutParams.leftMargin = x;
        } else if (getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) getLayoutParams();
            layoutParams.width = width;
            layoutParams.height = height;
            layoutParams.topMargin = y;
            layoutParams.leftMargin = x;
        } else if (getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) getLayoutParams();
            layoutParams.width = width;
            layoutParams.height = height;
            layoutParams.topMargin = y;
            layoutParams.leftMargin = x;
        }
        requestLayout();
    }

    @Override
    protected void onAttachedToWindow() {
        Log.i(TAG, "onAttachedToWindow.");
        super.onAttachedToWindow();
        if (mIsAutoStart) {
            startAnimation();
        }
    }

    @Override
    public void extMoveTo(Rect rect) {
        extMoveTo(rect.left, rect.top, rect.width(), rect.height());
    }

    @Override
    public boolean extOnKeyEvent(KeyEvent event) {
        return false;
    }

    @Override
    public void extOnFocus() {
        this.requestFocus();
    }

    @Override
    public void extOnBlur() {
        this.clearFocus();
    }

    @Override
    public void extShow() {
        Log.i(TAG, "extShow.");
        this.setVisibility(VISIBLE);
        if (mIsAutoStart) {
            startAnimation();
        }
    }

    @Override
    public void extHide() {
        Log.i(TAG, "extHide.");
        stopAnimation();
        this.setVisibility(GONE);
    }

    @Override
    public void extDestroy() {
        Log.i(TAG, "extDestroy.");
        extHide();
        removeAnimatorListener(mAnimationListener);
        ((ViewGroup) getParent()).removeView(this);
    }

    @Override
    public String getAttr(String key, String defVal) {
        return defVal;
    }

    @Override
    public boolean setAttr(String key, String val) {
        if (TextUtils.isEmpty(key)) {
            return false;
        }
        switch (key) {
            case ATTR_ANIMATION:
                mAnimation = val;
                break;
            case ATTR_IMG_SET:
                mImageSet = val;
                break;
            case ATTR_LOOP:
                mLoop = "true".equals(val);
                break;
            case ATTR_AUTO_START:
                mIsAutoStart = "true".equalsIgnoreCase(val);
                break;
            case ATTR_AUTO_HIDE:
                mIsAutoHide = "true".equalsIgnoreCase(val);
                break;
        }
        return false;
    }

    @Override
    public void extSyncData() {
        mAnimation = mXulView.getAttrString(ATTR_ANIMATION);
        mImageSet = mXulView.getAttrString(ATTR_IMG_SET);
        mIsAutoStart = "true".equalsIgnoreCase(mXulView.getAttrString(ATTR_AUTO_START));
        mLoop = "true".equalsIgnoreCase(mXulView.getAttrString(ATTR_LOOP));
        mIsAutoHide = "true".equalsIgnoreCase(mXulView.getAttrString(ATTR_AUTO_HIDE));
        Log.i(TAG, "extSyncData.mAnimation = " + mAnimation
                + ", mImageSet = " + mImageSet
                + ", mIsAutoStart = " + mIsAutoStart
                + ", mLoop = " + mLoop
        );
    }


    /**
     * 开启动画
     */
    public void startAnimation() {
        Log.i(TAG, "startAnimation.");
        if (TextUtils.isEmpty(mAnimation)) {
            return;
        }
        setVisibility(GONE);
        setProgress(0);
        clearAnimation();
        if (!TextUtils.isEmpty(mImageSet)) {
            setImageAssetsFolder(mImageSet);
        }
        setAnimation(mAnimation);
        loop(mLoop);
        playAnimation();
        setVisibility(VISIBLE);
        bringToFront();
    }

    /**
     * 停止动画
     */
    public void stopAnimation() {
        Log.i(TAG, "stopAnimation.");
        if (isAnimating()) {
            cancelAnimation();
            setProgress(0);
        }
        setVisibility(GONE);
    }


}
