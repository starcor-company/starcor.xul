package com.starcor.xul.Graphics;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;

import com.starcor.xul.XulManager;
import com.starcor.xul.XulUtils;

/**
 * Created by hy on 2014/6/20.
 */
public class XulNinePatchDrawable extends XulDrawable {
	Drawable _drawable;
	Bitmap _bmp;
	Rect _patchRect;
	int _width;
	int _height;


	public static XulDrawable build(Bitmap bmp, String url, String imageKey) {
		if (bmp == null) {
			return null;
		}

		byte[] ninePatchChunk = bmp.getNinePatchChunk();
		XulNinePatchDrawable drawable;
		if (ninePatchChunk != null) {
			drawable = new XulNinePatchDrawable();
			if (!drawable.attach(bmp, ninePatchChunk)) {
				return null;
			}
		} else if (bmp.getWidth() > 3 && bmp.getHeight() > 3) {
			drawable = new XulNinePatchDrawable();
			drawable.attach(bmp);
		} else {
			return null;
		}
		drawable._url = url;
		drawable._key = imageKey;
		return drawable;
	}

	private boolean attach(Bitmap bmp, byte[] ninePatchChunk) {
		if (ninePatchChunk == null) {
			return false;
		}
		_drawable = new NinePatchDrawable(bmp, ninePatchChunk, null, _key);
		_width = _drawable.getMinimumWidth();
		_height = _drawable.getMinimumHeight();
		return true;
	}


	private boolean attach(Bitmap bmp) {
		_width = bmp.getWidth();
		_height = bmp.getHeight();

		if (_width < 3 || _height < 3) {
			return false;
		}

		_patchRect = new Rect();
		_patchRect.left = _width;
		_patchRect.right = 0;
		_patchRect.top = _height;
		_patchRect.bottom = 0;
		for (int i = 0; i < _width; i++) {
			int pixel = bmp.getPixel(i, 0);
			if (pixel == Color.BLACK) {
				if (_patchRect.left > i) {
					_patchRect.left = i;
				}
				if (_patchRect.right < i) {
					_patchRect.right = i;
				}
			}
		}

		_patchRect.right = _width - _patchRect.right;

		for (int i = 0; i < _height; i++) {
			int pixel = bmp.getPixel(0, i);
			if (pixel == Color.BLACK) {
				if (_patchRect.top > i) {
					_patchRect.top = i;
				}
				if (_patchRect.bottom < i) {
					_patchRect.bottom = i;
				}
			}
		}
		_patchRect.bottom = _height - _patchRect.bottom;
		Matrix matrix = new Matrix();
		float xScalar = XulManager.getGlobalXScalar();
		float yScalar = XulManager.getGlobalYScalar();
		matrix.setScale(xScalar, yScalar);
		_bmp = Bitmap.createBitmap(bmp, 1, 1, _width - 2, _height - 2, matrix, true);
		XulUtils.offsetRect(_patchRect, -1, -1);
		_patchRect.left = XulUtils.roundToInt(_patchRect.left * xScalar);
		_patchRect.top = XulUtils.roundToInt(_patchRect.top * xScalar);
		_patchRect.right = XulUtils.roundToInt(_patchRect.right * xScalar);
		_patchRect.bottom = XulUtils.roundToInt(_patchRect.bottom * xScalar);
		_width = _bmp.getWidth();
		_height = _bmp.getHeight();
		return true;
	}

	@Override
	public boolean draw(Canvas canvas, Rect rc, Rect dst, Paint paint) {
		Drawable drawable = _drawable;
		if (drawable != null) {
			drawable.setBounds(dst);
			drawable.draw(canvas);
			return true;
		}
		Bitmap bmp = _bmp;
		if (bmp != null) {
			Rect src = XulDC._tmpRc0;
			Rect newDst = XulDC._tmpRc1;

			int sX1 = 0;
			int sX2 = _patchRect.left;
			int sX3 = _width - _patchRect.right;
			int sX4 = _width;

			int sY1 = 0;
			int sY2 = _patchRect.top;
			int sY3 = _height - _patchRect.bottom;
			int sY4 = _height;


			int dX1 = dst.left;
			int dX2 = dst.left + _patchRect.left;
			int dX3 = dst.right - _patchRect.right;
			int dX4 = dst.right;

			int dY1 = dst.top;
			int dY2 = dst.top + _patchRect.top;
			int dY3 = dst.bottom - _patchRect.bottom;
			int dY4 = dst.bottom;

			src.set(sX1, sY1, sX2, sY2);
			newDst.set(dX1, dY1, dX2, dY2);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX2, sY1, sX3, sY2);
			newDst.set(dX2, dY1, dX3, dY2);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX3, sY1, sX4, sY2);
			newDst.set(dX3, dY1, dX4, dY2);
			canvas.drawBitmap(bmp, src, newDst, paint);


