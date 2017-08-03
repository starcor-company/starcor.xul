package com.starcor.xul.Graphics;

import android.graphics.*;
import com.starcor.xul.Utils.XulCachedHashMap;
import com.starcor.xul.XulUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hy on 2014/6/20.
 */
public class XulFrameAnimationDrawable extends XulAnimationDrawable {
	private static class frameInfo {
		int delay;
		int frameImg;
		boolean isSrcOffset = false;
		Rect src = new Rect();
		boolean isDstOffset = false;
		Rect dst;
	}

	private static class frameAnimationDrawingCtx extends AnimationDrawingContext {
		boolean reset = false;
		int repeat = 0;
		int frameIdx = -1;
		Rect lastSrc = new Rect();
		Rect lastDst = new Rect();
		ArrayList<frameInfo> frames;
		Rect aniRc;
		int maxRepeat;
		long lastUpdateTimestamp = 0;

		@Override
		public boolean updateAnimation(long timestamp) {
			if (frames.isEmpty()) {
				return false;
			}
			if (frameIdx < 0 || reset) {
				reset = false;
				XulUtils.copyRect(aniRc, lastSrc);
				XulUtils.copyRect(aniRc, lastDst);
				frameIdx = 0;
				repeat = 0;
			} else if ((maxRepeat == 0 || repeat < maxRepeat) && timestamp > lastUpdateTimestamp) {
				++frameIdx;
				if (frameIdx >= frames.size()) {
					++repeat;
					if (maxRepeat == 0 || repeat < maxRepeat) {
						frameIdx = 0;
						XulUtils.copyRect(aniRc, lastSrc);
						XulUtils.copyRect(aniRc, lastDst);
					} else {
						--frameIdx;
						return false;
					}
				}
			} else {
				return false;
			}
			frameInfo frameInfo = frames.get(frameIdx);
			updateFrameRect(frameInfo);
			lastUpdateTimestamp = timestamp + frameInfo.delay;
			return true;
		}

		private void updateFrameRect(frameInfo frameInfo) {
			Rect frameSrc = frameInfo.src;
			Rect frameDst = frameInfo.dst;

			if (frameInfo.isSrcOffset) {
				lastSrc.left += frameSrc.left;
				lastSrc.top += frameSrc.top;
				lastSrc.right += frameSrc.left + frameSrc.right;
				lastSrc.bottom += frameSrc.top + frameSrc.bottom;
			} else {
				XulUtils.copyRect(frameSrc, lastSrc);
			}

			if (frameDst == null) {
			} else if (frameInfo.isDstOffset) {
				lastDst.left += frameDst.left;
				lastDst.top += frameDst.top;
				lastDst.right += frameDst.left + frameDst.right;
				lastDst.bottom += frameDst.top + frameDst.bottom;
			} else {
				XulUtils.copyRect(frameDst, lastDst);
			}
		}

		@Override
		public boolean isAnimationFinished() {
			if (frames.size() <= 1) {
				return true;
			}
			if (reset) {
				return false;
			}
			return maxRepeat != 0 && repeat >= maxRepeat;
		}

		@Override
		public void reset() {
			reset = true;
		}
	}

	static Pattern frameLinePattern = Pattern.compile("^([F])(?:\\+(\\d+))?,(\\d+),(.+?)(?:,(\\+)?\\((\\d+),(\\d+)(?:,(\\d+),(\\d+))?\\))?(?:,(\\+)?\\((\\d+),(\\d+)(?:,(\\d+),(\\d+))?\\))?$", Pattern.CASE_INSENSITIVE);

	ArrayList<XulDrawable> _frameImgs = new ArrayList<XulDrawable>();
	Rect _rc = new Rect();
	int _repeat = 1;
	ArrayList<frameInfo> _frames = new ArrayList<frameInfo>();

