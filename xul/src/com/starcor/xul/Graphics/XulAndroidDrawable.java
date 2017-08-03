package com.starcor.xul.Graphics;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
* Created by hy on 2014/7/10.
*/
class XulAndroidDrawable extends XulDrawable {
	private final Drawable drawable;

	public XulAndroidDrawable(Drawable drawable) {
		this.drawable = drawable;
	}

	@Override
	public boolean draw(Canvas canvas, Rect rc, Rect dst, Paint paint) {
		drawable.setBounds(dst);
		drawable.draw(canvas);
		return true;
	}

	@Override
	public boolean draw(Canvas canvas, Rect rc, RectF dst, Paint paint) {
		drawable.setBounds((int)dst.left, (int)dst.top, (int)dst.right, (int)dst.bottom);
		drawable.draw(canvas);
		return false;
	}

	@Override
	public int getHeight() {
		int height = drawable.getIntrinsicHeight();
		if (height <= 0) {
			height = drawable.getMinimumHeight();
		}
		return height;
	}

	@Override
	public int getWidth() {
		int width = drawable.getIntrinsicWidth();
		if (width <= 0) {
			width = drawable.getMinimumWidth();
		}
		return width;
	}
}
