package com.starcor.xuldemo.widget;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;

import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Render.Drawer.IXulAnimation;
import com.starcor.xul.Render.XulImageRender;
import com.starcor.xul.Render.XulRenderFactory;
import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.Utils.XulDownloadDrawableItem;
import com.starcor.xul.XulItem;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulView;
import com.starcor.xul.XulWorker;

/**
 * Created by ZFB on 2015/8/28.
 */
public class XulCustomRender extends XulImageRender {

	private static final String CUSTOM_RENDER_TYPE = "custom_render";
	private static final String CUSTOM_ATTR_ANI_DURATION = "indicator-animation-duration";
	private static final String CUSTOM_ATTR_ANI_COUNT = "indicator-animation-count";

	private static final float SCALE_RATIO = 0.5F;

	private IndicatorScaleAnimation mIndicatorAnimation = null;

	private XulDownloadDrawableItem _downloadDrawable;

	public XulCustomRender(XulRenderContext ctx, XulItem item) {
		super(ctx, item);
	}

	public static void register() {
		XulRenderFactory
			.registerBuilder("item", CUSTOM_RENDER_TYPE, new XulRenderFactory.RenderBuilder() {
				@Override
				protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
					assert view instanceof XulItem;
					return new XulCustomRender(ctx, (XulItem) view);
				}
			});
	}


	class IndicatorScaleAnimation implements IXulAnimation {

		private static final int DEF_ANIMATION_DURATION = 1000;
		private static final int DEF_ANIMATION_COUNT = 2;

		private long mBeginTime = 0;
		private int mAnimationDuration = 0;
		private int mAnimationCount = 0;

		private float mAnimationProgress = 0;

		public IndicatorScaleAnimation() {
		}

		@Override
		public boolean updateAnimation(long timestamp) {
			if (!_hasAnimation()) {
				// 不支持动画
				return false;
			}

			if (mBeginTime == 0) {
				mBeginTime = animationTimestamp();
				return true;
			}

			long progress = timestamp - mBeginTime;
			if (progress > mAnimationDuration) {
				// 一次动画结束
				mAnimationCount--;
				if (mAnimationCount > 0) {
					// 重复动画
					mBeginTime = 0;
					return true;
				} else {
					// 动画结束，清空动画
					animationFinished();
					return false;
				}
			}

			// 计算动画进度
			mAnimationProgress = 1.0F * progress / mAnimationDuration;

			markDirtyView();
			return true;
		}

		public void animationFinished() {
			mAnimationProgress = 1;
			markDirtyView();

			// 动画结束，发出通知事件
			_view.getOwnerPage()
				.invokeActionNoPopup(_view, "animationFinished");
		}

		public void prepareAnimation(int animationDuration, int animationCount) {
			mBeginTime = 0;
			mAnimationDuration = animationDuration > 0 ? animationDuration : DEF_ANIMATION_DURATION;
			mAnimationCount = animationCount > 0 ? animationCount : DEF_ANIMATION_COUNT;
		}

		public boolean hasAnimation() {
			return mAnimationCount > 0;
		}

		public float getAnimationProgress() {
			return mAnimationProgress;
		}
	}

	@Override
	public void syncData() {
		if (!_isDataChanged()) {
			return;
		}
		super.syncData();

		String testImage = _view.getAttrString("testImage");
		if (!TextUtils.isEmpty(testImage)) {
			if (_downloadDrawable == null) {
				_downloadDrawable = new XulDownloadDrawableItem(testImage, 64, 64, this);
			} else {
				_downloadDrawable.update(testImage, 64, 64);
			}
		} else {
			_downloadDrawable = null;
		}

		if (_hasAnimation()) {
			if (mIndicatorAnimation == null) {
				int animationDuration =
					XulUtils.tryParseInt(_view.getAttrString(CUSTOM_ATTR_ANI_DURATION));
				int animationCount =
					XulUtils.tryParseInt(_view.getAttrString(CUSTOM_ATTR_ANI_COUNT));
				mIndicatorAnimation = new IndicatorScaleAnimation();
				mIndicatorAnimation.prepareAnimation(animationDuration, animationCount);
				addAnimation(mIndicatorAnimation);
			}
		} else {
			if (mIndicatorAnimation != null) {
				removeAnimation(mIndicatorAnimation);
				mIndicatorAnimation = null;
			}
		}
	}

	@Override
	public void drawBackground(XulDC dc, Rect rect, int xBase, int yBase) {
		if ((mIndicatorAnimation != null) && mIndicatorAnimation.hasAnimation()) {
			RectF targetRc = getAnimRect();
			XulUtils.offsetRect(targetRc, _screenX + xBase, _screenY + yBase);

			Paint defSolidPaint = _ctx.getDefSolidPaint();
			final float animationProgress = mIndicatorAnimation.getAnimationProgress();
			int alphaVal = 0x88;
			if (animationProgress > 0.5) {
				alphaVal = (int) ((1.0F - animationProgress) / 0.5f * alphaVal);
			}
			defSolidPaint.setColor(0x222222 + alphaVal * 0x1000000);
			// 缩放比例从1到1.25
			float radius = targetRc.width() * (1 + animationProgress * SCALE_RATIO) / 2;
			dc.drawCircle(targetRc.centerX(), targetRc.centerY(), radius, defSolidPaint);
		}

		super.drawBackground(dc, rect, xBase, yBase);
	}

	@Override
	public void drawImages(XulDC dc, Rect rect, int xBase, int yBase) {
		if ((mIndicatorAnimation != null) && mIndicatorAnimation.hasAnimation()) {
			// 缩放动画不为空，执行缩放
			// 缩放比例从1到0.5
			float scalar = 1 - SCALE_RATIO * mIndicatorAnimation.getAnimationProgress();
			dc.save();
			final RectF animRect = getAnimRect();
			XulUtils.offsetRect(animRect, _screenX + xBase, _screenY + yBase);
			dc.scale(scalar, scalar, animRect.centerX(), animRect.centerY());
			super.drawImages(dc, rect, xBase, yBase);
			dc.restore();
		} else {
			super.drawImages(dc, rect, xBase, yBase);
		}
	}

	@Override
	public void draw(XulDC dc, Rect rect, int xBase, int yBase) {
		super.draw(dc, rect, xBase, yBase);
		RectF animRect = getAnimRect();
		_downloadDrawable.draw(dc, (int) (animRect.left + xBase), (int) (animRect.top + yBase)
			, XulUtils.calRectWidth(animRect) / 2, XulUtils.calRectHeight(animRect) / 2, XulRenderContext.getDefPicPaint());
	}

	@Override
	public XulWorker.DrawableItem collectPendingImageItem() {
		XulWorker.DrawableItem drawableItem = super.collectPendingImageItem();
		drawableItem = _downloadDrawable.collectPendingImageItem(drawableItem);
		return drawableItem;
	}
}
