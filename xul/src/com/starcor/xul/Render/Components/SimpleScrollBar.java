package com.starcor.xul.Render.Components;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Render.Drawer.IXulAnimation;
import com.starcor.xul.Render.XulRenderFactory;
import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulUtils;

/**
 * Created by hy on 2015/1/8.
 */
public class SimpleScrollBar extends BaseScrollBar implements IXulAnimation {
	private static final String TYPE = "simple";
	XulViewRender _render;
	float _pos = 1.0f;
	int _delay = 3000;
	float _size = 4.0f;
	int _color = 0xCCFFFFFF;
	float _radius = 2.0f;
	float _shadowSize = 0.0f;
	int _shadowColor = 0xFF000000;
	long _resetTime = -1;
	boolean _isVisible = false;

	int _bkgColor = 0xFF404040;
	int _scrollRangeColor = 0xCC101010;
	float _bkgCornerRadius = 0.0f;
	float _bkgPaddingLeft = 3;
	float _bkgPaddingTop = 5;
	float _bkgPaddingRight = 3;
	float _bkgPaddingBottom = 5;

	String _scrollBarDesc;

	public SimpleScrollBar(ScrollBarHelper helper, XulViewRender render) {
		super(helper);
		_render = render;
	}

	private boolean isDescChanged(String desc) {
		return !desc.equals(_scrollBarDesc);
	}

	private void saveDesc(String desc) {
		_scrollBarDesc = desc;
	}

	@Override
	public void recycle() {
		_resetTime = 0;
		_isVisible = false;
		_delay = 0;
	}

	private void _update(float pos, int delay, float size, float radius, int color, float shadowSize, int shadowColor, int bkgColor, float bkgRadius, int scrollRangeColor, float paddingLeft, float paddingTop, float paddingRight, float paddingBottom) {
		_pos = pos;
		_delay = delay;
		_size = size;
		_color = color;
		_shadowColor = shadowColor;
		_shadowSize = shadowSize;
		_radius = radius;
		_bkgColor = bkgColor;
		_bkgCornerRadius = bkgRadius;
		_scrollRangeColor = scrollRangeColor;
		_bkgPaddingLeft = paddingLeft;
		_bkgPaddingTop = paddingTop;
		_bkgPaddingRight = paddingRight;
		_bkgPaddingBottom = paddingBottom;
	}

	@Override
	public BaseScrollBar update(String desc, String[] descFields) {
		if (!isDescChanged(desc)) {
			return this;
		}

		int readPos = 0;
		String type = descFields[readPos++];
		if (!TYPE.equals(type)) {
			return null;
		}

		float xScalar = (float) _render.getXScalar();
		float yScalar = (float) _render.getYScalar();
		float scalar = isVertical() ? yScalar : xScalar;
		float pos = 1.0f;
		int delay = 3000;
		float size = 4.0f;
		float radius = 2.0f;
		int color = 0xCCFFFFFF;
		float shadowSize = 0;
		int shadowColor = 0;
		int bkgColor = 0;
		int scrollRangeColor = 0;
		float bkgRadius = 0;
		float bkgPaddingTop = 0;
		float bkgPaddingLeft = 0;
		float bkgPaddingBottom = 0;
		float bkgPaddingRight = 0;
		if (descFields.length >= 8) {
			pos = XulUtils.tryParseFloat(readPos >= descFields.length ? "" : descFields[readPos++], pos);
			delay = XulUtils.tryParseInt(readPos >= descFields.length ? "" : descFields[readPos++], delay);
			size = XulUtils.tryParseFloat(readPos >= descFields.length ? "" : descFields[readPos++], size) * scalar;
			radius = XulUtils.tryParseFloat(readPos >= descFields.length ? "" : descFields[readPos++], radius) * scalar;
			color = (int) XulUtils.tryParseHex(readPos >= descFields.length ? "" : descFields[readPos++], color);
			shadowSize = XulUtils.tryParseFloat(readPos >= descFields.length ? "" : descFields[readPos++], shadowSize) * scalar;
			shadowColor = (int) XulUtils.tryParseHex(readPos >= descFields.length ? "" : descFields[readPos++], shadowColor);
			// 背景色
			bkgColor = (int) XulUtils.tryParseHex(readPos >= descFields.length ? "" : descFields[readPos++], bkgColor);
			if (descFields.length == 14 || descFields.length == 11) {
				// 滚动范围颜色
				scrollRangeColor = (int) XulUtils.tryParseHex(readPos >= descFields.length ? "" : descFields[readPos++], scrollRangeColor);
			} else if (descFields.length == 15 || descFields.length == 12) {
				// 滚动范围颜色, 背景圆角
				scrollRangeColor = (int) XulUtils.tryParseHex(readPos >= descFields.length ? "" : descFields[readPos++], scrollRangeColor);
				bkgRadius = XulUtils.tryParseFloat(readPos >= descFields.length ? "" : descFields[readPos++], bkgRadius) * scalar;
			}

			if (descFields.length >= 13) {
				// padding-top, padding-left, padding-right, padding-bottom
				bkgPaddingTop = XulUtils.tryParseFloat(readPos >= descFields.length ? "" : descFields[readPos++], 0);
				bkgPaddingLeft = XulUtils.tryParseFloat(readPos >= descFields.length ? "" : descFields[readPos++], 0);
				bkgPaddingRight = XulUtils.tryParseFloat(readPos >= descFields.length ? "" : descFields[readPos++], 0);
				bkgPaddingBottom = XulUtils.tryParseFloat(readPos >= descFields.length ? "" : descFields[readPos++], 0);
			} else if (descFields.length >= 10) {
				// padding
				bkgPaddingLeft = bkgPaddingTop = bkgPaddingRight = bkgPaddingBottom = XulUtils.tryParseFloat(readPos >= descFields.length ? "" : descFields[readPos++], 0);
			}

			bkgPaddingLeft *= xScalar;
			bkgPaddingRight *= xScalar;
			bkgPaddingTop *= yScalar;
			bkgPaddingBottom *= yScalar;
		}

		_update(pos, delay, size, radius, color, shadowSize, shadowColor, bkgColor, bkgRadius, scrollRangeColor, bkgPaddingLeft, bkgPaddingTop, bkgPaddingRight, bkgPaddingBottom);
		reset();
		saveDesc(desc);
		return this;
	}

