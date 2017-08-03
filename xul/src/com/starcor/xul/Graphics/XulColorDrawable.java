package com.starcor.xul.Graphics;

import android.graphics.*;
import android.graphics.drawable.shapes.RoundRectShape;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulUtils;

/**
 * Created by hy on 2015/6/3.
 */
public class XulColorDrawable extends XulDrawable {
	int _color;
	int _width;
	int _height;
	float[] _radius = null;
	RoundRectShape _roundRectShape;

	public XulColorDrawable(int color, int width, int height) {
		this(color, width, height, 0f, 0f);
	}

	public XulColorDrawable(int color, int width, int height, float radiusX, float radiusY) {
		this(color, width, height, (radiusX >= 0.5f && radiusY >= 0.5f) ? new float[]{radiusX, radiusY} : null);
	}

	public XulColorDrawable(int color, int width, int height, float[] roundRectRadius) {
		this._color = color;
		this._width = width;
		this._height = height;
		_radius = roundRectRadius;
		if (_radius == null) {
			return;
		}
		if (_radius.length == 8) {
			_roundRectShape = new RoundRectShape(_radius, null, null);
		}
	}

	public void recycle() {
	}

	private int calDrawingColor(Paint paint) {
		int colorAlpha = Color.alpha(_color);
		if (colorAlpha == 0) {
			return 0;
		}
		int alpha = paint.getAlpha();
		colorAlpha *= alpha;
		if (colorAlpha == 0) {
			return 0;
		}
		colorAlpha /= 0xFF;
		return Color.argb(colorAlpha, Color.red(_color), Color.green(_color), Color.blue(_color));
	}

	private boolean drawRect(Canvas canvas, Rect dst, Paint paint) {
		int newColor = calDrawingColor(paint);
		if (newColor == 0) {
			return true;
		}

		XulUtils.saveCanvas(canvas);
		canvas.clipRect(dst);
		canvas.drawColor(newColor);
		XulUtils.restoreCanvas(canvas);
		return true;
	}

	private boolean drawRect(Canvas canvas, RectF dst, Paint paint) {
		int newColor = calDrawingColor(paint);
		if (newColor == 0) {
			return true;
		}

		XulUtils.saveCanvas(canvas);
		canvas.clipRect(dst);
		canvas.drawColor(newColor);
		XulUtils.restoreCanvas(canvas);
		return true;
	}

	private boolean drawRoundRect(Canvas canvas, Rect dst, Paint paint) {
		RectF rc = XulDC._tmpFRc1;
		rc.set(dst);
		return drawRoundRect(canvas, rc, paint);
	}

	private boolean drawRoundRect(Canvas canvas, RectF dst, Paint paint) {
		int newColor = calDrawingColor(paint);
		if (newColor == 0) {
			return true;
		}
		Paint defSolidPaint = XulRenderContext.getDefSolidPaint();
		defSolidPaint.setColor(newColor);
		if (_radius.length == 2) {
			canvas.drawRoundRect(dst, _radius[0], _radius[1], defSolidPaint);
		} else if (_roundRectShape != null) {
			XulUtils.saveCanvas(canvas);
			canvas.translate(dst.left, dst.top);
			_roundRectShape.resize(dst.width(), dst.height());
			_roundRectShape.draw(canvas, defSolidPaint);
			XulUtils.restoreCanvas(canvas);
		}
		return true;
	}

	@Override
	public boolean draw(Canvas canvas, Rect rc, Rect dst, Paint paint) {
		if (_radius != null) {
			return drawRoundRect(canvas, dst, paint);
		}
		return drawRect(canvas, dst, paint);
	}

	@Override
	public boolean draw(Canvas canvas, Rect rc, RectF dst, Paint paint) {
		if (_radius != null) {
			return drawRoundRect(canvas, dst, paint);
		}
		return drawRect(canvas, dst, paint);
	}

	@Override
	public int getHeight() {
		return _width;
	}

	@Override
	public int getWidth() {
		return _height;
	}
}