			src.set(sX1, sY2, sX2, sY3);
			newDst.set(dX1, dY2, dX2, dY3);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX2, sY2, sX3, sY3);
			newDst.set(dX2, dY2, dX3, dY3);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX3, sY2, sX4, sY3);
			newDst.set(dX3, dY2, dX4, dY3);
			canvas.drawBitmap(bmp, src, newDst, paint);


			src.set(sX1, sY3, sX2, sY4);
			newDst.set(dX1, dY3, dX2, dY4);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX2, sY3, sX3, sY4);
			newDst.set(dX2, dY3, dX3, dY4);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX3, sY3, sX4, sY4);
			newDst.set(dX3, dY3, dX4, dY4);
			canvas.drawBitmap(bmp, src, newDst, paint);
			return true;
		}
		return false;
	}

	@Override
	public boolean draw(Canvas canvas, Rect rc, RectF dst, Paint paint) {
		Drawable drawable = _drawable;
		if (drawable != null) {
			drawable.setBounds(XulUtils.roundToInt(dst.left), XulUtils.roundToInt(dst.top), XulUtils.roundToInt(dst.right), XulUtils.roundToInt(dst.bottom));
			drawable.draw(canvas);
			return true;
		}

		Bitmap bmp = _bmp;
		if (bmp != null) {
			Rect src = XulDC._tmpRc0;
			RectF newDst = XulDC._tmpFRc1;

			int sX1 = 0;
			int sX2 = _patchRect.left;
			int sX3 = _width - _patchRect.right;
			int sX4 = _width;

			int sY1 = 0;
			int sY2 = _patchRect.top;
			int sY3 = _height - _patchRect.bottom;
			int sY4 = _height;

			float dX1 = XulUtils.roundToInt(dst.left);
			float dX4 = XulUtils.roundToInt(dst.right);
			float dY1 = XulUtils.roundToInt(dst.top);
			float dY4 = XulUtils.roundToInt(dst.bottom);

			float dX2 = dX1 + _patchRect.left;
			float dX3 = dX4 - _patchRect.right;
			float dY2 = dY1 + _patchRect.top;
			float dY3 = dY4 - _patchRect.bottom;

			src.set(sX1, sY1, sX2, sY2);
			newDst.set(dX1, dY1, dX2, dY2);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX2, sY1, sX3, sY2);
			newDst.set(dX2, dY1, dX3, dY2);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX3, sY1, sX4, sY2);
			newDst.set(dX3, dY1, dX4, dY2);
			canvas.drawBitmap(bmp, src, newDst, paint);


			src.set(sX1, sY2, sX2, sY3);
			newDst.set(dX1, dY2, dX2, dY3);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX2, sY2, sX3, sY3);
			newDst.set(dX2, dY2, dX3, dY3);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX3, sY2, sX4, sY3);
			newDst.set(dX3, dY2, dX4, dY3);
			canvas.drawBitmap(bmp, src, newDst, paint);


			src.set(sX1, sY3, sX2, sY4);
			newDst.set(dX1, dY3, dX2, dY4);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX2, sY3, sX3, sY4);
			newDst.set(dX2, dY3, dX3, dY4);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX3, sY3, sX4, sY4);
			newDst.set(dX3, dY3, dX4, dY4);
			canvas.drawBitmap(bmp, src, newDst, paint);
			return true;

		}
		return false;
	}

	public boolean drawBorderOnly(Canvas canvas, Rect rc, Rect dst, Paint paint) {
		Drawable drawable = _drawable;
		if (drawable != null) {
			drawable.setBounds(dst);
			drawable.draw(canvas);
			return true;
		}
		Bitmap bmp = _bmp;
		if (bmp != null) {
			Rect src = XulDC._tmpRc0;
			Rect newDst = XulDC._tmpRc1;

			int sX1 = 0;
			int sX2 = _patchRect.left;
			int sX3 = _width - _patchRect.right;
			int sX4 = _width;

			int sY1 = 0;
			int sY2 = _patchRect.top;
			int sY3 = _height - _patchRect.bottom;
			int sY4 = _height;


			int dX1 = dst.left;
			int dX2 = dst.left + _patchRect.left;
			int dX3 = dst.right - _patchRect.right;
			int dX4 = dst.right;

			int dY1 = dst.top;
			int dY2 = dst.top + _patchRect.top;
			int dY3 = dst.bottom - _patchRect.bottom;
			int dY4 = dst.bottom;

			src.set(sX1, sY1, sX2, sY2);
			newDst.set(dX1, dY1, dX2, dY2);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX2, sY1, sX3, sY2);
			newDst.set(dX2, dY1, dX3, dY2);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX3, sY1, sX4, sY2);
			newDst.set(dX3, dY1, dX4, dY2);
			canvas.drawBitmap(bmp, src, newDst, paint);


			src.set(sX1, sY2, sX2, sY3);
			newDst.set(dX1, dY2, dX2, dY3);
			canvas.drawBitmap(bmp, src, newDst, paint);