	@Override
	public void reset() {
		if (!_isVisible) {
			_isVisible = true;
			if (_delay >= 0) {
				_render.addAnimation(this);
			}
			_render.markDirtyView();
		}
		_resetTime = XulUtils.timestamp();
	}

	@Override
	public boolean draw(XulDC dc, Rect rc, int xBase, int yBase) {
		return draw(dc, xBase, yBase);
	}

	@Override
	public boolean draw(XulDC dc, RectF updateRc, float xBase, float yBase) {
		return draw(dc, xBase, yBase);
	}

	private boolean draw(XulDC dc, float xBase, float yBase) {
		if (!_isVisible) {
			return true;
		}
		Rect padding = _render.getPadding();

		if (isVertical()) {
			int contentSize = getContentHeight();
			int viewHeight = _render.getHeight() - padding.top - padding.bottom;
			float scrollViewHeight = viewHeight - _bkgPaddingTop - _bkgPaddingBottom;
			if (contentSize <= viewHeight) {
				return true;
			}

			float xScalar = (float) _render.getXScalar();
			float size = _size * xScalar;
			float xPos = (_render.getWidth() - padding.left - padding.right - size - (_bkgPaddingLeft + _bkgPaddingRight)) * _pos + _bkgPaddingLeft;

			float scrollBarHeight = (scrollViewHeight * viewHeight) / contentSize;
			scrollBarHeight = Math.max(scrollBarHeight, size * 2);
			float scrollRange = scrollViewHeight - scrollBarHeight;

			float yPos = Math.abs(scrollRange * getScrollPos() / (contentSize - viewHeight)) + _bkgPaddingTop;

			drawScrollBar(padding, dc, xBase, yBase, xScalar, xPos, yPos, size, scrollBarHeight);
		} else {
			int contentSize = getContentWidth();
			int viewWidth = _render.getWidth() - padding.left - padding.right;
			float scrollViewWidth = viewWidth - _bkgPaddingLeft - _bkgPaddingRight;
			if (contentSize <= viewWidth) {
				return true;
			}

			float yScalar = (float) _render.getYScalar();
			float size = _size * yScalar;
			float yPos = (_render.getHeight() - padding.top - padding.bottom - size - (_bkgPaddingTop + _bkgPaddingBottom)) * _pos + _bkgPaddingTop;

			float scrollBarWidth = (scrollViewWidth * viewWidth) / contentSize;
			scrollBarWidth = Math.max(scrollBarWidth, size * 2);
			float scrollRange = scrollViewWidth - scrollBarWidth;

			float xPos = Math.abs(scrollRange * getScrollPos() / (contentSize - viewWidth)) + _bkgPaddingLeft;

			drawScrollBar(padding, dc, xBase, yBase, yScalar, xPos, yPos, scrollBarWidth, size);
		}
		return true;
	}

