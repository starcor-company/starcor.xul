package com.starcor.xul.Graphics;

import android.graphics.*;

/**
 * Created by hy on 2014/6/20.
 */
public class XulBitmapDrawable extends XulDrawable {
	Bitmap _bmp;

	@Override
	public boolean draw(Canvas canvas, Rect rc, Rect dst, Paint paint) {
		Bitmap bmp = _bmp;
		if (bmp == null) {
			return false;
		}
		canvas.drawBitmap(bmp, rc, dst, paint);
		return true;
	}

	@Override
	public boolean draw(Canvas canvas, Rect rc, RectF dst, Paint paint) {
		Bitmap bmp = _bmp;
		if (bmp == null) {
			return false;
		}
		canvas.drawBitmap(bmp, rc, dst, paint);
		return true;
	}

	@Override
	public int getHeight() {
		Bitmap bmp = _bmp;
		if (bmp == null) {
			return 0;
		}
		return bmp.getHeight();
	}

	@Override
	public int getWidth() {
		Bitmap bmp = _bmp;
		if (bmp == null) {
			return 0;
		}
		return bmp.getWidth();
	}

	@Override
	public XulDrawable makeClone() {
		return XulDrawable.fromBitmap(BitmapTools.createBitmap(_bmp), _url, _url);
	}

	public static Bitmap detachBitmap(XulBitmapDrawable bmp) {
		Bitmap b = bmp._bmp;
		bmp._bmp = null;
		return b;
	}
}