	public static XulFrameAnimationDrawable buildAnimation(AnimationPackage pkg) {
		XulFrameAnimationDrawable drawable = null;
		ArrayList<String> aniDesc = pkg.getAniDesc();
		XulCachedHashMap<String, Integer> imageMap = new XulCachedHashMap<String, Integer>();
		for (int i = 0; i < aniDesc.size(); i++) {
			String lineInfo = aniDesc.get(i);
			if (i == 0) {
				String[] frameAniParams = lineInfo.split(",");
				if (frameAniParams.length != 4 || !"frame".equals(frameAniParams[0])) {
					break;
				}
				drawable = new XulFrameAnimationDrawable();
				drawable._rc.set(0, 0, XulUtils.tryParseInt(frameAniParams[1], 0), XulUtils.tryParseInt(frameAniParams[2], 0));
				drawable._repeat = XulUtils.tryParseInt(frameAniParams[3], 1);
			} else {
				Matcher m = frameLinePattern.matcher(lineInfo);
				if (!m.matches()) {
					continue;
				}
				String mode = m.group(1);
				String repeat = m.group(2);
				String delay = m.group(3);
				String image = m.group(4);

				String srcIsOffset = m.group(5);
				String srcLeft = m.group(6);
				String srcTop = m.group(7);
				String srcWidth = m.group(8);
				String srcHeight = m.group(9);

				String dstIsOffset = m.group(10);
				String dstLeft = m.group(11);
				String dstTop = m.group(12);
				String dstWidth = m.group(13);
				String dstHeight = m.group(14);

				frameInfo fInfo = new frameInfo();

				fInfo.delay = XulUtils.tryParseInt(delay, 30);

				Integer imgId;
				if (image == null) {
					imgId = drawable._frameImgs.size() - 1;
				} else {
					imgId = imageMap.get(image);
					if (imgId == null) {
						Bitmap bmp = pkg.loadFrameImage(image);
						if (bmp != null) {
							drawable._frameImgs.add(XulBitmapDrawable.fromBitmap(bmp, "", ""));
							imgId = drawable._frameImgs.size() - 1;
						} else {
							imgId = -1;
						}
						imageMap.put(image, imgId);
					}
				}
				fInfo.frameImg = imgId;
				if ("+".equals(srcIsOffset)) {
					fInfo.isSrcOffset = true;
				}
				if ("+".equals(dstIsOffset)) {
					fInfo.isDstOffset = true;
				}
				int imgWidth = 0;
				int imgHeight = 0;
				if (imgId >= 0) {
					XulDrawable bitmap = drawable._frameImgs.get(imgId);
					imgWidth = bitmap.getWidth();
					imgHeight = bitmap.getHeight();
				}

				if (fInfo.isSrcOffset) {
					fInfo.src.set(XulUtils.tryParseInt(srcLeft, 0), XulUtils.tryParseInt(srcTop, 0), XulUtils.tryParseInt(srcWidth, 0), XulUtils.tryParseInt(srcHeight, 0));
				} else {
					fInfo.src.set(0, 0, XulUtils.tryParseInt(srcWidth, imgWidth), XulUtils.tryParseInt(srcHeight, imgHeight));
					XulUtils.offsetRect(fInfo.src, XulUtils.tryParseInt(srcLeft, 0), XulUtils.tryParseInt(srcTop, 0));
				}

				if (fInfo.isDstOffset) {
					if (dstHeight == null && dstWidth == null && dstLeft == null && dstTop == null) {
						fInfo.dst = null;
					} else {
						fInfo.dst = new Rect();
						fInfo.dst.set(XulUtils.tryParseInt(dstLeft, 0), XulUtils.tryParseInt(dstTop, 0), XulUtils.tryParseInt(dstWidth, 0), XulUtils.tryParseInt(dstHeight, 0));
					}
				} else {
					if (dstHeight == null && dstWidth == null && dstLeft == null && dstTop == null) {
						fInfo.dst = null;
					} else {
						fInfo.dst = new Rect();
						fInfo.dst.set(0, 0, XulUtils.tryParseInt(dstWidth, XulUtils.calRectWidth(drawable._rc)), XulUtils.tryParseInt(dstHeight, XulUtils.calRectHeight(drawable._rc)));
						XulUtils.offsetRect(fInfo.dst, XulUtils.tryParseInt(dstLeft, 0), XulUtils.tryParseInt(dstTop, 0));
					}
				}

				int repCount = XulUtils.tryParseInt(repeat, 1);
				if (repCount <= 0) {
					repCount = 1;
				}
				if (repCount > 1000) {
					repCount = 1000;
				}
				while (repCount > 0) {
					--repCount;
					drawable._frames.add(fInfo);
				}
			}
		}

		return drawable;
	}