	private void drawScrollBar(Rect padding, XulDC dc, float xBase, float yBase, float yScalar, float xPos, float yPos, float scrollBarWidth, float scrollBarHeight) {
		float viewLeft = padding.left + _render.getScreenX() + xBase;
		float viewTop = padding.top + _render.getScreenY() + yBase;

		int scrollBarX = XulUtils.roundToInt(viewLeft + xPos);
		int scrollBarY = XulUtils.roundToInt(viewTop + yPos);
		int scrollBarCX = XulUtils.roundToInt(scrollBarWidth);
		int scrollBarCY = XulUtils.roundToInt(scrollBarHeight);
		Paint defSolidPaint;

		if ((_bkgColor & 0xFF000000) != 0) {
			// draw background
			defSolidPaint = XulRenderContext.getDefSolidPaint();
			defSolidPaint.setColor(_bkgColor);
			float bkgX;
			float bkgY;
			float bkgCX;
			float bkgCY;
			if (isVertical()) {
				bkgX = scrollBarX - _bkgPaddingLeft;
				bkgCX = scrollBarCX + _bkgPaddingLeft + _bkgPaddingRight;

				bkgY = viewTop;
				bkgCY = _render.getHeight() - padding.top - padding.bottom;
			} else {
				bkgX = viewLeft;
				bkgCX = _render.getWidth() - padding.left - padding.right;

				bkgY = scrollBarY - _bkgPaddingTop;
				bkgCY = scrollBarCY + _bkgPaddingTop + _bkgPaddingBottom;
			}
			dc.drawRoundRect(bkgX, bkgY, bkgCX, bkgCY, _bkgCornerRadius * yScalar, _bkgCornerRadius * yScalar, defSolidPaint);
		}

		if ((_scrollRangeColor & 0xFF000000) != 0) {
			float bkgX;
			float bkgY;
			float bkgCX;
			float bkgCY;
			defSolidPaint = XulRenderContext.getDefSolidPaint();
			defSolidPaint.setColor(_scrollRangeColor);

			if (isVertical()) {
				bkgX = scrollBarX;
				bkgCX = scrollBarCX;

				bkgY = viewTop + _bkgPaddingTop;
				bkgCY = _render.getHeight() - padding.top - padding.bottom - _bkgPaddingTop - _bkgPaddingBottom;
			} else {
				bkgX = viewLeft + _bkgPaddingLeft;
				bkgCX = _render.getWidth() - padding.left - padding.right - _bkgPaddingLeft - _bkgPaddingRight;

				bkgY = scrollBarY;
				bkgCY = scrollBarCY;
			}
			dc.drawRoundRect(bkgX, bkgY, bkgCX, bkgCY, _radius * yScalar, _radius * yScalar, defSolidPaint);
		}

		if (_shadowSize > 0.5 && (_shadowColor & 0xFF000000) != 0) {
			defSolidPaint = XulRenderContext.getDefSolidShadowPaint();
			defSolidPaint.setShadowLayer(_shadowSize, 0, 0, _shadowColor);
		} else {
			defSolidPaint = XulRenderContext.getDefSolidPaint();
		}
		defSolidPaint.setColor(_color);
		dc.drawRoundRect(scrollBarX, scrollBarY, scrollBarCX, scrollBarCY, _radius * yScalar, _radius * yScalar, defSolidPaint);
	}

	@Override
	public boolean updateAnimation(long timestamp) {
		if (_isVisible && _resetTime > 0 && timestamp - _resetTime > _delay) {
			_isVisible = false;
			_resetTime = -1;
			_render.markDirtyView();
			return false;
		}
		return true;
	}

	public static void register() {
		XulRenderFactory.registerScrollBarFactory(SimpleScrollBar.TYPE, new XulRenderFactory.ScrollBarBuilder() {

			@Override
			public BaseScrollBar create(String desc, String[] descFields, ScrollBarHelper helper, XulViewRender render) {
				SimpleScrollBar simpleScrollBar = new SimpleScrollBar(helper, render);
				return simpleScrollBar.update(desc, descFields);
			}
		});
	}
}