//			src.set(sX2, sY2, sX3, sY3);
//			newDst.set(dX2, dY2, dX3, dY3);
//			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX3, sY2, sX4, sY3);
			newDst.set(dX3, dY2, dX4, dY3);
			canvas.drawBitmap(bmp, src, newDst, paint);


			src.set(sX1, sY3, sX2, sY4);
			newDst.set(dX1, dY3, dX2, dY4);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX2, sY3, sX3, sY4);
			newDst.set(dX2, dY3, dX3, dY4);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX3, sY3, sX4, sY4);
			newDst.set(dX3, dY3, dX4, dY4);
			canvas.drawBitmap(bmp, src, newDst, paint);
			return true;
		}
		return false;
	}

	public boolean drawBorderOnly(Canvas canvas, Rect rc, RectF dst, Paint paint) {
		Drawable drawable = _drawable;
		if (drawable != null) {
			drawable.setBounds(XulUtils.roundToInt(dst.left), XulUtils.roundToInt(dst.top), XulUtils.roundToInt(dst.right), XulUtils.roundToInt(dst.bottom));
			drawable.draw(canvas);
			return true;
		}

		Bitmap bmp = _bmp;
		if (bmp != null) {
			Rect src = XulDC._tmpRc0;
			RectF newDst = XulDC._tmpFRc1;

			int sX1 = 0;
			int sX2 = _patchRect.left;
			int sX3 = _width - _patchRect.right;
			int sX4 = _width;

			int sY1 = 0;
			int sY2 = _patchRect.top;
			int sY3 = _height - _patchRect.bottom;
			int sY4 = _height;

			float dX1 = XulUtils.roundToInt(dst.left);
			float dX4 = XulUtils.roundToInt(dst.right);
			float dY1 = XulUtils.roundToInt(dst.top);
			float dY4 = XulUtils.roundToInt(dst.bottom);

			float dX2 = dX1 + _patchRect.left;
			float dX3 = dX4 - _patchRect.right;
			float dY2 = dY1 + _patchRect.top;
			float dY3 = dY4 - _patchRect.bottom;

			src.set(sX1, sY1, sX2, sY2);
			newDst.set(dX1, dY1, dX2, dY2);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX2, sY1, sX3, sY2);
			newDst.set(dX2, dY1, dX3, dY2);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX3, sY1, sX4, sY2);
			newDst.set(dX3, dY1, dX4, dY2);
			canvas.drawBitmap(bmp, src, newDst, paint);


			src.set(sX1, sY2, sX2, sY3);
			newDst.set(dX1, dY2, dX2, dY3);
			canvas.drawBitmap(bmp, src, newDst, paint);

//			src.set(sX2, sY2, sX3, sY3);
//			newDst.set(dX2, dY2, dX3, dY3);
//			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX3, sY2, sX4, sY3);
			newDst.set(dX3, dY2, dX4, dY3);
			canvas.drawBitmap(bmp, src, newDst, paint);


			src.set(sX1, sY3, sX2, sY4);
			newDst.set(dX1, dY3, dX2, dY4);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX2, sY3, sX3, sY4);
			newDst.set(dX2, dY3, dX3, dY4);
			canvas.drawBitmap(bmp, src, newDst, paint);

			src.set(sX3, sY3, sX4, sY4);
			newDst.set(dX3, dY3, dX4, dY4);
			canvas.drawBitmap(bmp, src, newDst, paint);
			return true;

		}
		return false;
	}

	@Override
	public int getHeight() {
		return _height;
	}

	@Override
	public int getWidth() {
		return _width;
	}
}