	@Override
	public boolean drawAnimation(AnimationDrawingContext animationDrawingContext, XulDC dc, Rect dst, Paint paint) {
		if (animationDrawingContext == null || !(animationDrawingContext instanceof frameAnimationDrawingCtx)) {
			return false;
		}
		frameAnimationDrawingCtx ctx = (frameAnimationDrawingCtx) animationDrawingContext;
		int currentFrame = ctx.frameIdx;
		frameInfo frameInfo = _frames.get(currentFrame);
		Rect lastSrc = ctx.lastSrc;
		Rect lastDst = ctx.lastDst;

		int frameImg = frameInfo.frameImg;
		if (frameImg < 0 || frameImg >= _frameImgs.size()) {
			return false;
		}
		XulDrawable bmp = _frameImgs.get(frameImg);
		int outWidth = XulUtils.calRectWidth(dst);
		int outHeight = XulUtils.calRectHeight(dst);
		int aniWidth = XulUtils.calRectWidth(_rc);
		int aniHeight = XulUtils.calRectHeight(_rc);
		if (bmp != null && aniWidth > 0 && aniHeight > 0) {
			float xScalar = ((float) outWidth) / aniWidth;
			float yScalar = ((float) outHeight) / aniHeight;
			dc.drawBitmap(bmp, lastSrc.left, lastSrc.top, XulUtils.calRectWidth(lastSrc), XulUtils.calRectHeight(lastSrc),
				dst.left + lastDst.left * xScalar, dst.top + lastDst.top * yScalar, XulUtils.calRectWidth(lastDst) * xScalar, XulUtils.calRectHeight(lastSrc) * yScalar, paint);
		}
		return false;
	}

	@Override
	public boolean drawAnimation(AnimationDrawingContext animationDrawingContext, XulDC dc, RectF dst, Paint paint) {
		if (animationDrawingContext == null || !(animationDrawingContext instanceof frameAnimationDrawingCtx)) {
			return false;
		}
		frameAnimationDrawingCtx ctx = (frameAnimationDrawingCtx) animationDrawingContext;
		int currentFrame = ctx.frameIdx;
		frameInfo frameInfo = _frames.get(currentFrame);
		Rect lastSrc = ctx.lastSrc;
		Rect lastDst = ctx.lastDst;

		int frameImg = frameInfo.frameImg;
		if (frameImg < 0 || frameImg >= _frameImgs.size()) {
			return false;
		}
		XulDrawable bmp = _frameImgs.get(frameImg);
		float outWidth = XulUtils.calRectWidth(dst);
		float outHeight = XulUtils.calRectHeight(dst);
		int aniWidth = XulUtils.calRectWidth(_rc);
		int aniHeight = XulUtils.calRectHeight(_rc);
		if (bmp != null && aniWidth > 0 && aniHeight > 0) {
			float xScalar = outWidth / aniWidth;
			float yScalar = outHeight / aniHeight;
			dc.drawBitmap(bmp, lastSrc.left, lastSrc.top, XulUtils.calRectWidth(lastSrc), XulUtils.calRectHeight(lastSrc),
				dst.left + lastDst.left * xScalar, dst.top + lastDst.top * yScalar, XulUtils.calRectWidth(lastDst) * xScalar, XulUtils.calRectHeight(lastSrc) * yScalar, paint);
		}
		return false;
	}

	@Override
	public AnimationDrawingContext createDrawingCtx() {
		frameAnimationDrawingCtx ctx = new XulFrameAnimationDrawable.frameAnimationDrawingCtx();
		ctx.frames = _frames;
		ctx.aniRc = _rc;
		ctx.maxRepeat = _repeat;
		ctx.updateAnimation(XulUtils.timestamp());
		return ctx;
	}

	@Override
	public boolean draw(Canvas canvas, Rect rc, Rect dst, Paint paint) {
		return false;
	}

	@Override
	public boolean draw(Canvas canvas, Rect rc, RectF dst, Paint paint) {
		return false;
	}

	@Override
	public int getHeight() {
		return XulUtils.calRectHeight(_rc);
	}

	@Override
	public int getWidth() {
		return XulUtils.calRectWidth(_rc);
	}
}
