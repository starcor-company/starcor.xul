package com.starcor.xulapp.third.lottie;

import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.starcor.xul.Render.XulCustomViewRender;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulView;

public class XulLottieWrapper {

    private static final String TAG = "XulLottieWrapper";

    private XulView mLottieView;
    private XulLottieView mXulLottieView;

    public XulLottieWrapper(XulView lottieView) {
        mLottieView = lottieView;
        mXulLottieView = (XulLottieView) lottieView.getRender().getExternalView();
    }

    public static XulLottieWrapper fromXulView(XulView lottieView) {
        if (lottieView == null) {
            return null;
        }
        if (!(lottieView.getRender() instanceof XulCustomViewRender)) {
            return null;
        }
        if (lottieView.getRender().getExternalView() == null) {
            return null;
        }
        return new XulLottieWrapper(lottieView);
    }

    /**
     * 开始播放
     */
    public void startAnimation() {
        mXulLottieView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mXulLottieView.startAnimation();
            }
        }, 16);
        mLottieView.resetRender();
    }

    /**
     * 停止播放
     */
    public void stopAnimation() {
        mXulLottieView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mXulLottieView.stopAnimation();
            }
        }, 16);
    }

    /**
     * 重设位置
     * @param x
     * @param y
     * @return
     */
    public XulLottieWrapper moveTo(int x, int y) {
        mXulLottieView.stopAnimation();
        mLottieView.setAttr("x", String.valueOf(x));
        mLottieView.setAttr("y", String.valueOf(y));
        mLottieView.resetRender();
        return this;
    }

    /**
     * 重设大小
     * @param width
     * @param height
     * @return
     */
    public XulLottieWrapper resize(int width, int height) {
        mXulLottieView.stopAnimation();
        mLottieView.setAttr("width", String.valueOf(width));
        mLottieView.setAttr("height", String.valueOf(height));
        mLottieView.resetRender();
        return this;
    }

    /**
     *  将view移动到某个xulView上
     * @param targetView
     * @return
     */
    public XulLottieWrapper coverView(XulView targetView) {
        mXulLottieView.stopAnimation();
        int x = XulUtils.tryParseInt(targetView.getAttrString("x"));
        int y = XulUtils.tryParseInt(targetView.getAttrString("y"));
        int width = XulUtils.tryParseInt(targetView.getAttrString("width"));
        int height = XulUtils.tryParseInt(targetView.getAttrString("height"));

        Log.i(TAG, "moveTo:  x = " + x + ", y = " + y+
                ", width = " + width + ", height = " +height);
        String scale = targetView.getStyleString("scale");
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        if (!TextUtils.isEmpty(scale)) {
            String[] scaleArr = scale.split(",");
            scaleX = XulUtils.tryParseFloat(scaleArr[0], 1.0f);
            scaleY = XulUtils.tryParseFloat(scaleArr[1], 1.0f);
        }
        float scaleWidth = width;
        float scaleHeight = height;
        float scaleLeft = x;
        float scaleTop = y;
        if (scaleX != 1.0f || scaleY != 1.0f) {
            scaleWidth = scaleX * width;
            scaleHeight = scaleY * height;
            scaleLeft = x - (scaleWidth - width) * 0.5f;
            scaleTop = y - (scaleHeight - height) * 0.5f;
            Log.i(TAG, "moveTo: scaleLeft = " + scaleLeft + ", scaleTop = "
                    + scaleTop + ", scaleWidth = " + scaleWidth + ", scaleHeight = " + scaleHeight);
        }
        mLottieView.setAttr("x", String.valueOf((int) scaleLeft));
        mLottieView.setAttr("y", String.valueOf((int) scaleTop));
        mLottieView.setAttr("width", String.valueOf((int) scaleWidth));
        mLottieView.setAttr("height", String.valueOf((int) scaleHeight));
        mLottieView.resetRender();
        return this;
    }

    /**
     *  将view 绑定到新的viewGroup上
     * @param viewGroup
     * @return
     */
    public XulLottieWrapper reAttachTo(ViewGroup viewGroup) {
        mXulLottieView.stopAnimation();
        ((ViewGroup) mXulLottieView.getParent()).removeView(mXulLottieView);
        if (viewGroup instanceof FrameLayout
                && !(mXulLottieView.getLayoutParams() instanceof FrameLayout.LayoutParams)) {
            mXulLottieView.setLayoutParams(new FrameLayout.LayoutParams(mXulLottieView.getLayoutParams()));
        }
        if (viewGroup instanceof RelativeLayout
                && !(mXulLottieView.getLayoutParams() instanceof RelativeLayout.LayoutParams)) {
            mXulLottieView.setLayoutParams(new RelativeLayout.LayoutParams(mXulLottieView.getLayoutParams()));
        }
        if (viewGroup instanceof LinearLayout
                && !(mXulLottieView.getLayoutParams() instanceof LinearLayout.LayoutParams)) {
            mXulLottieView.setLayoutParams(new LinearLayout.LayoutParams(mXulLottieView.getLayoutParams()));
        }
        viewGroup.addView(mXulLottieView);
        mLottieView.resetRender();
        mXulLottieView.requestLayout();
        return this;
    }

}
