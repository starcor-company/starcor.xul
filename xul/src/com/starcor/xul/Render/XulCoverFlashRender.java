package com.starcor.xul.Render;

import android.graphics.*;
import android.text.TextUtils;
import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Prop.XulFocus;
import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.XulItem;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulView;

/**
 * 反光Render
 */
public class XulCoverFlashRender extends XulViewRender {

    private static final String TAG = "XulCoverFlashRender";

    private Rect mFlashRect;

    private boolean mFlashRunning;
    private float mFlashMultiple;
    private float mFlashAddFount;

    private int[] mFlashColors;
    private double mAngle = 1.1f;

    private int[] mSpecialColors;
    private double mSpecialAngle = 1.1f;

    private boolean mUseSpecialFlash;

    public static final String ATTR_ANGLE = "flash_angle";
    public static final String ATTR_COLORS = "flash_colors";
    public static final String ATTR_FLASH_CLASS = "flash_tracker_class";

    public static void register() {
        XulRenderFactory.registerBuilder("item", "cover_flash", new XulRenderFactory.RenderBuilder() {
            @Override
            protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
                assert view instanceof XulItem;
                return new XulCoverFlashRender(ctx, view);
            }
        });
    }

    @Override
    public void draw(XulDC dc, Rect rect, int xBase, int yBase) {
        if (_isInvisible()) {
            return;
        }
        super.draw(dc, rect, xBase, yBase);

        if (!mFlashRunning || mFlashRect == null) {
            return;
        }

        final int crossWidth = mFlashRect.width();
        final int crossHeight = (int) ((float)crossWidth / (mUseSpecialFlash ? mSpecialAngle : mAngle));
        final int crossTop = mFlashRect.height() / 2 + mFlashRect.top - crossHeight / 2;
        final int crossBottom = crossTop + crossHeight;

        int left = mFlashMultiple < 0.5f ? mFlashRect.left - crossWidth / 2 : mFlashRect.left + (int) ((mFlashMultiple - 0.5f) * 2 * crossWidth) ;
        int top = mFlashMultiple < 0.5f ? crossTop : crossTop + (int) ((mFlashMultiple - 0.5f) * 2 * crossHeight) ;
        int right = mFlashMultiple < 0.5f ? mFlashRect.right - (int) ((0.5f  - mFlashMultiple) * 2 * crossWidth) : mFlashRect.right + crossWidth / 2;
        int bottom = mFlashMultiple < 0.5f ? crossBottom - (int) ((0.5f  - mFlashMultiple) * 2 * crossHeight) : crossBottom ;

        Paint paint = new Paint();
        paint.setShader(new LinearGradient(
                left, top, right, bottom, mUseSpecialFlash ? mSpecialColors : mFlashColors,
                null, Shader.TileMode.CLAMP));
        dc.getCanvas().drawRect(mFlashRect, paint);
        if (mFlashMultiple >= 1.0f) {
            mFlashRunning = false;
            mFlashMultiple = 0f;
            mFlashAddFount = 0;
            reset();
            return;
        }
        mFlashMultiple = 5.7f * mFlashAddFount * mFlashAddFount;
        mFlashAddFount += 0.01f;
        reset();
    }

    public XulCoverFlashRender(XulRenderContext ctx, XulView view) {
        super(ctx, view);
        mFlashColors = new int[]{
                Color.TRANSPARENT,
                Color.parseColor("#05FFFFFF"),
                Color.parseColor("#10FFFFFF"),
                Color.parseColor("#20FFFFFF"),
                Color.parseColor("#2AFFFFFF"),
                Color.parseColor("#20FFFFFF"),
                Color.parseColor("#10FFFFFF"),
                Color.parseColor("#05FFFFFF"),
                Color.TRANSPARENT};
        mSpecialColors = mFlashColors;
    }

    @Override
    public int getDefaultFocusMode() {
        return XulFocus.MODE_NOFOCUS;
    }

    @Override
    public void refreshSizingMovingAnimation() {
        super.refreshSizingMovingAnimation();
        try {
            if (getView() != null) {
                int x = (int) (XulUtils.tryParseInt(getView().getAttr(XulPropNameCache.TagId.X).getStringValue()) * getXScalar());
                int y = (int) (XulUtils.tryParseInt(getView().getAttr(XulPropNameCache.TagId.Y).getStringValue()) * getXScalar());
                int width = (int) (XulUtils.tryParseInt(getView().getAttr(XulPropNameCache.TagId.WIDTH).getStringValue()) * getXScalar());
                int height = (int) (XulUtils.tryParseInt(getView().getAttr(XulPropNameCache.TagId.HEIGHT).getStringValue()) * getXScalar());
                mFlashRunning = true;
                mFlashMultiple = 0f;
                mFlashAddFount = 0;
                if (mFlashRect == null) {
                    mFlashRect = new Rect(x, y,  x + width,  y + height);
                    return;
                }
                mFlashRect.set(x, y,  x + width,  y + height);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopFlash() {
        mFlashRunning = false;
        mFlashRunning = false;
        mFlashMultiple = 0f;
        mFlashAddFount = 0;
        reset();
    }

    @Override
    public void syncData() {
        super.syncData();
        syncColors();
        syncAngle();
    }

    public void syncSpecialFlash(XulView view) {
        if (view == null) {
            mUseSpecialFlash = false;
            return;
        }
        String angle = view.getAttrString(ATTR_ANGLE);
        String colorAttr = view.getAttrString(ATTR_COLORS);

        if (TextUtils.isEmpty(angle) && TextUtils.isEmpty(colorAttr)) {
            mUseSpecialFlash = false;
            return;
        }
        if (!TextUtils.isEmpty(angle)) {
            mSpecialAngle = XulUtils.tryParseDouble(angle, 1.1);
            mUseSpecialFlash = true;
        }

        if (!TextUtils.isEmpty(colorAttr)) {
            String[] colors = colorAttr.split(",");
            if (colors == null || colors.length == 0) {
                return;
            }
            mUseSpecialFlash = true;
            mSpecialColors = new int[colors.length + 2];
            mSpecialColors[0] = Color.TRANSPARENT;
            mSpecialColors[colors.length + 1] = Color.TRANSPARENT;
            for (int i = 0; i < colors.length; i++) {
                mSpecialColors[i+1] = Color.parseColor("#" + colors[i]);
            }
        }
    }

    private void syncAngle() {
        String angle = _view.getAttrString(ATTR_ANGLE);
        if (TextUtils.isEmpty(angle)) {
            return;
        }
        mAngle = XulUtils.tryParseDouble(angle, 1.1);
    }

    private void syncColors() {
        String colorAttr = _view.getAttrString(ATTR_COLORS);
        if (TextUtils.isEmpty(colorAttr)) {
            return;
        }
        String[] colors = colorAttr.split(",");
        if (colors == null || colors.length == 0) {
            return;
        }
        mFlashColors = new int[colors.length + 2];
        mFlashColors[0] = Color.TRANSPARENT;
        mFlashColors[colors.length + 1] = Color.TRANSPARENT;
        for (int i = 0; i < colors.length; i++) {
            mFlashColors[i+1] = Color.parseColor("#" + colors[i]);
        }
    }


}
