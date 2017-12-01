package com.starcor.xul.Graphics;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/**
 * Created by hy on 2014/5/27.
 */
public abstract class XulDrawable {
	String _url;
	String _key;
	volatile boolean _isRecycled = false;

	public boolean cacheable() {
		return true;
	}

	public boolean isRecycled() {
		return _isRecycled;
	}

	public void recycle() {
		_isRecycled = true;
	}

	public static XulDrawable fromNinePatchBitmap(Bitmap bmp, String url, String imageKey) {
		if (bmp == null) {
			return null;
		}

		return XulNinePatchDrawable.build(bmp, url, imageKey);
	}

	public static XulDrawable fromColor(int color, int width, int height, String url, String imageKey) {
		XulColorDrawable drawable = new XulColorDrawable(color, width, height);
		drawable._url = url;
		drawable._key = imageKey;
		return drawable;
	}

	public static XulDrawable fromColor(int color, int width, int height, float radiusX, float radiusY, String url, String imageKey) {
		XulColorDrawable drawable = new XulColorDrawable(color, width, height, radiusX, radiusY);
		drawable._url = url;
		drawable._key = imageKey;
		return drawable;
	}

	public static XulDrawable fromColor(int color, int width, int height, float[] roundRectRadius, String url, String imageKey) {
		XulColorDrawable drawable = new XulColorDrawable(color, width, height, roundRectRadius);
		drawable._url = url;
		drawable._key = imageKey;
		return drawable;
	}

	public static XulDrawable fromBitmap(Bitmap bitmap, String url, String imageKey) {
		if (bitmap == null) {
			return null;
		}
		XulBitmapDrawable drawable = new XulBitmapDrawable();
		drawable._bmp = bitmap;
		drawable._url = url;
		drawable._key = imageKey;
		return drawable;
	}

	public static XulDrawable fromDrawable(final Drawable drawable, String url, String imageKey) {
		if (drawable == null) {
			return null;
		}
		XulDrawable xulDrawable = new XulAndroidDrawable(drawable);
		xulDrawable._url = url;
		xulDrawable._key = imageKey;
		return xulDrawable;
	}

	public abstract boolean draw(Canvas canvas, Rect rc, Rect dst, Paint paint);

	public abstract boolean draw(Canvas canvas, Rect rc, RectF dst, Paint paint);

	public abstract int getHeight();

	public abstract int getWidth();

	public final String getUrl() {
		return _url;
	}

	public final String getKey() {
		return _key;
	}

	public XulDrawable makeClone() {
		return null;
	}
}
