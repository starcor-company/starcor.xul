package com.starcor.xul.Graphics;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import com.starcor.xul.XulUtils;

/**
 * Created by hy on 2014/6/20.
 */
public class XulCommonDrawable extends XulDrawable {
	Drawable _drawable;

	@Override
	public boolean draw(Canvas canvas, Rect rc, Rect dst, Paint paint) {
		Drawable drawable = _drawable;
		if (drawable == null) {
			return false;
		}
		drawable.setBounds(dst);
		drawable.draw(canvas);
		return true;
	}

	@Override
	public boolean draw(Canvas canvas, Rect rc, RectF dst, Paint paint) {
		Drawable drawable = _drawable;
		if (drawable == null) {
			return false;
		}
		drawable.setBounds(XulUtils.roundToInt(dst.left), XulUtils.roundToInt(dst.top), XulUtils.roundToInt(dst.right), XulUtils.roundToInt(dst.bottom));
		drawable.draw(canvas);
		return true;
	}

	@Override
	public int getHeight() {
		Drawable drawable = _drawable;
		if (drawable == null) {
			return 0;
		}
		return drawable.getMinimumHeight();
	}

	@Override
	public int getWidth() {
		Drawable drawable = _drawable;
		if (drawable == null) {
			return 0;
		}
		return drawable.getMinimumWidth();
	}
}
