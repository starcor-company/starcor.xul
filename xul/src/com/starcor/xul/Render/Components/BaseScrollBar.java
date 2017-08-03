package com.starcor.xul.Render.Components;

import android.graphics.Rect;
import android.graphics.RectF;

import com.starcor.xul.Graphics.IXulDrawable;
import com.starcor.xul.Graphics.XulDC;

/**
 * Created by hy on 2015/1/8.
 */
public abstract class BaseScrollBar implements IXulDrawable {

	public BaseScrollBar(ScrollBarHelper helper) {
		this._helper = helper;
	}

	public abstract BaseScrollBar update(String desc, String[] descFields);

	public abstract void reset();

	public abstract boolean draw(XulDC dc, Rect rc, int xBase, int yBase);

	public abstract boolean draw(XulDC dc, RectF updateRc, float xBase, float yBase);

	public void recycle() {
	}

	public boolean isVertical() {
		return _helper.isVertical();
	}

	public int getScrollPos() {
		return _helper.getScrollPos();
	}

	public int getContentWidth() {
		return _helper.getContentWidth();
	}

	public int getContentHeight() {
		return _helper.getContentHeight();
	}

	private final ScrollBarHelper _helper;

	public static abstract class ScrollBarHelper {
		public abstract boolean isVertical();

		public abstract int getScrollPos();

		public abstract int getContentWidth();

		public abstract int getContentHeight();
	}
}
