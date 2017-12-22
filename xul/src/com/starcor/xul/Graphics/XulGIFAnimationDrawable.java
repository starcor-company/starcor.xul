package com.starcor.xul.Graphics;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.UrlQuerySanitizer;

import com.starcor.xul.XulUtils;

import java.io.InputStream;

/**
 * Created by john on 2017/12/19.
 */

public class XulGIFAnimationDrawable extends XulAnimationDrawable {
	XulGIFDecoder.GIFAnimationRender _gifRender;
	private float _speed;

	@Override
	protected void finalize() throws Throwable {
		if (this instanceof XulGIFAnimationDrawable) {
			BitmapTools.recycleBitmap(_gifRender._frameImage);
		}
		super.finalize();
	}

	public static XulDrawable buildAnimation(InputStream stream, String url, String imageKey) {
		if (stream == null) {
			return null;
		}

		UrlQuerySanitizer s = new UrlQuerySanitizer(url);
		boolean noLoop = s.hasParameter("NoLoop");
		boolean noTransparent = s.hasParameter("noTransparent");
		float speed = XulUtils.tryParseFloat(s.getValue("Speed"), 1.0f);
		if (speed <= 0) {
			speed = 0.01f;
		}

		XulGIFDecoder.GIFFrame[] frames = XulGIFDecoder.decode(stream, noLoop, noTransparent);

		if (frames.length == 1) {
			XulGIFDecoder.GIFStaticRender staticRenderer = XulGIFDecoder.createStaticRenderer(frames, noTransparent);
			return staticRenderer.extractDrawable(url, imageKey);
		} else {

			XulGIFAnimationDrawable drawable = new XulGIFAnimationDrawable();
			drawable._gifRender = XulGIFDecoder.createAnimationRenderer(frames, noLoop, noTransparent);
			drawable._url = url;
			drawable._key = imageKey;
			drawable._speed = speed;
			return drawable;
		}
	}

	@Override
	public boolean draw(Canvas canvas, Rect rc, Rect dst, Paint paint) {
		_gifRender.draw(canvas, 0, 0, _gifRender.getWidth(), _gifRender.getHeight(), dst.left, dst.top, XulUtils.calRectWidth(dst), XulUtils.calRectHeight(dst), paint);
		return true;
	}

	@Override
	public boolean draw(Canvas canvas, Rect rc, RectF dst, Paint paint) {
		_gifRender.draw(canvas, 0, 0, _gifRender.getWidth(), _gifRender.getHeight(), dst.left, dst.top, XulUtils.calRectWidth(dst), XulUtils.calRectHeight(dst), paint);
		return true;
	}

	@Override
	public int getHeight() {
		return _gifRender.getHeight();
	}

	@Override
	public int getWidth() {
		return _gifRender.getWidth();
	}

	@Override
	public boolean drawAnimation(AnimationDrawingContext ctx, XulDC dc, Rect dst, Paint paint) {
		_gifRender.draw(dc, 0, 0, _gifRender.getWidth(), _gifRender.getHeight(), dst.left, dst.top, XulUtils.calRectWidth(dst), XulUtils.calRectHeight(dst), paint);
		return true;
	}

	@Override
	public boolean drawAnimation(AnimationDrawingContext ctx, XulDC dc, RectF dst, Paint paint) {
		_gifRender.draw(dc, 0, 0, _gifRender.getWidth(), _gifRender.getHeight(), dst.left, dst.top, XulUtils.calRectWidth(dst), XulUtils.calRectHeight(dst), paint);
		return true;
	}

	@Override
	public AnimationDrawingContext createDrawingCtx() {
		return new frameAnimationDrawingCtx();
	}


	private class frameAnimationDrawingCtx extends AnimationDrawingContext {
		private boolean _reset = false;

		@Override
		public boolean updateAnimation(long timestamp) {
			if (_reset) {
				_gifRender.reset();
				_gifRender.decodeFrame();
				_reset = false;
				return true;
			}

			if (_gifRender.nextFrame(timestamp, _speed)) {
				_gifRender.decodeFrame();
				return true;
			}
			return false;
		}


		@Override
		public boolean isAnimationFinished() {
//			if (_reset) {
//				return true;
//			}
			return false;
		}

		@Override
		public void reset() {
			_reset = true;
		}
	}
}
